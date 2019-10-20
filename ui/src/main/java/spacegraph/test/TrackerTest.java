package spacegraph.test;

import boofcv.abst.tracker.ConfigComaniciu2003;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.video.Draw;
import spacegraph.video.VideoSource;
import spacegraph.video.VideoSurface;
import spacegraph.video.WebCam;

import java.awt.image.BufferedImage;

import static spacegraph.SpaceGraph.window;

/**
 * http://boofcv.org/index.php?title=Example_Tracker_Object
 */
public class TrackerTest extends VideoSurface {

    private boolean tracking = false;
    //private final GrayU8 frame = new GrayU8(1, 1);
    private final Planar frame = new Planar(GrayU8.class, 1, 1, 3);
    // Create the tracker.  Comment/Uncomment to change the tracker.
    TrackerObjectQuad tracker = null;
    Quadrilateral_F64 location = new Quadrilateral_F64(211.0, 162.0, 326.0, 153.0, 335.0, 258.0, 215.0, 249.0);


    public TrackerTest(VideoSource in) {
        super(in);

        // specify the target's initial location and initialize with the first frame

        // For displaying the results
        //TrackerObjectQuadPanel gui = new TrackerObjectQuadPanel(null);
        //gui.setPreferredSize(new Dimension(frame.getWidth(),frame.getHeight()));
        //gui.setImageUI((BufferedImage)video.getGuiImage());
        //gui.setTarget(location,true);
        //ShowImages.showWindow(gui,"Tracking Results", true);

        in.tensor.on(() -> {

            frame();

            if (tracker == null) {
                tracker =
//                FactoryTrackerObjectQuad.circulant(null, GrayU8.class);
//                        FactoryTrackerObjectQuad.sparseFlow(null, GrayU8.class, null);
//                FactoryTrackerObjectQuad.tld(null,GrayU8.class);

				//FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(), ImageType.pl(3, GrayU8.class));
				FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(true),ImageType.pl(3,GrayU8.class));
                // Mean-shift likelihood will fail in this video, but is excellent at tracking objects with
                // a single unique color.  See ExampleTrackerMeanShiftLikelihood
//				FactoryTrackerObjectQuad.meanShiftLikelihood(30,5,255, MeanShiftLikelihoodType.HISTOGRAM,ImageType.pl(3,GrayU8.class));

                tracker.initialize(frame, location);
            }

            // Track the object across each video frame and display the results
            long previous = 0;
            //while( video.hasNext() ) {
            //frame = video.next();


            tracking = tracker.process(frame, location);


            System.out.println(location);

            //gui.setImageUI((BufferedImage) video.getGuiImage());
            //gui.setTarget(location, visible);
            //gui.repaint();

            // shoot for a specific frame rate
            //}
        });
    }

    public static void main(String[] args) {
        window(new TrackerTest(/*new VideoEqualizer*/(WebCam.the())), 800, 800);
    }

    @Override
    public Surface finger(Finger finger) {
        boolean reset ;
        if ((reset=finger.pressedNow(2)) || finger.pressedNow(0)) {
            v2 p = finger.posRelative(innerBounds());
            p.scaled(frame.width,frame.height);
            float ch = 0.15f;
            float cw = 0.15f;
            float x = p.x, y = p.y, w = frame.width*cw, h = frame.height*ch;
            Quadrilateral_F64 location = new Quadrilateral_F64();
            location.setA(new Point2D_F64(x-w/2, y-h/2));
            location.setB(new Point2D_F64(x+w/2, y-h/2));
            location.setC(new Point2D_F64(x+w/2, y+h/2));
            location.setD(new Point2D_F64(x-w/2, y+h/2));
            if (reset)
                tracker.initialize(frame, location);

            tracker.hint(location);
        }
        return super.finger(finger);

    }

    @Override
    protected void renderContent(ReSurface r) {
        super.renderContent(r);
        Quadrilateral_F64 _l = location;
        if (_l != null) {
            if (tracking) {
                float w = frame.width;
                float h = frame.height;
                r.on((g) -> {
                    RectFloat i = innerBounds();
                    Quadrilateral_F64 l = location;
                    v2 a = i.normalize(new v2((float) l.a.x / w, (float) (l.a.y) / h));
                    v2 b = i.normalize(new v2((float) l.b.x / w, (float) (l.b.y) / h));
                    v2 c = i.normalize(new v2((float) l.c.x / w, (float) (l.c.y) / h));
                    v2 d = i.normalize(new v2((float) l.d.x / w, (float) (l.d.y) / h));
                    g.glLineWidth(4);
                    g.glColor3f(1, 1, 1);
                    Draw.line(a.x, a.y, b.x, b.y, g);
                    Draw.line(b.x, b.y, c.x, c.y, g);
                    Draw.line(c.x, c.y, d.x, d.y, g);
                    Draw.line(d.x, d.y, a.x, a.y, g);
                    //});
                });
            }
        }
    }

    protected ImageBase frame() {
        BufferedImage ii = in.image;
        if (ii != null) {
            ConvertBufferedImage.convertFrom(ii, true, frame);
        }
        return frame;
    }
}
