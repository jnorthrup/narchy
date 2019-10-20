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
import java.util.function.UnaryOperator;

/**
 * high-level interface for compiling premise deriver rules
 */
public enum PremiseRuleCompiler {
    ;

    public static DeriverProgram the(Collection<PremiseRule> rr, NAR nar, UnaryOperator<How> functionTransform) {
        return compile(rr,
            new PreDeriver.CentralMemoizer()
            //PreDeriver.DIRECT_DERIVATION_RUNNER
            //DeriverPlanner.ConceptMetaMemoizer
            , nar,
            functionTransform
        );
    }

    public static DeriverProgram compile(Collection<PremiseRule> rr, PreDeriver preDeriver, NAR n, UnaryOperator<How> actionTransform) {

        /** indexed by local (deriver-specific) id */
        int s = rr.size();
        assert(s > 0);

        How[] roots = new How[s];
        TermPerfectTrie<PREDICATE<PreDerivation>, How> paths = new TermPerfectTrie<PREDICATE<PreDerivation>, How>();

        Map<String,RuleCause> tags = new HashMap();

        short i = (short) 0;
        for (PremiseRule r : rr) {

            How added = paths.put(
                r.conditions(i),
                roots[(int) i++] = actionTransform.apply(r.action(n, tags))
            );

            assert (added == null);
        }

        assert (!paths.isEmpty());

        return new DeriverProgram(PremiseRuleCompiler.compile(paths), roots, preDeriver, n);
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

        //return compileSwitch(bb, 2);
        return bb.isEmpty() ? null : bb.toList();
    }

    public static PREDICATE<PreDerivation> factorFork(Collection<PREDICATE<PreDerivation>> _x, Function<List<PREDICATE<PreDerivation>>, PREDICATE<PreDerivation>> builder) {

        int n = _x.size();
        if (n == 0)
            return null;

        if (n == 1)
            return _x.iterator().next();

        ArrayHashSet<PREDICATE<PreDerivation>> x = new ArrayHashSet<PREDICATE<PreDerivation>>();
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
            boolean seen = false;
            SubCond best = null;
            Comparator<SubCond> comparator = Comparator.comparingDouble(SubCond::costIfBranch);
            for (SubCond subCond : conds.values()) {
                if (!seen || comparator.compare(subCond, best) > 0) {
                    seen = true;
                    best = subCond;
                }
            }
            SubCond fx = (seen ? Optional.of(best) : Optional.<SubCond>empty()).get();
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
                        r.add((int) cc);
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
                SubCond e1 = e;
                if (e1 == null) {
                    e1 = new SubCond(xx, branch);
                } else {
                    e1.bump(branch);
                }
                return e1;
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
            return s > 1 ? (float) s * p.cost() : Float.NEGATIVE_INFINITY;
        }
    }


}






























































































































































































































































































