package nars.link;

import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.pri.OverflowDistributor;
import jcog.pri.Prioritized;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.util.ArrayUtils;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.attention.Activator;
import nars.concept.Concept;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Img;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * default general-purpose termlink template impl. for compound terms
 * contains a fixed set of subterm components that can be term-linked with.
 * this implementation stores the conceptualizable terms in the lower part of a list
 * so that they can be accessed quickly as separate from non-conceptualizables.
 *
 * also caches Concept references until a concept becomes deleted
 */
public class TemplateTermLinker extends FasterList<Termed> implements TermLinker {


    //    /**
//     * how fast activation spreads from source concept to template target concepts
//     */
//    static final float activationRate =
//            1f;
//            //0.5f;




    /**
     * number of termlinks which are actually conceptualizable; sorted on construction
     */
    private final byte concepts;

    /**
     * create a batch of tasklinks, sharing common seed data
     */
    static void link(TaskLink tasklink, float pri, List<Concept> targets, @Nullable OverflowDistributor<Bag> overflow) {
        assert(!targets.isEmpty());

//        float pEach = Math.max(ScalarValue.EPSILON,
//                priTransferred / nTargets
//        );
        float pEach =
                //TODO abstract priority transfer function here
                //pri; //no division
                pri/targets.size(); //division


        for (Concept c : targets) {

            TaskLink tl =
                    tasklink.clone(pEach);
            if (tl!=null) {
                TaskLink.link(tl, c.tasklinks(), overflow);
            }

        }


    }


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

    protected TemplateTermLinker(TemplateTermLinker base) {
        super(base.items.length, base.items);
        concepts = base.concepts;
    }

    private TemplateTermLinker(Termed[] terms) {
        super(terms.length, terms);

        int lastConcept;
        if (size > 1) {
            int n = terms.length;
            MetalBitSet c = MetalBitSet.bits(n);
            lastConcept = 0;
            for (int i = 0; i < n; i++) {
                if (conceptualizable(terms[i])) {
                    c.set(i);
                    lastConcept++;
                }
            }
            if (lastConcept > 0 && lastConcept < n) {
                ArrayUtils.quickSort(0, n, c.indexComparatorReverse(), (a, b) -> {
                    Termed x = items[b];
                    items[b] = items[a];
                    items[a] = x;
                    c.swap(a,b);
                });
            }

        } else {
            lastConcept = conceptualizable(terms[0]) ? 1 : 0;
        }

        assert (lastConcept < 127);
        concepts = (byte) (lastConcept);
    }

    @Override
    protected Termed[] newArray(int newCapacity) {
        return new Termed[newCapacity];
    }


    /**
     * recurses into subterms
     */
    private static void add(Term x, Set<Term> tc, int depth, Term root, int maxDepth) {

        if (x instanceof Img)
            return;

        if (depth > 0) {
            if (!tc.add(x))
                return;
        }


        Op xo = x.op();
        boolean done = xo.atomic || !xo.conceptualizable;
        if (done)
            return;

        maxDepth += deeper(depth, root, x);
        if (++depth >= maxDepth)
            return;


        int nextDepth = depth, nextMaxDepth = maxDepth;

        x.subterms().forEachWith((sub,tgt) -> add(sub.unneg(), tgt, nextDepth, root, nextMaxDepth), tc);

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

    private static boolean conceptualizable(Termed x) {
        //return x.equals(x.normalize()) && x.op().conceptualizable;
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
        if (d.firedTasks.isEmpty())
            return;

        if (conceptualizeAndTermLink(a, d) > 0) {

            List<Concept> firedConcepts = d.firedConcepts.list;

            //default all to all exhausive matrix insertion
            //TODO configurable "termlink target concept x tasklink matrix" linking pattern: density, etc

            if (!firedConcepts.isEmpty()) {

                List<Task> taskedLinked = d.firedTasks.list;
                int n = taskedLinked.size();
                if (n > 0) {

                    OverflowDistributor<Bag> overflow = n > 1 ? new OverflowDistributor<>() : null;

                    NAR nar = d.nar;

                    float taskLinkRate = nar.taskLinkActivation.floatValue();

                    for (Task t : taskedLinked) {

                        //contextual compartmentalization: generify=true -> dont spam propagating tasklinks with the temporal specifics
                        TaskLink tt = TaskLink.tasklink(t, true, true, 0 /* pri will be set in each clone */, nar);

                        link(tt, t.priElseZero() * taskLinkRate, firedConcepts, overflow);

                        if (overflow != null) {
                            overflow.shuffle(d.random).redistribute((b, p) -> b.putAsync(tt.clone(p)));
                            overflow.clear();
                        }

                    }
                }

            }
        }


    }

    @Override
    public void sample(Random rng, Function<? super Term, SampleReaction> each) {
        each.apply(get(rng).term());
    }


    private int conceptualizeAndTermLink(Activate concept, Derivation d) {


        int n = concepts;
        if (n == 0)
            return 0;

        ArrayHashSet<Concept> firedConcepts = d.firedConcepts;
        firedConcepts.clear();


        n = Math.min(n, d.deriver.tasklinkSpread.intValue());

        float taskPriSum = Math.max(ScalarValue.EPSILON, (float) (((FasterList<Task>) (d.firedTasks.list))
                .sumOfFloat(Prioritized::priElseZero))* concept.priElseZero()) ;

        float conceptActivationEach =
                //(activationRate * conceptSrc.priElseZero()) / Util.clamp(concepts, 1, n); //TODO correct # of concepts fired in this batch
                taskPriSum / Math.max(1, n); //TODO correct # of concepts fired in this batch


//            float balance = nar.termlinkBalance.floatValue();

//            float termlinkReverse = Math.max(EPSILON, taskLinkPriSum * balance / n);

//        //calculate exactly according to the size of the subset that are actually conceptualizable
//        float budgetedForward = concepts == 0 ? 0 :
//                Math.max(Prioritized.EPSILON, pri * (1f - balance) / concepts);

//            float termlinkForward = Math.max(EPSILON, taskLinkPriSum * (1 - balance) / n);


//            NumberX refund = new MutableFloat(0);

//            Term srcTerm = src.term();

        NAR nar = d.nar;
        Activator linking = nar.attn.activating;

        Random rng = d.random;
        int j; boolean inc;
        if (n > 1) {
            j = rng.nextInt(n); //random starting position
            inc = rng.nextBoolean();
        } else {
            j = 0;
            inc = true;
        }

        OverflowDistributor<Concept> overflow = n > 1 ? new OverflowDistributor<>() : null;

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
                    linking.activate(tgtTerm, conceptActivationEach, nar, overflow);


            if (tgt != null) {

                if (!(tgtTerm instanceof Concept))
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

        if(overflow!=null)
            linking.activate(overflow, rng);

        return n;
    }

}
