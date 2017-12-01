package nars.util.signal;

import jcog.Util;
import jcog.learn.LivePredictor;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.control.CauseChannel;
import nars.control.DurService;
import nars.task.ITask;
import nars.task.SignalTask;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableFloat;

import static nars.Op.BELIEF;

/**
 * numeric prediction support
 */
public class BeliefPredict {

    final MutableFloat conf = new MutableFloat(0.75f);

    final DurService on;
    private final CauseChannel<ITask> predict;


    static FloatSupplier frequency(Term c, int dDur, NAR nar) {
        return () -> {
            Truth t = nar.truth(c, BELIEF, nar.time() + dDur * nar.dur());
            if (t == null)
                return 0.5f; //HACK
            else
                return t.freq();
        };
    }


    public BeliefPredict(Termed[] inConcepts, int history, Termed[] outConcepts, NAR nar) {

        this.predict = nar.newCauseChannel(this);

        FloatSupplier[] ins = Util.map(c -> frequency(c.term(), -1, nar),
                new FloatSupplier[inConcepts.length], inConcepts);

        FloatSupplier[] outs = Util.map(c -> frequency(c.term(), 0, nar),
                new FloatSupplier[outConcepts.length], outConcepts);

        LivePredictor.LSTMPredictor model = new LivePredictor.LSTMPredictor(0.1f, 2);
        LivePredictor p = new LivePredictor(model, new LivePredictor.HistoryFramer(ins, history, outs));

        this.on = DurService.on(nar, () -> {
            double[] predFreq = p.next();
            for (int i = 0; i < predFreq.length; i++) {
                double d = predFreq[i];
                Term o = outConcepts[i].term();
                long next = nar.time() + 1 * nar.time.dur();
                predict.input(
                    new SignalTask(o, BELIEF, $.t((float) predFreq[0], conf.floatValue()),
                            next, next, nar.time.nextStamp()
                    )
                );
            }
        });
    }
}
