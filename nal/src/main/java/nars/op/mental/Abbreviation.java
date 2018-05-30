package nars.op.mental;


import jcog.math.MutableIntRange;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.leak.TaskLeak;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static nars.Op.BELIEF;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;

/**
 * compound<->dynamic atom abbreviation.
 *
 * @param S serial term type
 */
public class Abbreviation/*<S extends Term>*/ {


    /**
     * generated abbreviation belief's confidence
     */
    public final MutableFloat abbreviationConfidence;
    private final TaskLeak bag;

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


    public Abbreviation(NAR nar, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
        super();
        bag = new TaskLeak(capacity, selectionRate, nar)
       {




        
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
               Concept abbreviable  = t.concept(nar, true);
               if (abbreviable!=null &&
                       !(abbreviable instanceof PermanentConcept) &&
                       abbreviable.term().equals(t.term()) && /* identical to its conceptualize */
                       abbreviable.meta("abbr") == null) {

                   Term abbreviation;
                   if ((abbreviation = abbreviate(t, abbreviable, super.nar))!=null) {
                       abbreviable.meta("abbr", abbreviation);
                       return 1;
                   }
               }

               return 0;
            }

            








        };
        

        this.termPrefix = termPrefix;
        this.abbreviationConfidence =
                new MutableFloat(nar.confDefault(BELIEF));
        
        
        volume = new MutableIntRange(volMin, volMax);
    }



































    protected String nextSerialTerm() {

        return termPrefix + Integer.toString(currentTermSerial.incrementAndGet(), 36);















        
    }






















    protected Term abbreviate(Task t, Concept abbreviable, NAR nar) {

        Term abbreviated = t.term();
        Concept abbrConcept = t.concept(nar, false);

        if (abbrConcept != null && !(abbrConcept instanceof AliasConcept) && !(abbrConcept instanceof PermanentConcept)) {

            

            



            Term aliasTerm = Atomic.the(nextSerialTerm());
                AliasConcept a1 = new AliasConcept(aliasTerm, abbrConcept, nar);
                nar.on(a1);





            Compound abbreviation = newRelation(abbreviated, aliasTerm);
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
                            ) {
                                @Override
                                public void run(NAR nar) {
                                    super.run(nar);
                                    @Nullable Concept tc = concept(nar, false);
                                    if (tc!=null)
                                        tc.meta("abbr", tc); 
                                }
                            };


                            
                            ta.log("Abbreviate"); 
                            ta.pri(pri); 

                            return ta;
                        });












                






















            if (abbreviationTask!=null) {
                nar.input(abbreviationTask);
                logger.info("{}", abbreviationTask.term());

                return aliasTerm;
            }

        }

        return null;
    }









    @Nullable
    Compound newRelation(Term abbreviated, Term id) {
        return compoundOrNull(
                $.sim(abbreviated, id)
                
        );
        
        

        
        
        
    }


















































}
