package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.util.FloatFloatToFloatFunction;
import nars.NAR;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.TruthWave;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;


public class BeliefTableChart extends DurSurface<Stacking> implements MetaFrame.Menu {

    final Term term;
    private final TruthGrid beliefGrid, goalGrid;

    public final FloatRange durs = new FloatRange(32, 0.5f, 128f);

    /**
     * (if > 0): draw additional projection wave to show truthpolation values for a set of evenly spaced points on the visible range
     */

    private long start, end;

    /**
     * TODO use double not float for precision that may be lost
     *
     * @param y (freq,conf)->y
     */
    public static void renderWaveLine(long minT, long maxT, GL2 gl, TruthWave wave, FloatFloatToFloatFunction y, Colorize colorize) {


        gl.glLineWidth(1);
        gl.glBegin(GL2.GL_LINE_STRIP);

        float dt = ((maxT - minT)/((float)wave.capacity()));
        int dMargin = Math.round(dt/8);
        wave.forEach((freq, conf, start, end) -> {


            float x;


            if ((start >= minT) && (start <= maxT)) {
                x = xTime(minT, maxT, start + dMargin);
            } else {
                return;
            }

            colorize.colorize(gl, freq, conf);


            float Y = y.apply(freq, conf);
            gl.glVertex2f(x, Y);


            if (start == end)
                return;

            if ((end >= minT) && (end <= maxT)) {
                x = xTime(minT, maxT, end-dMargin);
                gl.glVertex2f(x, Y);
            }

        });

        gl.glEnd();
    }

    public interface Colorize {
        void colorize(GL2 gl, float f, float c);
    }

    class TruthGrid extends BitmapMatrixView implements BitmapMatrixView.ViewFunction2D {

        private final TruthWave projected, tasks;
        private final boolean beliefOrGoal;
        private final Colorize colorize;
        private static final float taskHeight = 0.04f;

        public TruthGrid(int tDiv, int fDiv, boolean beliefOrGoal) {
          super(tDiv, fDiv);
          projected = new TruthWave(tDiv);
          tasks = new TruthWave(tDiv);
          this.beliefOrGoal = beliefOrGoal;
                this.colorize = beliefOrGoal ?
                            (ggl, frq, cnf) -> {
                                float a = 0.65f + 0.2f * cnf;
                                ggl.glColor4f(0.25f + 0.75f * cnf, 0.1f * (1f - cnf), 0, a);
                            } :
                            (ggl, frq, cnf) -> {
                                float a = 0.65f + 0.2f * cnf;
                                ggl.glColor4f(0.1f * (1f - cnf), 0.25f + 0.75f * cnf, 0, a);
                            };
        }


        void update(Concept c) {
            BeliefTable table = (BeliefTable) c.table(beliefOrGoal ? BELIEF : GOAL);
            projected.project(table, start, end, w, nar);
            tasks.set(table, start, end);
        }

        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {
            super.paint(gl, surfaceRender);
            Draw.bounds(bounds, gl, ggl->{
                renderWaveLine( /*hack*/ start, end, ggl, projected, (f, c)->f, colorize);
                renderTasks(start, end, ggl, tasks, beliefOrGoal);
            });
        }

        @Override
        public int update(int x, int y) {
            return Draw.rgbInt(nar.random().nextFloat(), 0, 0);
        }



        private void renderTasks(long minT, long maxT, GL2 gl, TruthWave wave, boolean beliefOrGoal) {
//            float[] confMinMax = wave.range();
//            if (confMinMax[0] == confMinMax[1]) {
//                confMinMax[0] = 0;
//                confMinMax[1] = 1;
//            }
            wave.forEach((freq, conf, s, e) -> {

                boolean eternal = (s != s);
                if (eternal)
                    return;





                final float ph =
                        taskHeight;

//                    if (e >= minT && s <= maxT) {
//                        s = Math.max(minT, s);
//                        e = Math.min(maxT, e);
//                    }
                    float start = xTime(minT, maxT, s);
                float end = xTime(minT, maxT, e + 1);



                if (beliefOrGoal) {
                    gl.glColor4f(1f, 0, 0.25f, 0.15f + 0.75f * conf);
                } else {
                    gl.glColor4f(0f, 1, 0.25f, 0.15f + 0.75f * conf);
                }
                float y = freq - ph / 2;
                Draw.rect(gl,
                        start, y,
                        end-start, ph);


            });
        }


    }

    private static float xTime(long minT, long maxT, long o) {
        if (minT == maxT) return 0.5f;
        o = Util.clamp(o, minT, maxT);
        return ((float)(Math.min(maxT, Math.max(minT, o)) - minT)) / (maxT - minT);
    }

    public BeliefTableChart(Termed term, NAR n) {
        super(new Stacking(), n);
        this.term = term.term();
        the.add(beliefGrid = new TruthGrid(16, 10, true));
        the.add(goalGrid = new TruthGrid(16, 10, false));
    }

    @Override
    public void update() {

        Concept ccd = nar.conceptualizeDynamic(term/* lookup by term, not the termed which could be a dead instance */);
        if (ccd == null)
            return;

        long now = nar.time();
        int dur = nar.dur();

        //TODO different time modes
        double durs = this.durs.doubleValue();
        start = now - Math.round(durs * dur);
        end = now + Math.round(durs * dur);


            beliefGrid.update(ccd);

            goalGrid.update(ccd);

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + term + "]";
    }

    @Override
    protected void paintIt(GL2 ggl) {


    }

    @Override
    public Surface menu() {
        return new Gridding(
            new VectorLabel(term.toString()),
            new ObjectSurface(durs),
            PushButton.awesome("search-plus").click(() -> NARui.conceptWindow(term, nar))
        );
    }


}
