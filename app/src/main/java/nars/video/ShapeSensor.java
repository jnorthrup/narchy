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
import nars.NAgent;
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
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.video.Draw;
import spacegraph.video.Tex;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import static com.jogamp.opengl.GL2.GL_POLYGON;
import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

//import boofcv.gui.feature.VisualizeShapes;
//import boofcv.gui.image.ImagePanel;
//import boofcv.gui.image.ScaleOptions;
//import boofcv.gui.image.ShowImages;

public class ShapeSensor extends NARService {

    private final Bitmap2D input;
    private final CauseChannel<ITask> in;
    private final Term id;


    /** filtered */
    GrayU8 img = null;

    /** unfiltered */
    GrayF32 imgF;

    //final static boolean debug = true;

    // Polynomial fitting tolerances
    static double minimumSideFraction = 0.25;

    //static ImagePanel gui = debug ? new ImagePanel(400, 200) : null;
    //private BufferedImage polyDebug;

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
        protected void paint(GL2 gl, int dtMS) {

            if (grid!=null) {
                final int[] i = {0};
                grid.image.forEach(pSet->{
                   //System.out.println(pSet);

                    float scale = Math.max(w(), h()) / Math.max(grid.gx, grid.gy);

                    float dx = x();
                    float dy = y();
                    gl.glLineWidth(2f);
                    //gl.glBegin(GL_TRIANGLE_FAN);
                    //gl.glBegin(GL_LINE_LOOP);
                    gl.glBegin(GL_POLYGON);
                    //int n = pSet.subs();
                    Draw.colorHash(gl, i[0], 0.75f);
                    for (Term xy : pSet.subterms()) {
                        int x = ((Int)xy.sub(0)).id;
                        int y = grid.gy - ((Int)xy.sub(1)).id;
                        gl.glVertex2f(dx + x * scale, dy + y * scale);
                    }
                    gl.glEnd();
                    i[0]++;
//                    g2.setColor(Color.getHSBColor(k / 10f, 0.8f, 0.8f));
//                    g2.setStroke(new BasicStroke(2));
//                    drawPolygon(outer, true, g2);
//                    //System.out.println(c + ": " + polygon);
                });
            }
        }
    }

    public ShapeSensor(Term id, Bitmap2D input, NAgent a) {
        super(a.nar());
        this.id = id;
        this.input = input;



//        if (debug) {
//
//            JFrame j = new JFrame();
//            //j.setSize(400,400);
//            j.setContentPane(gui);
//            j.pack();
//            j.setVisible(true);
//
//        }

        in = a.nar().newChannel(this);

        a.onFrame(this::update);

//        a.actionUnipolar($.p(id, $.the("R")), (v) -> {
//           return R = v;
//        });
//        a.actionUnipolar($.p(id, $.the("G")), (v) -> {
//           return G = v;
//        });
//        a.actionUnipolar($.p(id, $.the("B")), (v) -> {
//           return B = v;
//        });
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
                //img.set(x, y, Math.round(256f * b));
            }
        }

        //GrayU8 binary = new GrayU8(img.width, img.height);


//        if (debug) {
//            if (polyDebug == null)
//                polyDebug = new BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB);
//        }

        // the mean pixel value is often a reasonable threshold when creating a binary image
        float mean = ImageStatistics.mean(imgF);

        // create a binary image by thresholding
        ThresholdImageOps.threshold(imgF, img, mean, true);

        // reduce noise with some filtering
        ////for (int i = 0; i < 2; i++) {
        filtered = BinaryImageOps.dilate8(img, 1, null);
        filtered = BinaryImageOps.erode8(filtered, 1, null);
        ////}

        GrayU8 filteredShown = filtered.clone();
        byte[] data = filteredShown.data;
        for (int i = 0, dataLength = data.length; i < dataLength; i++) {
            data[i] = (byte) (data[i] * 255);
        }
        filteredRGB = filteredTex.update(filteredShown, filteredRGB);


        // Find the contour around the shapes


//		// Fit a polygon to each shape and draw the results
//        if (debug) {
//            if (g2 == null) {
//                g2 = polyDebug.createGraphics();
//            }
//            g2.setColor(Color.BLACK);
//            g2.clearRect(0, 0, polyDebug.getWidth(), polyDebug.getHeight());
//        } else {
//            g2 = null;
//        }

        List<Contour> contours = BinaryImageOps.contour(filtered,
                //ConnectRule.EIGHT,
                ConnectRule.FOUR,
                null);
        Grid g = new Grid(id, 12, 12, w, h) {

//            @Override
//            public Term line(int ax, int ay, int bx, int by) {
//                Term l = super.line(ax, ay, bx, by);
////                if (surface != null) {
////
////                    int aax = Math.round(ax * sx);
////                    int aay = Math.round(ay * sy);
////                    int bbx = Math.round(bx * sx);
////                    int bby = Math.round(by * sy);
////                    g2.setColor(Color.GRAY);
////                    g2.setStroke(new BasicStroke(1));
////                    g2.drawLine(
////                            Math.round(aax / sx), Math.round(aay / sy),
////                            Math.round(bbx / sx), Math.round(bby / sy));
////                }
//                return l;
//            }
        };

        int k = 0;
        for (Contour c : contours) {
            // Fit the polygon to the found external contour.  Note loop = true
            List<PointIndex_I32> outer = ShapeFittingOps.fitPolygon(c.external,
                    false, Math.min(g.w/g.gx, g.h/g.gy),
                    minimumSideFraction);



            g.addPoly(k++, outer, true);

//            // handle internal contours
//            for (List<Point2D_I32> internal : c.internal) {
//                List<PointIndex_I32> inner = ShapeFittingOps.fitPolygon(internal, true, splitFraction, minimumSideFraction, 100);
//                g2.setColor(Color.getHSBColor(c.id / 10f, 0.8f, 0.4f));
//                g2.setStroke(new BasicStroke(2));
//                drawPolygon(inner, true, g2);
//                g.addPoly(pk, inner, false);
//            }
        }

        g.input(in, last, nar);

        this.grid = g;

//        if (debug) {
//
//            gui.setImageRepaint(polyDebug);
//
//
//        }

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

            //lexicographic ordering and also handles if a==b
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
                //ts.add(ll);
                ts.add(ll.sub(0));
                ts.add(ll.sub(1)); //HACK
            }

            if(!ts.isEmpty())
                image.add($.sete(ts));
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


//    /**
//     * Fits polygons to found contours around binary blobs.
//     */
//    public static void fitBinaryImage(GrayF32 input) {
//
//        GrayU8 binary = new GrayU8(input.width, input.height);
//        BufferedImage polygon = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
//
//        // the mean pixel value is often a reasonable threshold when creating a binary image
//        double mean = ImageStatistics.mean(input);
//
//        // create a binary image by thresholding
//        ThresholdImageOps.threshold(input, binary, (float) mean, true);
//
//        // reduce noise with some filtering
//        GrayU8 filtered = BinaryImageOps.erode8(binary, 1, null);
//        filtered = BinaryImageOps.dilate8(filtered, 1, null);
//
//        // Find the contour around the shapes
//
//        List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.FOUR /* EIGHT */, null);
//
//        // Fit a polygon to each shape and draw the results
//        Graphics2D g2 = polygon.createGraphics();
//        g2.setStroke(new BasicStroke(3));
//
//        for (Contour c : contours) {
//            // Fit the polygon to the found external contour.  Note loop = true
//            List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external, true,
//                    splitFraction, minimumSideFraction, 100);
//
//            g2.setColor(Color.RED);
//            VisualizeShapes.drawPolygon(vertexes, true, g2);
//            System.out.println(c + ": " + vertexes);
//
//            // handle internal contours now
//            g2.setColor(Color.BLUE);
//            for (List<Point2D_I32> internal : c.internal) {
//                vertexes = ShapeFittingOps.fitPolygon(internal, true, splitFraction, minimumSideFraction, 100);
//                VisualizeShapes.drawPolygon(vertexes, true, g2);
//            }
//        }
//
//        gui.addImage(polygon, "Binary Blob Contours");
//    }
//
//    /**
//     * Fits a sequence of line-segments into a sequence of points found using the Canny edge detector.  In this case
//     * the points are not connected in a loop. The canny detector produces a more complex tree and the fitted
//     * points can be a bit noisy compared to the others.
//     */
//    public static void fitCannyEdges(GrayF32 input) {
//
//        BufferedImage displayImage = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
//
//        // Finds edges inside the image
//        CannyEdge<GrayF32, GrayF32> canny =
//                FactoryEdgeDetectors.canny(2, true, true, GrayF32.class, GrayF32.class);
//
//        canny.process(input, 0.1f, 0.3f, null);
//        List<EdgeContour> contours = canny.getContours();
//
//        Graphics2D g2 = displayImage.createGraphics();
//        g2.setStroke(new BasicStroke(2));
//
//        // used to select colors for each line
//        Random rand = new Random(234);
//
//        for (EdgeContour e : contours) {
//            g2.setColor(new Color(rand.nextInt()));
//
//            for (EdgeSegment s : e.segments) {
//                // fit line segments to the point sequence.  Note that loop is false
//                List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(s.points, false,
//                        splitFraction, minimumSideFraction, 100);
//
//                VisualizeShapes.drawPolygon(vertexes, false, g2);
//            }
//        }
//
//        gui.addImage(displayImage, "Canny Trace");
//    }
//
//    /**
//     * Detects contours inside the binary image generated by canny.  Only the external contour is relevant. Often
//     * easier to deal with than working with Canny edges directly.
//     */
//    public static void fitCannyBinary(GrayF32 input) {
//
//        BufferedImage displayImage = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
//        GrayU8 binary = new GrayU8(input.width, input.height);
//
//        // Finds edges inside the image
//        CannyEdge<GrayF32, GrayF32> canny =
//                FactoryEdgeDetectors.canny(2, false, true, GrayF32.class, GrayF32.class);
//
//        canny.process(input, 0.1f, 0.3f, binary);
//
//        List<Contour> contours = BinaryImageOps.contour(binary, ConnectRule.EIGHT, null);
//
//        Graphics2D g2 = displayImage.createGraphics();
//        g2.setStroke(new BasicStroke(2));
//
//        // used to select colors for each line
//        Random rand = new Random(234);
//
//        for (Contour c : contours) {
//            // Only the external contours are relevant.
//            List<PointIndex_I32> vertexes = ShapeFittingOps.fitPolygon(c.external, true,
//                    splitFraction, minimumSideFraction, 100);
//
//            g2.setColor(new Color(rand.nextInt()));
//            VisualizeShapes.drawPolygon(vertexes, true, g2);
//        }
//
//        gui.addImage(displayImage, "Canny Contour");
//    }
//
//    public static void main(String args[]) {
//        // load and convert the image into a usable format
//        BufferedImage image = UtilImageIO.loadImage("/home/me/2017-02-17-123605_1920x1080_scrot.png");
//        GrayF32 input = ConvertBufferedImage.convertFromSingle(image, null, GrayF32.class);
//
//        gui.addImage(image, "Original");
//
//        fitCannyEdges(input);
//        fitCannyBinary(input);
//        fitBinaryImage(input);
//
//        ShowImages.showWindow(gui, "Polygon from Contour", true);
//    }
//
//


//    /**
//     * Simple JPanel for displaying buffered images.
//     *
//     * @author Peter Abeles
//     */
//    public static class ImagePanel extends JPanel {
//        // the image being displayed
//        protected BufferedImage img;
//        // should it re-size the image based on the panel's size
////	protected ScaleOptions scaling = ScaleOptions.DOWN;
//
//        public double scale = 1;
//        public double offsetX;
//        public double offsetY;
//
//        //  this variable must only be touched inside the GUI thread
//        private final ScaleOffset adjustmentGUI = new ScaleOffset();
//
////	protected SaveImageOnClick mouseListener;
//
//        protected AffineTransform transform = new AffineTransform();
//        private boolean center;
//
//        public ImagePanel(BufferedImage img) {
////		this(img,ScaleOptions.NONE);
////	}
//
////	public ImagePanel(final BufferedImage img , ScaleOptions scaling ) {
//            this(true);
//            this.img = img;
////		this.scaling = scaling;
//            autoSetPreferredSize();
//        }
//
//        public ImagePanel(int width, int height) {
//            this(true);
//            setPreferredSize(new Dimension(width, height));
//        }
//
//        /**
//         * Adds the ability to save an image using the middle mouse button.  A dialog is shown to the user
//         * so that they know what has happened.  They can hide it in the future if they wish.
//         */
//
//        public ImagePanel(boolean addMouseListener) {
//            if (addMouseListener) {
//                // Adds the ability to save an image using the middle mouse button.  A dialog is shown to the user
//                // so that they know what has happened.  They can hide it in the future if they wish.
////			mouseListener = new SaveImageOnClick(this);
////			addMouseListener(mouseListener);
//            }
//        }
//
//        public ImagePanel() {
//            this(true);
//        }
//
//        @Override
//        public void paintComponent(Graphics g) {
//            super.paintComponent(g);
//            Graphics2D g2 = (Graphics2D) g;
//            AffineTransform original = g2.getTransform();
//            configureDrawImageGraphics(g2);
//
//            //draw the image
//            BufferedImage img = this.img;
//            if (img != null) {
//                computeOffsetAndScale(img, adjustmentGUI);
//                scale = adjustmentGUI.scale;
//                offsetX = adjustmentGUI.offsetX;
//                offsetY = adjustmentGUI.offsetY;
//
//                if (scale == 1) {
//                    g2.drawImage(img, (int) offsetX, (int) offsetY, this);
//                } else {
//                    transform.setTransform(scale, 0, 0, scale, offsetX, offsetY);
//                    g2.drawImage(img, transform, null);
//                }
//            }
//
//            g2.setTransform(original);
//        }
//
//        protected void configureDrawImageGraphics(Graphics2D g2) {
//        }
//
//        private void computeOffsetAndScale(BufferedImage img, ScaleOffset so) {
////		if( scaling != ScaleOptions.NONE ) {
//            double ratioW = (double) getWidth() / img.getWidth();
//            double ratioH = (double) getHeight() / img.getHeight();
//
//            so.scale = Math.min(ratioW, ratioH);
////			if( scaling == ScaleOptions.DOWN && so.scale >= 1 )
////				so.scale = 1;
//
//            if (center) {
//                so.offsetX = (getWidth() - img.getWidth() * so.scale) / 2;
//                so.offsetY = (getHeight() - img.getHeight() * so.scale) / 2;
//            } else {
//                so.offsetX = 0;
//                so.offsetY = 0;
//            }
//
//            if (scale == 1) {
//                so.offsetX = (int) so.offsetX;
//                so.offsetY = (int) so.offsetY;
//            }
////		} else {
////			if( center ) {
////				so.offsetX = (getWidth()-img.getWidth())/2;
////				so.offsetY = (getHeight()-img.getHeight())/2;
////			} else {
////				so.offsetX = 0;
////				so.offsetY = 0;
////			}
////
////			so.scale = 1;
////		}
//        }
//
//        /**
//         * Change the image being displayed. If panel is active then don't call unless inside the GUI thread.  Repaint()
//         * is not automatically called.
//         *
//         * @param image The new image which will be displayed.
//         */
//        public void setImage(BufferedImage image) {
//            this.img = image;
//        }
//
//        /**
//         * Changes the buffered image and calls repaint.  Does not need to be called in the UI thread.
//         */
//        public void setImageRepaint(BufferedImage image) {
//            // if image is larger before  than the new image then you need to make sure you repaint
//            // the entire image otherwise a ghost will be left
//            ScaleOffset workspace;
//            if (SwingUtilities.isEventDispatchThread()) {
//                workspace = adjustmentGUI;
//            } else {
//                workspace = new ScaleOffset();
//            }
//            repaintJustImage(img, workspace);
//            this.img = image;
//            repaintJustImage(img, workspace);
//        }
//
//        /**
//         * Changes the image and will be invoked inside the UI thread at a later time.  repaint() is automatically
//         * called.
//         *
//         * @param image The new image which will be displayed.
//         */
//        public void setImageUI(final BufferedImage image) {
//            SwingUtilities.invokeLater(() -> {
//                repaintJustImage(ImagePanel.this.img, adjustmentGUI);
//                ImagePanel.this.img = image;
//                repaintJustImage(ImagePanel.this.img, adjustmentGUI);
//            });
//        }
//
//        public boolean isCentered() {
//            return center;
//        }
//
//        public void setCentering(boolean center) {
//            this.center = center;
//        }
//
//        /**
//         * Repaints just the region around the image.
//         */
//        public void repaintJustImage() {
//            repaintJustImage(img, new ScaleOffset());
//        }
//
//        protected void repaintJustImage(BufferedImage img, ScaleOffset workspace) {
//            if (img == null) {
//                repaint();
//                return;
//            }
//            computeOffsetAndScale(img, workspace);
//
//            repaint((int) Math.round(workspace.offsetX) - 1, (int) Math.round(workspace.offsetY) - 1,
//                    (int) (img.getWidth() * workspace.scale + 0.5) + 2, (int) (img.getHeight() * workspace.scale + 0.5) + 2);
//        }
//
//        public BufferedImage getImage() {
//            return img;
//        }
//
//
//        public void autoSetPreferredSize() {
//            setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
//        }
//
//        public void setScale(double scale) {
//            this.scale = scale;
//        }
//
//
//    }

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

