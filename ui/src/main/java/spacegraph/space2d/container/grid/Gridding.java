package spacegraph.space2d.container.grid;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.data.list.FasterList;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Splitting;
import spacegraph.space2d.container.collection.MutableListContainer;
import spacegraph.space2d.widget.Widget;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import static jcog.Util.lerp;

/** TODO parameterize DX/DY to choose between row, column, or grid of arbitrary aspect ratio
       aspect ratio=0: row (x)
    aspect ratio=+inf: col (x)
                 else: grid( %x, %(ratio * x) )
 */
public class Gridding extends MutableListContainer {


    public static final float HORIZONTAL = 0f;
    public static final float VERTICAL = Float.POSITIVE_INFINITY;

    /** https://en.wikipedia.org/wiki/Golden_ratio */
    public static final float PHI = 0.618f;

    protected float margin = Widget.marginPctDefault;

    private float aspect;

    public Gridding() {
        this(PHI);
    }

    public Gridding(Surface... children) {
        this(PHI, children);
    }

    public Gridding(float margin, float aspect, Surface/*...*/ children) {
        this(aspect);
        set(children);
        this.margin = margin;
    }

    public Gridding(List<Surface> children) {
        this(PHI, children);
    }

    public Gridding(float aspect, Surface... children) {
        this(aspect);
        set(children);
    }

    protected Gridding(float aspect, List<Surface> children) {
        this(aspect);
        set(children);
    }

    private Gridding(float aspect) {
        super();
        this.aspect = (aspect);
    }

    public boolean isGrid() {
        float a = aspect;
        return a!=0 && a!=Float.POSITIVE_INFINITY;
    }

    public Gridding aspect(float gridAspect) {
        this.aspect = gridAspect;
        layout(); //TODO only if gridAspect change
        return this;
    }
    














    @Override
    public void doLayout(int dtMS) {

        Surface[] children = this.children();

        int n = children.length;
        if (n == 0) return;

        float a = aspect;




        if (a!=0 && Float.isFinite(a)) {

            
            

            

            float h = h();
            float w = w();
            if (w < Float.MIN_NORMAL || h < Float.MIN_NORMAL)
                return;

            float actualAspect = h / w;

            int x;
            int s = (int) Math.ceil((float)Math.sqrt(n));
            if (actualAspect/a > 1f) {
                x = Math.round(lerp((actualAspect)/n, s, 1f));
            } else if (actualAspect/a < 1f) {
                
                x = Math.round(lerp(1f-(1f/actualAspect)/n, n, (float)s));
            } else {
                x = s;
            }

            x = Math.max(1, x);
            int y = (int)Math.max(1, Math.ceil((float)n / x));

            assert(y*x >= s);

            if (y==1) {
                a = 0; 
            } else if (x == 1) {
                a = Float.POSITIVE_INFINITY; 
            } else {
                layoutGrid(children, x, y, margin);
                return;
            }
        }

        if (a == 0) {
            
            layoutGrid(children, n, 1, margin);
        } else /*if (!Float.isFinite(aa))*/ {
            
            layoutGrid(children, 1, n, margin);
        }


    }

    protected void layoutGrid(Surface[] children, int nx, int ny, float margin) {
        int i = 0;

        float hm = margin/2f;

        float mx = (1 + 1 + nx/2f) * hm;
        float my = (1 + 1 + ny/2f) * hm;

        float dx = nx > 0 ? (1f-hm)/nx : 0;
        float dxc = (1f - mx)/nx;
        float dy = ny > 0 ? (1f-hm)/ny : 0;
        float dyc = (1f - my)/ny;


        int n = children.length;



        float X = x(), Y = y(), W = w(), H = h();

        for (int y = 0; y < ny; y++) {

            float px = hm;

            final float py = (((ny-1)-y) * dy) + hm;
            float y1 = py * H;

            for (int x = 0; x < nx; x++) {
                

                Surface c = children[layoutIndex(i++)];

                float x1 = px * W;
                c.pos(X+x1, Y+y1, X+x1+dxc*W, Y+y1+dyc*H);

                px += dx;

                if (i >= n) break;
            }


            if (i >= n) break;

        }
    }

    protected int layoutIndex(int i) {
        return i;
    }

    public static Gridding grid(Iterable<? extends Surface> content) {
        return grid( Iterables.toArray(content, Surface.class ) );
    }

    public static Gridding grid(Stream<Surface> content) {
        return grid(content.toArray(Surface[]::new));
    }

    public static Gridding grid(Surface... content) {
        return new Gridding(new FasterList<>(content));
    }
    public static <S> Gridding grid(Collection<S> c, Function<S,Surface> builder) {
        List<Surface> ss  = new FasterList(c.size());
        for (S x : c)
            ss.add(builder.apply(x));
        return new Gridding(ss);
    }

    public static Gridding row(Collection<? extends Surface> content) {
        return row(array(content));
    }
    public static Gridding column(Collection<? extends Surface> content) {
        return column(array(content));
    }

    private static Surface[] array(Collection<? extends Surface> content) {
        return content.toArray(new Surface[0]);
    }

    public static Gridding row(Surface... content) {
        return new Gridding(HORIZONTAL, content);
    }

    public static Gridding column(Surface... content) {
        return new Gridding(VERTICAL, content);
    }

    public static Surface row(Surface x, Surface y) {
        return Splitting.row(x, y);
    }
    public static Surface column(Surface x, Surface y) {
        return Splitting.column(x, y);
    }

    public static Gridding grid(int num, IntFunction<Surface> build) {
        Surface[] x = Util.map(0, num, Surface[]::new, build);
        return new Gridding(x);
    }

    public void vertical() {
        aspect(VERTICAL);
    }
    public void horizontal() {
        aspect(HORIZONTAL);
    }
    public void square() {
        aspect(PHI);
    }

    public Gridding margin(float i) {
        this.margin = i;
        return this;
    }
}
