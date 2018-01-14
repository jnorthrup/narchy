package spacegraph.widget.windo;

import com.google.common.collect.Iterables;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.list.FasterList;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.input.FingerMove;
import spacegraph.input.Fingering;
import spacegraph.layout.Stacking;
import spacegraph.render.Draw;

import java.util.List;

import static com.jogamp.opengl.GL.*;
import static spacegraph.widget.windo.Windo.DragEdit.*;

public class MultiTrack extends Stacking {

    static class TracksState {
        public double start, end;

        /** configures the current visible range */
        public TracksState range(double s, double e) {
            this.start = s;
            this.end = e;
            return this;
        }
    }

    public final TracksState state = new TracksState().range(0, 10);

    /** track icon - generally these are draggable (horizontally only, and across tracks if allowed)
     * and manage their own appearance */
    abstract static class Tricon extends Windo {
        public double start, end;

        public Tricon(double start, double end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean fingerable(DragEdit d) {
            switch (d) {
                case MOVE:
                case RESIZE_W:
                case RESIZE_E:
                    return true;
            }
            return false;
        }

        @Override
        protected Fingering fingeringMove() {
            return new FingerMove(this, true, false); //x-axis only
        }

        @Override
        protected void paintBack(GL2 gl) {
            //super.paintBack(gl);
            //gl.glColor3f(0.8f, 0.5f, 0f);
            Draw.colorHash(gl, this);
            Draw.rect(gl, x(), y(), w(), h());
        }
    }

    abstract static class Track extends Stacking {
        transient private float x, width=0, y, height = 0;
        transient private TracksState tracks;

        abstract Iterable<Tricon> icons(double start, double end);

        /** updates telling what is visible */
        public void update(TracksState s, float x, float width, float y, float height) {
            this.y = y;
            this.x = x;
            this.tracks = s;
            this.width = width;
            this.height = height;
            layout();
        }

        @Override
        public void doLayout(int dtMS) {
            if (tracks == null)
                return;
            double s = tracks.start;
            double e = tracks.end;
            for (Tricon i : icons(s, e)) {
                float x1 = (float) ((i.start - s)/(e-s))*width+x;
                float x2 = (float) ((i.end - s)/(e-s))*width+x;
                i.pos(x1, y, x2, y+height);
                i.layout();
            }
            //super.doLayout(dtMS);
        }
    }

    @Override
    protected void paintBelow(GL2 gl) {

        super.paintBelow(gl);
//
//        //gl.glColorMask(false,false,false,false);                           // Set Color Mask to TRUE, TRUE, TRUE, TRUE
//
//        gl.glEnable(GL_STENCIL_TEST);                      // Enable Stencil Buffer For "marking" The Floor
//        gl.glClear(GL_STENCIL_BUFFER_BIT);
//        gl.glStencilFunc(GL_ALWAYS, 1, 1);                     // Always Passes, 1 Bit Plane, 1 As Mask
//        gl.glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);              // We Set The Stencil Buffer To 1 Where We Draw Any Polygon
//        // Keep If Test Fails, Keep If Test Passes But Buffer Test Fails
//        // Replace If Test Passes
//        //gl.glDisable(GL_DEPTH_TEST);                       // Disable Depth Testing
//
//        // We Only Want To Mark It In The Stencil Buffer
//
        Draw.rect(gl, bounds);
//
//
//        //gl.glEnable(GL_DEPTH_TEST);                        // Enable Depth Testing
//        gl.glColorMask(true,true,true,true);                           // Set Color Mask to TRUE, TRUE, TRUE, TRUE
//        gl.glStencilFunc(GL_EQUAL, 1, 1);                      // We Draw Only Where The Stencil Is 1
//        gl.glStencilMask(0x00);
//
//        // (I.E. Where The Floor Was Drawn)
//        gl.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);                 // Don't Change The Stencil Buffer
    }

//    @Override
//    protected void paintAbove(GL2 gl) {
//        gl.glDisable(GL_STENCIL_TEST);
//
//
//        super.paintAbove(gl);
//    }

    @Override
    public void doLayout(int dtMS) {

        float y = 0;
        float w = w();
        for (Surface t : children) {
            float th = h()/4; //HACK
            if (t instanceof Track) {
                ((Track)t).update(state, 0, w, y, th);
            }
            y += th;
        }
        super.doLayout(dtMS);
    }

    static class DummyTrack extends Track {

        final List<Tricon> icons = new FasterList();
        public DummyTrack(double s, double e, int n/* density etc */) {
            double t = s;
            for (int i = 0; i < n; i++) {
                double u = t + (e-s)/(2*n) + Math.random()*1.5+0.2;
                Tricon tr = new Tricon(t, u) {

                };
                icons.add(tr);
                add(tr);
                t = u + (e-s)/(2*n);
            }
        }

        @Override
        Iterable<Tricon> icons(double start, double end) {
            return Iterables.filter(icons, (i)->i.start < end || i.end > start);
        }

    }

    public static void main(String[] args) {
        //demo
        MultiTrack t = new MultiTrack() {
//            long when = 0;
//            @Override
//            protected void prePaint(int dtMS) {
//
//                when += dtMS;
//                state.range( 2 - Math.sin(when/200f)*2, 12 - 3*Math.cos(when/120f));
//                layout();
//
//                super.prePaint(dtMS);
//            }
        };
        t.add(new DummyTrack(1, 4, 4));
        t.add(new DummyTrack(2, 5, 5));

        t.layout();
        SpaceGraph.window(t, 1200, 800);


    }
}
