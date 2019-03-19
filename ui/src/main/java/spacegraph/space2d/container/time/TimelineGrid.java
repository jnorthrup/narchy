package spacegraph.space2d.container.time;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.video.Draw;

import java.util.function.BiConsumer;

public class TimelineGrid extends Surface implements Timeline2D.TimelineRenderable {

    int THICKNESS = 2;
    //int DIVISIONS = 10; //TODO

    double start, end;
    private BiConsumer<GL2, SurfaceRender> paintGrid;

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
