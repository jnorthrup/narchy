package spacegraph.container;

import com.google.common.collect.Iterables;
import jcog.Util;
import jcog.list.FasterList;
import spacegraph.Surface;

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
public class Gridding extends MutableContainer {


    public static final float HORIZONTAL = 0f;
    public static final float VERTICAL = Float.POSITIVE_INFINITY;
    public static final float SQUARE = 0.5f;

    protected float margin = 0.05f;
    float gridAspect = Float.NaN;

    public Gridding(Surface... children) {
        this(SQUARE, children);
    }

    public Gridding(float margin, float aspect, Surface... children) {
        this(children);
        this.margin = margin;
    }

    public Gridding(List<Surface> children) {
        this(SQUARE, children);
    }

    public Gridding(float aspect, Surface... children) {
        super();
        this.gridAspect = (aspect);
        set(children);
    }

    public Gridding(float aspect, List<Surface> children) {
        super();
        this.gridAspect = (aspect);
        set(children);
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
    //    @Override
//    public void transform(GL2 gl, v2 globalScale) {
//        super.transform(gl, globalScale);
//
//        if (!children.isEmpty() && isGrid())  {
//            float xx = scale.x * globalScale.x;
//            float yy = scale.y * globalScale.y;
//            //if ((lw != xx) || (lh != yy)) {
//                layout();
//                lw = xx;
//                lh = yy;
//            //}
//        }
//    }

    @Override
    public void doLayout(int dtMS) {

        Surface[] children = this.children();
        if (children == null) return;
        int n = children.length;
        float a = gridAspect;
//        if ((n < 3) && !((a==0) || (a == Float.POSITIVE_INFINITY)))
//            a = 0; //use linear layout for small n


        if (a!=0 && Float.isFinite(a)) {

            //determine the ideal rows and columns of the grid to match the visible aspect ratio
            //in a way that keeps each grid cell as close to 1:1 as possible

            //TODO use the 'a' value to adjust the x/y balance, currently it is not

            float actualAspect = h()/ w();

            int x;
            int s = (int) Math.floor((float)Math.sqrt(n));
            if (actualAspect/a > 1f) {
                x = Math.round(lerp((actualAspect)/n, s, 1f));
            } else if (actualAspect/a < 1f) {
                //TODO fix
                x = Math.round(lerp(1f-(1f/actualAspect)/n, n, (float)s));
            } else {
                x = s;
            }

            x = Math.max(1, x);
            int y = (int)Math.max(1, Math.ceil((float)n / x));

            assert(y*x >= s);

            if (y==1) {
                a = 0; //row
            } else if (x == 1) {
                a = Float.POSITIVE_INFINITY; //column
            } else {
                layoutGrid(children, x, y, margin);
                return;
            }
        }

        if (a == 0) {
            //horizontal
            layoutGrid(children, n, 1, margin);
        } else /*if (!Float.isFinite(aa))*/ {
            //vertical
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

            float px = hm;//margin / 2f;

            final float py = (((ny-1)-y) * dy) + hm;
            float y1 = py * H;

            for (int x = 0; x < nx; x++) {
                //System.out.println("\t" + px + " " + py);

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

    static Surface[] array(Collection<? extends Surface> content) {
        return content.toArray(new Surface[content.size()]);
    }

    public static Gridding row(Surface... content) {
        return new Gridding(HORIZONTAL, content);
    }
    public static Gridding col(Surface... content) {
        return new Gridding(VERTICAL, content);
    }
    public static Gridding grid(int num, IntFunction<Surface> build) {
        Surface[] x = Util.map(0, num, build, Surface[]::new);
        return new Gridding(new FasterList(x));
    }

}
