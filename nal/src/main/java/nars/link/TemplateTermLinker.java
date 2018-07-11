package nars.link;

import jcog.bag.Bag;
import jcog.data.ArrayHashSet;
import jcog.list.FasterList;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.concept.Concept;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static nars.Op.CONJ;
import static nars.Op.SetBits;
import static nars.time.Tense.XTERNAL;

/** default general-purpose termlink template impl. for compound terms
 *  contains a fixed set of subterm components that can be term-linked with.
 *  this implementation stores the conceptualizable terms in the lower part of a list
 *  so that they can be accessed quickly as separate from non-conceptualizables.
 *  */
public final class TemplateTermLinker extends FasterList<Term> implements TermLinker {


    /**
     * index of the last concept template; any others beyond this index are non-conceptualizable
     */
    private byte concepts;


    @Override
    public Stream<? extends Termed> targets() {
        return stream();
    }

    /**
     * default recursive termlink templates constructor
     */
    public static TermLinker of(Term term) {

        if (term.subs() > 0) {

            if (Param.DEBUG_EXTRA) {
                if (!term.equals(term.concept()))
                    throw new RuntimeException("templates only should be generated for rooted terms:\n\t" + term + "\n\t" + term.concept());
            }

            ArrayHashSet<Term> tc = new ArrayHashSet<>(term.volume() /* estimate */);

            add(term, tc, 0, term, layers(term) );

            int tcs = tc.size();
            if (tcs > 0)
                return new TemplateTermLinker(((FasterList<Term>) tc.list).toArrayRecycled(Term[]::new));
        }

        return Empty;
    }

    private TemplateTermLinker(Term[] terms) {
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

    @Override
    protected Object[] newArray(int newCapacity) {
        return new Term[newCapacity];
    }


    /**
     * recurses into subterms
     */
    private static void add(Term x, Set<Term> tc, int depth, Term root, int maxDepth) {

        if (x instanceof Bool || x == Op.imExt || x == Op.imInt)
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

        if (xo == CONJ && bb.hasAny(CONJ) && x.dt()!=XTERNAL) {

//            int xdt = x.dt();
            x.eventsWhile((when, what) -> {
                add(what.unneg(), tc, nextDepth, root, nextMaxDepth);
                return true;
            }, 0, true, true, true, 0);
            return;
        }


        bb.forEach(s -> add(s.unneg(), tc, nextDepth, root, nextMaxDepth));

    }

    /** depth extensions */
    private static int extraDepth(int depth, Term root, Term x) {
        if (depth >= 1 && depth <= 2) {
            switch (root.op()) {
                case SIM:
                    if (depth == 1 && x.isAny(
                            //Op.SectBits | Op.SetBits | Op.DiffBits | Op.PROD.bit | Op.INH.bit
                            Op.INH.bit
                    ))
                        return +1;
                    break;
                case INH:
                    if (depth == 1 && x.isAny(Op.SectBits | SetBits | Op.DiffBits | Op.PROD.bit))
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
                    if ( xo.statement || (depth == 1 && xo==CONJ))
                        return +1;
                    break;
            }
        }
        return 0;
    }


    /**
     * includes the host as layer 0, so if this returns 1 it will only include the host
     */
    private static int layers(Term x) {
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
//                if (x.subterms().OR(xx -> xx.unneg().isAny(Op.SetBits | Op.SectBits
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

    private static boolean conceptualizable(Term x) {
        return x.op().conceptualizable;
    }

//    /**
//     * creates a sub-array of the conceptualizable terms and shuffles them
//     */
//    @Deprecated @Override public Concept[] concepts(NAR nar) {
//        int concepts = this.concepts;
//        if (concepts == 0)
//            return Concept.EmptyArray;
//
//        Concept[] x = new Concept[concepts];
//        int nulls = 0;
//        for (int i = 0; i < concepts; i++) {
//            if ((x[i] = nar.conceptualize(items[i])) == null)
//                nulls++;
//        }
//        if (nulls == concepts)
//            return Concept.EmptyArray;
//        else if (nulls > 0) {
//            return ArrayUtils.removeNulls(x, Concept[]::new);
//        } else {
//            return x;
//        }
//    }


    /** balance = nar.termlinkBalance */
    @Override public void link(Concept src, float pri, List<TaskLink> fired, LinkActivations activated, Random rng, NAR nar) {


        //TODO move to param
        int termlinkFanoutMax = 10;

        int n = size();
        if (n == 0)
            return;


        n = Math.min(n, termlinkFanoutMax);

        Term srcTerm = src.term();

        float balance = nar.termlinkBalance.floatValue();

        float budgetedReverse = Math.max(Prioritized.EPSILON, pri * balance / n);

//        //calculate exactly according to the size of the subset that are actually conceptualizable
//        float budgetedForward = concepts == 0 ? 0 :
//                Math.max(Prioritized.EPSILON, pri * (1f - balance) / concepts);

        float budgetedForward = Math.max(Prioritized.EPSILON, pri * (1-balance) / n);

        List<Concept> targets = (concepts==0 ? List.of() : new FasterList<>(concepts));

        Bag<Term, PriReference<Term>> srcTermLinks = src.termlinks();
        MutableFloat refund = new MutableFloat(0);

        int j = rng.nextInt(n); //random starting position
        for (int i = 0; i < n; i++) {

            if (++j == n) j = 0;
            Term tgtTerm = get(j);

            boolean conceptualizable = j < concepts;
            if (conceptualizable) {

                /** TODO batch activations */
                @Nullable Concept tgt = nar.activate(tgtTerm, budgetedForward);

                if (tgt != null) {


                    targets.add(tgt);

                    tgtTerm = tgt.term();


                    activated.link(tgt, srcTerm, budgetedForward, refund);
//                    tgt.termlinks().put(
//                            new PLink<>(srcTerm, budgetedForward), refund
//                    );

                }

            } else {
                refund.add(budgetedForward);
            }



            //srcTermLinks.put(new PLink<>(tgtTerm, budgetedReverse), refund);
            activated.link(src, tgtTerm, budgetedReverse, refund);

        }


        //default all to all exhausive matrix insertion
        //TODO configurable "termlink target concept x tasklink matrix" linking pattern: density, etc
        if (!targets.isEmpty()) {

            for (TaskLink f : fired) {
                MutableFloat overflow = new MutableFloat(); //keep overflow specific to the tasklink
                Tasklinks.linkTask((TaskLink.GeneralTaskLink) f, f.priElseZero(), targets, overflow);
            }
        }
    }

}
