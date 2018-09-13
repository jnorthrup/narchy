package spacegraph.space2d.widget.sketch;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.signal.wave2d.Bitmap2D;
import org.apache.commons.math3.random.MersenneTwister;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.widget.button.ColorToggle;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.space2d.widget.slider.XYSlider;
import spacegraph.space2d.widget.tab.ButtonSet;
import spacegraph.util.math.v2;
import spacegraph.video.Tex;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static spacegraph.space2d.container.Gridding.grid;

/** see: http://perfectionkills.com/exploring-canvas-drawing-techniques/ */
public class Sketch2DBitmap extends Surface implements MetaFrame.Menu {

    private final Tex bmp = new Tex();
    public final int[] pix;
    private final BufferedImage buf;
    private final int pw, ph;

    private float brushWidth = 0.2f;
    private float brushAlpha = 0.5f;

    private final MersenneTwister rng = new MersenneTwister();


    public Sketch2DBitmap(int w, int h) {
        this.pw = w;
        this.ph = h;
        this.buf = new BufferedImage(w, h, TYPE_INT_RGB);
        this.pix = ((DataBufferInt) buf.getRaster().getDataBuffer()).getData();
    }


    /**
     * must call this to re-generate texture so it will display
     */
    private void update() {
        bmp.update(buf);
    }





    @Override
    public Surface tryTouch(Finger finger) {


        if (finger!=null) {
            v2 hitPoint = finger.relativePos(this);
            if (hitPoint.inUnit() && finger.pressing(0)) {




                int ax = Math.round(hitPoint.x * pw);

                int ay = Math.round((1f - hitPoint.y) * ph);





                float w = this.brushWidth * this.brushWidth;
                float a = brushAlpha * brushAlpha * 10;
                for (int i = 0; i < a; i++) {
                    int px = (int) (ax + rng.nextGaussian() * w);
                    if (px >= 0 && px < pw) {
                        int py = (int) (ay + rng.nextGaussian() * w);
                        if (py >= 0 && py < ph) {
                            
                            mix(pix, py * pw + px);
                        }
                    }
                }







                update();
                return this;
            }
        }

        return super.tryTouch(finger);
    }

    private void mix(int[] pix, int i) {
        int e = pix[i];
        float r = Bitmap2D.decode8bRed(e) * 0.5f + paintR * 0.5f;
        float g = Bitmap2D.decode8bGreen(e) * 0.5f + paintG * 0.5f;
        float b = Bitmap2D.decode8bBlue(e) * 0.5f + paintB * 0.5f;
        int f = Bitmap2D.encodeRGB8b(r, g, b);
        pix[i] = f;




    }

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        if (gl == null) {

            bmp.profile = gl.getGLProfile();
            update();
        }

        bmp.paint(gl, bounds);
    }

    private float paintR = 0.75f, paintG = 0.75f, paintB = 0.75f;

    /**
     * set paint (foreground) color
     */
    private void color(float r, float g, float b) {
        this.paintR = r;
        this.paintG = g;
        this.paintB = b;
    }


    @Override
    public Surface menu() {
        ButtonSet<ColorToggle> colorMenu = new ButtonSet<>(ButtonSet.Mode.One,
                new ColorToggle(0f, 0, 0), 
                new ColorToggle(1f, 0, 0), 
                new ColorToggle(1f, 0.5f, 0),
                new ColorToggle(0.75f, 0.75f, 0),
                new ColorToggle(0f, 1, 0), 
                new ColorToggle(0f, 0, 1), 
                new ColorToggle(1f, 0, 1), 
                new ColorToggle(0.5f, 0.5f, 0.5f), 
                new ColorToggle(1f, 1, 1) 
        );
        colorMenu.on((cc,e)->{
            if (e) {
                color(cc.r, cc.g, cc.b);
            }
        });

        Surface toolMenu = grid(
                new XYSlider().on((_width, _alpha)->{
                    brushWidth = Util.lerp(_width, 0.1f, 3f);
                    brushAlpha = Util.lerp(_alpha, 0.1f, 3f);
                }).set(0.5f, 0.75f)
        );

        return grid(colorMenu, toolMenu);
    }

    

    public static void main(String[] args) {


        SpaceGraph.window(new Sketch2DBitmap(256, 256)
                
                , 800, 800);
    }
}






































































































































































































