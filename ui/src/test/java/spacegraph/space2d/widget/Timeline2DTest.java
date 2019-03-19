package spacegraph.space2d.widget;

import jcog.signal.buffer.CircularFloatBuffer;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.container.time.Timeline2DEvents;
import spacegraph.space2d.container.time.TimelineGrid;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.WaveView;

import java.util.concurrent.ThreadLocalRandom;

public class Timeline2DTest {

    public static void main(String[] args) {

        int range = 50;

        Timeline2D t = new Timeline2D(0, range + 1);
        t.add(new TimelineGrid());
        t.add(wave(range));
        t.add(events(range));

        SpaceGraph.window(t.withControls(), 800, 600);
    }

    private static Surface wave(int range) {
        int samplesPerRange = 10;
        CircularFloatBuffer c = new CircularFloatBuffer(range * samplesPerRange);
        for (int i = 0; i < range * samplesPerRange; i++) {
            c.write(new float[] {ThreadLocalRandom.current().nextFloat()});
        }
        WaveView wv = new WaveView(c, 500, 500);
        wv.height.set(0.25f);
        wv.alpha.set(0.5f);
        return wv;
    }

    protected static Timeline2DEvents<Timeline2D.SimpleEvent> events(int range) {
        Timeline2D.SimpleTimelineModel dummyModel = new Timeline2D.SimpleTimelineModel();
        int events = 30;
        for (int i = 0; i < events; i++) {
            long start = (long) (Math.random() * range);
            long length = (long) (Math.random() * 10) + 1;
            dummyModel.add(new Timeline2D.SimpleEvent("x" + i, start, start + length));
        }

        return new Timeline2DEvents<>(dummyModel,
                (Graph2D.NodeVis<Timeline2D.SimpleEvent> e) ->
                        e.set(new Scale(new PushButton(e.id.toString()), 0.8f)));
    }
}