package spacegraph.space2d.widget.chip;

import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import spacegraph.audio.synth.string.StringSynth;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Bordering;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.port.BoolPort;
import spacegraph.space2d.widget.port.FloatPort;
import spacegraph.space2d.widget.port.TypedPort;
import spacegraph.space2d.widget.slider.FloatSlider;

import java.util.function.Consumer;

import static spacegraph.space2d.widget.text.LabeledPane.awesome;

public class StringSynthChip extends Bordering {
    final StringSynth h = new StringSynth();


    //                AtomicReference<CircularFloatBuffer> targetBuffer = new AtomicReference(null);
//    final PushButton e = new PushButton("p");

    //                e.click(()->{
//                    Synth
//                })
    final TypedPort<ObjectIntPair<float[]>> output = new TypedPort(ObjectIntPair.class, (Consumer<ObjectIntPair<float[]>>) (ObjectIntPair<float[]> mixTarget) -> h.next(mixTarget.getOne()));

    final FloatPort pitch = new FloatPort();
    final BoolPort pluck = new BoolPort ();

    public StringSynthChip() {
        super();

        h.amp(1f);

//        pitch.on(p -> {
//            h.keyPressed();
//        });
        pluck.on(b->{
            if (b) {
                h.keyPress(10, true);
            } else {
                h.keyRelease(10, true);
            }
        });

        set(N, new Gridding(awesome(pitch, "music"), awesome(pluck, "compress")));

        Surface o = awesome(output, "play");

        set(o);


        set(S, new Gridding(new PushButton("x").clicked(()->{
            h.keyPress(10, false);
        }),/*e, */new FloatSlider(h.amp(), 0, 8f).on(h::amp)));
    }
}
