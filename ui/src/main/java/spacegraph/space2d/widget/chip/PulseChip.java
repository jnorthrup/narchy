package spacegraph.space2d.widget.chip;

import jcog.exe.Loop;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.port.IntPort;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.text.LabeledPane;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Boolean.TRUE;

/** generates pulses at an adjustable fixed rate */
public class PulseChip extends Gridding {

    final IntPort periodMS = new IntPort();

    //TODO phase

    final TypedPort<Boolean> pulse = new TypedPort(Boolean.class);

    float p;
    Loop loop;
    AtomicBoolean busy;

    public PulseChip() {
        super();
        set(new LabeledPane("period(MS)", periodMS), new LabeledPane("trigger", pulse));

        p = Float.NaN;
        loop = null;
        busy = new AtomicBoolean();
        periodMS.on(x -> {
            synchronized (this) {

                if (x instanceof Number) {
                    p = ((Number) x).floatValue();
                } else {
                    p = Float.NaN;
                }

                if (p > 0.5f) {
                    if (loop == null)
                        loop = Loop.of(this::tick);
                    loop.setPeriodMS(Math.round(p));
                }

                if ((p != p || p < 0.5f)) {
                    if (loop != null)
                        loop.stop();
                }

            }
        });
    }

    protected void tick() {
        if (busy.compareAndSet(false, true)) {
            try {
                pulse.out(TRUE);
            } finally {
                busy.set(false);
            }
        }
    }

    @Override
    protected void stopping() {
        super.stopping();
        if (loop!=null) {
            loop.stop();
            loop = null;
        }
    }

}
