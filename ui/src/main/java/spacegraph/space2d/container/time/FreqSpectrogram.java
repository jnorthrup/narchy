package spacegraph.space2d.container.time;

import jcog.event.Off;
import jcog.signal.Tensor;
import jcog.signal.wave1d.FreqDomain;
import jcog.signal.wave1d.SignalInput;
import spacegraph.space2d.widget.meter.Spectrogram;
import spacegraph.video.Draw;

class FreqSpectrogram extends Spectrogram {
    private final SignalInput in;
    final FreqDomain freqDomain;
    private Off off;


    public FreqSpectrogram(SignalInput in, boolean leftOrDown, int T, int N) {
        super(leftOrDown, T, N);
        this.in = in;
        this.freqDomain = new FreqDomain(in, N, T);
    }

    @Override
    protected void starting() {
        super.starting();
        off = this.in.wave.on(raw-> {
            Tensor fft = freqDomain.next(raw);
            next(i -> {
                float v = fft.getAt(i);
                return Draw.colorHSB(0.3f * (1 - v), 0.9f, v);
            });
        });
    }

    @Override
    protected void stopping() {
        off.close();
        off = null;
        super.stopping();
    }
}
