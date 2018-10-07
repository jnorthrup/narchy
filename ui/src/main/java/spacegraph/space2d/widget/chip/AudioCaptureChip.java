package spacegraph.space2d.widget.chip;

import jcog.event.Off;
import jcog.signal.Tensor;
import spacegraph.audio.AudioSource;
import spacegraph.audio.WaveCapture;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.widget.port.Port;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.text.LabeledPane;

public class AudioCaptureChip extends Bordering {

    final Port out = new TypedPort<Tensor>(Tensor.class);
    private final WaveCapture au;
    private Off on;

    public AudioCaptureChip() {
        au = new WaveCapture(new AudioSource(20), 4f);
        au.setFPS(20f);

        set(au.view());
        set(S, LabeledPane.awesome(out, "play"));

    }

    @Override
    protected void starting() {
        super.starting();
        on = au.wave.on(this::update);
    }

    private void update() {
        out.out(au.wave);
    }

    @Override
    protected void stopping() {
        on.off();
        on = null;
        super.stopping();
    }


}
