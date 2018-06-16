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
import nars.term.atom.Bool;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static nars.Op.CONJ;

public class TermlinkTemplates extends FasterList<Term> {

    public static final TermlinkTemplates EMPTY = new TermlinkTemplates(Op.EmptyTermArray) {
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
            sortThisByBoolean(t -> !conceptualizable(t));


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
     * https:
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


                term, layers(term)
        );


        int tcs = tc.size();
        if (tcs > 0) {
            return new TermlinkTemplates(((FasterList<Term>)tc.list).toArrayRecycled(Term[]::new));
        }


        return EMPTY;
    }

    /**
     * recurses
     */
    static void templates(Term x, Set<Term> tc, int depth, Term root, int maxDepth) {

        if (x == Op.imExt || x == Op.imInt)
            return;

        Op xo = x.op();
        if (depth > 0) {
            tc.add(x);
        }

        maxDepth += extraDepth(depth, root, x);

        if ((++depth >= maxDepth) || xo.atomic || !xo.conceptualizable)
            return;

        Subterms bb = x.subterms();
        int nextDepth = depth;
        int nextMaxDepth = maxDepth;

        if (xo == CONJ && bb.hasAny(CONJ)) {

//            int xdt = x.dt();
            x.eventsWhile((when, what) -> {
                templates(what.unneg(), tc, nextDepth, root, nextMaxDepth);
                return true;
            }, 0, true, true, true, 0);
            return;
        }


        bb.forEach(s -> templates(s.unneg(), tc, nextDepth, root, nextMaxDepth));

    }

    /** depth extensions */
    private static int extraDepth(int depth, Term root, Term x) {
        if (depth >= 1 && depth <= 2) {
            switch (root.op()) {
                case SIM:
                case INH:
                    if (depth == 1 && x.isAny(Op.SectBits | Op.SetBits | Op.DiffBits
                            | Op.PROD.bit
                    ))
                        return +1;
                    break;
                case CONJ:
                    if (depth == 1 && x.op().statement)
                    //if (depth == 1 && x.op().statement)
//                    //if (x.op()==IMPL || x.op()==INH)
                        return +1;
                    break;
                case IMPL:
                    Op xo = x.op();
                    if (xo.statement || (depth == 1 && xo==CONJ))
                        return +1;
                    break;
            }
        }
        return 0;
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


            case SIM: {


//                if (x.subterms().OR(xx -> xx.unneg().isAny(SetBits | Op.SectBits | Op.PROD.bit)))
//                    return 3;
//                else
                    return 2;
            }

            case INH: {
//                if (x.subterms().OR(xx -> xx.unneg().isAny(SetBits | Op.SectBits
//                        | Op.PROD.bit
//                        )))
//                    return 3;

                return 2;
            }

            case IMPL:
//                if (x./*subterms().*/hasAny(Op.CONJ.bit)) {
//                    if (x.hasAny(Op.INH.bit))
//                        return 4;
//                    else
                        return 2;
//                }


            case CONJ:
//                if (x.hasAny(Op.IMPL))
//                    return 3;
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
            return Concept.EmptyArray;
        else if (nulls > 0) {
            return ArrayUtils.removeNulls(x, Concept[]::new);
        } else {
            return x;
        }
    }

    /**
     * termlink and activate the templates
     */
    public void linkAndActivate(Concept src, float pri, NAR nar) {


        int n = this.size();
        if (n == 0)
            return;

        MutableFloat refund = new MutableFloat(0);

        Term srcTerm = src.term();

        float balance = nar.termlinkBalance.floatValue();
        float budgetedForward = concepts == 0 ? 0 :
                Math.max(Prioritized.EPSILON, pri * (1f - balance) / concepts);
        float budgetedReverse = Math.max(Prioritized.EPSILON, pri * balance / n);

        Iterable<PriReference<Term>> srcTermLinks = src.termlinks();
        for (int i = 0; i < n; i++) {
            Term tgtTerm = get(i);

            boolean conceptualizable = i < concepts;
            if (conceptualizable) {

                @Nullable Concept tgt = nar.conceptualize(tgtTerm);

                if (tgt != null) {


                    tgt.termlinks().put(
                            new PLink<>(srcTerm, budgetedForward), refund
                    );

                    nar.activate(tgt, budgetedForward);

                    tgtTerm = tgt.term();
                }

            } else {
                refund.add(budgetedForward);
            }

            if (tgtTerm instanceof Bool)
                throw new RuntimeException("should not exist a Bool termlink");

            ((Bag) srcTermLinks).put(new PLink<>(tgtTerm, budgetedReverse), refund);

        }
    }

}
