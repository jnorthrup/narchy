package nars.link;

import jcog.data.ArrayHashSet;
import jcog.list.FasterList;
import nars.Op;
import nars.Param;
import nars.subterm.Subterms;
import nars.term.Term;

import java.util.Set;

import static nars.Op.*;

public enum TermLinks {
    ;


    /**
     * see:
     * https://github.com/opennars/opennars/blob/master/nars_core/nars/language/Terms.java#L367
     */
    public static TermlinkTemplates templates(Term term) {

        if (term.subs() > 0) {

            ArrayHashSet<Term> tc =
                    //new UnifiedSet<>(id.volume() /* estimate */);
                    new ArrayHashSet<>(term.volume());

            if (Param.DEBUG_EXTRA) {
                if (!term.equals(term.concept())) {
                    throw new RuntimeException("templates only should be generated for rooted terms:\n\t" + term + "\n\t" + term.concept());
                }
            }

            TermLinks.templates(term, tc, 0,
                    //2
                    //term.isAny(IMPL.bit | CONJ.bit | INH.bit) ? 4 : 2
                    term, layers(term)
            );

//            //"if ((tEquivalence || (tImplication && (i == 0))) && ((t1 instanceof Conjunction) || (t1 instanceof Negation))) {"
//            if (/*term.hasAll(IMPL.bit | CONJ.bit) && */term.op() == IMPL || term.op()==CONJ) {
//                for (int subjOrPred  = 0; subjOrPred < 2; subjOrPred++) {
//                    Term ic = term.sub(subjOrPred);
//                    //if (ic.op() == CONJ) {
//                        TermLinks.templates(ic, tc, 0, 2);
//                    //}
//                }
//            }


            int tcs = tc.size();
            if (tcs > 0) {
                return new TermlinkTemplates(((FasterList<Term>)(tc.list)).toArrayRecycled(Term[]::new)); //store as list for compactness and fast iteration
            }

        }


        return TermlinkTemplates.EMPTY;
    }

    /**
     * recurses
     */
    static void templates(Term _x, Set<Term> tc, int depth, Term root, int maxDepth) {

        Term x = _x;

//        switch (o) {
//            case VAR_QUERY:
//                return; //NO
//        }

        if (((depth > 0) || selfTermLink(x)) && !tc.add(x))
            return; //already added

        if ((++depth >= maxDepth) || !templateRecurseInto(root, depth, x))
            return;

        Subterms bb = x.subterms();
        int bs = bb.subs();
        if (bs > 0) {
            int nextDepth = depth;
            bb.forEach(s -> templates(s.unneg(), tc, nextDepth, root, maxDepth));
        }
    }

    /** whether to recurse templates past a certain subterm.
     *  implements specific structural exclusions */
    private static boolean templateRecurseInto(Term root, int depth, Term subterm) {
        Op s = subterm.op();
        if (!s.conceptualizable || s.atomic)
            return false;




        Op r = root.op();
        if (r == INH || r == SIM) {
//            if (depth >= 2) {
//                if (s.isAny(Op.SetBits))
//                    return false;
//            }

//            if (depth > 2) {
//                if (s.isAny(Op.SectBits))
//                    return false;
//            }
        }

//        if (r == IMPL) {
//            if (depth > 2) {
//                if (s.isAny())
//                    return false;
//            }
//        }

        if ((r == IMPL || r == CONJ) ) {
            if (depth >= 2)
                if (s.isAny(PROD.bit | Op.SetBits | Op.SectBits ))
                    return false;

        }
        return depth <= 2 || s != PROD;
    }


    /**
     * determines ability to structural transform, so those terms which have no structural transforms should not link to themselves
     */
    static boolean selfTermLink(Term b) {
        switch (b.op()) {
            case INH:
            case SIM:
                return b.hasAny(Op.SETe,Op.SETi,Op.SECTe,Op.SECTi);

            case PROD:
                return false;

            case IMPL: //<- check if IMPL needs it
                return false;
            case SECTe:
            case SECTi:
            case SETe:
            case SETi:
            case DIFFi:
            case DIFFe:
                return true;

            case CONJ:
                return true;

            default:
                throw new UnsupportedOperationException("what case am i missing");
        }
        //return false;
        //return b.isAny(Op.CONJ.bit | Op.SETe.bit | Op.SETi.bit  /* .. */);
        //return true;
    }

    /**
     * includes the host as layer 0, so if this returns 1 it will only include the host
     */
    static int layers(Term x) {
        switch (x.op()) {

            case PROD:
                return 2;

            case SETe:
            case SETi:
                return 2;

            case DIFFe:
            case DIFFi:
            case SECTi:
            case SECTe:
                return 2;


            case SIM:
                Subterms xx = x.subterms();
                if (xx.hasAny(Op.VariableBits) || (xx.sub(0).isAny(Op.SetBits) || xx.sub(1).isAny(Op.SetBits)))
                    return 3;
                else
                    return 2;

            case INH:
                return 3;

            case IMPL:
                //if (x.hasAny(Op.CONJ))
                return 4;
                //else
                  //  return 3;

            case CONJ:
                //if (x.hasAny(Op.INH))
                    return 3;
                //else return 2;

            default:
                throw new UnsupportedOperationException("unhandled operator type: " + x.op());

        }
    }


//    @Nullable
//    public static List<Termed> templates(Concept id, NAR nar) {
//
//        Collection<Termed> localTemplates = id.templates();
//        int n = localTemplates.size();
//        if (n <= 0)
//            return null;
//
//        Term thisTerm = id.term();
//
//        List<Termed> localSubConcepts =
//                //new HashSet<>(); //temporary for this function call only, so as not to retain refs to Concepts
//                //new UnifiedSet(); //allows concurrent read
//                $.newArrayList(n); //maybe contain duplicates but its ok, this is fast to construct
//
//        //Random rng = nar.random();
//        //float balance = Param.TERMLINK_BALANCE;
//
//        float spent = 0;
//        for (Termed localSub : localTemplates) {
//
//            //localSub = mutateTermlink(localSub.term(), rng); //for special Termed instances, ex: RotatedInt etc
////                if (localSub instanceof Bool)
////                    continue; //unlucky mutation
//
//
//            Termed target = localSub.term(); //if mutated then localSubTerm would change so do it here
//
//            float d;
//
//            if (target.op().conceptualizable && !target.equals(thisTerm)) {
//
//                Concept targetConcept = nar.conceptualize(localSub);
//                if (targetConcept != null) {
//                    target = (targetConcept);
//
//                }
//            }
//
//            localSubConcepts.add(target);
//        }
//
//        return !localSubConcepts.isEmpty() ? localSubConcepts : null;
//
//    }

//    /**
//     * preprocess termlink
//     */
//    private static Termed mutateTermlink(Term t, Random rng) {
//
//        if (Param.MUTATE_INT_CONTAINING_TERMS_RATE > 0) {
//            if (t.hasAny(INT)) {
//                Subterms ts = t.subterms();
//                if (ts.OR(Int.class::isInstance) && rng.nextFloat() <= Param.MUTATE_INT_CONTAINING_TERMS_RATE) {
//
//                    Term[] xx = ts.arrayClone();
//                    boolean changed = false;
//                    for (int i = 0; i < xx.length; i++) {
//                        Term y = xx[i];
//                        if (y instanceof Int && y.op() == INT) {
//                            int shift =
//                                    rng.nextInt(3) - 1;
//                            //nar.random().nextInt(5) - 2;
//                            if (shift != 0) {
//                                int yy = ((Int) y).id;
//                                int j =
//                                        Math.max(0 /* avoid negs for now */, yy + shift);
//                                if (yy != j) {
//                                    xx[i] = Int.the(j);
//                                    changed = true;
//                                }
//                            }
//                        }
//                    }
//                    if (changed)
//                        return t.op().the(t.dt(), xx);
//
//                }
//            }
//
//        }
//
//        return t;
//    }

//    @NotNull
//    public static Concept[] templateConcepts(List<Termed> templates) {
//        if (templates.isEmpty())
//            return Concept.EmptyArray;
//
//        FasterList<Concept> templateConcepts = new FasterList(0, new Concept[templates.size()]);
//        for (int i = 0, templatesSize = templates.size(); i < templatesSize; i++) {
//            Termed x = templates.get(i);
//            if (x instanceof Concept)
//                templateConcepts.add((Concept) x);
//        }
//        return templateConcepts.toArrayRecycled(Concept[]::new);
//    }


}
