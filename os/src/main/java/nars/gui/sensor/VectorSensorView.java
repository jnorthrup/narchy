package nars.gui.sensor;

import jcog.TODO;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.random.SplitMix64Random;
import nars.NAR;
import nars.agent.NAgent;
import nars.concept.TaskConcept;
import nars.concept.sensor.VectorSensor;
import nars.control.DurService;
import nars.gui.NARui;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import spacegraph.input.finger.Dragging;
import spacegraph.input.finger.Finger;
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
    private static final int OPEN_CONCEPT_BUTTON = 1; 

    private final VectorSensor sensor;
    private final NAR nar;

    private DurService on;

    private long start, end;
    private int dur;

    /** how much evidence to include in result */
    static final int truthPrecision = 6;

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
    private int _truthDur;

    public VectorSensorView(Bitmap2DSensor sensor, NAgent a) {
        super(sensor.width, sensor.height);
        this.sensor = sensor;
        this.nar = a.nar();

        this.concept = sensor.concepts.matrix;
    }

    public VectorSensorView(VectorSensor v, NAR n) {
        this(v, (int)Math.ceil(idealStride(v)), (int)Math.ceil(v.size()/idealStride(v)), n);
    }
    public VectorSensorView(VectorSensor v, int w, int h, NAR n) {
        super(w, h);
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

    public static float idealStride(VectorSensor v) {
        return (float) Math.ceil(sqrt(v.size()));
    }

    public VectorSensorView(Bitmap2DSensor sensor, NAR n) {
        super(sensor.width, sensor.height);
        this.sensor = sensor;
        this.nar = n;
        this.concept = sensor.concepts.matrix;
    }

    public void onConceptTouch(Consumer<TaskConcept > c) {
        touchMode = c;
    }


    @Override
    public boolean updateTouch(Finger finger) {
        if (super.updateTouch(finger)) {

            updateTouchedConcept(finger);

            if (finger.clickedNow(OPEN_CONCEPT_BUTTON, this)) {
                TaskConcept c = this.touchConcept;
                if (c != null)
                    NARui.conceptWindow(c, nar);
            } else {
                finger.tryFingering(affect);
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
        on = DurService.on(nar, this::accept);
    }

    @Override
    protected void stopping() {
        on.off();
        this.on = null;
        super.stopping();
    }

    private void accept(NAR nn) {
        dur = nn.dur();
        long now = Math.round(nn.time() + (dur * timeShift.floatValue()));
        float window = this.window.floatValue();

        this.start = Math.round(now - dur * window);
        this.end = Math.round(now + dur * window);

        this._truthDur = Math.round(truthDur.floatValue() * dur);
        updateIfNotShowing();
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

        Term template =
                null; //s.target;

        if (beliefs.get()) {
            Truth b = s.beliefs().truth(start, end, template, null, truthPrecision, _truthDur, nar);
            bf = b != null ? b.freq() : noise();
        }

        R = bf * 0.75f; G = bf * 0.75f; B = bf * 0.75f;

        if (goals.get()) {
            Truth d = s.goals().truth(start, end, template, null, truthPrecision, _truthDur, nar);
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
        return new Splitting(this, new CameraSensorViewControls(this), 0.1f);
    }


    /** TODO use DurSurface */
    public static class CameraSensorViewControls extends Gridding {

        private final VectorSensorView view;
        private DurService on;

        /** the procedure to run in the next duration. limits activity to one
         * of these per duration. */
        private final AtomicReference<Runnable> next = new AtomicReference<>();

        @Override
        protected void starting() {
            super.starting();
            on = DurService.on(view.nar, this::commit);
        }

        @Override
        protected void stopping() {
            on.off();
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
