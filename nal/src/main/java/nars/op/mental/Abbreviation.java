package nars.op.mental;


import jcog.math.MutableIntRange;
import jcog.data.atomic.AtomicFloat;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.leak.TaskLeak;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

/**
 * compound<->dynamic atom abbreviation.
 *
 * @param S serial term type
 */
public class Abbreviation/*<S extends Term>*/ {


    /**
     * whether to use a (strong, proxying) alias atom concept
     */


    private static final Logger logger = LoggerFactory.getLogger(Abbreviation.class);
    private static final AtomicInteger currentTermSerial = new AtomicInteger(0);
    /**
     * generated abbreviation belief's confidence
     */
    public final Number abbreviationConfidence;
    /**
     * accepted volume range, inclusive
     */
    public final MutableIntRange volume;
    private final TaskLeak bag;
    private final String termPrefix;


    public Abbreviation(NAR nar, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
        super();
        bag = new TaskLeak(capacity, selectionRate, nar) {


            @Override
            public float value() {
                return 1f;
            }

            @Override
            protected boolean preFilter(Task next) {
                int vol = next.volume();
                if (vol < volume.lo() || vol > volume.hi())
                    return false;

                return super.preFilter(next);
            }

            @Override
            protected float leak(Task t) {
                Concept abbreviable = t.concept(nar, true);
                if (abbreviable != null &&
                        !(abbreviable instanceof PermanentConcept) &&
                        !(abbreviable instanceof AliasConcept) &&
                        abbreviable.term().equals(t.term()) && /* identical to its conceptualize */
                        abbreviable.meta(Abbreviation.class.getName()) == null) {

                    Term abbreviation;
                    if ((abbreviation = abbreviate(t, abbreviable, super.nar)) != null) {
                        abbreviable.meta(Abbreviation.class.getName(), abbreviation);
                        return 1;
                    }
                }

                return 0;
            }


        };


        this.termPrefix = termPrefix;
        this.abbreviationConfidence =
                new AtomicFloat(nar.confDefault(BELIEF));


        volume = new MutableIntRange(volMin, volMax);
    }


    protected String nextSerialTerm() {

        return termPrefix + Integer.toString(currentTermSerial.incrementAndGet(), 36);


    }


    protected Term abbreviate(Task t, Concept abbrConcept, NAR nar) {

        Term abbreviated = abbrConcept.term();

        if (abbrConcept != null && !(abbrConcept instanceof AliasConcept) && !(abbrConcept instanceof PermanentConcept)) {


            Term aliasTerm = Atomic.the(nextSerialTerm());
            AliasConcept a1 = new AliasConcept(aliasTerm, abbrConcept);
            a1.meta(Abbreviation.class.getName(), a1);
            //nar.on(a1);

            nar.concepts.set(a1.term(), a1);
            nar.concepts.set(abbreviated, a1); //redirect reference from the original concept to the alias


            Term abbreviation = newRelation(abbreviated, aliasTerm);
            if (abbreviation == null)
                return null;

            float pri = t.priElseZero();
            Task abbreviationTask = Task.tryTask(abbreviation, BELIEF,
                    $.t(1f, abbreviationConfidence.floatValue()),
                    (te, tr) -> {

                        NALTask ta = new NALTask(
                                te, BELIEF, tr,
                                nar.time(), ETERNAL, ETERNAL,
                                nar.evidence()
                        );


                        ta.log("Abbreviate");
                        ta.pri(pri);

                        return ta;
                    });


            if (abbreviationTask != null) {
                nar.input(abbreviationTask);
                logger.info("{}", abbreviationTask.term());

                return aliasTerm;
            }

        }

        return null;
    }


    @Nullable
    Term newRelation(Term abbreviated, Term id) {
        return $.sim(abbreviated, id);
    }


}
