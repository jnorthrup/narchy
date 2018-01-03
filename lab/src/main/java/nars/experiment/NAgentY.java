package nars.experiment;

import jcog.learn.Agent;
import jcog.math.FloatSupplier;
import jcog.math.IntIntToObjectFunc;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.concept.ActionConcept;
import nars.concept.ScalarConcepts;
import nars.concept.SensorConcept;
import nars.control.AgentService;
import nars.task.ITask;
import nars.term.Term;
import nars.util.signal.Bitmap2D;
import nars.util.signal.CameraSensor;
import nars.util.signal.Signal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.LongSupplier;
import java.util.stream.Stream;

import static nars.Op.BELIEF;

abstract public class NAgentY extends NAgentX {

    final AgentService.AgentBuilder spec;

    private AgentService agent;

    public NAgentY(NAR nar, IntIntToObjectFunc<Agent> ctl) {
        this("", nar, ctl);
    }

    public NAgentY(String id, NAR nar, IntIntToObjectFunc<Agent> ctl) {
        super(id, nar);
        spec = new AgentService.AgentBuilder(ctl, () -> reward);
    }

    @Override
    protected void start(NAR nar) {

        spec.out(1, (ignored)->{ /* do nothing */ });

        agent = spec.get(nar);

        //agent.ons.off(); //HACK
        super.start(nar);
    }


    @Override
    public void addSensor(SensorConcept c) {
        super.addSensor(c);
        spec.in(c.signal::asFloat);
    }

    @Override
    protected <C extends Bitmap2D> CameraSensor<C> addCamera(CameraSensor<C> c) {
        c.pixels.forEach((CameraSensor<C>.PixelConcept p) -> {
            spec.in(() -> p.sensor.currentValue);
        });
        return super.addCamera(c);
    }

    @Override
    public ScalarConcepts senseNumber(FloatSupplier v, ScalarConcepts.ScalarEncoder model, Term... states) {
        ScalarConcepts sc = super.senseNumber(v, model, states);
        sc.forEach(x -> spec.in(x.signal::asFloat) );
        return sc;
    }

    public class RLActionConcept extends ActionConcept {

        private final Signal belief;

        protected RLActionConcept(@NotNull Term term, @NotNull NAR n) {
            super(term, n);
            this.belief = new Signal(BELIEF, n.freqResolution);
        }

        void believe(float freq) {
            long pStart = now;
            long pEnd = now + nar.dur();
            LongSupplier stamper = nar.time::nextStamp;
            belief.set(this, $.t(freq, nar.confDefault(BELIEF)), stamper, pStart, nar.dur(), nar);
        }

        public void enable() {
            believe(1);
        }


        @Override
        public Stream<ITask> update(long now, int dur, NAR nar) {
            return Stream.of(belief.get());
        }
    }

    @Override
    public @Nullable void actionToggle(@NotNull Term t, float latchValueUnused, @NotNull Runnable on, @NotNull Runnable off) {

        RLActionConcept m = new RLActionConcept(t, nar);

        spec.out(2, (i) -> {
            switch (i) {
                case 0:
                    off.run();
                    m.believe(0);
                    break;
                case 1:
                    on.run();
                    m.believe(1);
                    break;
            }
        });

        addAction(m);
    }
}
