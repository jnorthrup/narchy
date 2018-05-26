package nars.link;

import jcog.bag.Bag;
import jcog.data.ArrayHashSet;
import jcog.list.FasterList;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.concept.Concept;
import nars.subterm.Subterms;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static nars.Op.CONJ;

public class TermlinkTemplates extends FasterList<Term> {

    static final TermlinkTemplates EMPTY = new TermlinkTemplates(Op.EmptyTermArray) {
        {
            concepts = 0;
        }
    };

    /**
     * index of the last concept template; any others beyond this index are non-conceptualizable
     */
    byte concepts;

    TermlinkTemplates(Term[] terms) {
        super(terms.length, terms);

        if (size > 1)
            sortThisByBoolean((t) -> !conceptualizable(t));

        //scan backwards to find the index where the first non-conceptualizable occurrs
        int lastConcept = size - 1;
        for (; lastConcept >= 0; lastConcept--) {
            if (conceptualizable(get(lastConcept)))
                break;
        }
        assert (lastConcept < 127);
        concepts = (byte) (lastConcept + 1);
    }

    /**
     * see:
     * https://github.com/opennars/opennars/blob/master/nars_core/nars/language/Terms.java#L367
     */
    public static TermlinkTemplates templates(Term term) {

        if (term.subs() <= 0)
            return EMPTY;



        ArrayHashSet<Term> tc =
                new ArrayHashSet<>(term.volume() /* estimate */);

        if (Param.DEBUG_EXTRA) {
            if (!term.equals(term.concept())) {
                throw new RuntimeException("templates only should be generated for rooted terms:\n\t" + term + "\n\t" + term.concept());
            }
        }

        templates(term, tc, 0,
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
            return new TermlinkTemplates(tc.list.toArrayRecycled(Term[]::new)); //store as list for compactness and fast iteration
        }


        return EMPTY;
    }

    /**
     * recurses
     */
    static void templates(Term x, Set<Term> tc, int depth, Term root, int maxDepth) {

        if (x == Op.imExt || x == Op.imInt)
            return; //NO

        Op xo = x.op();


        if (depth > 0)
            tc.add(x);
//        if (((depth > 0) || selfTermLink(x))/* && !tc.add(x)*/) //tc.add condition isnt valid if the subterm appears in a different level it may prevent its subterms from being included
//            return; //already added

        if ((++depth >= maxDepth) || xo.atomic || !xo.conceptualizable)
            return;


        Subterms bb = x.subterms();
        int nextDepth = depth;

        if (xo == CONJ && bb.hasAny(CONJ)) {
            //special case: decompose complex conj directly to individual events
            x.eventsWhile((when,what) -> {
                templates(what.unneg(), tc, nextDepth, root, maxDepth);
                return true;
            }, 0, true, true, true, 0);
            return;
        }

//        switch (o) {
//            case VAR_QUERY:
//                return; //NO
//        }


//        int bs = bb.subs();
//        if (bs > 0) {
        bb.forEach(s -> templates(s.unneg(), tc, nextDepth, root, maxDepth));
//        }
    }

//    /**
//     * whether to recurse templates past a certain subterm.
//     * implements specific structural exclusions
//     */
//    private static boolean templateRecurseInto(Term root, int depth, Term subterm) {
//        Op s = subterm.op();
//        return !(s.atomic || !s.conceptualizable);
//
////        Op r = root.op();
////        if (r == INH || r == SIM) {
////            if (depth >= 2) {
////                if (s.isAny(Op.SetBits))
////                    return false;
////            }
//
////            if (depth > 2) {
////                if (s.isAny(Op.SectBits))
////                    return false;
////            }
////        }
//
////        if (r == IMPL) {
////            if (depth > 2) {
////                if (s.isAny())
////                    return false;
////            }
////        }
////
////        if ((r == IMPL || r == CONJ) ) {
////            if (depth >= 2)
////                if (s.isAny(PROD.bit | Op.SetBits | Op.SectBits ))
////                    return false;
////
////        }
////        return depth <= 2 || s != PROD;
//    }

    /**
     * determines ability to structural transform, so those terms which have no structural transforms should not link to themselves
     */
    static boolean selfTermLink(Term b) {
//        switch (b.op()) {
//            case INH:
//            case SIM:
//                //return b.hasAny(Op.SETe,Op.SETi,Op.SECTe,Op.SECTi);
//                //return true;
//                return false;
//
//            case IMPL: //<- check if IMPL needs it
//                //return false;
//                return false;
//
//            case PROD:
//                return false;
//
//
//            case SECTe:
//            case SECTi:
//            case SETe:
//            case SETi:
//                return false;
//            case DIFFi:
//            case DIFFe:
//                return false;
//
//            case CONJ:
//                return false;
//
//            default:
//                throw new UnsupportedOperationException("what case am i missing");
//        }
        return false;
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

            case SECTi:
            case SECTe:
                return 2;

            case DIFFe:
            case DIFFi:
                return 2;


            case SIM:
//                Subterms xx = x.subterms();
//                if (xx.hasAny(Op.VariableBits) )
////                        (xx.hasAny(SetBits) && (xx.sub(0).isAny(SetBits) || xx.sub(1).isAny(SetBits))))
//                    return 3;
//                else
                return 2;

            case INH:
                if (x.subterms().OR(xx -> xx.isAny(Op.SetBits | Op.SectBits)))
                    return 3;
                else
                    return 2;

            case IMPL:
                if (x./*subterms().*/hasAny(Op.CONJ.bit )) {
                    if (x.hasAny(Op.INH.bit))
                        return 4;
                    else
                        return 3;
                }
//                else if (x./*subterms().*/hasAny(Op.CONJ.bit ))
//                    return 3;
                else
                    return 2;

            case CONJ:
                //if (x.hasAny(Op.INH))
                //return 3; else
                return 2;


            default:
                throw new UnsupportedOperationException("unhandled operator type: " + x.op());

        }
    }

    public static boolean conceptualizable(Term x) {
        return x.op().conceptualizable;
    }

    /**
     * creates a sub-array of the conceptualizable terms and shuffles them
     */
    public Concept[] concepts(NAR nar, boolean conceptualize) {
        int concepts = this.concepts;
        if (concepts == 0)
            return Concept.EmptyArray;

        Concept[] x = new Concept[concepts];
        int nulls = 0;
        for (int i = 0; i < concepts; i++) {
            if ((x[i] = nar.concept(items[i], conceptualize)) == null)
                nulls++;
        }
        if (nulls == concepts)
            return Concept.EmptyArray; //none conceptualizable
        else if (nulls > 0) {
            return ArrayUtils.removeNulls(x, Concept[]::new);
        } else {
            return x;
        }
    }

    /**
     * link and activate the templates
     */
    public void linkAndActivate(Concept src, float pri, NAR nar) {

        //activate the task's concept
//        float conceptPri = pri * nar.activateConceptRate.floatValue();
//        nar.activate(src, conceptPri);

        int n = this.size();
        if (n == 0)
            return;

        MutableFloat refund = new MutableFloat(0);

        Term srcTerm = src.term();
        Iterable<PriReference<Term>> srcTermLinks = src.termlinks();

        float balance = nar.termlinkBalance.floatValue();
        float budgetedForward = concepts == 0 ? 0 :
                Math.max(Prioritized.EPSILON, pri * (1f - balance) / concepts); //concept targets (subset of all targets)
        float budgetedReverse = Math.max(Prioritized.EPSILON, pri * balance / n); //all targets

        for (int i = 0; i < n; i++) {
            Term tgtTerm = get(i);

            boolean conceptualizable = i < concepts;
            if (conceptualizable) {

                @Nullable Concept tgt = nar.conceptualize(tgtTerm);

                if (tgt != null) {

                    //insert termlink
                    tgt.termlinks().put(
                            new PLink<>(srcTerm, budgetedForward), refund
                    );

                    nar.activate(tgt, budgetedForward);

                    tgtTerm = tgt.term(); //use the concept's id
                }

            } else {
                refund.add(budgetedForward);
            }

            ((Bag) srcTermLinks).put(new PLink<>(tgtTerm, budgetedReverse), refund);

        }
    }

}
