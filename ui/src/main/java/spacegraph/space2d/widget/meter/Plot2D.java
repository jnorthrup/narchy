package spacegraph.space2d.widget.meter;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import jcog.event.On;
import jcog.data.list.FasterList;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.video.Draw;

import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

import static jcog.Texts.n2;

public class Plot2D extends Widget {
    private final List<Series> series;
    private String title;
    private On on;

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

        void autorange() {
            minValue = Float.POSITIVE_INFINITY;
            maxValue = Float.NEGATIVE_INFINITY;
            forEach(v -> {
                if (v < minValue) minValue = v;
                if (v > maxValue) maxValue = v;
                
            });
        }

        public void range(float min, float max) {
            minValue = min;
            maxValue = max;
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


    private PlotVis plotVis;
    

    public Plot2D(int history, PlotVis vis) {
        

        this.series = new FasterList();
        this.maxHistory = history;

        this.plotVis = vis;

    }

    public Plot2D add(Series s) {
        series.add(s);
        return this;
    }

    public Plot2D add(String name, DoubleSupplier valueFunc, float min, float max) {
        Series s;
        add(s = new Series(name, maxHistory) {
            @Override
            public void update() {
                double v = valueFunc.getAsDouble();

                limit();
                if (v != v) {
                    
                    super.add(Float.NaN);
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
        Draw.bounds(gl, bounds, this::paintUnit);
    }

    private void paintUnit(GL2 gl) {

        List<Series> series = this.series;

        
        if (series.isEmpty()) {
            return;
        }













        
        gl.glColor4fv(backgroundColor, 0);
        Draw.rect(gl, 0, 0, 1, 1);

        plotVis.draw(series, gl, minValue, maxValue);

        if (title != null) {



            
            gl.glColor3f(1f, 1f, 1f);
            gl.glLineWidth(1f);
            Draw.text(gl, title, 0.1f, 0.5f, 0.5f, 0);

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
                for (int i = 0; i < len; i++) {
                    float v = ss[i];

                    float py = (v - minValue) / (maxValue - minValue);
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



    private float[] backgroundColor = {0, 0, 0, 0.75f};

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

        Draw.text(gl, n2(minValue), 0.04f, 0, 0, 0, Draw.TextAlignment.Left);
        Draw.text(gl, n2(maxValue), 0.04f, 0, H, 0, Draw.TextAlignment.Left);


        for (Series s : series) {

            float mid = ypos(minValue, maxValue, (s.minValue + s.maxValue) / 2f);


            int ss = s.size();

            float[] ssh = s.array();

            int histSize = ss;


            gl.glLineWidth(3);
            gl.glColor3fv(s.color, 0);

            gl.glBegin(
                    GL.GL_LINE_STRIP


            );
            float range = maxValue - minValue;
            float yy = Float.NaN;
            float x = 0;
            float dx = (W / histSize);


            for (int i = 0; i < ss; i++) {

                float v = ssh[i];
                float ny = (v == v) ? ypos(minValue, range, v) : mid /*HACK for NaN*/;


                gl.glVertex2f(x, yy = ny);


                x += dx;
            }
            gl.glEnd();

            gl.glLineWidth(2);
            Draw.text(gl, s.name, 0.04f, W, yy, 0, Draw.TextAlignment.Right);

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
        }
    }


















































}

