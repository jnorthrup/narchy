package spacegraph.space2d.widget.chip;

import jcog.event.Off;
import jcog.signal.Tensor;
import spacegraph.audio.AudioSource;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.text.LabeledPane;

public class AudioCaptureChip extends Bordering {

    final Port out = new TypedPort<Tensor>(Tensor.class);
//    private final SignalSampling au;
    private Off on;

    public AudioCaptureChip() {
        AudioSource a = new AudioSource();
//        au = new SignalSampling(c->a.writeTo(c), a.sampleRate, 4f);
//
//        set(new SignalView(au));
        set(S, LabeledPane.awesome(out, "play"));

    }



//    private void update() {
//        out.out(au.wave);
//    }

    @Override
    protected void stopping() {
        on.close();
        on = null;
        super.stopping();
    }


}
