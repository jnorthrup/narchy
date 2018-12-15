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
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;


public class BeliefTableChart extends DurSurface<Stacking> implements MetaFrame.Menu {

    final Term term;
    private final TruthGrid beliefGrid, goalGrid;

    public final FloatRange durs = new FloatRange(32, 0.5f, 2048f);

    /**
     * (if > 0): draw additional projection wave to show truthpolation values for a set of evenly spaced points on the visible range
     */

    private long start, end;

    @Override
    protected void paintIt(GL2 gl, SurfaceRender r) {
        gl.glColor3f(0,0,0);
        Draw.rect(bounds, gl);
    }

    /**
     * TODO use double not float for precision that may be lost
     *
     * @param y (freq,conf)->y
     */
    public static void renderWaveLine(long minT, long maxT, GL2 gl, TruthWave wave, FloatFloatToFloatFunction y, Colorize colorize) {

        gl.glLineWidth(4);
        gl.glBegin(GL2.GL_LINE_STRIP);

        float dt = ((maxT - minT)/((float)wave.capacity()));
        int dMargin = Math.round(dt/8);
        wave.forEach((freq, conf, start, end) -> {

            colorize.colorize(gl, freq, conf);

            float Y = y.apply(freq, conf);
            gl.glVertex2f(xTime(start+dMargin, minT, maxT), Y);

            if (start != end) {
                if ((end >= minT) && (end <= maxT)) {
                    gl.glVertex2f(xTime(end - dMargin, minT, maxT), Y);
                }
            }

        });

        gl.glEnd();
    }
    public static void renderWaveArea(long minT, long maxT, GL2 gl, TruthWave wave, FloatFloatToFloatFunction y, Colorize colorize) {

        gl.glBegin(GL2.GL_QUADS);

        float midY = y.apply(0.5f, 0.5f);
//
//        gl.glVertex2f(xTime(minT, minT, maxT), midY);
//        gl.glVertex2f(xTime(minT, minT, maxT), midY);

        wave.forEach((freq, conf, start, end) -> {

            if (start > maxT || end < minT)
                return;

            colorize.colorize(gl, freq, conf);

            float Y = y.apply(freq, conf);

            float x1 = xTime(start, minT, maxT);
            gl.glVertex2f(x1, midY);
            gl.glVertex2f(x1, Y);

            if (start == end)
                end = start+1;

            float x2 = xTime(end, minT, maxT);
            gl.glVertex2f(x2, Y);
            gl.glVertex2f(x2, midY);

        });

//        gl.glVertex2f(xTime(maxT, minT, maxT), midY);
//        gl.glVertex2f(xTime(maxT, minT, maxT), midY);

        gl.glEnd();
    }

    public interface Colorize {
        void colorize(GL2 gl, float f, float c);
    }

    class TruthGrid extends Surface {

        private final TruthWave projected, tasks;
        private final boolean beliefOrGoal;
        private final Colorize colorize;
        private static final float taskWidthMin = 0.01f;
        private static final float taskHeightMin = 0.04f;
        private final int projections;

        public TruthGrid(int projections, boolean beliefOrGoal) {
          super();
          this.projections = projections;
          projected = new TruthWave(projections);
          tasks = new TruthWave(256);
          this.beliefOrGoal = beliefOrGoal;
          this.colorize = beliefOrGoal ?
                            (gl, frq, cnf) -> {
                                float a = 0.3f + 0.6f * cnf;
                                gl.glColor4f(0.5f + 0.5f * cnf, 0, 0.1f, a);
                            } :
                            (gl, frq, cnf) -> {
                                float a = 0.3f + 0.6f * cnf;
                                gl.glColor4f(0, 0.5f + 0.5f * cnf, 0.1f, a);
                            };
        }


        void update(Concept c) {
            BeliefTable table = (BeliefTable) c.table(beliefOrGoal ? BELIEF : GOAL);
            projected.project(table, start, end, projections, term, nar);
            tasks.set(table, start, end);
        }


        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {


            Draw.bounds(bounds, gl, ggl->{

                if (beliefOrGoal) {
                    renderWaveLine
                    /*renderWaveArea*/( start, end, ggl, projected, (f, c)->f, colorize);

                } else {
                    renderWaveLine( start, end, ggl, projected, (f, c)->f, colorize);
                }
                renderTasks(start, end, ggl, tasks, colorize);
            });
        }




        private void renderTasks(long minT, long maxT, GL2 gl, TruthWave wave, Colorize colorize) {
//            float[] confMinMax = wave.range();
//            if (confMinMax[0] == confMinMax[1]) {
//                confMinMax[0] = 0;
//                confMinMax[1] = 1;
//            }
            final float ph =
                    Math.max(taskHeightMin, nar.freqResolution.floatValue());

            wave.forEach((freq, conf, s, e) -> {

                boolean eternal = (s != s);
                if (eternal)
                    return;


//                    if (e >= minT && s <= maxT) {
//                        s = Math.max(minT, s);
//                        e = Math.min(maxT, e);
//                    }
                    float start = xTime(s, minT, maxT);
                float end = xTime(e + 1, minT, maxT);
                if (start == end)
                    return; //squashed against the edge or out of bounds completely

                colorize.colorize(gl, freq, conf);

                float y = freq - ph / 2;
                Draw.rect(start, y, Math.max(taskWidthMin,end-start), ph, gl);

            });
        }


    }

    private static float xTime(long o, long minT, long maxT) {
        if (minT == maxT) return 0.5f;
        o = Util.clamp(o, minT, maxT);
        return ((float)(Math.min(maxT, Math.max(minT, o)) - minT)) / (maxT - minT);
    }

    public BeliefTableChart(Termed term, NAR n) {
        super(new Stacking(), n);
        this.term = term.term();
        the.add(beliefGrid = new TruthGrid(16, true));
        the.add(goalGrid = new TruthGrid(16, false));
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
    public Surface menu() {
        return new Gridding(
            new VectorLabel(term.toString()),
            new ObjectSurface(durs),
            PushButton.awesome("search-plus").click(() -> NARui.conceptWindow(term, nar))
        );
    }


}
