package nars.video;

import jcog.Util;
import jcog.math.FloatRange;
import nars.NAR;
import nars.agent.NAgent;
import nars.concept.TaskConcept;
import nars.control.DurService;
import nars.gui.NARui;
import nars.sensor.Bitmap2DSensor;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerDragging;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.ButtonSet;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.video.Draw;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * displays a CameraSensor pixel data as perceived through its concepts (belief/goal state)
 * monochrome
 */
public class Bitmap2DConceptsView extends BitmapMatrixView implements BitmapMatrixView.ViewFunction2D {

    private static final int AFFECT_CONCEPT_BUTTON = 0;
    private static final int OPEN_CONCEPT_BUTTON = 1; 

    private final Bitmap2DSensor cam;
    private final NAR nar;

    private DurService on;

    private long start, end;
    private int dur;

    /** in durs */
    public final FloatRange timeShift = new FloatRange(0, -64, +64);
    public final AtomicBoolean visBelief = new AtomicBoolean(true);
    public final AtomicBoolean visGoal = new AtomicBoolean(true);
    public final AtomicBoolean visPri = new AtomicBoolean(true);



    private TaskConcept touchConcept;

    private Consumer<TaskConcept> touchMode = (x) -> { };

    public Bitmap2DConceptsView(Bitmap2DSensor cam, NAgent a) {
        this(cam, a.nar());
    }

    public Bitmap2DConceptsView(Bitmap2DSensor cam, NAR n) {
        super(cam.width, cam.height);
        this.cam = cam;
        this.nar = n;
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

    final FingerDragging affect = new FingerDragging(AFFECT_CONCEPT_BUTTON) {
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
            touchConcept = cam.get(touchPixel.x, cam.height - 1 - touchPixel.y);
        }
    }


    void onTouch(TaskConcept touchConcept) {
        touchMode.accept(touchConcept);
    }

    @Override
    public boolean start(@Nullable SurfaceBase parent) {
        if (super.start(parent)) {
            on = DurService.on(nar, this::accept);
            return true;
        }
        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            if (on != null) {
                on.off();
                this.on = null;
            }
            return true;
        }
        return false;
    }



    private void accept(NAR nn) {
        dur = nn.dur();
        long now = Math.round(nn.time() + (dur * timeShift.floatValue()));

        this.start = now - dur;
        this.end = now + dur;

        update();



    }

    @Override
    public int update(int x, int y) {


        TaskConcept s = cam.get(x,y);

        float R = 0, G = 0 , B = 0;
        float bf = 0;
        if (visBelief.get()) {
            Truth b = s.beliefs().truth(start, end, s.term, nar);
            bf = b != null ? b.freq() : 0.5f;
        }

        R = bf * 0.75f; G = bf * 0.75f; B = bf * 0.75f;

        if (visGoal.get()) {
            Truth d = s.goals().truth(start, end, s.term, nar);
            if (d != null) {
                float f = d.expectation();

                if (f > 0.5f) {
                    G += 0.25f * (f - 0.5f) * 2f;
                } else {
                    R += 0.25f * (0.5f - f) * 2f;
                }
            }
        }


        if (visPri.get()) {
            float pri = nar.concepts.pri(s, 0);
            B += 0.25f * pri;
        }

        return Draw.rgbInt(
                Util.unitize(R), Util.unitize(G), Util.unitize(B)
                /*, 0.5f + 0.5f * p*/);
    }

    public Surface withControls() {
        return new Splitting(this, new CameraSensorViewControls(this), 0.1f);
    }


    /** TODO use DurSurface */
    public static class CameraSensorViewControls extends Gridding {

        private final Bitmap2DConceptsView view;
        private DurService on;

        /** the procedure to run in the next duration. limits activity to one
         * of these per duration. */
        private final AtomicReference<Runnable> next = new AtomicReference<>();

        @Override
        public boolean start(SurfaceBase parent) {
            if (super.start(parent)) {
                on = DurService.on(view.nar, this::commit);
                return true;
            }
            return false;
        }

        @Override
        public boolean stop() {

            if (super.stop()) {
                on.off();
                on = null;
                return true;
            }
            return false;

        }

        protected void commit() {
            Runnable next = this.next.getAndSet(null);
            if (next!=null) {
                next.run();
            }
        }

        public CameraSensorViewControls(Bitmap2DConceptsView view) {
            super();

            this.view = view;

            /** TODO use MutableEnum */
            set(new ButtonSet(ButtonSet.Mode.One,
                new CheckBox("Pri+", ()->{
                    view.onConceptTouch((c)->{
                        next.set(()->
                            view.nar.activate(c, 1f)
                        );
                    });
                }),
                goalCheckBox(view, "Goal+", 1f),
                goalCheckBox(view, "Goal+-", 0.5f),
                goalCheckBox(view, "Goal-", 0f)
            ), new ObjectSurface(List.of(view.visBelief, view.visGoal, view.visPri, view.timeShift)),
                    new FloatSlider("Pri", view.cam.pri()));
        }

        @NotNull
        public CheckBox goalCheckBox(Bitmap2DConceptsView view, String s, float v) {
            return new CheckBox(s, () -> {
                view.onConceptTouch((c) -> {
                    next.set(() ->
                            view.nar.want(c.term(), Tense.Present, v)
                    );
                });
            });
        }

    }
}
