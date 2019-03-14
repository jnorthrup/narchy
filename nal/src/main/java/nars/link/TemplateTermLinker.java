package nars.link;

import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtils;
import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Img;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * default general-purpose termlink template impl. for compound terms
 * contains a fixed set of subterm components that can be target-linked with.
 * this implementation stores the conceptualizable terms in the lower part of a list
 * so that they can be accessed quickly as separate from non-conceptualizables.
 *
 * also caches Concept references until a concept becomes deleted
 */
public final class TemplateTermLinker extends FasterList<Term> implements TermLinker {


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


    @Override
    public Stream<? extends Term> targets() {
        return stream();
    }

    public static TermLinker of(Term term) {
        return of(term, 2);
    }

    public static TermLinker of(Term term, Term... additional) {
        return of(term, 2, additional);
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
                return new TemplateTermLinker(tc.toArray(Term[]::new));
        }

        return NullLinker;
    }

//    protected TemplateTermLinker(TemplateTermLinker base) {
//        super(base.items.length, base.items);
//        concepts = base.concepts;
//    }

    private TemplateTermLinker(Term[] terms) {
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
                    Term x = items[b];
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



    private static boolean conceptualizable(Term x) {
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

//
//    /**
//     * spread a tasklink to subconcepts of the concept that owns this linker
//     */
//    @Override
//    public void link(TaskLink tasklink, Task task, Derivation d) {
//
//        Collection<Concept> subConcepts = subConcepts(d);
//
//        if (subConcepts.isEmpty())
//            return;
//
//        NAR nar = d.nar;
//
//
//        float pri =
//                task.priElseZero();
//                //tasklink.priPunc(task.punc());
//                //task.priElseZero() * tasklink.priElseZero();
//                //Math.min(task.priElseZero(), tasklink.priElseZero());
//
//        float pEach =
//                //TODO abstract priority transfer function here
//                pri; //no division
//                //pri/subConcepts.size(); //division
//
//        for (Concept c : subConcepts) {
//            TaskLink.link(
//                    TaskLink.tasklink(c.term(), task, pEach),
//                    nar);
//
//        }
//
//    }

    @Override
    public void sample(Random rng, Function<? super Term, SampleReaction> each) {
        Termed termed = get(rng);
        each.apply(termed.term());
    }


//    private Collection<Concept> subConcepts(Derivation d) {
//
//        int n = concepts;
//        if (n == 0)
//            return Collections.emptyList();
//
//        Collection<Concept> firedConcepts = new FasterList<>(32);
//
//        n = Math.min(n, d.deriver.tasklinkSpread.intValue());
//
//
//
//        //taskPriSum *= concept.priElseZero();
////        pri = Math.max(pri, ScalarValue.EPSILON);
//
//        NAR nar = d.nar;
////        AbstractConceptIndex koncepts = (AbstractConceptIndex) nar.concepts;
////        PriBuffer<Concept> linking = ((BufferedBag<Term,Concept,?>) koncepts.active).buffer; //HACK
//
////        float conceptActivationEach =
////                //(activationRate * conceptSrc.priElseZero()) / Util.clamp(concepts, 1, n); //TODO correct # of concepts fired in this batch
////                pri / Math.max(1, n); //TODO correct # of concepts fired in this batch
////
////        conceptActivationEach *= koncepts.activationRate.floatValue(); //HACK
//
////            float balance = nar.termlinkBalance.floatValue();
//
////            float termlinkReverse = Math.max(EPSILON, taskLinkPriSum * balance / n);
//
////        //calculate exactly according to the size of the subset that are actually conceptualizable
////        float budgetedForward = concepts == 0 ? 0 :
////                Math.max(Prioritized.EPSILON, pri * (1f - balance) / concepts);
//
////            float termlinkForward = Math.max(EPSILON, taskLinkPriSum * (1 - balance) / n);
//
//
////            NumberX refund = new MutableFloat(0);
//
////            Term srcTerm = src.target();
//
//
//        Random rng = d.random;
//        int j; boolean inc;
//        if (n > 1) {
//            int r = rng.nextInt(); //using only one RNG call
//            inc = r >= 0;
//            j = (r & 0b01111111111111111111111111111111) % n;
//
////            j = rng.nextInt(n); //random starting position
////            inc = rng.nextBoolean();
//        } else {
//            j = 0;
//            inc = true;
//        }
//
////        OverflowDistributor<Concept> overflow = n > 1 ? new OverflowDistributor<>() : null;
//
//        for (int i = 0; i < n; i++) {
//            if (inc) {
//                if (++j == n) j = 0;
//            } else {
//                if (--j == -1) j = n - 1;
//            }
//
//            Termed tgtTerm = get(j);
//
//            Concept tgt = tgtTerm instanceof Concept ?
//                    ((Concept)tgtTerm)
//                    :
//                    nar.conceptualize(tgtTerm);
//
//
//            if (tgt !=null) {
////                linking.put(tgt, conceptActivationEach, overflow);
//
//                firedConcepts.add(tgt);
//            }
//
//        }
//
////        if(overflow!=null)
////            linking.put(overflow, rng);
//
//        return firedConcepts;
//    }

}
