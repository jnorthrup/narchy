package nars.gui.sensor;

import jcog.Util;
import jcog.data.list.FastCoWList;
import jcog.func.IntIntToFloatFunction;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.math.IntRange;
import jcog.random.SplitMix64Random;
import nars.NAL;
import nars.NAR;
import nars.attention.What;
import nars.concept.TaskConcept;
import nars.game.Game;
import nars.game.sensor.Signal;
import nars.game.sensor.VectorSensor;
import nars.gui.NARui;
import nars.sensor.Bitmap2DSensor;
import nars.task.util.Answer;
import nars.time.part.DurLoop;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.state.Dragging;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.button.ButtonSet;
import spacegraph.space2d.widget.button.CheckBox;
import spacegraph.space2d.widget.meta.ObjectSurface;
import spacegraph.space2d.widget.meter.BitmapMatrixView;
import spacegraph.video.Draw;

import java.util.function.Consumer;

import static java.lang.Math.sqrt;

/**
 * displays a CameraSensor pixel data as perceived through its concepts (belief/goal state)
 * monochrome
 */
public class VectorSensorChart extends BitmapMatrixView implements BitmapMatrixView.ViewFunction2D {

    private static final int AFFECT_CONCEPT_BUTTON = 0;
    private static final int OPEN_CONCEPT_BUTTON = 2;

    private final VectorSensor sensor;
    private final NAR nar;
    private final FloatSupplier dur;
    final FastCoWList<Layer> layers = new FastCoWList<>(Layer[]::new);

    private DurLoop on;

    /** how much evidence to include in result */
    public final IntRange truthPrecision = new IntRange(NAL.ANSWER_BELIEF_MATCH_CAPACITY, 1, 16);

    /** in durs */
    public final FloatRange timeShift = new FloatRange(0, -64, +64);

    /** durs around target time */
    public final FloatRange window = new FloatRange(1, 0, 4);



//    public final AtomicBoolean beliefs = new AtomicBoolean(true);
//    public final AtomicBoolean goals = new AtomicBoolean(true);
    //public final AtomicBoolean pris = new AtomicBoolean(true);


    public abstract static class Layer {
        float[] value;
        public final FloatRange opacity = new FloatRange(0.75f, 0, 1);

        public final void doUpdate(VectorSensorChart v) {
            if (value == null)
                value = new float[v.w * v.h];
            update(v);
        }

        public abstract void blend(float v, float opacity, float[] rgbTarget);

        public abstract void update(VectorSensorChart v);

        protected void update(VectorSensorChart v, IntIntToFloatFunction f) {
            int w = v.w;
            int h = v.h;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    value[y * w + x] = f.value(x, y);
                }
            }
        }

        public float value(int x, int w, int y) {
            return value[y * w + x];
        }
    }

    public abstract static class ColoredLayer extends Layer {

        private final float[] color;

        public ColoredLayer(float r, float g, float b) {
            this.color = new float[] { r, g, b};
        }

        @Override
        public void blend(float v, float opacity, float[] rgb) {
            VectorSensorChart.blend(v * opacity, color, rgb);
        }

    }
    static void blend(float v, float[] color, float[] rgb) {
        rgb[0] += v * color[0];
        rgb[1] += v * color[1];
        rgb[2] += v * color[2];
    }



    private TaskConcept touchConcept;

    private Consumer<TaskConcept> touchMode = (x) -> { };
    public final transient TaskConcept[][] concept;

//    final AtomicBoolean ready = new AtomicBoolean(true);

    private transient Answer answer = null;
    private transient int answerTries;



    public VectorSensorChart(VectorSensor v, NAR n) {
        this(v, n::dur, n);
    }

    public VectorSensorChart(VectorSensor v, int w, int h, NAR n) {
        this(v, w, h, n::dur, n);
    }

    public VectorSensorChart(VectorSensor v, Game g) {
        this(v, g::dur, g.nar());
    }
    public VectorSensorChart(VectorSensor v, int w, int h, Game g) {
        this(v, w, h, g::dur, g.nar());
    }


    public VectorSensorChart(VectorSensor v, FloatSupplier dur, NAR n) {
        this(v,
            v instanceof Bitmap2DSensor ? ((Bitmap2DSensor)v).width : (int)Math.ceil(idealStride(v)),
            v instanceof Bitmap2DSensor ? ((Bitmap2DSensor)v).height : (int)Math.ceil(v.size()/idealStride(v)),
            dur, n);
    }

    public VectorSensorChart(VectorSensor v, int w, int h, FloatSupplier dur, NAR n) {
        super(w, h);
        this.dur = dur;
        this.sensor = v;
        this.nar = n;

        this.concept = new TaskConcept[h][w];
        int x = 0, y = 0;

        for (TaskConcept c : v) {
            concept[y][x++] = c;
            if (x == w) {
                x = 0;
                y++;
            }
        }

        initLayers();
    }

    @Deprecated protected void initLayers() {
        /* beliefs */
        layers.add(new ColoredLayer(1f, 1f, 1f) {
            {
                opacity.set(0.8f);
            }

            @Override
            public void update(VectorSensorChart v) {

                update(v, new XYConcept() {
                    @Override protected float floatValue(int x, int y, TaskConcept c) {
                        Truth b = answer.clear(answerTries).match(c.beliefs())
                            .truth();
                        return b != null ? b.freq() : Float.NaN;
                    }
                });
                //Util.normalize(value);
            }

        });
        /* goals */
        layers.add(new Layer() {

            public final MutableBoolean normalize = new MutableBoolean(true);
            public final MutableBoolean freqOrExp = new MutableBoolean(false);

            {
                opacity.set(0.5f);
            }

            @Override
            public void blend(float vv, float opacity, float[] rgbTarget) {
                float v = (vv-0.5f)*2 * opacity;
                if (v == 0) {
                    //nothing
                } else if (v < 0)
                    rgbTarget[0] += -v;
                else
                    rgbTarget[1] += v;
            }

            @Override
            public void update(VectorSensorChart v) {

                update(v, new XYConcept() {
                    @Override
                    protected float floatValue(int x, int y, TaskConcept c) {
                        Truth g = answer.clear(answerTries).match(c.goals()).truth();
                        return g != null ? (freqOrExp.booleanValue() ? g.freq() : g.expectation()) : 0.5f;
                    }
                });

                if (normalize.booleanValue()) {
                    //balance bipolar normalize around 0.5
                    float[] minmax = Util.minmax(value);
                    float min = minmax[0], max = minmax[1];
                    if (max-min > Float.MIN_NORMAL) {
                        float max5 = max - 0.5f;
                        float min5 = 0.5f - min;
                        if (max5 > min5) {
                            min = 0.5f - max5;
                        } else if (min5 > max5) {
                            max = 0.5f + min5;
                        }
                        Util.normalizeSafe(value, min, max);
                    }
                }
            }
        });

    }
    public VectorSensorChart(VectorSensor sensor, Signal[][] matrix, int width, int height, FloatSupplier dur, NAR n) {
        super(width, height);
        this.dur = dur;
        this.sensor = sensor;
        this.concept = matrix;
        this.nar = n;
        initLayers();
    }

    static float idealStride(VectorSensor v) {
        return (float) Math.ceil(sqrt(v.size()));
    }

    public VectorSensorChart(Bitmap2DSensor sensor, What w) {
        this(sensor, w::dur, w.nar);
    }

    public VectorSensorChart(Bitmap2DSensor sensor, FloatSupplier dur, NAR n) {
        this(sensor, sensor.concepts.matrix, sensor.width, sensor.height, dur, n);
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
        touchConcept = finger == null ? null :
                concept(touchPixel.x, height() - 1 - touchPixel.y);
    }

    private @Nullable TaskConcept concept(int x, int y) {
        //if (x < width() && y < height())
            return concept[y][x];
//        else
//            return null;
    }

    public int height() {
        return concept.length;
    }

    public int width() {
        return concept[0].length;
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

        if (!showing())
            return;

        float dur = this.dur.asFloat();

        long now = n.time() + Math.round((dur * timeShift.floatValue()));

        long windowRadius = Math.round(dur * this.window.floatValue() / 2);

        int answerDetail = truthPrecision.intValue();
        this.answerTries = (int)Math.ceil(answerDetail);

        long end = now + windowRadius;
        long start = now - windowRadius;
        if (answer == null) {
            this.answer = Answer
                    .taskStrength(true, answerDetail, start, end, null, null, nar);
        }

        this.answer.time(start, end).dur(dur /* * perceptDur.floatValue() */);

        for (Layer l : layers)
            l.doUpdate(this);

        update();

    }


    private final SplitMix64Random noise = new SplitMix64Random(1);

    private float noise() {
        return noise.nextFloat();
    }


    @Override
    public int color(int x, int y) {

        float[] rgb = { 0, 0, 0 };
        for (Layer l : layers) {
            l.blend(l.value(x,w,y), l.opacity.floatValue(), rgb);
        }
        return Draw.rgbInt(
                Util.unitize(rgb[0]), Util.unitize(rgb[1]), Util.unitize(rgb[2])
                /*, 0.5f + 0.5f * p*/);

//        float bf = 0;
//        TaskConcept s = concept(x,y);
//        float R, G, B;
//
//        Answer a = this.answer;
//        if (s == null)
//            bf = noise();
//        else {
//            if (beliefs.getOpaque()) {
//                Truth b = a != null ? a.clear(tries).match(s.beliefs()).truth() : null;
//                if (b != null) bf = b.freq();
//                else
//                    bf = noise();
//            }
//        }
//
//        R = bf * 0.75f; G = bf * 0.75f; B = bf * 0.75f;
//
//        if (s!=null) {
//            if (goals.getOpaque()) {
//                Truth d = a!=null ? a.clear(tries).match(s.goals()).truth() : null;
//                if (d != null) {
//                    float f = d.expectation();
//
//                    if (f > 0.5f) {
//                        G += 0.25f * (f - 0.5f) * 2f;
//                    } else {
//                        R += 0.25f * (0.5f - f) * 2f;
//                    }
//                }
//            }
//        }
//
//
//
//
//
//
//
//
////        if (pris.get()) {
////            float pri = nar.concepts.pri(s, 0);
////            B += 0.25f * pri;
////        }
//
//        return Draw.rgbInt(
//                Util.unitize(R), Util.unitize(G), Util.unitize(B)
//                /*, 0.5f + 0.5f * p*/);
    }

    public Splitting withControls() {
        return new Splitting(this, 0.1f, new Splitting(new ObjectSurface(layers), 0.5f, new CameraSensorViewControls()).resizeable()).resizeable();
    }


    /** TODO use DurSurface */
    class CameraSensorViewControls extends Gridding {

        private DurLoop on;

//        /** the procedure to run in the next duration. limits activity to one
//         * of these per duration. */
//        private final AtomicReference<Runnable> next = new AtomicReference<>();

//        @Override
//        protected void starting() {
//            super.starting();
////            on = view.nar.onDur(this::commit);
//        }

        @Override
        protected void stopping() {
            on.close();
            on = null;
            super.stopping();
        }

//        protected void commit() {
//            Runnable next = this.next.getAndSet(null);
//            if (next!=null) {
//                next.run();
//            }
//        }

        public CameraSensorViewControls() {
            super();

            /** TODO use MutableEnum */
            set(new ButtonSet<>(ButtonSet.Mode.One,
                    new CheckBox("Pri+", ()->{
//                        view.onConceptTouch((c)->{
//                            next.set(()-> {
//                                        //view.nar.activate(c, 1f)
//                                        throw new TODO();
//                                    }
//                            );
//                        });
                    }),


                goalCheckBox("Goal-", 0f),
                goalCheckBox( "Goal-+", 0f, 1f),
                goalCheckBox( "Goal~", 0.5f),
                goalCheckBox( "Goal+-", 1f, 0f),
                goalCheckBox( "Goal+", 1f)
            ), new ObjectSurface(view)
                    //TODO attn node plot: supply/demand
                    //new FloatSlider("Supply", view.sensor.attn.supply)
            );
        }

        CheckBox goalCheckBox(String s, /* TODO */ float value) {
            return goalCheckBox(s, value, value);
        }

        /** from,to allows specifying a transition, ex: (--x &&+1 x) or (x &&+1 --x) if they differe */
        CheckBox goalCheckBox(String s, /* TODO */ float fromValue, float toValue) {
            return new CheckBox(s, () -> {
//                view.onConceptTouch((c) -> {
//                    next.set(() -> view.nar.want(c.term(), Tense.Present, toValue));
//                });
            });
        }

    }

    abstract class XYConcept implements IntIntToFloatFunction {

        protected abstract float floatValue(int x, int y, TaskConcept c);

        @Override
        public float value(int x, int y) {
            TaskConcept[] cx = concept[y];
            if (cx != null) {
                TaskConcept c = cx[x];
                if (c!=null) {
                    float b = floatValue(x, y, c);
                    if (b == b)
                        return b;
                    else
                        return noise();
                }
            }
            return 0.5f; //noise();
        }

    }
}
