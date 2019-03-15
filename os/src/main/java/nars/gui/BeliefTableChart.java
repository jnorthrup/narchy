package nars.gui;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES3;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.util.FloatFloatToFloatFunction;
import nars.NAR;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.TruthWave;
import spacegraph.space2d.Labeled;
import spacegraph.space2d.MenuSupplier;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.text.VectorLabel;
import spacegraph.video.Draw;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;


public class BeliefTableChart extends DurSurface<Stacking> implements Labeled, MenuSupplier {

    final Term term;
    private final TruthGrid beliefGrid, goalGrid;

    public final FloatRange rangeDurs = new FloatRange(32, 0.5f, 2048f);

    public final FloatRange projectDurs;
    /**
     * (if > 0): draw additional projection wave to show truthpolation values for a set of evenly spaced points on the visible range
     */

    private long start, end;

    @Override
    protected void paintIt(GL2 gl, SurfaceRender r) {
        gl.glColor3f(0, 0, 0);
        Draw.rect(bounds, gl);
    }

    /**
     * TODO use double not float for precision that may be lost
     *
     * @param y (freq,conf)->y
     */
    public void renderWaveLine(GL2 gl, TruthWave wave, FloatFloatToFloatFunction y, Colorize colorize) {

        gl.glLineWidth(4);
        gl.glBegin(GL.GL_LINE_STRIP);

        wave.forEach((freq, conf, start, end) -> {

            colorize.colorize(gl, freq, conf);

            float Y = y(y.apply(freq, conf));


            gl.glVertex2f(
                    end - start > 1 ? (xTime(start) + xTime(end)) / 2 : xTime(start),
                    Y);

            //int dMargin = 0; //(int)(end-start)/3;
            //gl.glVertex2f(xTime(start+dMargin), Y);

            //if (start != end) {
            //if ((end >= minT) && (end <= maxT)) {
            //gl.glVertex2f(xTime(end - dMargin), Y);
            //}
            //}

        });

        gl.glEnd();
    }

    public void renderWaveArea(long minT, long maxT, GL2 gl, TruthWave wave, FloatFloatToFloatFunction y, Colorize colorize) {

        gl.glBegin(GL2ES3.GL_QUADS);

        float midY = y.apply(0.5f, 0.5f);
//
//        gl.glVertex2f(xTime(minT, minT, maxT), midY);
//        gl.glVertex2f(xTime(minT, minT, maxT), midY);

        wave.forEach((freq, conf, start, end) -> {

            if (start > maxT || end < minT)
                return;

            colorize.colorize(gl, freq, conf);

            float Y = y.apply(freq, conf);

            float x1 = xTime(start);
            gl.glVertex2f(x1, midY);
            gl.glVertex2f(x1, Y);

            if (start == end)
                end = start + 1;

            float x2 = xTime(end);
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
        private final Colorize colorizeLine, colorizeFill;
        private static final float taskWidthMin = 0.005f;
        private static final float taskHeightMin = 0.04f;
        private final int projections;

        public TruthGrid(int projections, boolean beliefOrGoal) {
            super();
            this.projections = projections;
            projected = new TruthWave(projections);
            tasks = new TruthWave(256);
            this.beliefOrGoal = beliefOrGoal;
            this.colorizeLine = beliefOrGoal ?
                    (gl, f, c) -> {
                        float a = 0.8f + 0.1f * c;
                        float i = 0.1f + 0.9f * c;  //intensity
                        float j = 0.05f * (1 - c);
                        gl.glColor4f(i, j, j, a);
                    }
                    :
                    (gl, f, c) -> {
                        float a = 0.8f + 0.1f * c;
                        float i = 0.1f + 0.9f * c;  //intensity
                        float j = 0.05f * (1 - c);
                        gl.glColor4f(j, i, j, a);
                    };
            this.colorizeFill = beliefOrGoal ?
                    (gl, f, c) -> {
                        float a = 0.25f + 0.65f * c;
                        gl.glColor4f(1, 0, 0, a);
                    }
                    :
                    (gl, f, c) -> {
                        float a = 0.25f + 0.65f * c;
                        gl.glColor4f(0, 1, 0, a);
                    };

        }


        void update(Concept c) {
            BeliefTable table = (BeliefTable) c.table(beliefOrGoal ? BELIEF : GOAL);
            int dither = Math.max(1,
                    (int) Math.round(((double) (end - start)) / (projections)));
            long projStart = Util.round(start-dither/2, dither);
            long projEnd = Math.max(Util.round(end+dither/2, dither), Util.round(start + 1, dither));

            int dur = Math.round(nar.dur() * projectDurs.floatValue());
            projected.project(table, projStart, projEnd, projections, term, dur, nar);
            tasks.set(table, start, end);
        }



        @Override
        protected void paint(GL2 _gl, SurfaceRender surfaceRender) {

            float mid = xTime(nar.time());

            Draw.bounds(bounds, _gl, gl -> {

                //render present line
                gl.glColor3f(0.5f, 0.5f, 0.5f);
                gl.glLineWidth(2f);
                Draw.line(mid, 0, mid, 1, gl);

                renderTasks(gl, tasks, colorizeFill);

                FloatFloatToFloatFunction FtoY = (f, c) -> f;
                if (beliefOrGoal) {
                    renderWaveLine(gl, projected, FtoY, colorizeLine);
                } else {
                    renderWaveLine(gl, projected, FtoY, colorizeLine);
                }
            });
        }


        private void renderTasks(GL2 gl, TruthWave wave, Colorize colorize) {

            final float ph =
                    Math.max(taskHeightMin, nar.freqResolution.floatValue());

            wave.forEach((freq, conf, s, e) -> {

                boolean eternal = (s != s);
                if (eternal)
                    return;

                float start = xTime(s);
                if (start > 1)
                    return;

                float end = xTime(e + 1);
                if (end < 0)
                    return;

                colorize.colorize(gl, freq, conf);

                float yBottom = BeliefTableChart.this.y(freq) - ph / 2;
                float width = end - start;
                if (width < taskWidthMin) {
                    //point-like
                    float w = taskWidthMin; //visible width
                    float center = (end + start) / 2;
//                    float yMid = freq;
//                    float thick = taskWidthMin/32;
                    //Draw.rectFrame(center, yMid, w, thick, ph, gl);
                    Draw.rectStroke(center - w / 2, yBottom, w, ph, gl);
                } else {
                    //solid
                    Draw.rect(start, yBottom, width, ph, gl);
                }

            });
        }


    }

    private float y(float y) {
        //TODO map to space within some margin
        return y;
    }

    private float xTime(long o) {
        o = Util.clampSafe(o, start, end);
        return (float) (((double) (o - start)) / (end - start));
    }


    public BeliefTableChart(Termed term, NAR n) {
        super(new Stacking(), n);

        this.projectDurs = new FloatRange(1, 0, 32);

        this.term = term.term();
        the.add(beliefGrid = new TruthGrid(16, true));
        the.add(goalGrid = new TruthGrid(16, false));
    }

    @Override
    public void doUpdate() {

        Concept ccd = nar.conceptualizeDynamic(term/* lookup by target, not the termed which could be a dead instance */);
        if (ccd == null)
            return;

        long now = nar.time();

        //TODO different time modes
        int narDur = nar.dur();
        double visDurs = this.rangeDurs.doubleValue();
        long start = now - Math.round(visDurs * narDur);
        long end = now + Math.round(visDurs * narDur);
        if (end == start) end = start + 1;
        this.start = start;
        this.end = end;


        beliefGrid.update(ccd);

        goalGrid.update(ccd);

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + term + ']';
    }


    @Override
    public Surface label() {
        return new VectorLabel(term.toString());
    }

    @Override
    public Surface menu() {
        return Splitting.row(
            ObjectSurface.the(this),
            0.9f,
            PushButton.awesome("search-plus").click(() -> NARui.conceptWindow(term, nar))
        );
    }


}
