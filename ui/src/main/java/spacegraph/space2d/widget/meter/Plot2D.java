package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.event.On;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.video.Draw;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

import static java.lang.Float.NaN;
import static jcog.Texts.n4;

public class Plot2D extends Widget {
    private final List<Series> series;
    private String title;
    private On on;
    private volatile boolean requireUpdate = false;

    public void setTitle(String title) {
        this.title = title;
    }

    public Plot2D on(Function<Runnable, On> trigger) {
        synchronized (series) {
            if (on != null)
                on.off();
            this.on = trigger.apply(this::update);
        }
        return this;
    }


    public static class Series extends FloatArrayList {

        String name;

        /**
         * history size
         */
        private final int capacity;

        transient float maxValue;
        transient float minValue;

        float[] color = {1, 1, 1, 0.75f};

        @Override
        public float[] toArray() {
            return items;
        }



        @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
        public Series(String name, int capacity) {
            super(capacity);
            setName(name);
            this.capacity = capacity;
        }

        public Series(String name, float[] data) {
            super(data);
            setName(name);
            capacity = data.length;
        }

        void setName(String name) {
            this.name = name;
            Draw.colorHash(name, color);
        }


        @Override
        public String toString() {
            return name + '[' + size() + '/' + capacity + ']';
        }

        public void update() {

        }

        public Series autorange() {
            minValue = Float.POSITIVE_INFINITY;
            maxValue = Float.NEGATIVE_INFINITY;
            forEach(v -> {
                if (v < minValue) minValue = v;
                if (v > maxValue) maxValue = v;
            });
            return this;
        }

        public Series range(float min, float max) {
            minValue = min;
            maxValue = max;
            return this;
        }

        void limit() {
            int over = size() - (this.capacity - 1);
            for (int i = 0; i < over; i++)
                removeAtIndex(0);
        }

        float[] array() {
            return items;
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
        return add(new Series(name, data).autorange());
    }

    public Plot2D add(String name, float[] data, float min, float max) {
        return add(new Series(name, data).range(min, max));
    }

    public Plot2D add(String name, DoubleSupplier valueFunc, float min, float max) {
        Series s;
        add(s = new Series(name, maxHistory) {
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
        add(new Series(name, maxHistory) {
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
    protected void paintWidget(GL2 gl, RectFloat2D bounds) {
        Draw.bounds(bounds, gl, this::paintUnit);
    }

    private void paintUnit(GL2 gl) {

        if (requireUpdate) {
            update();
            requireUpdate = false;
        }

        List<Series> series = this.series;


        if (series.isEmpty()) {
            return;
        }


        gl.glColor4fv(backgroundColor, 0);
        Draw.rect(gl, 0, 0, 1, 1);

        vis.draw(series, gl, minValue, maxValue);

        if (title != null) {


            gl.glColor3f(1f, 1f, 1f);
            gl.glLineWidth(1f);
            Draw.hersheyText(gl, title, 0.1f, 0.5f, 0.5f, 0);

        }

    }

    @FunctionalInterface
    public interface PlotVis {

        /**
         * externally triggered update function
         */
        default void update() {

        }

        void draw(List<Series> series, GL2 g, float minValue, float maxValue);

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

    public static final PlotVis BarWave = (List<Series> series, GL2 g, float minValue, float maxValue) -> {
        if (minValue != maxValue) {

            float w = 1.0f;
            float h = 1.0f;


            for (Series s : series) {
                int histSize = s.size();

                float dx = (w / histSize);

                float x = 0;
                float prevX = 0;

                float[] ss = s.toArray();
                int len = Math.min(s.size(), ss.length);
                float range = maxValue - minValue;
                for (int i = 0; i < len; i++) {
                    float v = ss[i];

                    float py = (v - minValue) / range;
                    if (py < 0) py = 0;
                    if (py > 1.0) py = 1.0f;

                    float y = py * h;

                    g.glColor4fv(s.color, 0);

                    Draw.rect(g, prevX, h / 2.0f - y / 2f, dx, y);

                    prevX = x;
                    x += dx;
                }

            }
        }
    };


    private final float[] backgroundColor = {0, 0, 0, 0.75f};

// TODO
//    float _minValue = NaN, _maxValue = NaN;
//    String minValueStr = "", maxValueStr = "";

    public static final PlotVis Line = (List<Series> series, GL2 gl, float minValue, float maxValue) -> {
        if (minValue == maxValue) {
            float center = minValue;
            minValue = center - (center / 2);
            maxValue = center + (center / 2);
        }

        gl.glColor4f(1f, 1f, 1f, 1f);

        gl.glLineWidth(2);

        float W = 1.0f;
        Draw.line(gl, 0, 0, W, 0);
        float H = 1.0f;
        Draw.line(gl, 0, H, W, H);

        Draw.hersheyText(gl, n4(minValue), 0.04f, 0, 0, 0, Draw.TextAlignment.Left);
        Draw.hersheyText(gl, n4(maxValue), 0.04f, 0, H, 0, Draw.TextAlignment.Left);


        for (Series s : series) {

            float mid = ypos(minValue, maxValue, (s.minValue + s.maxValue) / 2f);


            int ss = s.size();

            float[] ssh = s.array();

            int histSize = ss;


            float range = maxValue - minValue;
            float yy = NaN;
            if (range > Float.MIN_NORMAL) {

                gl.glLineWidth(3);
                gl.glColor3fv(s.color, 0);
                gl.glBegin(
                        GL.GL_LINE_STRIP


                );
                float x = 0;
                float dx = (W / histSize);


                for (int i = 0; i < ss; i++) {

                    float v = ssh[i];
                    float ny = (v == v) ? ypos(minValue, range, v) : mid /*HACK for NaN*/;


                    gl.glVertex2f(x, yy = ny);


                    x += dx;
                }
                gl.glEnd();
            }

            if (yy != yy)
                yy = 0.5f;

            gl.glLineWidth(2);
            Draw.hersheyText(gl, s.name, 0.04f, W, yy, 0, Draw.TextAlignment.Right);

        }
    };

    private static float ypos(float minValue, float range, float v) {
        float ny = (v - minValue) / range;


        return ny;
    }


    public void update() {
        synchronized (series) {

            series.forEach(Series::update);

            minValue = Float.POSITIVE_INFINITY;
            maxValue = Float.NEGATIVE_INFINITY;
            series.forEach((Series s) -> {
                minValue = Math.min(minValue, s.minValue);
                maxValue = Math.max(maxValue, s.maxValue);
            });

            vis.update();
        }
    }


    public static class BitmapWave implements PlotVis, BitmapMatrixView.BitmapPainter {
        BitmapMatrixView bmp = null;
        private final int w;
        private final int h;


        transient private List<Series> series;
        transient private float minValue, maxValue;
        private Graphics gfx;

        volatile boolean update = false;

        public BitmapWave(int w, int h) {
            this.w = w;
            this.h = h;
        }

        @Override
        public void stop() {
            if (gfx!=null)  {
                gfx.dispose();
                gfx = null;
            }
            if (bmp!=null) {
                bmp.stop();
                bmp = null;
            }
        }

        @Override
        public void draw(List<Series> series, GL2 g, float minValue, float maxValue) {
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
                update = !bmp.update(); //keep updating till updated
            }

            bmp.paintMatrix(g);
        }

        @Override
        public void update() {
            update = true;
        }



        @Override
        public void update(BufferedImage buf, int[] pix) {


            if (gfx==null) {
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



            float alpha= 1f / ns;
            for (Series s : series) {
                int n = s.size();
                for (int x = 0; x < w; x++) {
                    float sStart = Math.max(0, (sampleX(x,w,n)));
                    int iStart = Util.clamp((int)Math.floor(sStart+1), 0, n-1);
                    float sEnd = Math.min(n, (sampleX(x+1,w,n)));
                    int iEnd = Util.clamp((int)Math.ceil(sEnd-1), 0, n-1);
                    float amp = 0;

                    amp += (iStart - sStart) * s.get(iStart);
                    for (int i = iStart+1; i < iEnd-1; i++)
                        amp += s.get(i);
                    amp += (sEnd - iEnd) * s.get(iEnd);

                    amp /= (sEnd-sStart);

                    float ampNormalized = (amp - minValue) / yRange;

                    float intensity = Math.abs(amp)/absRange;
                    //gfx.setColor(Color.getHSBColor(intensity, 0.7f, 0.7f));
                    float[] sc = s.color;
                    float iBase = Util.unitize(intensity/2 + 0.5f);
                    gfx.setColor(new Color(sc[0] * iBase, sc[1] * iBase, sc[2] * iBase, alpha));

                    int ah = Math.round(ampNormalized * h);
                    gfx.drawLine(x, h/2 - ah/2, x, h/2 + ah/2);
                }
            }
        }

        private float sampleX(int x, int w, int n) {
            return ( ((float)x) / w * n );
        }
    }
}

