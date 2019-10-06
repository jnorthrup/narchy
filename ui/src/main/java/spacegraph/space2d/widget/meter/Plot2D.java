package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.list.MetalConcurrentQueue;
import jcog.event.Off;
import jcog.signal.tensor.TensorRing;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import spacegraph.space2d.widget.Widget;
import spacegraph.video.Draw;
import spacegraph.video.font.HersheyFont;

import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

import static java.lang.Float.NaN;

public class Plot2D extends Widget {
    public static final PlotVis Line = (List<Series> series, float minValue, float maxValue, GL2 gl) -> plotLine(series, gl, minValue, maxValue, false, false);
    public static final PlotVis LineLanes = (List<Series> series, float minValue, float maxValue, GL2 gl) -> plotLine(series, gl, minValue, maxValue, true, false);
    public static final PlotVis BarLanes = (List<Series> series, float minValue, float maxValue, GL2 gl) -> plotLine(series, gl, minValue, maxValue, true, true);
    public final List<Series> series;
    private final int maxHistory;
    private final PlotVis vis;
    private final float[] backgroundColor = {0, 0, 0, 0.75f};
    private String title;
    private Off on;
    private volatile boolean invalid = false;
    private transient float minMinValue, maxMaxValue;


    public Plot2D(int history, PlotVis vis) {


        this.series = new FasterList();
        this.maxHistory = history;

        this.vis = vis;

    }

    private static void plotLine(List<Series> series, GL2 gl, float minValue, float maxValue, boolean lanes, boolean filled) {
        if (minValue == maxValue) {
            float center = minValue;
            minValue = center - (center / 2);
            maxValue = center + (center / 2);
        }

        gl.glColor4f(1f, 1f, 1f, 1f);

        gl.glLineWidth(2);

        float W = 1.0f;
        Draw.line(0, 0, W, 0, gl);
        float H = 1.0f;
        Draw.line(0, H, W, H, gl);

        HersheyFont.hersheyText(gl, Texts.n(minValue, 7), 0.04f, 0, 0, 0, Draw.TextAlignment.Left);
        HersheyFont.hersheyText(gl, Texts.n(maxValue, 7), 0.04f, 0, H, 0, Draw.TextAlignment.Left);

        int seriesSize = series.size();
        float textScale = 1f / (seriesSize) * 0.5f;
        float range = maxValue - minValue;
        float laneHeight = 1f / seriesSize;

        for (int i = 0; i < seriesSize; i++)
            lineplot(gl, minValue, filled, W, textScale, range, laneHeight, i, series.get(i));
    }

    private static void lineplot(GL2 gl, float minValue, boolean filled, float w, float textScale, float range, float laneHeight, int i, Series s) {
        //float mid = ypos((s.minValue() + s.maxValue()) / 2f, lanes, sn, seriesSize, maxValue-minValue, minValue);
//            float base = ypos(s.minValue(), lanes, i, seriesSize,
//                    maxValue-minValue, minValue);
        float Y = NaN;
        float baseY = i * laneHeight;

        if (range > Float.MIN_NORMAL) {

            int histSize = s.size(), histCap = s.capacity();

            float dx = (w / histCap);
            float x = (histCap - histSize) * dx;


            float[] color = s.color();
            float r = color[0], g = color[1], b = color[2], a = 0;

            if (!filled) {
                gl.glLineWidth(3);
                gl.glBegin(GL.GL_LINE_STRIP);
            }

            float laneOffset = laneHeight * i;

            for (int j = 0; j < histSize; j++) {

                float y = s.get(j);
                if (y == y) {

                    Y = ypos(y, minValue, range) * laneHeight + laneOffset;

                    float nextA = Util.lerpSafe(((y - minValue) / range), 0.5f, 0.95f);
                    if (!Util.equals(a, nextA, 1f/256f)) {
                        gl.glColor4f(r, g, b, a = nextA);
                    }

                    if (filled) {
                        //TODO fill to an edge (barchart), or the center (waveform)
                        gl.glRectf(x - dx, baseY, x, Y);
                    } else {
                        gl.glVertex2f(x, Y);
                    }
                }
                x += dx;
            }

            if (filled) {

            } else {
                gl.glEnd();
            }


            //draw mean line
            float mean = s.meanValue();
            if (mean == mean) {
                gl.glColor4f(color[0], color[1], color[2], 0.5f);
                float yMean = ypos(mean, minValue, range) * laneHeight + laneOffset;
                float t = 0.1f;
                Draw.rect(0, yMean - laneHeight * t/2, w, laneHeight * t/2, gl);
            }

        }

        if (Y != Y)
            Y = baseY + laneHeight / 2;


        gl.glColor3f(1, 1, 1);

        gl.glLineWidth(2);

        HersheyFont.hersheyText(gl, s.name(), 0.04f * textScale, w, filled ? 0 : Y, 0, Draw.TextAlignment.Right);


    }

    private static float ypos(float v, boolean lanes, int sn, int seriesSize, float range, float minValue) {
        return lanes ? ypos(minValue, range, v, sn, seriesSize) : ypos(v, minValue, range);
    }

    private static float ypos(float v, float minValue, float range) {
        return (v - minValue) / range;
    }

    private static float ypos(float minValue, float range, float v, int lane, int numLanes) {
        return (v == v ? ((v - minValue) / range) : (0.5f)) / numLanes + (((float) lane) / numLanes);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Plot2D on(Function<Runnable, Off> trigger) {
        synchronized (series) {
            if (on != null)
                on.close();
            this.on = trigger.apply(this::commit);
        }
        return this;
    }

    public Plot2D add(Series s) {
        series.add(s);
        invalid = true;
        return this;
    }

    public Plot2D add(String name, float[] data) {
        return add(newSeries(name, data).autorange());
    }

    protected AbstractSeries newSeries(String name, float[] data) {
        //return new ArraySeries(name, data);
        return new RingTensorSeries(name, data);
    }

    public Plot2D add(String name, float[] data, float min, float max) {
        return add(newSeries(name, data).range(min, max));
    }


//    public static final PlotVis BarWave = (List<Series> series, GL2 g, float minValue, float maxValue) -> {
//        if (minValue != maxValue) {
//
//            float w = 1.0f;
//            float h = 1.0f;
//
//
//            for (Series s : series) {
//                int histSize = s.size();
//
//                float dx = (w / histSize);
//
//                float x = 0;
//                float prevX = 0;
//
//                float[] ss = s.toArray();
//                int len = Math.min(s.size(), ss.length);
//                float range = maxValue - minValue;
//                for (int i = 0; i < len; i++) {
//                    float v = ss[i];
//
//                    float py = (v - minValue) / range;
//                    if (py < 0) py = 0;
//                    if (py > 1.0) py = 1.0f;
//
//                    float y = py * h;
//
//                    g.glColor4fv(s.color(), 0);
//
//                    Draw.rect(g, prevX, h / 2.0f - y / 2f, dx, y);
//
//                    prevX = x;
//                    x += dx;
//                }
//
//            }
//        }
//    };

    public Plot2D add(String name, DoubleSupplier valueFunc, float min, float max) {
        AbstractSeries s;
        add(s = new RingTensorSeries(name, maxHistory) {
            @Override
            public void update() {
                double v = valueFunc.getAsDouble();

                limit();
                if (v != v) {

                    add(NaN);
                } else {
                    if (v < min) v = min;
                    if (v > max) v = max;
                    add((float) v);
                }

            }


        });
        s.minValue = min;
        s.maxValue = max;
        return this;
    }

// TODO
//    float _minValue = NaN, _maxValue = NaN;
//    String minValueStr = "", maxValueStr = "";

    public Plot2D add(String name, DoubleSupplier valueFunc) {
        add(new RingTensorSeries(name, maxHistory) {
            @Override
            public void update() {
                limit();
                add((float) valueFunc.getAsDouble());
                autorange();
            }
        });
        return this;
    }

    /**
     * TODO use a FloatRingBuffer or something non-Box
     */
    @Deprecated
    public Plot2D add(String name, MetalConcurrentQueue<Float> buffer) {
        add(new RingTensorSeries(name, maxHistory) {
            @Override
            public void update() {
                limit();
                buffer.clear(this::add);
                autorange();
            }
        });
        return this;
    }

    @Override
    protected void paintWidget(RectFloat bounds, GL2 gl) {
        Draw.bounds(bounds, gl, this::paintUnit);
    }

    private void paintUnit(GL2 gl) {

        if (invalid) {
            commit();
            invalid = false;
        }

        List<Series> series = this.series;


        if (series.isEmpty()) {
            return;
        }


        gl.glColor4fv(backgroundColor, 0);
        Draw.rect(gl, 0, 0, 1, 1);

        vis.draw(series, minMinValue, maxMaxValue, gl);

        if (title != null) {


            gl.glColor3f(1f, 1f, 1f);
            gl.glLineWidth(1f);
            HersheyFont.hersheyText(gl, title, 0.1f, 0.5f, 0.5f, 0);

        }

    }

    @Override
    protected void stopping() {
        vis.stop();
        super.stopping();
    }

    public void commit() {
        synchronized (series) {


            final float[] minValue = {Float.POSITIVE_INFINITY};
            final float[] maxValue = {Float.NEGATIVE_INFINITY};
            series.forEach((Series s) -> {
                s.update();

                float min = s.minValue();
                if (min == min) {
                    float max = s.maxValue();
                    if (max == max) {
                        minValue[0] = Math.min(minValue[0], min);
                        maxValue[0] = Math.max(maxValue[0], max);
                    }
                }
            });

            if (Float.isFinite(minValue[0]) && Float.isFinite(maxValue[0])) {
                this.minMinValue = minValue[0];
                this.maxMaxValue = maxValue[0];
                vis.update();
            }
//            } else
//                throw new WTF();

        }
    }

    public interface Series {

        String name();

        void update();

        float maxValue();

        float minValue();

        float meanValue();

        float[] color();

        int capacity();

        int size();

        float get(int i);

        void clear();



        //void forEach(IntFloatConsumer value);
    }

    @FunctionalInterface
    public interface PlotVis {

        /**
         * externally triggered update function
         */
        default void update() {

        }

        void draw(List<Series> series, float minValue, float maxValue, GL2 g);

        default void stop() {

        }
    }

    public static class ArraySeries extends AbstractSeries {

        protected final MyFloatArrayList data;

        @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
        public ArraySeries(String name, int capacity) {
            data = new MyFloatArrayList(capacity);
            setName(name);
            this.capacity = capacity;
        }

        public ArraySeries(String name, float[] data) {
            this.data = new MyFloatArrayList(data);
            setName(name);
            capacity = data.length;
        }

        @Override
        public float get(int i) {
            return data.get(i);
        }

        @Override
        public void clear() {
            data.clear();
        }

        @Override
        public int size() {
            return data.size();
        }

        @Override
        public void forEach(FloatProcedure f) {
            data.forEach(f);
        }

        @Override
        void limit() {
            int over = data.size() - (ArraySeries.this.capacity - 1);
            for (int i = 0; i < over; i++)
                data.removeAtIndex(0);
        }

        private static final class MyFloatArrayList extends FloatArrayList {
            public MyFloatArrayList(int initialCapacity) {
                super(initialCapacity);
            }

            public MyFloatArrayList(float... array) {
                super(array);
            }

            @Override
            public float[] toArray() {
                return items;
            }

            @Override
            public float get(int index) {
                float[] ii = this.items;
                if (ii.length > index)
                    return ii[index];
                else
                    return Float.NaN; //HACK
            }
        }
    }
    public static class RingTensorSeries extends AbstractSeries {

        private final TensorRing data;

        public RingTensorSeries(String name, int capacity) {
            data = new TensorRing(1, capacity, true);
            setName(name);
            clear();
            this.capacity = capacity;
        }

        public RingTensorSeries(String name, float[] data) {
            this(name, data.length);
            for (float f : data)
                add(f);
        }

        public void add(float v) {
            data.setSpin(v);
        }

        @Override
        public float get(int i) {
            return data.getAt(i);
        }

        @Override
        public void clear() {
            data.fillAll(Float.NaN);
        }

        @Override
        public int size() {
            return capacity;
        }

        @Override
        public void forEach(FloatProcedure f) {
            data.forEach(f);
        }

        @Override
        void limit() {
            //N/A
        }
    }

//    /**
//     * TODO merge with BitmapWave
//     */
//    @Deprecated
//    public static class BitmapPlot implements PlotVis, BitmapMatrixView.BitmapPainter {
//        BitmapMatrixView bmp = null;
//        private final int w;
//        private final int h;
//
//
//        transient private List<Series> series;
//        transient private float minValue, maxValue;
//        private Graphics gfx;
//
//        volatile boolean update = false;
//
//        /**
//         * visualization bounds
//         */
//        float first = 0f, last = 1f;
//
//        public BitmapPlot(int w, int h) {
//            this.w = w;
//            this.h = h;
//        }
//
//        public float first() {
//            return first;
//        }
//
//        public float last() {
//            return last;
//        }
//
//        public int firstSample() {
//            return (int) Math.floor(first * (series.get(0).size() - 1) /* TODO */);
//        }
//
//        public int lastSample() {
//            return (int) Math.ceil(last * (series.get(0).size() - 1) /* TODO */);
//        }
//
//        @Override
//        public void stop() {
//            if (gfx != null) {
//                gfx.dispose();
//                gfx = null;
//            }
//            if (bmp != null) {
//                bmp.stop();
//                bmp = null;
//            }
//        }
//
//        @Override
//        public void draw(List<Series> series, float minValue, float maxValue, GL2 g) {
//            if (bmp == null) {
//                bmp = new BitmapMatrixView(w, h, this) {
//                    @Override
//                    public boolean alpha() {
//                        return true;
//                    }
//                };
//            }
//            this.series = series;
//            this.minValue = minValue;
//            this.maxValue = maxValue;
//
//            if (update) {
//                update = !bmp.updateIfShowing(); //keep updating till updated
//            }
//
//            bmp.paint(g, null);
//        }
//
//        @Override
//        public void update() {
//            update = true;
//        }
//
//
//        @Override
//        public synchronized void color(BufferedImage buf, int[] pix) {
//
//
//            if (gfx == null) {
//                gfx = buf.getGraphics();
//            }
//
//            gfx.clearRect(0, 0, w, h);
//
//            int ns = series.size();
//            if (ns == 0)
//                return;
//
//            int w = this.w;
//            int h = this.h;
//            float minValue = this.minValue;
//            float maxValue = this.maxValue;
//
//
//            float yRange = ((maxValue) - (minValue));
//            float absRange = Math.max(Math.abs(maxValue), Math.abs(minValue));
//            if (absRange < Float.MIN_NORMAL) absRange = 1;
//
//
//            float alpha = 1f / ns;
//            int first = firstSample(), last = lastSample();
//            assert (series.size() == 1) : "only size=1 support for now";
//            int sn = series.get(0).size();
//            for (Series s : series) {
//
//                for (int x = 0; x < w; x++) {
//
//                    float sStart = first + (last - first) * (x / ((float) w));
//                    float sEnd = first + (last - first) * ((x + 1) / ((float) w));
//
//                    int iStart = Util.clampSafe((int) Math.ceil(sStart), 0, sn - 1);
//                    int iEnd = Util.clampSafe((int) Math.floor(sEnd), 0, sn - 1);
//                    float amp = 0;
//
//                    amp += (iStart - sStart) * s.get(iStart);
//                    for (int i = iStart + 1; i < iEnd - 1; i++)
//                        amp += s.get(i);
//                    amp += (sEnd - iEnd) * s.get(iEnd);
//
//                    amp /= (sEnd - sStart);
//
//                    float ampNormalized = (amp - minValue) / yRange;
//
//                    float intensity = Math.abs(amp) / absRange;
//                    //gfx.setColor(Color.getHSBColor(intensity, 0.7f, 0.7f));
//                    float[] sc = s.color();
//                    float iBase = Util.unitize(intensity / 2 + 0.5f);
//                    gfx.setColor(new Color(sc[0] * iBase, sc[1] * iBase, sc[2] * iBase, alpha));
//
//                    int ah = Math.round(ampNormalized * h);
//                    gfx.drawLine(x, h / 2 - ah / 2, x, h / 2 + ah / 2);
//                }
//            }
//        }
//
//        private float sampleX(int x, int w, int first, int last) {
//            return ((float) x) / w * (last - first) + first;
//        }
//
//        public synchronized void pan(float pct) {
//            float width = last - first;
//            if (width < 1) {
//                float mid = ((first + last) / 2);
//                float nextMid = mid + (pct * width);
//
//                float first = nextMid - width / 2;
//                float last = nextMid + width / 2;
//                if (first < 0) {
//                    first = 0;
//                    last = first + width;
//                } else if (last > 1) {
//                    last = 1;
//                    first = last - width;
//                }
//
//                this.first = first;
//                this.last = last;
//            }
//
//            update();
//        }
//
//        public synchronized void scale(float pct) {
//
//            float first = this.first, last = this.last;
//            float view = last - first;
//            float mid = (last + first) / 2;
//            float viewNext = Util.clamp(view * pct, ScalarValue.EPSILON, 1);
//
//            first = mid - viewNext / 2;
//            last = mid + viewNext / 2;
//            if (last > 1) {
//                last = 1;
//                first = last - viewNext;
//            }
//            if (first < 0) {
//                first = 0;
//                last = first + viewNext;
//            }
//
//            this.first = first;
//            this.last = last;
//            update();
//        }
//
//    }

    public abstract static class AbstractSeries implements Series {
        protected int capacity;
        private final float[] color = {1, 1, 1, 0.75f};
        private String name;
        protected transient float minValue, maxValue, meanValue;

        @Override
        public abstract float get(int i);

        @Override
        public abstract void clear();

        protected void setName(String name) {
            AbstractSeries.this.name = name;
            Draw.colorHash(name, color());
        }

        @Override
        public void update() {

        }

        @Override
        public abstract int size();

        private final AutoRange autoRange = new AutoRange();

        public Series autorange() {

            forEach(autoRange.restart());

            int n = autoRange.count[0];
            if (n == 0) {
                this.minValue = this.maxValue = this.meanValue = Float.NaN;
                return this;
            }

            AbstractSeries.this.minValue = autoRange.minMax[0];
            AbstractSeries.this.maxValue = autoRange.minMax[1];

            AbstractSeries.this.meanValue = (float) (autoRange.mean[0] / n);
            return AbstractSeries.this;
        }

        abstract public void forEach(FloatProcedure f);

        public Series range(float min, float max) {
            AbstractSeries.this.minValue = min;
            AbstractSeries.this.maxValue = max;
            return AbstractSeries.this;
        }

        abstract void limit();

        @Override
        public String name() {
            return name;
        }

        @Override
        public int capacity() {
            return capacity;
        }

        @Override
        public float maxValue() {
            return maxValue;
        }

        @Override
        public float minValue() {
            return minValue;
        }

        @Override
        public float meanValue() {
            return meanValue;
        }

        @Override
        public float[] color() {
            return color;
        }

        private static class AutoRange implements FloatProcedure {
            /** min, max, mean */
            final float[] minMax = new float[2];
            final double[] mean = {0};
            final int[] count = {0}; //counts non-NaN values

            AutoRange restart() {
                minMax[0] = Float.POSITIVE_INFINITY; minMax[1] = Float.NEGATIVE_INFINITY;
                mean[0] = 0; count[0] = 0;
                return this;
            }

            @Override
            public void value(float v) {
                if (v == v) {
                    if (v < minMax[0]) minMax[0] = v;
                    if (v > minMax[1]) minMax[1] = v;
                    mean[0] += v;
                    count[0]++;
                }
            }


        }
    }
}

