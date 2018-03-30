package nars.video;

import jcog.Util;
import nars.NAR;
import nars.NAgent;
import nars.concept.TaskConcept;
import nars.concept.scalar.Scalar;
import nars.control.DurService;
import nars.gui.Vis;
import nars.time.Tense;
import nars.truth.Truth;
import nars.util.signal.Bitmap2DSensor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerDragging;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.tab.ButtonSet;
import spacegraph.video.Draw;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * displays a CameraSensor pixel data as perceived through its concepts (belief/goal state)
 * monochrome
 */
public class CameraSensorView extends BitmapMatrixView implements BitmapMatrixView.ViewFunction2D {

    private static final int OPEN_CONCEPT_BUTTON = 1; //middle button

    private final Bitmap2DSensor cam;
    private final NAR nar;

    private DurService on;
//    private float maxConceptPriority;
    private long start, end;
    int dur;

    private Scalar touchConcept;

    private Consumer<Scalar> touchMode = (x) -> { };

    public CameraSensorView(Bitmap2DSensor cam, NAgent a) {
        this(cam, a.nar());
    }

    public CameraSensorView(Bitmap2DSensor cam, NAR n) {
        super(cam.width, cam.height);
        this.cam = cam;
        this.nar = n;
        this.dur = n.dur();
    }

    public void onConceptTouch(Consumer<Scalar> c) {
        touchMode = c;
    }


    @Override
    public void updateTouch(Finger finger) {


        super.updateTouch(finger);

        if (finger == null) {
            touchConcept = null;
        } else {
            touchConcept = cam.get(touchPixel.x, cam.height - 1 - touchPixel.y);
        }

        if (finger.clickedNow(OPEN_CONCEPT_BUTTON, this)) {
            Scalar c = this.touchConcept;
            if (c != null)
                Vis.conceptWindow(c, nar);
        }

        finger.tryFingering(new FingerDragging(0) {
            @Override
            protected boolean drag(Finger f) {
                updateTouch(finger);
                Scalar c = touchConcept;
                if (c!=null)
                    onTouch(touchConcept);
                return true;
            }
        });



    }


    public void onTouch(Scalar touchConcept) {
        touchMode.accept(touchConcept);
    }

    @Override
    public void start(@Nullable SurfaceBase parent) {
        super.start(parent);
        on = DurService.on(nar, this::accept);
    }

    @Override
    public void stop() {
        super.stop();
        if (on!=null) { on.off(); this.on = null; }
    }



    public void accept(NAR nn) {
        long now = nn.time();
        dur = nn.dur();
        this.start = now - dur;
        this.end = now + dur;
//        maxConceptPriority = 1;
        update();
//            nar instanceof Default ?
//                ((Default) nar).focus.active.priMax() :
//                1; //HACK TODO cache this
    }

    @Override
    public int update(int x, int y) {


        TaskConcept s = cam.get(x,y);
        Truth b = s.beliefs().truth(start, end, nar);
        float bf = b != null ? b.freq() : 0.5f;

        Truth d = s.goals().truth(start, end, nar);
        float R = bf*0.75f, G = bf*0.75f, B = bf*0.75f;
        if (d!=null) {
            float f = d.expectation();
            //float c = d.conf();
            if (f > 0.5f) {
                G += 0.25f * (f - 0.5f)*2f;
            } else {
                R += 0.25f * (0.5f - f)*2f;
            }
        }

//        float p = 1f;//nar.pri(s);
//        if (p!=p) p = 0;

        //p /= maxConceptPriority;



        return Draw.rgbInt(
                Util.unitize(R), Util.unitize(G), Util.unitize(B)
                /*, 0.5f + 0.5f * p*/);
    }

    public Surface withControls() {
        return new Splitting(this, new CameraSensorViewControls(this), 0.1f);
    }

    public static class CameraSensorViewControls extends Gridding {

        private final CameraSensorView view;
        private DurService on;

        /** the procedure to run in the next duration. limits activity to one
         * of these per duration. */
        private final AtomicReference<Runnable> next = new AtomicReference<>();

        @Override
        public void start(SurfaceBase parent) {
            synchronized (this) {
                super.start(parent);
                on = DurService.on(view.nar, this::commit);
            }
        }

        @Override
        public void stop() {

            synchronized (this) {
                on.off();
                on = null;
                super.stop();
            }

        }

        protected void commit() {
            Runnable next = this.next.getAndSet(null);
            if (next!=null) {
                next.run();
            }
        }

        public CameraSensorViewControls(CameraSensorView view) {
            super();

            this.view = view;


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
            ), new FloatSlider("Pri", view.cam.pixelPri));
        }

        @NotNull
        public CheckBox goalCheckBox(CameraSensorView view, String s, float v) {
            return new CheckBox(s, () -> {
                view.onConceptTouch((c) -> {
                    next.set(() ->
                            view.nar.goal(c.term(), Tense.Present, v)
                    );
                });
            });
        }

    }
}
