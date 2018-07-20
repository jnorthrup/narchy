package nars.agent;

import jcog.math.FloatExpMovingAverage;
import jcog.math.FloatNormalizer;
import jcog.math.FloatPolarNormalizer;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.concept.sensor.FilteredScalar;
import nars.table.DefaultBeliefTable;
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
                        new FloatExpMovingAverage(0.02f)
                )),


                pair($.func("acute", id), compose(
                        new FloatExpMovingAverage(0.1f, false),
                        new FloatPolarNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE_FAST)
                ))
        );

        {
             //TODO add these to On/Off
            agent.alwaysWantEternally(concept.filter[0].term, nar.confDefault(GOAL));
            agent.alwaysWantEternally(concept.filter[1].term, nar.confDefault(GOAL) /* * 0.5f */); //chronic
            agent.alwaysWantEternally(concept.filter[2].term, nar.confDefault(GOAL) * 0.5f); //acute
            for (FilteredScalar.Filter x : concept.filter) {
                ((DefaultBeliefTable) x.beliefs()).eternal.setCapacity(0); //HACK this should be an Empty table

                //should normally be able to create these beliefs but if you want to filter more broadly:
                //((DefaultBeliefTable)x.goals()).temporal.setCapacity(0); //HACK this should be an Empty table

            }
        }

    }



    @Override
    public void run() {
        super.run();

        NAR nar = nar();

        concept.update(agent.last, agent.now(), nar);

//            Truth happynowT = nar.beliefTruth(concept, last, now);
//            float happynow = happynowT != null ? (happynowT.freq() - 0.5f) * 2f : 0;
//            nar.emotion.happy(/* motivation.floatValue() * */ dexterity(last, now) * happynow /* /nar.confDefault(GOAL) */);

    }
}
