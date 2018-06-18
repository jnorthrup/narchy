package nars.derive.premise;

import jcog.Util;
import jcog.list.FasterList;
import jcog.tree.perfect.TrieNode;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.step.Branchify;
import nars.term.control.AndCondition;
import nars.term.control.Fork;
import nars.term.control.OpSwitch;
import nars.term.control.PrediTerm;
import nars.unify.op.TaskBeliefMatch;
import nars.unify.op.TermMatch;
import nars.unify.op.UnifyTerm;
import nars.util.term.TermTrie;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Function;

/**
 * high-level interface for compiling premise deriver rules
 */
public enum PremiseDeriverCompiler {
    ;

    private static final IntToFloatFunction throttleFlat =
            (int choice) -> 1f;


    public static PremiseDeriver the(Set<PremiseDeriverProto> r) {
        return the(r, null);
    }

    public static PremiseDeriver the(Set<PremiseDeriverProto> r, @Nullable Function<PrediTerm<Derivation>, PrediTerm<Derivation>> each) {
        assert (!r.isEmpty());

        /** indexed by local (deriver-specific) id */
        int rules = r.size();

        /** map preconditions to conclusions by local conclusion id.
         * the key is an array with a null placeholder at the end to be completed later in this stage
         * */
        final List<Pair<PrediTerm<Derivation>[], DeriveAction>> pairs = new FasterList<>(rules);

        r.forEach(rule -> pairs.add(rule.rule));

        final TermTrie<PrediTerm<Derivation>, DeriveAction> path = new TermTrie<>();


        DeriveAction[] rootBranches = new DeriveAction[rules];

        for (int i = 0; i < rules; i++) {
            Pair<PrediTerm<Derivation>[], DeriveAction> pair = pairs.get(i);

            DeriveAction POST = pair.getTwo();


//            Cause[] causes = Util.map(c -> c.channel, Cause[]::new, Util.map(b -> (Taskify) AndCondition.last(((UnifyTerm.UnifySubtermThenConclude)
//                    AndCondition.last(b)
//            ).eachMatch), Taskify[]::new, branches));


            PrediTerm<Derivation>[] pre = pair.getOne();

            RoaringBitmap idR = new RoaringBitmap();
            idR.add(i);

            assert (pre[pre.length - 1] == null); //null placeholder left for this
            pre[pre.length - 1] = new Branchify(/* branch ID */  idR);

            DeriveAction added = path.put(new FasterList(pre), POST);
            assert (added == null);

            rootBranches[i] = POST;
        }

        assert (!path.isEmpty());


        PrediTerm<Derivation> compiledPaths = PremiseDeriverCompiler.compile(path, each);


        return new PremiseDeriver(rootBranches, compiledPaths);
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

            //TODO
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


    static PrediTerm<Derivation> compile(TermTrie<PrediTerm<Derivation>, DeriveAction> trie, Function<PrediTerm<Derivation>, @Nullable PrediTerm<Derivation>> each) {
        Collection<PrediTerm<Derivation>> bb = compile(trie.root);

        PrediTerm<Derivation> tf = Fork.fork(bb, (f) -> new Fork(f));
        if (each != null)
            tf = tf.transform(each);

        return tf;
    }


    @Nullable
    static Set<PrediTerm<Derivation>> compile(TrieNode<List<PrediTerm<Derivation>>, DeriveAction> node) {


        Set<PrediTerm<Derivation>> bb = new HashSet();


        node.forEach(n -> {

            Set<PrediTerm<Derivation>> branches = compile(n);

            int nStart = n.start();
            int nEnd = n.end();
            PrediTerm<Derivation> branch = PrediTerm.compileAnd(
                    n.seq().subList(nStart, nEnd),
                    //n.seq().stream().skip(nStart).limit(nEnd - nStart).collect(Collectors.toList()),
                    branches != null /* && !branches.isEmpty() */ ?
                            factorFork(branches, (f) -> new Fork(f))
                            : null
            );

            if (branch != null)
                bb.add(branch);
        });

        if (bb.isEmpty())
            return null;
        else {
            return compileSwitch(bb, 2);
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
                Branchify bb = new Branchify(r);
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
            /** TermMatch as field of TaskBeliefMatch */
            Map<TermMatch.Is, PrediTerm<Derivation>> cases = new UnifiedMap(8);
            Set<PrediTerm<Derivation>> removed = new UnifiedSet(8);
            bb.forEach(p -> {
                if (p instanceof AndCondition) {
                    AndCondition ac = (AndCondition) p;
                    ac.forEach(x -> {
                        if (x instanceof TaskBeliefMatch) {
                            TaskBeliefMatch tb = (TaskBeliefMatch) x;
                            TermMatch m = tb.match;
                            if (m instanceof TermMatch.Is) {
                                if (tb.trueOrFalse && tb.task == taskOrBelief && tb.belief == !taskOrBelief) {
                                    PrediTerm acw = ac.without(tb);
                                    if (null == cases.putIfAbsent((TermMatch.Is)m, acw)) {
                                        removed.add(p);
                                    }
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
                    int cs = c.struct;
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






























































































































































































































































































