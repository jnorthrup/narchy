package nars.gui.sensor;

import jcog.TODO;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.random.SplitMix64Random;
import nars.NAR;
import nars.NAL;
import nars.agent.Game;
import nars.attention.What;
import nars.concept.TaskConcept;
import nars.concept.sensor.Signal;
import nars.concept.sensor.VectorSensor;
import nars.gui.NARui;
import nars.sensor.Bitmap2DSensor;
import nars.task.util.Answer;
import nars.time.Tense;
import nars.time.part.DurLoop;
import nars.truth.Truth;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.state.Dragging;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.ButtonSet;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.video.Draw;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.lang.Math.sqrt;

/**
 * displays a CameraSensor pixel data as perceived through its concepts (belief/goal state)
 * monochrome
 */
public class VectorSensorView extends BitmapMatrixView implements BitmapMatrixView.ViewFunction2D {

    private static final int AFFECT_CONCEPT_BUTTON = 0;
    private static final int OPEN_CONCEPT_BUTTON = 2;

    private final VectorSensor sensor;
    private final NAR nar;
    private final FloatSupplier baseDur;

    private DurLoop on;

    private long start, end;

    /** how much evidence to include in result */
    static final int truthPrecision = 8;

    /** in durs */
    public final FloatRange timeShift = new FloatRange(0, -64, +64);

    /** durs around target time */
    public final FloatRange window = new FloatRange(1, 0, 4);

    /** truth duration */
    public final FloatRange truthDur = new FloatRange(1, 0, 4);

    public final AtomicBoolean beliefs = new AtomicBoolean(true);
    public final AtomicBoolean goals = new AtomicBoolean(true);
    //public final AtomicBoolean pris = new AtomicBoolean(true);



    private TaskConcept touchConcept;

    private Consumer<TaskConcept> touchMode = (x) -> { };
    private final TaskConcept[][] concept;

    private Answer answer = null;

    public VectorSensorView(Bitmap2DSensor sensor, Game a) {
        this(sensor, a.what());
    }

    public VectorSensorView(VectorSensor v, NAR n) {
        this(v, n::dur, n);
    }
    public VectorSensorView(VectorSensor v, Game g) {
        this(v, g::dur, g.nar());
    }

    public VectorSensorView(VectorSensor v, FloatSupplier baseDur, NAR n) {
        this(v, (int)Math.ceil(idealStride(v)), (int)Math.ceil(v.size()/idealStride(v)), baseDur, n);
    }

    public VectorSensorView(VectorSensor v, int w, int h, FloatSupplier baseDur, NAR n) {
        super(w, h);
        this.baseDur = baseDur;
        this.sensor = v;
        this.nar = n;

        this.concept = new TaskConcept[w][h];
        int x = 0, y = 0;

        for (TaskConcept c : v) {
            concept[x++][y] = c;
            if (x == w) {
                x = 0;
                y++;
            }
        }

    }

    public VectorSensorView(VectorSensor sensor, Signal[][] matrix, int width, int height, FloatSupplier baseDur, NAR n) {
        super(width, height);
        this.baseDur = baseDur;
        this.sensor = sensor;
        this.concept = matrix;
        this.nar = n;
    }

    public static float idealStride(VectorSensor v) {
        return (float) Math.ceil(sqrt(v.size()));
    }


    public VectorSensorView(Bitmap2DSensor sensor, What w) {
        this(sensor, w::dur, w.nar);
    }

    public VectorSensorView(Bitmap2DSensor sensor, NAR n) {
        this(sensor, n::dur, n);
    }

    public VectorSensorView(Bitmap2DSensor sensor, FloatSupplier baseDur, NAR n) {
        this(sensor, sensor.concepts.matrix, sensor.width, sensor.height, baseDur, n);
    }

    public void onConceptTouch(Consumer<TaskConcept > c) {
        touchMode = c;
    }


    @Override
    public boolean updateTouch(Finger finger) {
        if (super.updateTouch(finger)) {

            updateTouchedConcept(finger);

            TaskConcept c = this.touchConcept;
            if (c!=null && finger.releasedNow(OPEN_CONCEPT_BUTTON) && !finger.dragging(OPEN_CONCEPT_BUTTON)) {
                NARui.conceptWindow(c, nar);
            } else {
                finger.test(affect);
            }
            return true;
        }

        return false;
    }

    final Dragging affect = new Dragging(AFFECT_CONCEPT_BUTTON) {
        @Override
        protected boolean drag(Finger f) {
//            updateTouchedConcept(f);
            TaskConcept c = touchConcept;
            if (c!=null) {
                onTouch(touchConcept);
                return true;
            }
            return false;
        }
    };

    private void updateTouchedConcept(Finger finger) {
        if (finger == null) {
            touchConcept = null;
        } else {
            touchConcept = concept(touchPixel.x, height() - 1 - touchPixel.y);
        }
    }

    private TaskConcept concept(int x, int y) {
        return concept[x][y];
    }

    public int height() {
        return concept[0].length;
    }

    public int width() {
        return concept.length;
    }


    void onTouch(TaskConcept touchConcept) {
        touchMode.accept(touchConcept);
    }

    @Override
    protected void starting() {
        super.starting();
        on = nar.onDur(this::accept);
    }

    @Override
    protected void stopping() {
        on.close();
        this.on = null;
        super.stopping();
    }

    private void accept(NAR n) {

        if (showing()) {

            float baseDur = this.baseDur.asFloat();
            long now = Math.round(n.time() + (baseDur * timeShift.floatValue()));

            double windowRadius = this.window.floatValue() * baseDur / 2;

            this.start = Math.round(now - windowRadius);
            this.end = Math.round(now + windowRadius);


            if (answer == null)
                this.answer = Answer
                        .relevant(true, truthPrecision, start, end, null, null, nar);

            this.answer.time(start, end).dur(Math.round(baseDur * truthDur.floatValue()));
        }


        updateIfShowing();
    }


    private final SplitMix64Random noise = new SplitMix64Random(1);

    private float noise() {
        return noise.nextFloat();
    }

    @Override
    public int color(int x, int y) {


        TaskConcept s = concept(x,y);
        if (s == null)
            return 0;

        float R = 0, G = 0 , B = 0;
        float bf = 0;

        Answer a = this.answer;
        int tries = (int) Math.ceil(truthPrecision * NAL.ANSWER_COMPLETENESS);
        if (beliefs.get()) {
            Truth b = a!=null ? a.clear(tries).match(s.beliefs()).truth() : null;
            bf = b != null ? b.freq() : noise();
        }

        R = bf * 0.75f; G = bf * 0.75f; B = bf * 0.75f;

        if (goals.get()) {
            Truth d = a!=null ? a.clear(tries).match(s.goals()).truth() : null;
            if (d != null) {
                float f = d.expectation();

                if (f > 0.5f) {
                    G += 0.25f * (f - 0.5f) * 2f;
                } else {
                    R += 0.25f * (0.5f - f) * 2f;
                }
            }
        }


//        if (pris.get()) {
//            float pri = nar.concepts.pri(s, 0);
//            B += 0.25f * pri;
//        }

        return Draw.rgbInt(
                Util.unitize(R), Util.unitize(G), Util.unitize(B)
                /*, 0.5f + 0.5f * p*/);
    }

    public Splitting withControls() {
        return new Splitting(this, 0.1f, new CameraSensorViewControls(this));
    }


    /** TODO use DurSurface */
    public static class CameraSensorViewControls extends Gridding {

        private final VectorSensorView view;
        private DurLoop on;

        /** the procedure to run in the next duration. limits activity to one
         * of these per duration. */
        private final AtomicReference<Runnable> next = new AtomicReference<>();

        @Override
        protected void starting() {
            super.starting();
            on = view.nar.onDur(this::commit);
        }

        @Override
        protected void stopping() {
            on.close();
            on = null;
            super.stopping();
        }

        protected void commit() {
            Runnable next = this.next.getAndSet(null);
            if (next!=null) {
                next.run();
            }
        }

        public CameraSensorViewControls(VectorSensorView view) {
            super();

            this.view = view;

            /** TODO use MutableEnum */
            set(new ButtonSet<>(ButtonSet.Mode.One,
                    new CheckBox("Pri+", ()->{
                        view.onConceptTouch((c)->{
                            next.set(()-> {
                                        //view.nar.activate(c, 1f)
                                        throw new TODO();
                                    }
                            );
                        });
                    }
                ),
                goalCheckBox(view, "Goal+", 1f),
                goalCheckBox(view, "Goal+-", 0.5f),
                goalCheckBox(view, "Goal-", 0f)
            ), new ObjectSurface<>(view)
                    //TODO attn node plot: supply/demand
                    //new FloatSlider("Supply", view.sensor.attn.supply)
            );
        }

        public CheckBox goalCheckBox(VectorSensorView view, String s, float v) {
            return new CheckBox(s, () -> {
                view.onConceptTouch((c) -> {
                    next.set(() -> view.nar.want(c.term(), Tense.Present, v));
                });
            });
        }

    }
}
