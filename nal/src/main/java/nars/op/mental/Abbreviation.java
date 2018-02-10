package nars.op.mental;


import jcog.bag.impl.ConcurrentArrayBag;
import jcog.math.FloatRange;
import jcog.math.MutableIntRange;
import jcog.pri.PLink;
import jcog.pri.Prioritized;
import jcog.pri.op.PriMerge;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.leak.DtLeak;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.control.DurService;
import nars.control.TaskService;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static nars.Op.BELIEF;
import static nars.term.Terms.compoundOrNull;

/**
 * compound<->dynamic atom abbreviation.
 *
 * @param S serial term type
 */
public class Abbreviation/*<S extends Term>*/ extends TaskService {


    /**
     * generated abbreviation belief's confidence
     */
    @NotNull
    public final MutableFloat abbreviationConfidence;
    private final DtLeak<Compound, PLink<Compound>> bag;

    /**
     * whether to use a (strong, proxying) alias atom concept
     */


    private static final Logger logger = LoggerFactory.getLogger(Abbreviation.class);

    private static final AtomicInteger currentTermSerial = new AtomicInteger(0);

    private final String termPrefix;

    /**
     * accepted volume range, inclusive
     */
    public final MutableIntRange volume;
    private DurService onDur;


    public Abbreviation(@NotNull NAR nar, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
        super(nar);
        bag = new DtLeak<>(new ConcurrentArrayBag<Compound, PLink<Compound>>(PriMerge.plus, capacity) {
            @Nullable
            @Override
            public Compound key(PLink<Compound> l) {
                return l.get();
            }
        }, new FloatRange(selectionRate)) {

            @Override
            protected Random random() {
                return nar.random();
            }

            @Override
            protected float receive(PLink<Compound> b) {
                return abbreviate(b.get(), b, nar) ? 1f : 0f;
            }
        };
        bag.setCapacity(capacity);

        this.termPrefix = termPrefix;
        this.abbreviationConfidence =
                new MutableFloat(nar.confDefault(BELIEF));
        //new MutableFloat(1f - nar.truthResolution.floatValue());
        //new MutableFloat(nar.confidenceDefault(Symbols.BELIEF));
        volume = new MutableIntRange(volMin, volMax);
    }

    @Override
    protected void start(NAR nar) {
        super.start(nar);

        onDur = DurService.on(nar, this::update);
    }

    @Override
    public synchronized void off() {
        onDur.off();
        super.off();
    }

    protected void update(NAR nar) {
        bag.commit(nar.time(), nar.dur(), 1f);
    }



    @Override
    public void clear() {
        bag.clear();
    }

    @Override
    public void accept(NAR nar, Task task) {

        Term taskTerm = task.term();
        if ((!(taskTerm instanceof Compound)) || taskTerm.vars() > 0)
            return;

        Prioritized b = task;

        input(b, bag.bag::put, (Compound) taskTerm, 1f, nar);
    }

    private void input( Prioritized b, Consumer<PLink<Compound>> each, Compound t, float scale, NAR nar) {
        int vol = t.volume();
        if (vol < volume.lo())
            return;

        if (vol <= volume.hi()) {
            if (t.conceptual().equals(t) /* identical to its conceptualize */) {
                Concept abbreviable = nar.concept(t);
                if ((abbreviable != null) &&
                        !(abbreviable instanceof PermanentConcept) &&
                                abbreviable.meta("abbr") == null) {

                    each.accept(new PLink<>(t, b.priElseZero()));
                }
            }
        } else {
            //recursiely try subterms of a temporal or exceedingly large concept
            //budget with a proportion of this compound relative to their volume contribution
            float subScale = 1f / (1 + t.subs());
            t.forEach(x -> {
                if (x.subs() > 0)
                    input(b, each, ((Compound) x), subScale, nar);
            });
        }
    }


    @NotNull
    protected String newSerialTerm() {

        return termPrefix + Integer.toString(currentTermSerial.incrementAndGet(), 36);

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
    }

//    private float scoreIfExceeds(Budget task, float min) {
//        float s = or(task.priIfFiniteElseZero(), task.qua());
//        if (s >= min) {
//            s *= abbreviationProbability.floatValue();
//            if (s >= min) {
//
//                //higher probability for terms nearer the thresh. smaller and larger get less chance
////                s *= 1f - unitize(
////                        Math.abs(task.volume() - volThresh) /
////                                (threshFalloffRate) );
//
//                if (s >= min)
//                    return s;
//            }
//        }
//        return -1;
//    }


    //private boolean createRelation = false;


    protected boolean abbreviate(@NotNull Compound abbreviated, @NotNull Prioritized b, NAR nar) {

        @Nullable Concept abbrConcept = nar.concept(abbreviated);
        if (abbrConcept != null && !(abbrConcept instanceof AliasConcept) && !(abbrConcept instanceof PermanentConcept)) {

            final boolean[] succ = {false};

            abbrConcept.meta("abbr", (ac) -> {

                Term abbreviatedTerm =abbreviated.term();

                AliasConcept a1 = new AliasConcept(newSerialTerm(), abbrConcept, nar);

                nar.on(a1);
                nar.terms.set(abbreviated.term(), a1); //set the abbreviated term to resolve to the abbreviation
                if (!abbreviatedTerm.equals(abbreviated.term()))
                    nar.terms.set(abbreviatedTerm, a1); //set the abbreviated term to resolve to the abbreviation

//                Compound abbreviation = newRelation(abbreviated, id);
//                if (abbreviation == null)
//                    return null; //maybe could happen
//
//                Task abbreviationTask = Task.tryTask(abbreviation, BELIEF, $.t(1f, abbreviationConfidence.floatValue()),
//                        (te, tr) -> {
//
//                            NALTask ta = new NALTask(
//                                    te, BELIEF, tr,
//                                    nar.time(), ETERNAL, ETERNAL,
//                                    new long[]{nar.time.nextStamp()}
//                            );
//
//
//                            ta.meta(Abbreviation.class, new Term[]{abbreviatedTerm, aliasTerm.term()});
//                            ta.log("Abbreviate"); //, abbreviatedTerm, aliasTerm
//                            ta.setPri(b);
//
//                            nar.runLater(()->nar.input(ta));
                logger.info("{} => {}", a1, abbreviatedTerm);
//

//
//                            return ta;
//
//                            //if (abbreviation != null) {
//
//                            //logger.info("{} <=== {}", alias, abbreviatedTerm);
//
//                        });


                //abbreviationTask.priority();
//        if (srcCopy == null) {
//            delete();
//        } else {
//            float p = srcCopy.priSafe(-1);
//            if (p < 0) {
//                delete();
//            } else {
//                setPriority(p);
//            }
//        }
//
//        return this;


                succ[0] = true;
                return a1;

            });

            return succ[0];

        }

        return false;
    }

//        final NLPGen nlpGen = new NLPGen();
//
//        @Nullable private String newCanonicalTerm(@NotNull Termed abbreviated) {
//            if (abbreviated.volume() < 12)
//                return "\"" + nlpGen.toString(abbreviated.term(), 1f, 1f, Tense.Eternal) + "\"";
//            return null;
//        }

    @Nullable
    Compound newRelation(@NotNull Compound abbreviated, @NotNull String id) {
        return compoundOrNull(
                $.sim(abbreviated, Atomic.the(id))
                //$.equi
        );
        //(Compound) $.equi(abbreviated.term(), id);
        //(Compound) $.secte(abbreviated.term(), id);

        //old 1.6 pattern:
        //Operation compound = Operation.make(
        //    Product.make(termArray(termAbbreviating)), abbreviate);*/
    }


//    public static class AbbreviationAlias extends Abbreviation {
//        public AbbreviationAlias(@NotNull NAR n, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
//            super(n, termPrefix, volMin, volMax, selectionRate, capacity);
//        }
//
//        protected void abbreviate(@NotNull CompoundConcept abbreviated, Budget b) {
//
//            String id = newSerialTerm();
//
//            Compound abbreviatedTerm = abbreviated.term();
//
//            AliasConcept alias = new AliasConcept(id, abbreviated, nar);
//            nar.concepts.set(alias, alias);
//            nar.concepts.set(abbreviatedTerm, alias); //override the abbreviated on lookups
//            logger.info("{} <=== {}", alias, abbreviatedTerm);
//
//        }
//    }
//


//    public static class AbbreviationTask extends NALTask {
//
//        @NotNull
//        private final Compound abbreviated;
//        @NotNull
//        private final Term alias;
//
//        public AbbreviationTask(Compound term, byte punc, Truth truth, long creation, long start, long end, long[] evidence, @NotNull Compound abbreviated, @NotNull Termed alias) {
//            super(term, punc, truth, creation, start, end, evidence);
//            this.abbreviated = abbreviated;
//            this.alias = alias.term();
//        }
//
//
////        @Override
////        public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, @NotNull NAR nar) {
////
////            super.feedback(delta, deltaConfidence, deltaSatisfaction, nar);
////
////
////            if (deltaConfidence == deltaConfidence /* wasn't deleted, even for questions */ && !isDeleted()) {
////                @Nullable Concept c = concept(nar);
////                c.put(Abbreviation.class, alias);
////
////            }
////        }
//    }
}
