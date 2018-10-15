package nars.link;

import jcog.data.MutableFloat;
import jcog.data.NumberX;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.bag.Bag;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.concept.Concept;
import nars.derive.Derivation;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.var.ImDep;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static jcog.pri.ScalarValue.EPSILON;
import static nars.Op.CONJ;
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

    public static TermLinker of(Term term) {
        return of(term, layers(term));
    }

    public static TermLinker of(Term term, Term... additional) {
        return of(term, layers(term), additional);
    }

    /**
     * default recursive termlink templates constructor
     */
    public static TermLinker of(Term term, int layers, Term... additional) {

        if (term.subs() > 0) {

            if (Param.DEBUG_EXTRA) {
                if (!term.equals(term.concept()))
                    throw new RuntimeException("templates only should be generated for rooted terms:\n\t" + term + "\n\t" + term.concept());
            }

            ArrayHashSet<Term> tc = new ArrayHashSet<>(term.volume() /* estimate */);

            add(term, tc, 0, term,  layers);

            Collections.addAll(tc, additional);

            int tcs = tc.size();
            if (tcs > 0)
                return new TemplateTermLinker(((FasterList<Term>) tc.list).toArrayRecycled(Term[]::new));
        }

        return NullLinker;
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

        if (x instanceof Bool || x instanceof ImDep)
            return;

        Op xo = x.op();
        if (depth > 0) {
            tc.add(x);
        }

        boolean stopping = xo.atomic || !xo.conceptualizable;
        if (!stopping)
            maxDepth += deeper(depth, root, x);

        if ((++depth >= maxDepth) || stopping)
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
        } else {

            bb.forEach(s -> add(s.unneg(), tc, nextDepth, root, nextMaxDepth));
        }

    }

    /** depth extensions */
    private static int deeper(int depth, Term root, Term x) {

        if (depth < 1 || depth >= 4)
            return 0; //no change

        Op xo = x.op();
        switch (root.op()) {
            case SIM:
            case INH:
                if (depth == 1 && x.hasAny(
                        //Op.Variable
                        Op.VAR_INDEP.bit
                ))
                    return +1;
                break;

//            case CONJ:
////                if (depth <=2 && x.hasAny(Op.Variable) )
////                    return +1;
//
////                if (depth <=2 && xo.isAny(INH.bit | SETe.bit | SETi.bit | INH.bit) )
//
//                //                    return +1;
////                    if (depth ==1 && (xo.statement && x.hasAny(Op.VAR_DEP)))
////                        return +1; //necessary for certain NAL6 unification cases
////                    if (depth > 1 && !x.hasAny(Op.VAR_DEP))
////                        return -1; //event subterm without any var dep, dont actually recurse
//                break;
            case IMPL:
                if (depth >=1 && depth <=2 && ((xo == CONJ || x.hasAny(
                        //Op.VAR_INDEP.bit
                        Op.Variable
                ))))
                    return +1;
//                if (depth <=3 && xo.isAny(INH.bit | SETe.bit | SETi.bit | INH.bit) )
//                    return +1;
//                    if (depth == 1 && (xo.statement && x.hasAny(Op.VariableBits) ) )
//                        return +1;
//                    if (depth > 1 && (!xo.isAny(CONJ.bit | SETe.bit | SETi.bit | PROD.bit)) && !x.hasAny(Op.VariableBits))
//                        return -1;
//                    if (depth >= 1 && depth <= 2 && xo.isAny(CONJ.bit | SETe.bit | SETi.bit | PROD.bit | INH.bit))
//                        return +1;

//                    if (depth ==1 && (xo ==CONJ || (xo.statement)))
//                        return +1;
//                    if (depth > 1 && !x.hasAny(Op.VAR_INDEP))
//                        return -1;

                break;
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
//                if (x./*subterms().*/hasAny(Op.CONJ.bit))
//                    return 3;
//                else
                    return 2;
//                }


            case CONJ:
//                if (x.hasAny(Op.IMPL))
//                    return 3;
                return 2;


            default:
                /** atomics,etc */
                return 0;

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
    @Override public void link(Activate a, Derivation d) {



        List<Concept> firedConcepts = conceptualizeAndTermLink(a.id, d);
        //default all to all exhausive matrix insertion
        //TODO configurable "termlink target concept x tasklink matrix" linking pattern: density, etc
        if (!firedConcepts.isEmpty()) {

            for (TaskLink f : d.firedTaskLinks) {
                NumberX overflow = new MutableFloat(); //keep overflow specific to the tasklink

//                UnitPri allocated = new UnitPri();
//                allocated.take(f, linkDecayRate,false,false);
//                //float priDispersed = linkDecayRate * f.priElseZero();
//                //f.priSub(priDispersed);
//                float p = Math.max(EPSILON, allocated.priElseZero());

                float p = Math.max(EPSILON, f.priElseZero());
                Tasklinks.linkTask((TaskLink.GeneralTaskLink) f, p, firedConcepts, overflow);
            }

        }


    }

    @Override
    public void init(Bag<Term, PriReference<Term>> termlinks) {
        for (Term template : this) {
            termlinks.putAsync(ActivatedLinks.termlink(template, EPSILON));
        }
    }

    private List<Concept> conceptualizeAndTermLink(Concept src, Derivation d) {



        ArrayHashSet<Concept> firedConcepts = d.firedConcepts;
        firedConcepts.clear();

        int n = size();
        if (n > 0) {

            NAR nar = d.nar;

            n = Math.min(n, Param.TermLinkFanoutMax);

            float taskLinkPriSum = Math.max(EPSILON, (float) (((FasterList<TaskLink>) (d.firedTaskLinks.list))
                    .sumOfFloat(Prioritized::priElseZero)));

            float conceptForward =
                    //Math.max(EPSILON, a.priElseZero() / n);
                    taskLinkPriSum / Math.max(1, concepts);



            float balance = nar.termlinkBalance.floatValue();

            float termlinkReverse = Math.max(EPSILON, taskLinkPriSum * balance / n);

//        //calculate exactly according to the size of the subset that are actually conceptualizable
//        float budgetedForward = concepts == 0 ? 0 :
//                Math.max(Prioritized.EPSILON, pri * (1f - balance) / concepts);

            float termlinkForward = Math.max(EPSILON, taskLinkPriSum * (1 - balance) / n);


            NumberX refund = new MutableFloat(0);

            ActivatedLinks linking = d.deriver.linked;
            Term srcTerm = src.term();

            int j = d.random.nextInt(n); //random starting position
            boolean inc = d.random.nextBoolean();
            for (int i = 0; i < n; i++) {
                if (inc) {
                    if (++j == n) j = 0;
                } else {
                    if (--j == -1) j = n-1;
                }

                Term tgtTerm = get(j);

                boolean conceptualizable = j < concepts;
                if (conceptualizable) {

                    /** TODO batch activations */
                    @Nullable Concept tgt = nar.activate(tgtTerm,
                            //conceptForward
                            EPSILON

                            ,

                            //tgtTerm instanceof Atomic
                            true
                    );

                    if (tgt != null) {

                        firedConcepts.add(tgt);

                        linking.linkPlus(tgt, srcTerm, termlinkForward, refund);

                        tgtTerm = tgt.term();

                    }

                } else {
                    refund.add(termlinkForward);
                }

                linking.linkPlus(src, tgtTerm, termlinkReverse, refund);
            }

        }
        return firedConcepts.list;
    }

}
