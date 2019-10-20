package spacegraph.test;

import jcog.math.Longerval;
import jcog.signal.Tensor;
import jcog.signal.buffer.CircularFloatBuffer;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.container.time.Timeline2DEvents;
import spacegraph.space2d.container.time.Timeline2DSequence;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.WaveBitmap;

import java.util.concurrent.ThreadLocalRandom;

public class Timeline2DTest {

    public static void main(String[] args) {
        var t = timeline2dTest();
        SpaceGraph.window(t, 800, 600);
    }

    protected static Surface timeline2dTest() {
        var range = 50;

        var t = new Timeline2D(0, range + 1);
        t.add(new Timeline2D.TimelineGrid());
        t.add(wave(range));
        t.add(events(range));
        //t.add(waveEvents());
        return t.withControls();
    }

    private static Surface wave(int range) {
        var samplesPerRange = 10;
        var c = new CircularFloatBuffer(range * samplesPerRange);
        for (var i = 0; i < range * samplesPerRange; i++) {
            c.write(new float[] {ThreadLocalRandom.current().nextFloat()});
        }
        var wv = new WaveBitmap(c, 500, 500);
        wv.height.set(0.25f);
        wv.alpha.set(0.5f);
        return wv;
    }

    protected static Timeline2DEvents<Timeline2D.SimpleEvent> events(int range) {
        var dummyModel = new Timeline2D.SimpleEventBuffer();
        var events = 30;
        for (var i = 0; i < events; i++) {
            var start = (long) (Math.random() * range);
            var length = (long) (Math.random() * 10) + 1;
            dummyModel.add(new Timeline2D.SimpleEvent("x" + i, start, start + length));
        }

        return new Timeline2DEvents<>(dummyModel,
                e ->
                        e.set(new Scale(new PushButton(e.id.toString()), 0.8f)), new Timeline2DEvents.LaneTimelineUpdater());
    }

    protected static Timeline2DEvents<Pair<Longerval,Tensor>> waveEvents() {

        var sampleRate = 100;
        var s = new Timeline2DSequence(sampleRate, 32);
        for (var i = 0; i < 32; i++) {
            var noise = new float[s.buffer.width];
            for (var j = 0; j < noise.length; j++)
                noise[j] = ((float) Math.random() - 0.5f)*2;
            s.buffer.set(noise);
        }

        return new Timeline2DEvents<>(s, e ->
            e.set(
                    new Widget(new WaveBitmap(e.id.getTwo(), 1, 128, 32))), new Timeline2DEvents.LaneTimelineUpdater()
        );
    }
}