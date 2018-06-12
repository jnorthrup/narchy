package nars.derive.premise;

import jcog.Util;
import jcog.list.FasterList;
import jcog.tree.perfect.TrieNode;
import nars.Op;
import nars.control.Cause;
import nars.derive.Derivation;
import nars.derive.control.ValueFork;
import nars.derive.step.Branchify;
import nars.derive.step.Taskify;
import nars.term.control.AndCondition;
import nars.term.control.Fork;
import nars.term.control.OpSwitch;
import nars.term.control.PrediTerm;
import nars.unify.constraint.MatchConstraint;
import nars.unify.op.TaskBeliefIs;
import nars.unify.op.UnifyTerm;
import nars.util.term.TermTrie;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * high-level interface for compiling premise deriver rules
 */
public enum PremiseDeriverCompiler {
    ;

    private static final IntToFloatFunction throttleFlat =
            (int choice) -> 1f;


    private static final float[] ONE_CHOICE = new float[]{Float.NaN};

    public static PremiseDeriver the(Set<PremiseDeriverProto> r) {
        return the(r, null);
    }

    public static PremiseDeriver the(Set<PremiseDeriverProto> r, @Nullable Function<PrediTerm<Derivation>, PrediTerm<Derivation>> each) {
        assert (!r.isEmpty());

        /** indexed by local (deriver-specific) id */
        int rules = r.size();

        List<PrediTerm<Derivation>> conclusions = new FasterList<>(rules);

        /** map preconditions to conclusions by local conclusion id.
         * the key is an array with a null placeholder at the end to be completed later in this stage
         * */
        final List<Pair<PrediTerm<Derivation>[], RoaringBitmap>> pairs = new FasterList<>(rules);

        ///** local conclusion id requiring this precondition */
        //Map<PrediTerm<Derivation>, RoaringBitmap> reach = new HashMap<>(r.size());


        r.forEach(rule -> {

            assert (rule.POST != null) : "null POSTconditions:" + rule;

            Pair<PrediTerm<Derivation>[], PrediTerm<Derivation>> c = rule.build(rule.POST);

            int id = conclusions.size();
            boolean unique = conclusions.add(compileBranch(c.getTwo()));
            assert(unique);

//            c.getOne().forEach(k -> {
//                reach.computeIfAbsent(k, x -> new RoaringBitmap()).add(id);
//            });

            RoaringBitmap b = new RoaringBitmap();
            b.add(id);
            pairs.add(Tuples.pair(c.getOne(), b));
        });

        final TermTrie<PrediTerm<Derivation>, PrediTerm<Derivation>> path = new TermTrie<>();
        final FasterList<ValueFork> postChoices = new FasterList(rules);


        pairs.forEach((pair) -> {


            RoaringBitmap v = pair.getTwo();
            PrediTerm<Derivation>[] branches = StreamSupport.stream(v.spliterator(), false)
                    .map(conclusions::get).toArray(PrediTerm[]::new);
            assert (branches.length > 0);


            Cause[] causes = Util.map(c -> c.channel, Cause[]::new, Util.map(b -> (Taskify) AndCondition.last(((UnifyTerm.UnifySubtermThenConclude)
                    AndCondition.last(b)
            ).eachMatch), Taskify[]::new, branches));


            IntToFloatFunction throttle =
                    i -> causes[i].amp();


            ValueFork f;
            if (branches.length > 1) {
                f = new ValueFork(branches, causes,


                        d -> Util.map(causes.length, throttle, new float[causes.length]),


                        (d, choice) -> {
                            branches[choice].test(d);
                        }
                );
            } else {

                f = new ValueFork(branches, causes, d -> ONE_CHOICE, (d, theOneChoice) -> {
                    branches[0].test(d);
                });
            }


            PrediTerm<Derivation>[] pre = pair.getOne();
            assert (pre[pre.length - 1] == null); //null placeholder left for this
            pre[pre.length - 1] = new Branchify(/* branch ID */ postChoices.size(), v);

            PrediTerm added = path.put(new FasterList(pre), f);
            assert(added==null);

            postChoices.add(f);

        });

        assert (!path.isEmpty());


        PrediTerm<Derivation> compiledPaths = PremiseDeriverCompiler.compile(path, each);

        ValueFork[] rootBranches = postChoices.toArrayRecycled(ValueFork[]::new);


        IntToFloatFunction branchThrottle = branch ->
                Util.sum(

                        Cause::amp,
                        rootBranches[branch].causes
                );

        return new PremiseDeriver(compiledPaths,
                new ValueFork(rootBranches,
                        Stream.of(rootBranches).flatMap(b -> Stream.of(b.causes)).toArray(Cause[]::new),


                        d -> {
                            short[] will = d.will;
                            int n = will.length;
                            if (n == 1)
                                return ONE_CHOICE;
                            else
                                return Util.map(n, choice -> branchThrottle.valueOf(will[choice]), new float[n]);
                        },


                        (d, choice) ->
                                rootBranches[d.will[choice]].test(d)

                ));
    }


    static PrediTerm compileBranch(PrediTerm<Derivation> branch) {
        return branch instanceof AndCondition ?
                ((AndCondition) branch).transform(y -> y instanceof AndCondition ?
                                MatchConstraint.combineConstraints((AndCondition) y)
                                :
                                y
                        , null)
                :
                branch;
    }

    public static void print(Object p, PrintStream out, int indent) {

        TermTrie.indent(indent);

        if (p instanceof PremiseDeriver) {

            PremiseDeriver r = (PremiseDeriver) p;
            r.print(out, indent);

        } else if (p instanceof UnifyTerm.UnifySubtermThenConclude) {
            UnifyTerm.UnifySubtermThenConclude u = (UnifyTerm.UnifySubtermThenConclude) p;
            out.println("unify(" + UnifyTerm.label(u.subterm) + "," + u.pattern + ") {");
            print(u.eachMatch, out, indent + 2);
            TermTrie.indent(indent);
            out.println("}");
        } else if (p instanceof AndCondition) {
            out.println("and {");
            AndCondition ac = (AndCondition) p;
            for (PrediTerm b : ac.cond) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent);
            out.println("}");
        } /*else if (p instanceof Try) {
            out.println("eval {");
            Try ac = (Try) p;
            int i = 0;
            for (PrediTerm b : ac.branches) {
                TermTrie.indent(indent + 2);
                out.println(i + ":");
                print(b, out, indent + 4);
                i++;
            }
            TermTrie.indent(indent);
            out.println("}");
        } */ else if (p instanceof Fork) {
            out.println(Util.className(p) + " {");
            Fork ac = (Fork) p;
            for (PrediTerm b : ac.branch) {
                print(b, out, indent + 2);
            }
            TermTrie.indent(indent);
            out.println("}");

        } else if (p instanceof OpSwitch) {
            OpSwitch sw = (OpSwitch) p;
            out.println("switch(op(" + (sw.taskOrBelief ? "task" : "belief") + ")) {");
            int i = -1;
            for (PrediTerm b : sw.swtch) {
                i++;
                if (b == null) continue;

                TermTrie.indent(indent + 2);
                out.println('"' + Op.values()[i].toString() + "\": {");
                print(b, out, indent + 4);
                TermTrie.indent(indent + 2);
                out.println("}");

            }
            TermTrie.indent(indent);
            out.println("}");
        } else {
            out.print( /*Util.className(p) + ": " +*/ p);
            out.println();
        }


    }


    public static void print(PrediTerm<Derivation> d) {
        print(d, System.out);
    }

    public static void print(Object d, PrintStream out) {
        PremiseDeriverCompiler.print(d, out, 0);


    }


    static PrediTerm<Derivation> compile(TermTrie<PrediTerm<Derivation>, PrediTerm<Derivation>> trie, Function<PrediTerm<Derivation>, @Nullable PrediTerm<Derivation>> each) {
        Collection<PrediTerm<Derivation>> bb = compile(trie.root);

        PrediTerm<Derivation> tf = Fork.fork(bb, (f) -> new Fork(f));
        if (each != null)
            tf = tf.transform(each);

        return tf;
    }


    @Nullable
    static Set<PrediTerm<Derivation>> compile(TrieNode<List<PrediTerm<Derivation>>, PrediTerm<Derivation>> node) {


        Set<PrediTerm<Derivation>> bb = new HashSet();


        node.forEach(n -> {

            Set<PrediTerm<Derivation>> branches = compile(n);

            int nStart = n.start();
            int nEnd = n.end();
            PrediTerm<Derivation> branch = PrediTerm.compileAnd(
                    n.seq().subList(nStart, nEnd),
                    //n.seq().stream().skip(nStart).limit(nEnd - nStart).collect(Collectors.toList()),
                    branches != null /* && !branches.isEmpty() */ ?
                            factorFork(branches, (f)->new Fork(f))
                            : null
            );

            if (branch != null)
                bb.add(branch);
        });

        if (bb.isEmpty())
            return null;
        else {
            return compileSwitch(bb, 3);
            //return bb;
        }
    }

    private static PrediTerm<Derivation> factorFork(Set<PrediTerm<Derivation>> _x, Function<PrediTerm[], PrediTerm<Derivation>> builder) {

        int n = _x.size();
        if (n == 0)
            return null;

        if (n == 1)
            return _x.iterator().next();

        FasterList<PrediTerm<Derivation>> x = new FasterList<>(_x);

        Map<PrediTerm, SubCond> conds = new HashMap(x.size());
        for (int b = 0, xSize = n; b < xSize; b++) {
            PrediTerm p = x.get(b);
            if (p instanceof AndCondition) {
                for (PrediTerm xx : ((AndCondition<PrediTerm>) p).cond)
                    SubCond.bumpCond(conds, xx, b);
            } else if (p instanceof Fork) {

            } else {
                SubCond.bumpCond(conds, p, b);
            }
        }

        if (!conds.isEmpty()) {
            SubCond fx = conds.values().stream().max(Comparator.comparingDouble(zz -> zz.costIfBranch())).get();
            if (fx.size() < 2) {

            } else {
                Collection<PrediTerm<Derivation>> bundle = new HashSet();
                int i = 0;
                Iterator<PrediTerm<Derivation>> xx = x.iterator();
                while (xx.hasNext()) {
                    PrediTerm px = xx.next();
                    if (fx.branches.contains(i)) {
                        xx.remove();

                        if (px instanceof AndCondition) {
                            px = AndCondition.the(ArrayUtils.removeAllOccurences(((AndCondition) px).cond, fx.p));
                            bundle.add(px);
                        } else {

                        }
                    }
                    i++;
                }
                if (!bundle.isEmpty()) {
                    assert (bundle.size() > 1);
                    x.add(AndCondition.the(fx.p, Fork.fork(bundle, builder)));

                    if (x.size() == 1)
                        return x.get(0);
                }
            }

        }

        //HACK
        //force merge

        if (x.size() > 1) {
            RoaringBitmap r = new RoaringBitmap();
            x.removeIf(zz -> {
                if (zz instanceof Branchify) {
                    r.or(((Branchify) zz).can);
                    return true;
                }
                return false;
            });
            if (!r.isEmpty()) {
                Branchify bb = new Branchify(-1, r);
                if (x.isEmpty()) {
                    return bb;
                } else {
                    x.add(bb);
                }
            }
        }

        return Fork.fork(x, builder);
    }

    protected static Set<PrediTerm<Derivation>> compileSwitch(Set<PrediTerm<Derivation>> branches, int minCases) {

        if (branches.size() < minCases)
            return branches; //dont bother

        branches = factorSubOpToSwitch(branches, true, minCases);
        branches = factorSubOpToSwitch(branches, false, minCases);

        return branches;
    }

    private static Set<PrediTerm<Derivation>> factorSubOpToSwitch(Set<PrediTerm<Derivation>> bb, boolean taskOrBelief, int minToCreateSwitch) {
        if (!bb.isEmpty()) {
            Map<TaskBeliefIs, PrediTerm<Derivation>> cases = new UnifiedMap(8);
            Set<PrediTerm<Derivation>> removed = new UnifiedSet(8);
            bb.forEach(p -> {
                if (p instanceof AndCondition) {
                    AndCondition ac = (AndCondition) p;
                    ac.forEach(x -> {
                        if (x instanceof TaskBeliefIs) {
                            TaskBeliefIs so = (TaskBeliefIs) x;
                            if (so.isOrIsNot && so.task == taskOrBelief && so.belief == !taskOrBelief) {
                                PrediTerm acw = ac.without(so);
                                if (null == cases.putIfAbsent(so, acw)) {
                                    removed.add(p);
                                }
                            }
                        }
                    });

                }
            });


            int numCases = cases.size();
            if (numCases >= minToCreateSwitch) {
                if (numCases != removed.size()) {
                    throw new RuntimeException("switch fault");
                }

                EnumMap<Op, PrediTerm<Derivation>> caseMap = new EnumMap(Op.class);
                cases.forEach((c, p) -> {
                    int cs = c.structure;
                    Op[] opVals = Op.values();
                    for (int i = 0; i < opVals.length; i++) {
                        if ((cs & (1 << i)) != 0)
                            caseMap.put(opVals[i], p);
                    }

                });
                bb.removeAll(removed);
                bb.add(new OpSwitch<>(taskOrBelief, caseMap));
            }
        }

        return bb;
    }

    private static class SubCond {
        public final PrediTerm p;
        final RoaringBitmap branches = new RoaringBitmap();

        private SubCond(PrediTerm p, int branch) {
            this.p = p;
            branches.add(branch);
        }

        static void bumpCond(Map<PrediTerm, SubCond> conds, PrediTerm p, int branch) {
            float pc = p.cost();
            if (!Float.isFinite(pc))
                return;

            conds.compute(p, (xx, e) -> {
                if (e == null) {
                    e = new SubCond(xx, branch);
                } else {
                    e.bump(branch);
                }
                return e;
            });
        }

        void bump(int branch) {
            branches.add(branch);
        }

        @Override
        public String toString() {
            return p + " x " + branches;
        }

        public int size() {
            return branches.getCardinality();
        }


        public float costIfBranch() {
            int s = size();
            if (s > 1)
                return s * p.cost();
            else
                return Float.NEGATIVE_INFINITY;
        }
    }


}






























































































































































































































































































