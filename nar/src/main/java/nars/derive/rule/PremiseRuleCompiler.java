package nars.derive.rule;

import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.tree.perfect.TrieNode;
import nars.NAR;
import nars.derive.PreDerivation;
import nars.derive.PreDeriver;
import nars.derive.action.How;
import nars.derive.util.Forkable;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.control.AND;
import nars.term.control.FORK;
import nars.term.control.PREDICATE;
import nars.term.util.map.TermPerfectTrie;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.*;
import java.util.function.Function;

/**
 * high-level interface for compiling premise deriver rules
 */
public enum PremiseRuleCompiler {
    ;

    public static DeriverProgram the(Collection<PremiseRule> rr, NAR nar, Function<How, How> functionTransform) {
        return compile(rr,
            new PreDeriver.CentralMemoizer()
            //PreDeriver.DIRECT_DERIVATION_RUNNER
            //DeriverPlanner.ConceptMetaMemoizer
            , nar,
            functionTransform
        );
    }

    public static DeriverProgram compile(Collection<PremiseRule> rr, PreDeriver preDeriver, NAR nar, Function<How, How> functionTransform) {

        /** indexed by local (deriver-specific) id */
        int n = rr.size();
        short i = 0;
        assert(n > 0);

        How[] roots = new How[n];
        final TermPerfectTrie<PREDICATE<PreDerivation>, How> paths = new TermPerfectTrie<>();

        for (PremiseRule r : rr) {

            PREDICATE<PreDerivation>[] condition = r.condition;

            FasterList<PREDICATE<PreDerivation>> pre = new FasterList<>(condition.length + 1);
            pre.addAll(condition);

            How conc = r.action.apply(nar);

            pre.add(new Forkable(/* branch ID */  i));

            How added = paths.put(pre,
                roots[i] = functionTransform.apply(conc)
            );
            assert (added == null);

            i++;
        }

        assert (!paths.isEmpty());

        return new DeriverProgram(PremiseRuleCompiler.compile(paths), roots, preDeriver, nar);
    }




    static PREDICATE<PreDerivation> compile(TermPerfectTrie<PREDICATE<PreDerivation>, How> trie) {
        return FORK.fork(compile(trie.root), FORK::new);
    }


    static @Nullable List<PREDICATE<PreDerivation>> compile(TrieNode<List<PREDICATE<PreDerivation>>, How> node) {


        UnifiedSet<PREDICATE<PreDerivation>> bb = new UnifiedSet();


        node.forEach(n -> {

            List<PREDICATE<PreDerivation>> branches = compile(n);

            PREDICATE<PreDerivation> branch = PREDICATE.andFlat(
                    n.seq().subList(n.start(), n.end()),
                    //n.seq().stream().skip(nStart).limit(nEnd - nStart).collect(Collectors.toList()),
                    branches != null ? factorFork(branches, FORK::new) : null
            );

            if (branch != null)
                bb.add(branch);
        });

        if (bb.isEmpty())
            return null;
        else {
            //return compileSwitch(bb, 2);
            return bb.toList();
        }
    }

    public static PREDICATE<PreDerivation> factorFork(Collection<PREDICATE<PreDerivation>> _x, Function<List<PREDICATE<PreDerivation>>, PREDICATE<PreDerivation>> builder) {

        int n = _x.size();
        if (n == 0)
            return null;

        if (n == 1)
            return _x.iterator().next();

        ArrayHashSet<PREDICATE<PreDerivation>> x = new ArrayHashSet<>();
        x.addAll(_x);
        FasterList<PREDICATE<PreDerivation>> X = x.list;

        Map<PREDICATE, SubCond> conds = new HashMap(x.size());
        for (int b = 0; b < n; b++) {
            PREDICATE  p = X.get(b);
            if (p instanceof AND) {
                int bb = b;
                ((AND) p).subStream().forEach((xx)-> SubCond.bumpCond(conds, (PREDICATE)xx, bb));
            } else if (p instanceof FORK) {

            } else {
                SubCond.bumpCond(conds, p, b);
            }
            b++;
        }

        if (!conds.isEmpty()) {
            SubCond fx = conds.values().stream().max(Comparator.comparingDouble(SubCond::costIfBranch)).get();
            if (fx.size() < 2) {

            } else {
                List<PREDICATE<PreDerivation>> bundle = null;
                int i = 0;
                Iterator<PREDICATE<PreDerivation>> xx = x.iterator();
                while (xx.hasNext()) {
                    PREDICATE px = xx.next();
                    if (fx.branches.contains(i)) {
                        xx.remove();

                        if (px instanceof AND) {
                            Subterms pxSub = px.subterms();
                            if (pxSub.contains(fx.p)) {
                                px = AND.the(pxSub.removing(fx.p));
                            }
                            if (bundle == null) bundle = new FasterList();
                            bundle.add(px);
                        } else {

                        }
                    }
                    i++;
                }
                if (bundle!=null) {
                    assert (bundle.size() > 1);
                    x.add(AND.the(new Term[] { fx.p, FORK.fork(bundle, (Function)builder  )}));

                    if (x.size() == 1)
                        return x.iterator().next();
                }
            }

        }

        //HACK
        //force merge

        if (x.size() > 1) {
            RoaringBitmap r = new RoaringBitmap();
            x.removeIf(zz -> {
                if (zz instanceof Forkable) {
                    for (short cc : ((Forkable) zz).can)
                        r.add(cc);
                    return true;
                }
                return false;
            });
            if (!r.isEmpty()) {
                Forkable bb = new Forkable(r);
                if (x.isEmpty()) {
                    return bb;
                } else {
                    x.add(bb);
                }
            }
        }

        return FORK.fork(X, builder);
    }

//    private static Set<PREDICATE<Derivation>> compileSwitch(Set<PREDICATE<Derivation>> branches, int minCases) {
//
//        if (branches.size() < minCases)
//            return branches; //dont bother
//
////        branches = factorSubOpToSwitch(branches, true, minCases);
////        branches = factorSubOpToSwitch(branches, false, minCases);
//
//        return branches;
//    }

//    //broken temporarily
//    private static Set<PREDICATE<Derivation>> factorSubOpToSwitch(Set<PREDICATE<Derivation>> bb, boolean taskOrBelief, int minToCreateSwitch) {
//        if (!bb.isEmpty()) {
//            /** TermMatch as field of TaskBeliefMatch */
//            Map<TermMatch.Is, PREDICATE<Derivation>> cases = new UnifiedMap(8);
//            Set<PREDICATE<Derivation>> removed = new UnifiedSet(8);
//            bb.forEach(p -> {
//                if (p instanceof AND) {
//                    AND ac = (AND) p;
//                    ac.forEach(x -> {
//                        if (x instanceof TermMatchPred) {
//                            TermMatchPred tb = (TermMatchPred) x;
//                            TermMatch m = tb.match;
//                            if (m instanceof TermMatch.Is) {
//                                if ((taskOrBelief && tb.resolve==TaskTerm) || (!taskOrBelief && tb.resolve==BeliefTerm)) {
//                                    PREDICATE acw = ac.without(tb);
//                                    if (null == cases.putIfAbsent((TermMatch.Is)m, acw)) {
//                                        removed.addAt(p);
//                                    }
//                                }
//                            }
//                        }
//                    });
//
//                }
//            });
//
//
//            int numCases = cases.size();
//            if (numCases >= minToCreateSwitch) {
//                if (numCases != removed.size()) {
//                    throw new RuntimeException("switch fault");
//                }
//
//                EnumMap<Op, PREDICATE<Derivation>> caseMap = new EnumMap(Op.class);
//                cases.forEach((c, p) -> {
//                    int cs = c.struct;
//                    Op[] opVals = Op.values();
//                    for (int i = 0; i < opVals.length; i++) {
//                        if ((cs & (1 << i)) != 0)
//                            caseMap.put(opVals[i], p);
//                    }
//
//                });
//                bb.removeAll(removed);
//                bb.addAt(new SWITCH<>(taskOrBelief, caseMap));
//            }
//        }
//
//        return bb;
//    }

    private static class SubCond {
        final PREDICATE p;
        final RoaringBitmap branches = new RoaringBitmap();

        private SubCond(PREDICATE p, int branch) {
            this.p = p;
            branches.add(branch);
        }

        static <X> void bumpCond(Map<PREDICATE, SubCond> conds, PREDICATE p, int branch) {
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

        int size() {
            return branches.getCardinality();
        }


        float costIfBranch() {
            int s = size();
            if (s > 1)
                return s * p.cost();
            else
                return Float.NEGATIVE_INFINITY;
        }
    }


}






























































































































































































































































































