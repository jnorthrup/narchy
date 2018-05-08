package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.util.FloatFloatToFloatFunction;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.truth.TruthWave;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.text.Label;
import spacegraph.video.Draw;

import java.util.function.BiFunction;

import static java.lang.Math.PI;
import static nars.util.time.Tense.ETERNAL;


public class BeliefTableChart extends DurSurface implements MetaFrame.Menu {

    private static final float taskHeight = 0.04f;
    private static final float CROSSHAIR_THICK = 3;
    private static final float angleSpeed = 0.5f;
    final Term term;
    private final TruthWave beliefs;
    private final TruthWave beliefProj;
    private final TruthWave goals;
    private final TruthWave goalProj;
    private final boolean showTaskLinks = false;
    @Deprecated
    private final boolean showEternal = false;
    private TaskConcept cc; //cached concept
//    private float cp; //cached priority
    private float beliefTheta, goalTheta;
    /**
     * (if > 0): draw additional projection wave to show truthpolation values for a set of evenly spaced points on the visible range
     */
    private int projections = 32;
    private long[] range;
    //private int dur; //cached dur
    private long now; //cached time
    private String termString; //cached string
    private BiFunction<Long, long[], long[]> rangeControl = (now, range) -> range; //default: no change

//    public BeliefTableChart(NAR n, Termed term) {
//        this(n, term, null);
//    }

    public BeliefTableChart(NAR n, Termed term, long[] range) {
        super(new Label(term.toString()), n);
        this.term = term.term();

        this.range = range;

//        label.textColor.a(0.5f);
//        //label.scale(0.5f, 0.5f);
//
//        content(label);

        beliefs = new TruthWave(0);
        beliefProj = new TruthWave(0);
        goals = new TruthWave(0);
        goalProj = new TruthWave(0);

        beliefTheta = goalTheta = 0;

    }

    private static void drawCrossHair(GL2 gl, float x, float gew, float freq, float conf, double theta) {
        gl.glLineWidth(CROSSHAIR_THICK);


        //ge.strokeLine(bcx, border, bcx, geh - border);
        //ge.strokeLine(border, bcy, gew - border, bcy);
        double r = gew * (0.5f + 0.5f * conf);


        double dx0 = Math.cos(theta) * r;
        double dy0 = Math.sin(theta) * r;
        Draw.line(gl, dx0 + x, dy0 + freq, -dx0 + x, -dy0 + freq);

        double hpi = PI / 2.0;
        double dx1 = Math.cos(theta + hpi) * r;
        double dy1 = Math.sin(theta + hpi) * r;
        Draw.line(gl, dx1 + x, dy1 + freq, -dx1 + x, -dy1 + freq);
    }

    /**
     * TODO use double not float for precision that may be lost
     *
     * @param y (freq,conf)->y
     */
    private static void renderWaveLine(float nowX, long minT, long maxT, GL2 gl, TruthWave wave, FloatFloatToFloatFunction y, Colorize colorize) {


        gl.glBegin(GL2.GL_LINE_STRIP);

        float dt = ((maxT - minT)/((float)wave.capacity()));
        float dMargin = dt/8;
        wave.forEach((freq, conf, start, end) -> {

            boolean eternal = (start != start);
            float x;
//            float pw = baseTaskSize;// + gew / (1f / conf) / 4f;//10 + 10 * conf;
//            float ph = baseTaskSize;// + geh / (1f / conf) / 4f;//10 + 10 * conf;

            //float mid = (start + end)/2;

            if (eternal) {
                x = nowX; //???
            } else if ((start >= minT) && (start <= maxT)) {
                x = xTime(minT, maxT, start + dMargin);
            } else {
                return;
            }

            colorize.colorize(gl, freq, conf);

            //r.renderTask(gl, qua, conf, pw, ph, x, freq);
            float Y = y.apply(freq, conf);
            gl.glVertex2f(x, Y);


            if (start == end)
                return; //just the one point

            if (eternal)
                return;

            if ((end >= minT) && (end <= maxT)) {
                x = xTime(minT, maxT, end-dMargin);
                gl.glVertex2f(x, Y);
            }

        });

        gl.glEnd();
    }

    private static float xTime(long minT, long maxT, float o) {
        if (minT == maxT) return 0.5f;
        return (Math.min(maxT, Math.max(minT, o)) - minT) / (maxT - minT);
    }


//    final static ColorMatrix beliefColors = new ColorMatrix(8, 8, (f, c) ->
//            new Color(0.6f + 0.38f * c, 0.2f, 1f, 0.39f + 0.6f * c)
//    );
//    final static ColorMatrix goalColors = new ColorMatrix(8, 8, (f, c) ->
//            new Color(0.2f + 0.4f * c, 1f, 0.2f, 0.39f + 0.6f * c)
//    );

    @Override
    public void update() {

        long now = this.now = nar.time();
        int dur = /*this.dur = */nar.dur();

        Concept ccd = nar.concept(term/* lookup by term, not the termed which could be a dead instance */);
        if (!(ccd instanceof TaskConcept))
            return;

        cc = (TaskConcept) ccd;

        long minT, maxT;
        if (range != null) {
            minT = range[0];
            maxT = range[1];
            if (minT >= maxT)
                return;
        } else {
            minT = Long.MIN_VALUE;
            maxT = Long.MAX_VALUE;
        }

        if (cc != null) {
//            cp = 1f; /*nar.pri(cc);*/

            long nowEnd = now + dur / 2;
            long nowStart = now - dur / 2;
            BeliefTable ccb = cc.beliefs();
            this.beliefs.set(ccb, now, dur, nar, minT, maxT);
            this.beliefs.current = ccb.truth(nowStart, nowEnd, nar);
            BeliefTable ccg = cc.goals();
            this.goals.set(ccg, now, dur, nar, minT, maxT);
            this.goals.current = ccg.truth(nowStart, nowEnd, nar);

            if (projections > 0 && minT != maxT) {
                beliefProj.project(cc, true, minT, maxT, dur, projections, nar);
                goalProj.project(cc, false, minT, maxT, dur, projections, nar);
            }

        } else {
//            cp = 0;
            beliefs.clear();
            beliefs.current = null;
            goals.clear();
            goals.current = null;
            beliefProj.clear();
            goalProj.clear();
        }


    }

    @Override
    public String toString() {
        return "BeliefTableChart[" + term + "]";
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
                    //TODO handle GeneralizedTaskLink's specified time directly without this Task lookup
                    Task x = tl.get(nar);
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
    protected void paintIt(GL2 ggl) {

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
                if (termString!=null)
                    termString = cc.toString();
            } else {
                if (termString!=null)
                    termString = term.toString();
            }
            //label.text(termString);
        });

        //        gl.glColor4f(0.75f, 0.75f, 0.75f, 0.8f + 0.2f * cp);
//        gl.glLineWidth(1);
//        Draw.text(gl, termString, (1f/termString.length()) * (0.5f + 0.25f * cp), 1 / 2f, 1 / 2f, 0);
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
            for (boolean freqOrExp : new boolean[]{true, false}) {
                TruthWave pwave = beliefOrGoal ? beliefProj : goalProj;

                if (beliefOrGoal && !freqOrExp) continue; //HACK dont show expectation for beliefs

                Colorize colorize;
                if (freqOrExp) {
                    colorize = beliefOrGoal ?
                            (ggl, frq, cnf) -> {
                                float a = 0.65f + 0.2f * cnf;
                                ggl.glColor4f(0.25f + 0.75f * cnf, 0.1f * (1f - cnf), 0, a);
                            } :
                            (ggl, frq, cnf) -> {
                                float a = 0.65f + 0.2f * cnf;
                                ggl.glColor4f(0.1f * (1f - cnf), 0.25f + 0.75f * cnf, 0, a);
                            };
                } else {
                    colorize = beliefOrGoal ?
                            (ggl, frq, cnf) -> {
                                ggl.glColor4f(cnf, cnf / 2f, 0, 0.85f);
                            } :
                            (ggl, frq, cnf) -> {
                                ggl.glColor4f(cnf / 2f, cnf, 0, 0.85f);
                            };
                }


                FloatFloatToFloatFunction y =
                        freqOrExp ? (frq, cnf) -> frq : TruthFunctions::expectation;

                //gl.glLineWidth((freqOrExp && !beliefOrGoal) ? 2f : 4f); //HACK show goal freq in thinner line
                gl.glLineWidth(4f);

                renderWaveLine(nowX, minT, maxT, gl, pwave, y, colorize);
            }
        }

        float chSize = 0.1f;

        Truth bc = wave.current;
        if (bc != null) {
            float theta;
            float expectation = bc.expectation();
            float dTheta = (expectation - 0.5f) * angleSpeed;
            float conf = bc.conf();
            if (beliefOrGoal) {
                this.beliefTheta += dTheta;
                theta = beliefTheta;
                gl.glColor4f(1f, 0f, 0, 0.2f + 0.8f * conf);
                drawCrossHair(gl, nowX, chSize, bc.freq(), conf, theta);
            } else {
                this.goalTheta += dTheta;
                theta = goalTheta;

//                //freq
//                gl.glColor4f(0f, 1f, 0, 0.2f + 0.8f * conf);
//                drawCrossHair(gl, nowX, chSize, bc.freq(), conf, theta);

                //expectation
                gl.glColor4f(0f, 1f, 0, 0.2f + 0.8f * conf);
                drawCrossHair(gl, nowX, chSize, expectation, expectation, theta);

            }
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
                    taskHeight;
            //Util.lerp(conf, 0.2f, /* down to */ baseTaskSize / 64f); //smudge a low confidence task across more of the frequency range

            float start, end;
            if (showEternal && eternal) {
                start = end = nowX;
            } else {
                if (e >= minT && s <= maxT) {
                    s = Math.max(minT, s);
                    e = Math.min(maxT, e);
                }
                start = xTime(minT, maxT, s);
                end = xTime(minT, maxT, e + 1);
            }


            if (beliefOrGoal) {
                gl.glColor4f(1f, 0, 0.25f, 0.1f + 0.5f * conf);
            } else {
                gl.glColor4f(0f, 1, 0.25f, 0.1f + 0.5f * conf);
            }
            float y = freq - ph / 2;
            Draw.rect(gl,
                    start, y,
                    end-start, ph);


        });
    }

    @Override
    public Surface menu() {
        return new Gridding(
                new PushButton("Expand", () -> Vis.conceptWindow(term, nar))
                //...
        );
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


    interface Colorize {
        void colorize(GL2 gl, float f, float c);
    }


}
