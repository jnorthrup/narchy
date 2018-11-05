package nars.link;

import jcog.Util;
import jcog.data.MutableFloat;
import jcog.data.NumberX;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.pri.Prioritized;
import jcog.pri.ScalarValue;
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
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static nars.Op.CONJ;
import static nars.time.Tense.XTERNAL;

/**
 * default general-purpose termlink template impl. for compound terms
 * contains a fixed set of subterm components that can be term-linked with.
 * this implementation stores the conceptualizable terms in the lower part of a list
 * so that they can be accessed quickly as separate from non-conceptualizables.
 *
 * also caches Concept references until a concept becomes deleted
 */
public final class TemplateTermLinker extends FasterList<Termed> implements TermLinker {


    /** whether to decompose conjunction sequences to each event, regardless of a conjunction's sub-conjunction structure */
    private static final boolean decomposeConjEvents = false;

//    /**
//     * how fast activation spreads from source concept to template target concepts
//     */
//    static final float activationRate =
//            1f;
//            //0.5f;

    static final float tasklinkSpreadRate = 1f;


    /**
     * number of termlinks which are actually conceptualizable; sorted on construction
     */
    private final byte concepts;


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

            Set<Term> tc = new UnifiedSet<>(term.volume() /* estimate */ + additional.length);

            add(term, tc, 0, term, layers);

            Collections.addAll(tc, additional);

            int tcs = tc.size();
            if (tcs > 0)
                return new TemplateTermLinker(tc.toArray(Termed[]::new));
        }

        return NullLinker;
    }

    private TemplateTermLinker(Termed[] terms) {
        super(terms.length, terms);

        if (size > 1)
            sortThisByBoolean(t -> !conceptualizable((Term)t));


        int lastConcept = size - 1;
        for (; lastConcept >= 0; lastConcept--) {
            if (conceptualizable((Term)get(lastConcept)))
                break;
        }
        assert (lastConcept < 127);
        concepts = (byte) (lastConcept + 1);
    }

    @Override
    protected Termed[] newArray(int newCapacity) {
        return new Termed[newCapacity];
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

        if (xo == CONJ && decomposeConjEvents && bb.hasAny(CONJ) && x.dt() != XTERNAL) {

//            int xdt = x.dt();
            x.eventsWhile((when, what) -> {
                add(what.unneg(), tc, nextDepth, root, nextMaxDepth);
                return true;
            }, 0, true, true, true, 0);
        } else {

            bb.forEach(s -> add(s.unneg(), tc, nextDepth, root, nextMaxDepth));
        }

    }

    /**
     * depth extensions
     */
    private static int deeper(int depth, Term root, Term x) {

//        if (depth < 1 || depth >= 4)
//            return 0; //no change

//        Op xo = x.op();
//        Op ro = root.op();
//        switch (ro) {
//            case SIM:
//            case INH:
//                if (depth == 1 && x.hasAny(
//                        //Op.Variable
//                        Op.VAR_INDEP.bit
//                )
////                        ||
////                        (x.isAny(Op.Sect | Op.Set | Op.Diff))
//                )
//                    return +1;
//                break;

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
//            case IMPL:
//                if (depth >=1 && depth <=2 && ((xo == CONJ || x.hasAny(
//                        Op.VAR_INDEP.bit | Op.VAR_DEP.bit
//                        //Op.VAR_INDEP.bit
//                        //Op.Variable
//                ))))
//                        return +1;
//
//
//                break;
//        }

        return 0;
    }


    /**
     * includes the host as layer 0, so if this returns 1 it will only include the host
     */
    private static int layers(Term x) {
        return 2;
//        switch (x.op()) {
//
//            case PROD:
//                return 2;
//
//            case SETe:
//            case SETi:
//                return 2;
//
//            case SECTi:
//            case SECTe:
//                return 2;
//
//            case DIFFe:
//            case DIFFi:
//                return 2;
//
//
//            case SIM: {
//
//
////                if (x.subterms().OR(xx -> xx.unneg().isAny(SetBits | Op.SectBits | Op.PROD.bit)))
////                    return 3;
////                else
//                    return 2;
//            }
//
//            case INH: {
////                if (x.subterms().OR(xx -> xx.unneg().isAny(Op.SetBits | Op.SectBits
////                        | Op.PROD.bit
////                        )))
////                    return 3;
//
//                return 2;
//            }
//
//            case IMPL:
////                if (x./*subterms().*/hasAny(Op.CONJ.bit))
////                    return 3;
////                else
//                    return 2;
////                }
//
//
//            case CONJ:
////                if (x.hasAny(Op.IMPL))
////                    return 3;
//                return 2;
//
//
//            default:
//                /** atomics,etc */
//                return 0;
//
//        }
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


    /**
     * balance = nar.termlinkBalance
     */
    @Override
    public void link(Activate a, Derivation d) {


        if (conceptualizeAndTermLink(a, d) > 0) {

            List<Concept> firedConcepts = d.firedConcepts.list;

            //default all to all exhausive matrix insertion
            //TODO configurable "termlink target concept x tasklink matrix" linking pattern: density, etc
            if (!firedConcepts.isEmpty()) {

                NumberX overflow = new MutableFloat();


                for (TaskLink f : d.firedTaskLinks) {


//                UnitPri allocated = new UnitPri();
//                allocated.take(f, linkDecayRate,false,false);
//                //float priDispersed = linkDecayRate * f.priElseZero();
//                //f.priSub(priDispersed);
//                float p = Math.max(EPSILON, allocated.priElseZero());

                    float p = f.priElseZero() * tasklinkSpreadRate;
                    Tasklinks.linkTask((TaskLink.GeneralTaskLink) f, p, firedConcepts, overflow);
                    overflow.set(0); //clear after each and re-use
                }

            }
        }


    }

    @Override
    public void sample(Random rng, Function<? super Term, SampleReaction> each) {
        each.apply(get(rng).term());
    }


    private int conceptualizeAndTermLink(Activate conceptSrc, Derivation d) {


        int n = concepts;
        if (n > 0) {

            ArrayHashSet<Concept> firedConcepts = d.firedConcepts;
            firedConcepts.clear();


            n = Math.min(n, Param.LinkFanoutMax);

            float taskLinkPriSum = Math.max(ScalarValue.EPSILON, (float) (((FasterList<TaskLink>) (d.firedTaskLinks.list))
                    .sumOfFloat(Prioritized::priElseZero)));

            float conceptActivationEach =
                    //(activationRate * conceptSrc.priElseZero()) / Util.clamp(concepts, 1, n); //TODO correct # of concepts fired in this batch
                    taskLinkPriSum / Util.clamp(concepts, 1, n); //TODO correct # of concepts fired in this batch


//            float balance = nar.termlinkBalance.floatValue();

//            float termlinkReverse = Math.max(EPSILON, taskLinkPriSum * balance / n);

//        //calculate exactly according to the size of the subset that are actually conceptualizable
//        float budgetedForward = concepts == 0 ? 0 :
//                Math.max(Prioritized.EPSILON, pri * (1f - balance) / concepts);

//            float termlinkForward = Math.max(EPSILON, taskLinkPriSum * (1 - balance) / n);


//            NumberX refund = new MutableFloat(0);

            Activator linking = d.deriver.linked;
//            Term srcTerm = src.term();

            NAR nar = d.nar;

            int j = n > 1 ? d.random.nextInt(n) : 0; //random starting position
            boolean inc = n <= 1 || d.random.nextBoolean();
            for (int i = 0; i < n; i++) {
                if (inc) {
                    if (++j == n) j = 0;
                } else {
                    if (--j == -1) j = n - 1;
                }

                Termed tgtTerm = get(j);

                if (tgtTerm instanceof Concept && ((Concept)tgtTerm).isDeleted())
                    setFast(j, tgtTerm = tgtTerm.term()); //concept was deleted, so revert back to term

                @Nullable Concept tgt =
                        linking.activate(tgtTerm, conceptActivationEach, nar);


                if (tgt != null) {

                    if (!(tgtTerm instanceof Concept) || tgtTerm!=tgt)
                        setFast(j, tgt); //cache concept for the entry

                    firedConcepts.add(tgt);

//                        linking.linkPlus(tgt, srcTerm, termlinkForward, refund);

//                        tgtTerm = tgt.term();

                }

//                } else {
//                    refund.add(termlinkForward);
//                }

//                linking.linkPlus(src, tgtTerm, termlinkReverse, refund);
            }

        }

        return n;
    }

}
