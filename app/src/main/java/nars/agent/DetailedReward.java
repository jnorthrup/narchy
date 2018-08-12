package nars.agent;

import jcog.math.FloatAveraged;
import jcog.math.FloatNormalizer;
import jcog.math.FloatPolarNormalizer;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.concept.sensor.FilteredScalar;
import nars.concept.sensor.Signal;
import nars.table.BeliefTables;
import nars.term.Term;

import static jcog.Util.compose;
import static nars.Op.GOAL;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

public class DetailedReward extends Reward {

    public final FilteredScalar concept;

    public DetailedReward(Term id, FloatSupplier r, NAgent a) {
        super(a, r);

        NAR nar = a.nar();

        concept = new FilteredScalar( () -> reward,

                //(prev,next) -> next==next ? $.t(Util.unitize(next), Math.max(nar.confMin.floatValue(),  Math.abs(next-0.5f)*2f * nar.confDefault(BELIEF))) : null,
                truther(),

                nar,

                pair(id,
                        new FloatNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE)),


                pair($.func("chronic", id), compose(
                        new FloatNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE),
                        new FloatAveraged(0.02f)
                )),


                pair($.func("acute", id), compose(
                        new FloatAveraged(0.1f, false),
                        new FloatPolarNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE_FAST)
                ))
        );

        {
             //TODO add these to On/Off
            agent.//alwaysWant/*Eternally*/
                    alwaysWantEternally
                    (concept.filter.get(0).term, nar.confDefault(GOAL) * 0.75f );
            agent.//alwaysWant/*Eternally*/
                    alwaysWantEternally(concept.filter.get(1).term, nar.confDefault(GOAL));
            agent.//alwaysWant/*Eternally*/
                    alwaysWantEternally(concept.filter.get(2).term, nar.confDefault(GOAL) * 0.5f); //acute
            for (Signal x : concept.filter) {
                ((BeliefTables) x.beliefs()).eternal.setCapacity(0); //HACK this should be an Empty table

                //should normally be able to create these beliefs but if you want to filter more broadly:
                //((DefaultBeliefTable)x.goals()).temporal.setCapacity(0); //HACK this should be an Empty table

            }
        }

    }

    @Override
    public Term term() {
        return concept.term;
    }

    @Override
    public void update(long prev, long now, long next) {
        super.update(prev, now, next);

        NAR nar = nar();

        concept.update(prev, now, next, nar);

//            Truth happynowT = nar.beliefTruth(concept, last, now);
//            float happynow = happynowT != null ? (happynowT.freq() - 0.5f) * 2f : 0;
//            nar.emotion.happy(/* motivation.floatValue() * */ dexterity(last, now) * happynow /* /nar.confDefault(GOAL) */);

    }
}
