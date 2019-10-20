package nars.video;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.PointIndex_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import com.jogamp.opengl.GL2;
import georegression.struct.point.Point2D_I32;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.control.NARPart;
import nars.control.channel.CauseChannel;
import nars.game.Game;
import nars.table.dynamic.SeriesBeliefTable;
import nars.term.Term;
import nars.term.atom.IdempotInt;
import spacegraph.SpaceGraph;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.video.Draw;
import spacegraph.video.Tex;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.jogamp.opengl.GL2.GL_POLYGON;
import static nars.Op.BELIEF;
import static nars.Op.SETe;
import static nars.time.Tense.ETERNAL;


public class ShapeSensor extends NARPart {

    static double minimumSideFraction = 0.25;
    private final Bitmap2D input;
    private final CauseChannel<Task> in;
    private final Term id;
    private final Tex filteredTex = new Tex();
    private final What what;
    /**
     * filtered
     */
    GrayU8 img = null;
    /**
     * unfiltered
     */
    GrayF32 imgF;
    long now = ETERNAL;
    private Grid grid;
    private BufferedImage filteredRGB;

    public ShapeSensor(Term id, Bitmap2D input, Game a) {
        super(a.nar());
        this.id = id;
        this.input = input;
        this.what = a.what();

        in = a.nar().newChannel(this);

        a.onFrame(this::update);


    }

    public static boolean isConvex(List<PointIndex_I32> poly) {

        var n = poly.size();
        if (n < 4)
            return true;

        var sign = false;
        for (var i = 0; i < n; i++) {
            var a = poly.get((i + 2) % n);
            var b = poly.get((i + 1) % n);
            double dx1 = a.x - b.x;
            double dy1 = a.y - b.y;
            var c = poly.get(i);
            double dx2 = c.x - b.x;
            double dy2 = c.y - b.y;
            var zcrossproduct = dx1 * dy2 - dy1 * dx2;
            if (i == 0)
                sign = zcrossproduct > 0;
            else if (sign != (zcrossproduct > 0))
                return false;
        }
        return true;
    }

    public static <T extends Point2D_I32> void drawPolygon(List<T> vertexes, boolean loop, Graphics2D g2) {
        for (var i = 0; i < vertexes.size() - 1; i++) {
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

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        SpaceGraph.window(new ShapeSensorControl(), 400, 800);
    }

    public void update() {

        var last = now;
        now = nar.time();
        if (last == ETERNAL)
            last = now;

        input.updateBitmap();

        if (imgF == null || imgF.width != input.width() || imgF.height != input.height()) {
            imgF = new GrayF32(input.width(), input.height());
            img = new GrayU8(input.width(), input.height());
        }

        var w = imgF.width;
        var h = imgF.height;
        for (var x = 0; x < w; x++) {
            for (var y = 0; y < h; y++) {
                var b1 = 1f;
                var g = 1f;
                var r = 1f;
                var b = input.brightness(x, y, r, g, b1);
                imgF.unsafe_set(x, y, b);

            }
        }


        var mean = ImageStatistics.mean(imgF);


        ThresholdImageOps.threshold(imgF, img, mean, true);


        var filtered = BinaryImageOps.dilate8(img, 1, null);
        filtered = BinaryImageOps.erode8(filtered, 1, null);


        var filteredShown = filtered.clone();
        var data = filteredShown.data;
        for (int i = 0, dataLength = data.length; i < dataLength; i++) {
            data[i] *= 255;
        }
        filteredRGB = filteredTex.set(filteredShown, filteredRGB);


        var contours = BinaryImageOps.contour(filtered, ConnectRule.FOUR, null);
        var g = new Grid(id, 12, 12, w, h) {


        };

        var k = 0;
        for (var c : contours) {

            var outer = ShapeFittingOps.fitPolygon(c.external,
                    false, Math.min(g.w / g.gx, g.h / g.gy),
                    minimumSideFraction);


            g.addPoly(k++, outer, true);


        }

        g.input(in, last, what);

        this.grid = g;


    }
//
//    private void inputQuadBlob(int k, List<PointIndex_I32> polygon, float w, float h) {
//        Polygon2D_I32 p = new Polygon2D_I32(polygon.size());
//        for (PointIndex_I32 v : polygon)
//            p.vertexes.add(v);
//        Rectangle2D_I32 quad = new Rectangle2D_I32();
//        UtilPolygons2D_I32.bounding(p, quad);
//        float cx = ((quad.x0 + quad.x1) / 2f) / w;
//        float cy = ((quad.y0 + quad.y1) / 2f) / h;
//        float cw = quad.getWidth() / w;
//        float ch = quad.getHeight() / h;
//        Term pid = $.p(id, $.the(k));
//        float conf = nar.confDefault(BELIEF);
//
//        long now = nar.time();
//        believe(now, $.inh(pid, $.the("x")), $.t(cx, conf));
//        believe(now, $.inh(pid, $.the("y")), $.t(cy, conf));
//        believe(now, $.inh(pid, $.the("w")), $.t(cw, conf));
//        believe(now, $.inh(pid, $.the("h")), $.t(ch, conf));
//
//    }

//    private void believe(long now, Term term, Truth truth) {
//        float pri = nar.priDefault(BELIEF);
//        in.accept(NALTask.the(term, BELIEF, truth, now, now, now, nar.evidence()).pri(pri));
//    }

    static class Grid {
        public final int gx;
        public final int gy;
        public final int w;
        public final int h;
        final Set<Term> image = new LinkedHashSet();
        private final Term id;
        float sx;
        float sy;

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
            var x = Math.round(px * sx);
            var y = Math.round(py * sy);
            return $.p($.the(x), $.the(y));
        }

        public Term line(int ax, int ay, int bx, int by) {
            var a = point(ax, ay);
            var b = point(bx, by);


            var ab = a.compareTo(b);
            return ab <= 0 ? $.p(a, b) : $.p(b, a);
        }

        public void input(CauseChannel<Task> t, long last, What what) {
            var n = what.nar;
            var now = n.time();
            for (var x : image) {
                Task xx = ((Task) new SeriesBeliefTable.SeriesTask($.inh(x, id), BELIEF, $.t(1f, n.confDefault(BELIEF)),
					last, now, new long[]{n.time.nextStamp()})).pri(n.priDefault(BELIEF));
                t.accept(xx, what);
            }
        }

        public void addPoly(int polyID, List<PointIndex_I32> poly, boolean outerOrInner) {
            TreeSet<Term> ts = new TreeSet();
            var ps = poly.size();
            for (var i = 0; i < ps; i++) {
                var a = poly.get(i);
                var b = poly.get((i + 1) % ps);
                var ll = line(a.x, a.y, b.x, b.y);

                ts.add(ll.sub(0));
                ts.add(ll.sub(1));
            }

            if (!ts.isEmpty())
                image.add(SETe.the(ts));
        }
    }

    /**
     * Demonstration of how to convert a point sequence describing an objects outline/contour into a sequence of line
     * segments.  Useful when analysing shapes such as squares and triangles or when trying to simply the low level
     * pixel output.
     *
     * @author Peter Abeles
     */


    private static class ScaleOffset {
        double scale;
        double offsetX;
        double offsetY;
    }

    class ShapeSensorControl extends Gridding {
        public ShapeSensorControl() {
            super(
                    new ShapeSensorSurface(),
                    filteredTex.view()
            );
        }
    }

    class ShapeSensorSurface extends PaintSurface {

        @Override
        protected void paint(GL2 gl, ReSurface reSurface) {

            if (grid != null) {
                int[] i = {0};
                for (var pSet : grid.image) {
                    var scale = Math.max(w(), h()) / Math.max(grid.gx, grid.gy);

                    var dx = x();
                    var dy = y();
                    gl.glLineWidth(2f);


                    gl.glBegin(GL_POLYGON);

                    Draw.colorHash(gl, i[0], 0.75f);
                    for (var xy : pSet.subterms()) {
                        var x = ((IdempotInt) xy.sub(0)).i;
                        var y = grid.gy - ((IdempotInt) xy.sub(1)).i;
                        gl.glVertex2f(dx + x * scale, dy + y * scale);
                    }
                    gl.glEnd();
                    i[0]++;


                }
            }
        }
    }
}

