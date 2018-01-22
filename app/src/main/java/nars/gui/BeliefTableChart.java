package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.tree.rtree.rect.RectFloat2D;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.DurService;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.truth.TruthWave;
import nars.truth.Truthed;
import org.jetbrains.annotations.Nullable;
import spacegraph.SurfaceBase;
import spacegraph.render.Draw;
import spacegraph.widget.text.Label;
import spacegraph.widget.windo.Widget;

import java.util.function.BiFunction;

import static java.lang.Math.PI;
import static nars.time.Tense.ETERNAL;


public class BeliefTableChart extends Widget {

    public static final float baseTaskSize = 0.04f;
    public static final float CROSSHAIR_THICK = 3;
    final Term term;
    final TruthWave beliefs;
    final TruthWave beliefProj;
    final TruthWave goals;
    final TruthWave goalProj;

    private DurService on;


    TaskConcept cc; //cached concept
    float cp; //cached priority
    private int dur; //cached dur
    private long now; //cached time
    private String termString; //cached string

    private final NAR nar;

    static final float angleSpeed = 0.5f;

    private BiFunction<Long, long[], long[]> rangeControl = (now, range) -> range; //default: no change
    float beliefTheta, goalTheta;

    private final Label label;

    /**
     * (if > 0): draw additional projection wave to show truthpolation values for a set of evenly spaced points on the visible range
     */
    int projections = 32;

    private final boolean showTaskLinks = true;
    @Deprecated
    private final boolean showEternal = true;

    long[] range;

    public BeliefTableChart(NAR n, Termed term) {
        this(n, term, null);
    }

    public BeliefTableChart(NAR n, Termed term, long[] range) {
        super();
        this.term = term.term();
        this.nar = n;

        this.range = range;

        label = new Label(this.term.toString());
        label.textColor.a(0.5f);
        //label.scale(0.5f, 0.5f);

        children(label);

        beliefs = new TruthWave(0);
        beliefProj = new TruthWave(0);
        goals = new TruthWave(0);
        goalProj = new TruthWave(0);

        beliefTheta = goalTheta = 0;

    }

    @Override
    public void start(@Nullable SurfaceBase parent) {
        synchronized (this) {
            super.start(parent);
            on = DurService.on(nar, this::update);
        }
    }

    @Override
    public void stop() {
        synchronized (this) {
            if (on != null) {
                on.off();
                on = null;
            }
            super.stop();
        }
    }

//    @Override
//    public Surface hide() {
//        on.off();
//        return this;
//    }

    public void update(NAR nar) {



            long now = this.now = nar.time();
            int dur = this.dur = nar.dur();

            cc = (TaskConcept) nar.concept(term/* lookup by term, not the termed which could be a dead instance */);

            long minT, maxT;
            if (range != null) {
                minT = range[0];
                maxT = range[1];
            } else {
                minT = Long.MIN_VALUE;
                maxT = Long.MAX_VALUE;
            }

            if (cc != null) {
                cp = 1f; /*nar.pri(cc);*/

                beliefs.set(cc.beliefs(), now, dur, nar, minT, maxT);
                long nowEnd = now + dur / 2;
                long nowStart = now - dur / 2;
                beliefs.current = nar.beliefTruth(cc, nowStart, nowEnd);
                goals.set(cc.goals(), now, dur, nar, minT, maxT);
                goals.current = nar.goalTruth(cc, nowStart, nowEnd);

                if (projections > 0 && minT != maxT) {
                    beliefProj.project(cc, true, minT, maxT, dur, projections, nar);
                    goalProj.project(cc, false, minT, maxT, dur, projections, nar);
                }

            } else {
                cp = 0;
                beliefs.clear();
                beliefs.current = null;
                goals.clear();
                goals.current = null;
                beliefProj.clear();
                goalProj.clear();
            }


    }


    protected void draw(Termed tt, Concept cc, GL2 gl, long minT, long maxT) {


        TruthWave beliefs = this.beliefs;
        //if (!beliefs.isEmpty()) {
        renderTable(cc, minT, maxT, now, gl, beliefs, true);
        //}

        TruthWave goals = this.goals;
        //if (!goals.isEmpty()) {
        renderTable(cc, minT, maxT, now, gl, goals, false);
        //}

        if (showTaskLinks) {
            gl.glLineWidth(1f);
            float nowX = xTime(minT, maxT, now);
            cc.tasklinks().forEach(tl -> {
                if (tl != null) {
                    Task x = tl.get();
                    if ((x != null) && (x.isBeliefOrGoal())) {
                        long o = x.start();
                        float tlx = o == ETERNAL ? nowX : xTime(minT, maxT, o);
                        if (tlx > 0 && tlx < 1) {
                            float tly = x.freq();
                            float ii = 0.3f + 0.7f * x.conf();
                            gl.glColor4f(ii / 2f, 0, ii, 0.5f + tl.pri() * 0.5f);
                            float w = 0.05f;
                            float h = 0.05f;
                            Draw.rectStroke(gl, tlx - w / 2, tly - h / 2, w, h);
                        }
                    }
                }
            });
        }

        //gl.glLineWidth(1f);
        //gl.glColor4f(1f, 1f, 1f, 0.3f);
        //Draw.strokeRect(gl, 0, 0, gew, geh);
        //Draw.strokeRect(gl, gew, 0, tew, teh);

    }

    @Override
    protected void paintWidget(GL2 ggl, RectFloat2D bounds) {

        /*if (!redraw.compareAndSet(true, false)) {
            return;
        }*/

        //swapBuffers();


        //clear
        //clear(1f /*0.5f*/);

        Draw.bounds(ggl, bounds, (gl) -> {


            long minT, maxT;

            if (range != null) {
                minT = range[0];
                maxT = range[1];
            } else {

                //compute bounds from combined min/max of beliefs and goals so they align correctly
                minT = Long.MAX_VALUE;
                maxT = Long.MIN_VALUE;


                TruthWave b = this.beliefs;
                if (!b.isEmpty()) {
                    long start = b.start();
                    if (start != ETERNAL) {
                        minT = Math.min(start, minT);
                        maxT = Math.max(b.end(), maxT);
                    }
                }
                TruthWave g = this.goals;
                if (!g.isEmpty()) {

                    long start = g.start();
                    if (start != ETERNAL) {
                        minT = Math.min(start, minT);
                        maxT = Math.max(g.end(), maxT);
                    }


                }

                long[] newRange = rangeControl.apply(now, new long[]{minT, maxT});
                minT = newRange[0];
                maxT = newRange[1];
            }


            gl.glColor3f(0, 0, 0); //background
            Draw.rect(gl, 0, 0, 1, 1);

            gl.glLineWidth(1f);
            gl.glColor3f(0.5f, 0.5f, 0.5f); //border
            Draw.rectStroke(gl, 0, 0, 1, 1);

            //String currentTermString = termString;
            if (cc != null) {
                draw(term, cc, gl, minT, maxT);
                termString = cc.toString();
            } else {
                termString = term.toString();
            }
            label.text(termString);
        });

        //        gl.glColor4f(0.75f, 0.75f, 0.75f, 0.8f + 0.2f * cp);
//        gl.glLineWidth(1);
//        Draw.text(gl, termString, (1f/termString.length()) * (0.5f + 0.25f * cp), 1 / 2f, 1 / 2f, 0);
    }


//    final static ColorMatrix beliefColors = new ColorMatrix(8, 8, (f, c) ->
//            new Color(0.6f + 0.38f * c, 0.2f, 1f, 0.39f + 0.6f * c)
//    );
//    final static ColorMatrix goalColors = new ColorMatrix(8, 8, (f, c) ->
//            new Color(0.2f + 0.4f * c, 1f, 0.2f, 0.39f + 0.6f * c)
//    );

    public static void drawCrossHair(GL2 gl, float x, float gew, Truthed truth, double theta) {
        gl.glLineWidth(CROSSHAIR_THICK);

        float conf = truth.conf();


        float bcy = truth.freq();

        //ge.strokeLine(bcx, border, bcx, geh - border);
        //ge.strokeLine(border, bcy, gew - border, bcy);
        double r = gew * (0.5f + 0.5f * conf);


        double dx0 = Math.cos(theta) * r;
        double dy0 = Math.sin(theta) * r;
        Draw.line(gl, dx0 + x, dy0 + bcy, -dx0 + x, -dy0 + bcy);

        double hpi = PI / 2.0;
        double dx1 = Math.cos(theta + hpi) * r;
        double dy1 = Math.sin(theta + hpi) * r;
        Draw.line(gl, dx1 + x, dy1 + bcy, -dx1 + x, -dy1 + bcy);
    }


    private void renderTable(Concept c, long minT, long maxT, long now, GL2 gl, TruthWave wave, boolean beliefOrGoal) {

        if (c == null)
            return;

        float nowX = xTime(minT, maxT, now);

        //Present axis line
        if ((now <= maxT) && (now >= minT)) {

            gl.glColor4f(1f, 1f, 1f, 0.5f);
            Draw.line(gl, nowX, 0, nowX, 1);

            //float nowLineWidth = 0.005f;
            //Draw.rect(gl, nowX - nowLineWidth / 2f, 0, nowLineWidth, 1);
        }

        /** drawn "pixel" dimensions*/

        renderWave(nowX, minT, maxT, gl, wave, beliefOrGoal);

        //draw projections
        if (projections > 0 && minT != maxT) {
            TruthWave pwave = beliefOrGoal ? beliefProj : goalProj;
            renderWaveLine(nowX, minT, maxT, gl, pwave, beliefOrGoal);
        }

        Truth bc = wave.current;
        if (bc != null) {
            float theta;
            float dTheta = (bc.expectation() - 0.5f) * angleSpeed;
            if (beliefOrGoal) {
                this.beliefTheta += dTheta;
                theta = beliefTheta;
                gl.glColor4f(1f, 0f, 0, 0.2f + 0.8f * bc.conf());
            } else {
                this.goalTheta += dTheta;
                theta = goalTheta;
                gl.glColor4f(0f, 1f, 0, 0.2f + 0.8f * bc.conf());
            }
            float chSize = 0.1f;
            drawCrossHair(gl, nowX, chSize, bc, theta);
        }

    }

    private void renderWave(float nowX, long minT, long maxT, GL2 gl, TruthWave wave, boolean beliefOrGoal) {
        float[] confMinMax = wave.range(1);
        if (confMinMax[0] == confMinMax[1]) {
            confMinMax[0] = 0;
            confMinMax[1] = 1;
        }
        wave.forEach((freq, conf, s, e) -> {

            boolean eternal = (s != s);

            //normalize to range
            //conf = (conf - confMinMax[0]) / (confMinMax[1] - confMinMax[0]);


            final float ph =
                    baseTaskSize;
                    //Util.lerp(conf, 0.2f, /* down to */ baseTaskSize / 64f); //smudge a low confidence task across more of the frequency range

            float start, end;
            if (showEternal && eternal) {
                start = end = nowX;
            } else if (((e <= maxT) && (e >= minT)) || ((s >= minT) && (s <= maxT))) {
                start = xTime(minT, maxT, (long) s);
                end = s == e ? start : xTime(minT, maxT, (long) e);
            } else {
                return;
            }


            //r.renderTask(gl, qua, conf, pw, ph, xStart, xEnd, freq);

            float mid = (end + start) / 2f;
            float W = Util.max((end - start), baseTaskSize/4);

            if (beliefOrGoal) {
                gl.glColor4f( 1f, 0, 0.25f, 0.1f + 0.5f * conf);
            } else {
                gl.glColor4f( 0f, 1, 0.25f, 0.1f + 0.5f * conf);
            }
            float y = freq - ph / 2;
            float x = mid - W / 2;
            Draw.rect(gl,
                    x, y,
                    W, ph);


        });
    }

    /**
     * TODO use double not float for precision that may be lost
     */
    private static void renderWaveLine(float nowX, long minT, long maxT, GL2 gl, TruthWave wave, boolean beliefOrGoal) {

        gl.glLineWidth(3.0f);
        gl.glBegin(GL2.GL_LINE_STRIP);

        wave.forEach((freq, conf, start, end) -> {

            boolean eternal = (start != start);
            float x;
            float pw = baseTaskSize;// + gew / (1f / conf) / 4f;//10 + 10 * conf;
            float ph = baseTaskSize;// + geh / (1f / conf) / 4f;//10 + 10 * conf;

            if (eternal) {
                x = nowX; //???
            } else if ((start >= minT) && (start <= maxT)) {
                x = xTime(minT, maxT, (long) start);
            } else {
                return;
            }

            float a = 0.25f + 0.7f * conf;
            if (beliefOrGoal) {
                gl.glColor4f(0.25f + 0.75f * conf, 0.1f * (1f-conf), 0f, a);
            } else {
                gl.glColor4f(0.1f * (1f-conf),  0.25f + 0.75f * conf, 0f, a);
            }

            //r.renderTask(gl, qua, conf, pw, ph, x, freq);
            gl.glVertex2f(x, freq);


            if (start == end)
                return; //just the one point

            if (eternal) {
                //x = nowX; //??
                return;
            } else if ((end >= minT) && (end <= maxT)) {
                x = xTime(minT, maxT, (long) end);
            }

            gl.glVertex2f(x, freq);
        });

        gl.glEnd();
    }

//    private static float yPos(float f, float eh /* drawn object height, padding */) {
//        return (eh) * (f);
//    }

    /*private static float eternalX(float width, float b, float w, float cc) {
        return b + (width - b - w) * cc;
    }*/

    public BeliefTableChart time(BiFunction<Long, long[], long[]> rangeControl) {
        this.rangeControl = rangeControl;
        return this;
    }

    public BeliefTableChart timeRadius(long nowRadius) {
        this.time((now, range) -> {
            long low = range[0];
            long high = range[1];

            if (now - low > nowRadius)
                low = now - nowRadius;
            if (high - now > nowRadius)
                high = now + nowRadius;
            return new long[]{low, high};
        });
        return this;
    }


    private static float xTime(long minT, long maxT, long o) {
        if (minT == maxT) return 0.5f;
        return (Math.min(maxT, Math.max(minT, o)) - minT) / ((float) (maxT - minT));
    }


}
