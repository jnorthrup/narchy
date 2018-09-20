package spacegraph.space2d.container;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.data.list.FasterList;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.collection.MutableListContainer;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

import static jcog.Util.lerp;

/** TODO parameterize DX/DY to choose between row, column, or grid of arbitrary aspect ratio
       aspect ratio=0: row (x)
    aspect ratio=+inf: col (x)
                 else: grid( %x, %(ratio * x) )
 */
public class Gridding extends MutableListContainer {


    public static final float HORIZONTAL = 0f;
    public static final float VERTICAL = Float.POSITIVE_INFINITY;
    private static final float SQUARE = 0.5f;

    protected float margin = 0.05f;
    private float gridAspect;

    public Gridding(Surface... children) {
        this(SQUARE, children);
    }

    public Gridding(float margin, float aspect, Surface/*...*/ children) {
        this(aspect);
        set(children);
        this.margin = margin;
    }

    public Gridding(List<Surface> children) {
        this(SQUARE, children);
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
        this.gridAspect = (aspect);
    }

    public boolean isGrid() {
        float a = gridAspect;
        return a!=0 && a!=Float.POSITIVE_INFINITY;
    }

    public Gridding aspect(float gridAspect) {
        this.gridAspect = gridAspect;
        layout();
        return this;
    }
    














    @Override
    public void doLayout(int dtMS) {

        Surface[] children = this.children();

        int n = children.length;
        if (n == 0) return;

        float a = gridAspect;




        if (a!=0 && Float.isFinite(a)) {

            
            

            

            float h = h();
            float w = w();
            if (w < Float.MIN_NORMAL || h < Float.MIN_NORMAL)
                return;

            float actualAspect = h / w;

            int x;
            int s = (int) Math.floor((float)Math.sqrt(n));
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

    private void layoutGrid(Surface[] children, int nx, int ny, float margin) {
        int i = 0;

        float hm = margin/2f;

        float mx = (1 + 1 + nx/2f) * hm;
        float my = (1 + 1 + ny/2f) * hm;

        float dx = nx > 0 ? (1f-hm)/nx : 0;
        float dxc = (1f - mx)/nx;
        float dy = ny > 0 ? (1f-hm)/ny : 0;
        float dyc = (1f - my)/ny;


        int n = children.length;



        float X = x();
        float Y = y();
        float W = w();
        float H = h();

        for (int y = 0; y < ny; y++) {

            float px = hm;

            final float py = (((ny-1)-y) * dy) + hm;
            float y1 = py * H;

            for (int x = 0; x < nx; x++) {
                

                Surface c = children[i++];

                float x1 = px * W;
                c.pos(X+x1, Y+y1, X+x1+dxc*W, Y+y1+dyc*H);
                c.layout();

                px += dx;

                if (i >= n) break;
            }


            if (i >= n) break;

        }
    }

    public static Gridding grid(Iterable<? extends Surface> content) {
        return grid( Iterables.toArray(content, Surface.class ) );
    }

    public static Gridding grid(Surface... content) {
        return new Gridding(new FasterList<>(content));
    }
    public static <S> Gridding grid(Collection<S> c, Function<S,Surface> builder) {
        Surface ss [] = new Surface[c.size()];
        int i = 0;
        for (S x : c) {
            ss[i++] = builder.apply(x);
        }
        return grid(ss);
    }

    public static Gridding row(Collection<? extends Surface> content) {
        return row(array(content));
    }
    public static Gridding col(Collection<? extends Surface> content) {
        return col(array(content));
    }

    private static Surface[] array(Collection<? extends Surface> content) {
        return content.toArray(new Surface[0]);
    }

    public static Gridding row(Surface... content) {
        return new Gridding(HORIZONTAL, content);
    }
    public static Gridding col(Surface... content) {
        return new Gridding(VERTICAL, content);
    }
    public static Gridding grid(int num, IntFunction<Surface> build) {
        Surface[] x = Util.map(0, num, build, Surface[]::new);
        return new Gridding(x);
    }

}
