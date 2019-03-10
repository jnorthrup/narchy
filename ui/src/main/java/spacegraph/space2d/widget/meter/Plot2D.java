package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.event.Off;
import jcog.pri.ScalarValue;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import spacegraph.space2d.widget.Widget;
import spacegraph.video.Draw;
import spacegraph.video.font.HersheyFont;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

import static java.lang.Float.NaN;

public class Plot2D extends Widget {
    public final List<Series> series;
    private String title;
    private Off on;
    private volatile boolean requireUpdate = false;

    public void setTitle(String title) {
        this.title = title;
    }

    public Plot2D on(Function<Runnable, Off> trigger) {
        synchronized (series) {
            if (on != null)
                on.off();
            this.on = trigger.apply(this::commit);
        }
        return this;
    }


    public interface Series {

        String name();

        void update();

        float maxValue();

        float minValue();

        float[] color();

        int capacity();

        int size();

        float get(int i);

        void clear();

        //void forEach(IntFloatConsumer value);
    }

    public static class ArraySeries extends FloatArrayList implements Series {

        private String name;

        private final int capacity;

        private transient float maxValue;
        private transient float minValue;

        private float[] color = {1, 1, 1, 0.75f};

        @Override
        public float[] toArray() {
            return items;
        }

        @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
        public ArraySeries(String name, int capacity) {
            super(capacity);
            setName(name);
            this.capacity = capacity;
        }

        public ArraySeries(String name, float[] data) {
            super(data);
            setName(name);
            capacity = data.length;
        }

        private void setName(String name) {
            this.name = name;
            Draw.colorHash(name, color());
        }

        @Override
        public float get(int index) {
            float[] ii = this.items;
            if (ii.length > index)
                return ii[index];
            else
                return Float.NaN; //HACK
        }

        @Override
        public String toString() {
            return name();
        }

        @Override
        public void update() {

        }

        public Series autorange() {
            this.minValue = Float.POSITIVE_INFINITY;
            this.maxValue = Float.NEGATIVE_INFINITY;
            forEach(v -> {
                if (v < minValue()) this.minValue = v;
                if (v > maxValue()) this.maxValue = v;
            });
            return this;
        }

        public Series range(float min, float max) {
            this.minValue = min;
            this.maxValue = max;
            return this;
        }

        void limit() {
            int over = size() - (this.capacity - 1);
            for (int i = 0; i < over; i++)
                removeAtIndex(0);
        }

        public float[] array() {
            return items;
        }

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
        public float[] color() {
            return color;
        }


    }

    private transient float minValue, maxValue;

    private final int maxHistory;


    private final PlotVis vis;


    public Plot2D(int history, PlotVis vis) {


        this.series = new FasterList();
        this.maxHistory = history;

        this.vis = vis;

    }

    public Plot2D add(Series s) {
        series.add(s);
        requireUpdate = true;
        return this;
    }

    public Plot2D add(String name, float[] data) {
        return add(new ArraySeries(name, data).autorange());
    }

    public Plot2D add(String name, float[] data, float min, float max) {
        return add(new ArraySeries(name, data).range(min, max));
    }

    public Plot2D add(String name, DoubleSupplier valueFunc, float min, float max) {
        ArraySeries s;
        add(s = new ArraySeries(name, maxHistory) {
            @Override
            public void update() {
                double v = valueFunc.getAsDouble();

                limit();
                if (v != v) {

                    super.add(NaN);
                } else {
                    if (v < min) v = min;
                    if (v > max) v = max;
                    super.add((float) v);
                }

            }
        });
        s.minValue = min;
        s.maxValue = max;
        return this;
    }

    public Plot2D add(String name, DoubleSupplier valueFunc) {
        add(new ArraySeries(name, maxHistory) {
            @Override
            public void update() {
                limit();
                super.add((float) valueFunc.getAsDouble());
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

        if (requireUpdate) {
            commit();
            requireUpdate = false;
        }

        List<Series> series = this.series;


        if (series.isEmpty()) {
            return;
        }


        gl.glColor4fv(backgroundColor, 0);
        Draw.rect(gl, 0, 0, 1, 1);

        vis.draw(series, minValue, maxValue, gl);

        if (title != null) {


            gl.glColor3f(1f, 1f, 1f);
            gl.glLineWidth(1f);
            HersheyFont.hersheyText(gl, title, 0.1f, 0.5f, 0.5f, 0);

        }

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

    @Override
    public boolean stop() {
        if (super.stop()) {
            vis.stop();
            return true;
        }
        return false;
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


    private final float[] backgroundColor = {0, 0, 0, 0.75f};

// TODO
//    float _minValue = NaN, _maxValue = NaN;
//    String minValueStr = "", maxValueStr = "";

    public static final PlotVis Line = (List<Series> series, float minValue, float maxValue, GL2 gl) -> {
        plotLine(series, gl, minValue, maxValue, false,false);
    };
    public static final PlotVis LineLanes = (List<Series> series, float minValue, float maxValue, GL2 gl) -> {
        plotLine(series, gl, minValue, maxValue, true, false);
    };
    public static final PlotVis BarLanes = (List<Series> series, float minValue, float maxValue, GL2 gl) -> {
        plotLine(series, gl, minValue, maxValue, true, true);
    };

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
        float textScale = 1f/(seriesSize) * 0.5f;
        float range = maxValue - minValue;
        float laneHeight = 1f/seriesSize;

        for (int i = 0; i < seriesSize; i++) {
            Series s = series.get(i);


            //float mid = ypos((s.minValue() + s.maxValue()) / 2f, lanes, sn, seriesSize, maxValue-minValue, minValue);
//            float base = ypos(s.minValue(), lanes, i, seriesSize,
//                    maxValue-minValue, minValue);
            float yy = NaN;
            float baseY = i * laneHeight;

            if (range > Float.MIN_NORMAL) {

                int histSize = s.size(), histCap = s.capacity();

                float dx = (W / histCap);
                float x = (histCap - histSize) * dx;


                float[] color = s.color();
                float r = color[0], g = color[1], b = color[2];

                if (!filled) {
                    gl.glLineWidth(3);
                    gl.glBegin(GL.GL_LINE_STRIP);
                }

                for (int j = 0; j < histSize; j++) {

                    float v = s.get(j);

                    //gl.glColor3fv(color, 0);
                    float a = ((v==v) ? ((v-minValue)/range) : 0) * 0.5f + 0.5f;
                    gl.glColor4f(r,g,b,a);

                    yy = yLane(ypos(v, minValue, range), i, laneHeight);

                    if (filled) {
                        //TODO fill to an edge (barchart), or the center (waveform)
                        gl.glRectf(x-dx, baseY, x, yy);
                    } else {
                        gl.glVertex2f(x, yy);
                    }
                    x += dx;
                }
                if (filled) {

                } else {
                    gl.glEnd();
                }
            }

            if (yy != yy)
                yy = baseY + laneHeight/2;


            gl.glColor3f(1,1,1);

            gl.glLineWidth(2);


            HersheyFont.hersheyText(gl, s.name(), 0.04f * textScale, W , filled ? 0 : yy, 0, Draw.TextAlignment.Right);


        }
    }

    private static float yLane(float unitY, int i, float laneHeight) {
        return unitY * laneHeight + (laneHeight * i);
    }

    private static float ypos(float v, boolean lanes, int sn, int seriesSize, float range, float minValue) {
        return lanes ? ypos(minValue, range, v, sn, seriesSize) : ypos(v, minValue, range);
    }

    private static float ypos(float v, float minValue, float range) {
        return (v - minValue) / range;
    }
    private static float ypos(float minValue, float range, float v, int lane, int numLanes) {
        return (v == v ? ((v - minValue) / range) : (0.5f)) /numLanes + (((float)lane)/numLanes);
    }

    public void commit() {
        synchronized (series) {


            final float[] minValue = {Float.POSITIVE_INFINITY};
            final float[] maxValue = {Float.NEGATIVE_INFINITY};
            series.forEach((Series s) -> {
                s.update();
                float min = s.minValue();
                float max = s.maxValue();
                if (min==min && max == max) {
                    minValue[0] = Math.min(minValue[0], min);
                    maxValue[0] = Math.max(maxValue[0], max);
                }
            });

            if (Float.isFinite(minValue[0]) && Float.isFinite(maxValue[0])) {
                this.minValue = minValue[0];
                this.maxValue = maxValue[0];
                vis.update();
            }

        }
    }


    /**
     * TODO merge with BitmapWave
     */
    @Deprecated
    public static class BitmapPlot implements PlotVis, BitmapMatrixView.BitmapPainter {
        BitmapMatrixView bmp = null;
        private final int w;
        private final int h;


        transient private List<Series> series;
        transient private float minValue, maxValue;
        private Graphics gfx;

        volatile boolean update = false;

        /**
         * visualization bounds
         */
        float first = 0f, last = 1f;

        public BitmapPlot(int w, int h) {
            this.w = w;
            this.h = h;
        }

        public float first() {
            return first;
        }

        public float last() {
            return last;
        }

        public int firstSample() {
            return (int) Math.floor(first * (series.get(0).size() - 1) /* TODO */);
        }

        public int lastSample() {
            return (int) Math.ceil(last * (series.get(0).size() - 1) /* TODO */);
        }

        @Override
        public void stop() {
            if (gfx != null) {
                gfx.dispose();
                gfx = null;
            }
            if (bmp != null) {
                bmp.stop();
                bmp = null;
            }
        }

        @Override
        public void draw(List<Series> series, float minValue, float maxValue, GL2 g) {
            if (bmp == null) {
                bmp = new BitmapMatrixView(w, h, this) {
                    @Override
                    public boolean alpha() {
                        return true;
                    }
                };
            }
            this.series = series;
            this.minValue = minValue;
            this.maxValue = maxValue;

            if (update) {
                update = !bmp.updateIfNotShowing(); //keep updating till updated
            }

            bmp.paint(g, null);
        }

        @Override
        public void update() {
            update = true;
        }


        @Override
        public synchronized void color(BufferedImage buf, int[] pix) {


            if (gfx == null) {
                gfx = buf.getGraphics();
            }

            gfx.clearRect(0, 0, w, h);

            int ns = series.size();
            if (ns == 0)
                return;

            int w = this.w;
            int h = this.h;
            float minValue = this.minValue;
            float maxValue = this.maxValue;


            float yRange = ((maxValue) - (minValue));
            float absRange = Math.max(Math.abs(maxValue), Math.abs(minValue));
            if (absRange < Float.MIN_NORMAL) absRange = 1;


            float alpha = 1f / ns;
            int first = firstSample(), last = lastSample();
            assert (series.size() == 1) : "only size=1 support for now";
            int sn = series.get(0).size();
            for (Series s : series) {

                for (int x = 0; x < w; x++) {

                    float sStart = first + (last - first) * (x / ((float) w));
                    float sEnd = first + (last - first) * ((x + 1) / ((float) w));

                    int iStart = Util.clampSafe((int) Math.ceil(sStart), 0, sn - 1);
                    int iEnd = Util.clampSafe((int) Math.floor(sEnd), 0, sn - 1);
                    float amp = 0;

                    amp += (iStart - sStart) * s.get(iStart);
                    for (int i = iStart + 1; i < iEnd - 1; i++)
                        amp += s.get(i);
                    amp += (sEnd - iEnd) * s.get(iEnd);

                    amp /= (sEnd - sStart);

                    float ampNormalized = (amp - minValue) / yRange;

                    float intensity = Math.abs(amp) / absRange;
                    //gfx.setColor(Color.getHSBColor(intensity, 0.7f, 0.7f));
                    float[] sc = s.color();
                    float iBase = Util.unitize(intensity / 2 + 0.5f);
                    gfx.setColor(new Color(sc[0] * iBase, sc[1] * iBase, sc[2] * iBase, alpha));

                    int ah = Math.round(ampNormalized * h);
                    gfx.drawLine(x, h / 2 - ah / 2, x, h / 2 + ah / 2);
                }
            }
        }

        private float sampleX(int x, int w, int first, int last) {
            return ((float) x) / w * (last - first) + first;
        }

        public synchronized void pan(float pct) {
            float width = last - first;
            if (width < 1) {
                float mid = ((first + last) / 2);
                float nextMid = mid + (pct * width);

                float first = nextMid - width / 2;
                float last = nextMid + width / 2;
                if (first < 0) {
                    first = 0;
                    last = first + width;
                } else if (last > 1) {
                    last = 1;
                    first = last - width;
                }

                this.first = first;
                this.last = last;
            }

            update();
        }

        public synchronized void scale(float pct) {

            float first = this.first, last = this.last;
            float view = last - first;
            float mid = (last + first) / 2;
            float viewNext = Util.clamp(view * pct, ScalarValue.EPSILON, 1);

            first = mid - viewNext / 2;
            last = mid + viewNext / 2;
            if (last > 1) {
                last = 1;
                first = last - viewNext;
            }
            if (first < 0) {
                first = 0;
                last = first + viewNext;
            }

            this.first = first;
            this.last = last;
            update();
        }

    }
}

