package nars.agent;

import com.google.common.collect.Iterators;
import jcog.math.FloatAveraged;
import jcog.math.FloatNormalizer;
import jcog.math.FloatPolarNormalizer;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.concept.sensor.FilteredScalar;
import nars.concept.sensor.Signal;
import nars.control.channel.CauseChannel;
import nars.table.BeliefTables;
import nars.table.eternal.EternalTable;
import nars.task.ITask;
import nars.term.Term;

import java.util.Iterator;

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

                pair(id, (x)->x),
                        //new FloatNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE)),


                pair($.func("chronic", id), compose(
                        new FloatAveraged(0.02f),
                        new FloatNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE)
                )),


                pair($.func("acute", id), compose(
                        new FloatAveraged(0.1f, false),
                        new FloatPolarNormalizer().relax(Param.HAPPINESS_RE_SENSITIZATION_RATE_FAST)
                ))
        ) {
            @Override
            protected CauseChannel<ITask> newChannel(NAR nar) {
                return in;
            }
        };
        concept.attn.parent(attn);

        {
             //TODO add these to On/Off
            agent.//alwaysWant/*Eternally*/
                    alwaysWantEternally
                    (concept.components.get(0).term, nar.confDefault(GOAL) * 0.75f );
            agent.//alwaysWant/*Eternally*/
                    alwaysWantEternally(concept.components.get(1).term, nar.confDefault(GOAL));
            agent.//alwaysWant/*Eternally*/
                    alwaysWantEternally(concept.components.get(2).term, nar.confDefault(GOAL) * 0.5f); //acute
            for (Signal x : concept.components) {
                EternalTable ete = ((BeliefTables)x.beliefs()).tableFirst(EternalTable.class);
                if (ete!=null)
                    ete.setTaskCapacity(0); //HACK this should be an Empty table

                //should normally be able to create these beliefs but if you want to filter more broadly:
                //((DefaultBeliefTable)x.goals()).temporal.setCapacity(0); //HACK this should be an Empty table

            }
        }

    }

    @Override
    public Iterator<Signal> iterator() {
        return Iterators.transform(concept.components.iterator(), x-> x);
    }

    @Override
    public Term term() {
        return concept.term;
    }

    @Override
    protected void updateReward(long prev, long now, long next) {
        concept.update(prev, now, next, nar());
    }
}
