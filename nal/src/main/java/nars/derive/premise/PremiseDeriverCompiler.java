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
import nars.term.Term;
import nars.term.control.AndCondition;
import nars.term.control.Fork;
import nars.term.control.OpSwitch;
import nars.term.control.PrediTerm;
import nars.unify.constraint.MatchConstraint;
import nars.unify.op.TaskBeliefOp;
import nars.unify.op.UnifyTerm;
import nars.util.term.TermTrie;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.NotNull;
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

    public static PremiseDeriver the(Set<PremiseDeriverProto> r, @Nullable Function<PrediTerm<Derivation>, PrediTerm<Derivation>> each) {
        assert (!r.isEmpty());


        final TermTrie<PrediTerm<Derivation>, PrediTerm<Derivation>> path = new TermTrie<>();

        final FasterList<ValueFork> postChoices = new FasterList(128);

        Map<Set<PrediTerm<PreDerivation>>, RoaringBitmap> post = new HashMap<>(r.size());
        List<PrediTerm<Derivation>> conclusions = new FasterList<>(r.size() * 2);

        ObjectIntHashMap<Term> preconditionCount = new ObjectIntHashMap<>(256);

        r.forEach(rule -> {

            assert (rule.POST != null) : "null POSTconditions:" + rule;


            int id = conclusions.size();

            Pair<Set<PrediTerm<PreDerivation>>, PrediTerm<Derivation>> c = rule.build(rule.POST);

            c.getOne().forEach(k -> preconditionCount.addToValue(k, 1));

            conclusions.add(c.getTwo());

            post.computeIfAbsent(c.getOne(), x -> new RoaringBitmap()).add(id);

        });

        Comparator<PrediTerm> sortPrecondition = PrediTerm.sort(preconditionCount::get);

        post.forEach((k, v) -> {

            FasterList<PrediTerm<Derivation>> pre = new FasterList(k, PrediTerm[]::new, +1).sortThis(sortPrecondition);

            PrediTerm<Derivation>[] branches = StreamSupport.stream(v.spliterator(), false)
                    .map(i -> compileBranch(conclusions.get(i))).toArray(PrediTerm[]::new);
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


            {

                pre.add(new Branchify(
                        /* branch ID */ postChoices.size(), v));

                PrediTerm<Derivation> prev = path.put(pre, f);

                postChoices.add(f);

                assert (prev == null);
            }


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
        SortedSet<PrediTerm<Derivation>> bb = compile(trie.root);

        PrediTerm<Derivation> tf = Fork.fork(bb.toArray(PrediTerm.EmptyPrediTermArray), Fork::new);
        if (each != null)
            tf = tf.transform(each);

        return tf;
    }


    @Nullable
    static SortedSet<PrediTerm<Derivation>> compile(TrieNode<List<PrediTerm<Derivation>>, PrediTerm<Derivation>> node) {


        SortedSet<PrediTerm<Derivation>> bb = new TreeSet();


        node.forEach(n -> {

            SortedSet<PrediTerm<Derivation>> branches = compile(n);

            int nStart = n.start();
            int nEnd = n.end();
            PrediTerm<Derivation> branch = PrediTerm.compileAnd(
                    n.seq().stream().skip(nStart).limit(nEnd - nStart),
                    branches != null /* && !branches.isEmpty() */ ?
                            factorFork(branches, Fork::new)
                            : null
            );

            if (branch != null)
                bb.add(branch);
        });

        if (bb.isEmpty())
            return null;
        else
            return compileSwitch(bb);
    }

    private static <X> PrediTerm<X> factorFork(SortedSet<PrediTerm<X>> _x, Function<PrediTerm<X>[], PrediTerm<X>> builder) {

        int n = _x.size();
        if (n == 0)
            return null;

        if (n == 1)
            return _x.first();

        FasterList<PrediTerm<X>> x = new FasterList<>(_x);

        MutableMap<PrediTerm, SubCond> conds = new UnifiedMap(x.size());
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

        SubCond fx = conds.maxBy(SubCond::costIfBranch);
        if (fx.size() < 2) {

        } else {
            SortedSet<PrediTerm> bundle = new TreeSet();
            int i = 0;
            Iterator<PrediTerm<X>> xx = x.iterator();
            while (xx.hasNext()) {
                PrediTerm<X> px = xx.next();
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
                x.add(AndCondition.the(fx.p, Fork.fork(bundle.toArray(new PrediTerm[bundle.size()]), builder)));

                if (x.size() == 1)
                    return x.get(0);
            }
        }


        return Fork.fork(x.toSortedSet().toArray(PrediTerm.EmptyPrediTermArray), builder);
    }

    protected static SortedSet<PrediTerm<Derivation>> compileSwitch(SortedSet<PrediTerm<Derivation>> branches) {


        branches = factorSubOpToSwitch(branches, true, 2);
        branches = factorSubOpToSwitch(branches, false, 2);

        return branches;
    }

    @NotNull
    private static <D extends PreDerivation> SortedSet<PrediTerm<D>> factorSubOpToSwitch(SortedSet<PrediTerm<D>> bb, boolean taskOrBelief, int minToCreateSwitch) {
        if (!bb.isEmpty()) {
            Map<TaskBeliefOp, PrediTerm<D>> cases = new UnifiedMap(8);
            Set<PrediTerm<D>> removed = new UnifiedSet(8);
            bb.removeIf(p -> {
                if (p instanceof AndCondition) {
                    AndCondition ac = (AndCondition) p;
                    return ac.OR(x -> {
                        if (x instanceof TaskBeliefOp) {
                            TaskBeliefOp so = (TaskBeliefOp) x;
                            if (so.isOrIsNot && so.task == taskOrBelief && so.belief == !taskOrBelief) {
                                PrediTerm acw = ac.without(so);
                                if (null == cases.putIfAbsent(so, acw)) {
                                    removed.add(p);
                                    return true;
                                }
                            }
                        }
                        return false;
                    });

                }
                return false;
            });


            int numCases = cases.size();
            if (numCases >= minToCreateSwitch) {
                if (numCases != removed.size()) {
                    throw new RuntimeException("switch fault");
                }

                EnumMap<Op, PrediTerm<D>> caseMap = new EnumMap(Op.class);
                cases.forEach((c, p) -> {
                    int cs = c.structure;
                    Op[] opVals = Op.values();
                    for (int i = 0; i < opVals.length; i++) {
                        if ((cs & (1 << i)) > 0)
                            caseMap.put(opVals[i], p);
                    }

                });
                bb.add(new OpSwitch<>(taskOrBelief, caseMap));
            } else {
                bb.addAll(removed);
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
            if (pc == Float.POSITIVE_INFINITY)
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






























































































































































































































































































