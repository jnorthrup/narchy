package spacegraph.space2d.widget;

import com.jogamp.opengl.GL2;
import jcog.signal.buffer.CircularFloatBuffer;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.space2d.container.time.Timeline2D;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meter.WaveView;
import spacegraph.video.Draw;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public class Timeline2DTest {

    public static void main(String[] args) {

        int range = 50;

        Timeline2D t = new Timeline2D(0, range + 1);
        t.add(new TimelineGrid());
//        t.add(wave(range));
        t.add(events(range));

        SpaceGraph.window(t.withControls(), 800, 600);
    }

    public static class TimelineGrid extends Surface implements Timeline2D.TimelineRenderable {

        int THICKNESS = 2;
        //int DIVISIONS = 10; //TODO

        double start, end;
        private BiConsumer<GL2,SurfaceRender> paintGrid;

        @Override
        public void setTime(double tStart, double tEnd) {
            this.start = tStart; this.end = tEnd;
            paintGrid = null; //invalidate
        }

        @Override
        protected void compile(SurfaceRender r) {
            if (paintGrid == null) {
                double range = end-start;
                double interval = interval(range);
                double phase = start % interval;
                double iMax = (range / interval) + 0.5f;
                paintGrid = (gl,sr)->{
                    float H = h(), W = w(), LEFT = x(), BOTTOM = y();
                    gl.glColor4f(0.3f,0.3f,0.3f,0.9f);

                    gl.glLineWidth(THICKNESS);
                    double x = start - phase;
                    for (int i = 0; i <= iMax; i++) {
                        float xx = Timeline2D.x(start, end, LEFT, W, x);
                        Draw.line(xx, BOTTOM, xx, BOTTOM + H, gl);
                        x += interval;
                    }
                };
            }
            r.on(paintGrid);
        }

        /** TODO refine */
        static double interval(double range) {
            double x = Math.pow(10.0, Math.floor(Math.log10(range)));
            if (range / (x / 2.0) >= 10)
                return x / 2.0;
            else if (range / (x / 5.0) >= 10)
                return x / 5.0;
            else
                return x / 10.0;
        }
    }

    private static Surface wave(int range) {
        int samplesPerRange = 10;
        CircularFloatBuffer c = new CircularFloatBuffer(range * samplesPerRange);
        for (int i = 0; i < range * samplesPerRange; i++) {
            c.write(new float[] {ThreadLocalRandom.current().nextFloat()});
        }
        return new WaveView(c,500,500);
    }

    protected static Timeline2D.Timeline2DEvents<Timeline2D.SimpleEvent> events(int range) {
        Timeline2D.SimpleTimelineModel dummyModel = new Timeline2D.SimpleTimelineModel();
        int events = 30;
        for (int i = 0; i < events; i++) {
            long start = (long) (Math.random() * range);
            long length = (long) (Math.random() * 10) + 1;
            dummyModel.add(new Timeline2D.SimpleEvent("x" + i, start, start + length));
        }

        return new Timeline2D.Timeline2DEvents<>(dummyModel,
                (Graph2D.NodeVis<Timeline2D.SimpleEvent> e) ->
                        e.set(new Scale(new PushButton(e.id.toString()), 0.8f)));
    }
}