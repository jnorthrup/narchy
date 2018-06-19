package nars.video;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import com.jogamp.opengl.GL2;
import georegression.geometry.UtilPolygons2D_I32;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_I32;
import georegression.struct.shapes.Rectangle2D_I32;
import jcog.signal.Bitmap2D;
import nars.$;
import nars.NAR;
import nars.agent.NAgent;
import nars.control.NARService;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.term.atom.Int;
import nars.truth.Truth;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.video.Draw;
import spacegraph.video.Tex;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL2.GL_POLYGON;
import static nars.Op.BELIEF;
import static nars.Op.SETe;
import static nars.time.Tense.ETERNAL;






public class ShapeSensor extends NARService {

    private final Bitmap2D input;
    private final CauseChannel<ITask> in;
    private final Term id;


    /** filtered */
    GrayU8 img = null;

    /** unfiltered */
    GrayF32 imgF;

    

    
    static double minimumSideFraction = 0.25;

    
    

    private final float R = 1f;
    private final float G = 1f;
    private final float B = 1f;

    private Grid grid;

    private GrayU8 filtered;
    private final Tex filteredTex = new Tex();
    private BufferedImage filteredRGB;


    class ShapeSensorControl extends Gridding {
        public ShapeSensorControl() {
            super(
                    new ShapeSensorSurface(),
                    filteredTex.view()
            );
        }
    }
    class ShapeSensorSurface extends Surface {

        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {

            if (grid!=null) {
                final int[] i = {0};
                grid.image.forEach(pSet->{
                   

                    float scale = Math.max(w(), h()) / Math.max(grid.gx, grid.gy);

                    float dx = x();
                    float dy = y();
                    gl.glLineWidth(2f);
                    
                    
                    gl.glBegin(GL_POLYGON);
                    
                    Draw.colorHash(gl, i[0], 0.75f);
                    for (Term xy : pSet.subterms()) {
                        int x = ((Int)xy.sub(0)).id;
                        int y = grid.gy - ((Int)xy.sub(1)).id;
                        gl.glVertex2f(dx + x * scale, dy + y * scale);
                    }
                    gl.glEnd();
                    i[0]++;




                });
            }
        }
    }

    public ShapeSensor(Term id, Bitmap2D input, NAgent a) {
        super(a.nar());
        this.id = id;
        this.input = input;













        in = a.nar().newChannel(this);

        a.onFrame(this::update);










    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        SpaceGraph.window(new ShapeSensorControl(), 400, 800);
    }

    public static boolean isConvex(List<PointIndex_I32> poly) {

        int n = poly.size();
        if (n < 4)
            return true;

        boolean sign = false;
        for (int i = 0; i < n; i++) {
            PointIndex_I32 a = poly.get((i + 2) % n);
            PointIndex_I32 b = poly.get((i + 1) % n);
            double dx1 = a.x - b.x;
            double dy1 = a.y - b.y;
            PointIndex_I32 c = poly.get(i);
            double dx2 = c.x - b.x;
            double dy2 = c.y - b.y;
            double zcrossproduct = dx1 * dy2 - dy1 * dx2;
            if (i == 0)
                sign = zcrossproduct > 0;
            else if (sign != (zcrossproduct > 0))
                return false;
        }
        return true;
    }


    long now = ETERNAL;

    public void update() {

        long last = now;
        now = nar.time();
        if (last == ETERNAL)
            last = now;

        input.update();

        if (imgF == null || imgF.width != input.width() || imgF.height != input.height()) {
            imgF = new GrayF32(input.width(), input.height());
            img = new GrayU8(input.width(), input.height());
        }

        int w = imgF.width;
        int h = imgF.height;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                float b = input.brightness(x, y, R, G, B);
                imgF.unsafe_set(x, y, b);
                
            }
        }

        







        
        float mean = ImageStatistics.mean(imgF);

        
        ThresholdImageOps.threshold(imgF, img, mean, true);

        
        
        filtered = BinaryImageOps.dilate8(img, 1, null);
        filtered = BinaryImageOps.erode8(filtered, 1, null);
        

        GrayU8 filteredShown = filtered.clone();
        byte[] data = filteredShown.data;
        for (int i = 0, dataLength = data.length; i < dataLength; i++) {
            data[i] = (byte) (data[i] * 255);
        }
        filteredRGB = filteredTex.update(filteredShown, filteredRGB);


        













        List<Contour> contours = BinaryImageOps.contour(filtered,
                
                ConnectRule.FOUR,
                null);
        Grid g = new Grid(id, 12, 12, w, h) {


















        };

        int k = 0;
        for (Contour c : contours) {
            
            List<PointIndex_I32> outer = ShapeFittingOps.fitPolygon(c.external,
                    false, Math.min(g.w/g.gx, g.h/g.gy),
                    minimumSideFraction);



            g.addPoly(k++, outer, true);









        }

        g.input(in, last, nar);

        this.grid = g;








    }

    static class Grid {
        public final int gx, gy, w, h;
        private final Term id;
        float sx, sy;
        final Set<Term> image = new LinkedHashSet();

        public Grid(Term id, int gx, int gy, int w, int h) {
            this.id = id;
            this.gx = gx;
            this.gy = gy;
            this.w = w;
            this.h = h;
            this.sx = (gx - 1f) / w;
            this.sy = (gy - 1f) / h;
        }

        public void clear() {
            image.clear();
        }

        public Term point(int px, int py) {
            int x = Math.round(px * sx);
            int y = Math.round(py * sy);
            return $.p($.the(x), $.the(y));
        }

        public Term line(int ax, int ay, int bx, int by) {
            Term a = point(ax, ay);
            Term b = point(bx, by);

            
            int ab = a.compareTo(b);
            return ab <= 0 ? $.p(a, b) : $.p(b, a);
        }

        public void input(Consumer<ITask> t, long last, NAR n) {
            long now = n.time();
            for (Term x : image) {
                ITask xx = new SignalTask($.inh(x,id), BELIEF, $.t(1f, n.confDefault(BELIEF)),
                        last, now, n.time.nextStamp()).pri(n.priDefault(BELIEF));
                t.accept(xx);
            }
        }

        public void addPoly(int polyID, List<PointIndex_I32> poly, boolean outerOrInner) {
            TreeSet<Term> ts = new TreeSet();
            int ps = poly.size();
            for (int i = 0; i < ps; i++) {
                PointIndex_I32 a = poly.get(i);
                PointIndex_I32 b = poly.get((i + 1) % ps);
                Term ll = line(a.x, a.y, b.x, b.y);
                
                ts.add(ll.sub(0));
                ts.add(ll.sub(1)); 
            }

            if(!ts.isEmpty())
                image.add(SETe.the((Collection) ts));
        }
    }



    private void inputQuadBlob(int k, List<PointIndex_I32> polygon, float w, float h) {
        Polygon2D_I32 p = new Polygon2D_I32(polygon.size());
        for (PointIndex_I32 v : polygon)
            p.vertexes.add(v);
        Rectangle2D_I32 quad = new Rectangle2D_I32();
        UtilPolygons2D_I32.bounding(p, quad);
        float cx = ((quad.x0 + quad.x1) / 2f) / w;
        float cy = ((quad.y0 + quad.y1) / 2f) / h;
        float cw = quad.getWidth() / w;
        float ch = quad.getHeight() / h;
        Term pid = $.p(id, $.the(k));
        float conf = nar.confDefault(BELIEF);

        long now = nar.time();
        believe(now, $.inh(pid, $.the("x")), $.t(cx, conf));
        believe(now, $.inh(pid, $.the("y")), $.t(cy, conf));
        believe(now, $.inh(pid, $.the("w")), $.t(cw, conf));
        believe(now, $.inh(pid, $.the("h")), $.t(ch, conf));

    }

    private void believe(long now, Term term, Truth truth) {
        float pri = nar.priDefault(BELIEF);
        in.input(new NALTask(term, BELIEF, truth, now, now, now, nar.evidence()).pri(pri));
    }


    /**
     * Demonstration of how to convert a point sequence describing an objects outline/contour into a sequence of line
     * segments.  Useful when analysing shapes such as squares and triangles or when trying to simply the low level
     * pixel output.
     *
     * @author Peter Abeles
     */




















































































































































































































































































































































    private static class ScaleOffset {
        double scale, offsetX, offsetY;
    }

    public static <T extends Point2D_I32> void drawPolygon(List<T> vertexes, boolean loop, Graphics2D g2) {
        for (int i = 0; i < vertexes.size() - 1; i++) {
            Point2D_I32 p0 = vertexes.get(i);
            Point2D_I32 p1 = vertexes.get(i + 1);
            g2.drawLine(p0.x, p0.y, p1.x, p1.y);
        }
        if (loop && !vertexes.isEmpty()) {
            Point2D_I32 p0 = vertexes.get(0);
            Point2D_I32 p1 = vertexes.get(vertexes.size() - 1);
            g2.drawLine(p0.x, p0.y, p1.x, p1.y);
        }
    }
}

