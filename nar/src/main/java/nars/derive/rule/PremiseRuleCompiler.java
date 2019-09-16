package nars.derive.rule;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.tree.perfect.TrieNode;
import nars.NAR;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.DeriveAction;
import nars.derive.PreDeriver;
import nars.derive.op.Branchify;
import nars.derive.op.PremiseUnify;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.control.AND;
import nars.term.control.FORK;
import nars.term.control.PREDICATE;
import nars.term.control.SWITCH;
import nars.term.util.map.TermPerfectTrie;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Function;

/**
 * high-level interface for compiling premise deriver rules
 */
public enum PremiseRuleCompiler {
    ;

//    public static DeriverRules the(Set<PremiseRuleProto> r, NAR nar) {
//        return the.apply(r);
//    }

//    private static final Function<Set<PremiseRuleProto>, DeriverRules> the =
//            //Memoizers.the.memoize(PremiseDeriverCompiler.class.getSimpleName(), 64, PremiseDeriverCompiler::_the);
//            CaffeineMemoize.build(PremiseRuleCompiler::_the, 64, false);

    public static DeriverRules the(PremiseRuleSet rr) {
        return the(rr.rules, rr.nar);
    }

    public static DeriverRules the(Collection<PremiseRule> rr, NAR nar) {
        return _the(rr,
            new PreDeriver.CentralMemoizer()
            //PreDeriver.DIRECT_DERIVATION_RUNNER
            //DeriverPlanner.ConceptMetaMemoizer
            , nar
        );
    }

    private static DeriverRules _the(Collection<PremiseRule> rr, PreDeriver preDeriver, NAR nar) {

        /** indexed by local (deriver-specific) id */
        int n = rr.size();
        short i = 0;
        assert(n > 0);

        DeriveAction[] rootBranches = new DeriveAction[n];
        final TermPerfectTrie<PREDICATE<Derivation>, DeriveAction> path = new TermPerfectTrie<>();

        for (PremiseRule r : rr) {

            PREDICATE<Derivation>[] condition = r.condition;

            FasterList<PREDICATE<Derivation>> pre = new FasterList<>(condition.length + 1);
            pre.addAll(condition);

            pre.add(new Branchify(/* branch ID */  i));

            DeriveAction added = path.put(pre,
                rootBranches[i] = r.action.apply(nar)
            );
            assert (added == null);

            i++;
        }

        assert (!path.isEmpty());

        return new DeriverRules(PremiseRuleCompiler.compile(path), rootBranches, preDeriver);
    }


    public static void print(Object p, PrintStream out, int indent) {

        TermPerfectTrie.indent(indent);

        if (p instanceof DeriverRules) {

            DeriverRules r = (DeriverRules) p;
            r.print(out, indent);

        } else if (p instanceof PremiseUnify) {
            out.println(((PremiseUnify)p).term());
            print(((PremiseUnify)p).taskify, out, indent + 2);
            TermPerfectTrie.indent(indent);
            out.println("}");
        } else if (p instanceof AND) {
            out.println("and {");
            AND ac = (AND) p;
            ac.subStream().forEach(b->
                print(b, out, indent + 2)
            );
            TermPerfectTrie.indent(indent);
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
        } */ else if (p instanceof FORK) {

            //TODO
            out.println(Util.className(p) + " {");
            FORK ac = (FORK) p;
            for (PREDICATE b : ac.branch) {
                print(b, out, indent + 2);
            }
            TermPerfectTrie.indent(indent);
            out.println("}");

        } else if (p instanceof SWITCH) {
            SWITCH sw = (SWITCH) p;
            out.println("switch(op(" + (sw.taskOrBelief ? "task" : "belief") + ")) {");
            int i = -1;
            for (PREDICATE b : sw.swtch) {
                i++;
                if (b == null) continue;

                TermPerfectTrie.indent(indent + 2);
                out.println('"' + Op.values()[i].toString() + "\": {");
                print(b, out, indent + 4);
                TermPerfectTrie.indent(indent + 2);
                out.println("}");

            }
            TermPerfectTrie.indent(indent);
            out.println("}");
        } else {
            out.print( /*Util.className(p) + ": " +*/ p);
            out.println();
        }


    }


    public static void print(PREDICATE<Derivation> d) {
        print(d, System.out);
    }

    public static void print(Object d, PrintStream out) {
        PremiseRuleCompiler.print(d, out, 0);


    }


    static PREDICATE<Derivation> compile(TermPerfectTrie<PREDICATE<Derivation>, DeriveAction> trie) {
        Collection<PREDICATE<Derivation>> bb = compile(trie.root);

        PREDICATE<Derivation> tf = FORK.fork(bb, FORK::new);
//        if (each != null)
//            tf = tf.transform(each);

        return tf;
    }


    @Nullable
    static Set<PREDICATE<Derivation>> compile(TrieNode<List<PREDICATE<Derivation>>, DeriveAction> node) {


        Set<PREDICATE<Derivation>> bb = new HashSet();


        node.forEach(n -> {

            Set<PREDICATE<Derivation>> branches = compile(n);

            int nStart = n.start();
            int nEnd = n.end();
            PREDICATE<Derivation> branch = PREDICATE.compileAnd(
                    n.seq().subList(nStart, nEnd),
                    //n.seq().stream().skip(nStart).limit(nEnd - nStart).collect(Collectors.toList()),
                    branches != null /* && !branches.isEmpty() */ ?
                            factorFork(branches, FORK::new)
                            : null
            );

            if (branch != null)
                bb.add(branch);
        });

        if (bb.isEmpty())
            return null;
        else {
            //return compileSwitch(bb, 2);
            return bb;
        }
    }

    private static PREDICATE<Derivation> factorFork(Set<PREDICATE<Derivation>> _x, Function<PREDICATE<Derivation>[], PREDICATE<Derivation>> builder) {

        int n = _x.size();
        if (n == 0)
            return null;

        if (n == 1)
            return _x.iterator().next();

        FasterList<PREDICATE<Derivation>> x = new FasterList<>(_x);

        Map<PREDICATE, SubCond> conds = new HashMap(x.size());
        for (int b = 0, xSize = n; b < xSize; b++) {
            PREDICATE  p = x.get(b);
            if (p instanceof AND) {
                int bb = b;
                ((AND) p).subStream().forEach((xx)-> SubCond.bumpCond(conds, (PREDICATE)xx, bb));
            } else if (p instanceof FORK) {

            } else {
                SubCond.bumpCond(conds, p, b);
            }
        }

        if (!conds.isEmpty()) {
            SubCond fx = conds.values().stream().max(Comparator.comparingDouble(SubCond::costIfBranch)).get();
            if (fx.size() < 2) {

            } else {
                Collection<PREDICATE<Derivation>> bundle = null;
                int i = 0;
                Iterator<PREDICATE<Derivation>> xx = x.iterator();
                while (xx.hasNext()) {
                    PREDICATE px = xx.next();
                    if (fx.branches.contains(i)) {
                        xx.remove();

                        if (px instanceof AND) {
                            Subterms pxSub = px.subterms();
                            if (pxSub.contains(fx.p)) {
                                px = AND.the((Term[]) pxSub.removing(fx.p));
                            }
                            if (bundle == null) bundle = new HashSet();
                            bundle.add(px);
                        } else {

                        }
                    }
                    i++;
                }
                if (bundle!=null) {
                    assert (bundle.size() > 1);
                    x.add(AND.the(fx.p, FORK.fork(bundle, (Function)builder)));

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
                    for (short cc : ((Branchify) zz).can)
                        r.add(cc);
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

        return FORK.fork(x, builder);
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






























































































































































































































































































