package spacegraph.space2d.widget.sketch;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.v2;
import jcog.signal.wave2d.Bitmap2D;
import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.collections.api.block.procedure.primitive.FloatFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectBooleanProcedure;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.MenuSupplier;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.space2d.widget.button.ButtonSet;
import spacegraph.space2d.widget.button.ColorToggle;
import spacegraph.space2d.widget.slider.XYSlider;
import spacegraph.video.Tex;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static spacegraph.space2d.container.grid.Gridding.grid;

/**
 * see: http://perfectionkills.com/exploring-canvas-drawing-techniques/
 */
public class Sketch2DBitmap extends PaintSurface implements MenuSupplier {

    public final int[] pix;
    private final Tex bmp = new Tex();
    private final BufferedImage buf;
    private final int pw;
    private final int ph;
    private final MersenneTwister rng = new MersenneTwister();
    private float brushWidth = 0.2f;
    private float brushAlpha = 0.5f;
    private float paintR = 0.75f;
    private float paintG = 0.75f;
    private float paintB = 0.75f;


    public Sketch2DBitmap(int w, int h) {
        this.pw = w;
        this.ph = h;
        this.buf = new BufferedImage(w, h, TYPE_INT_RGB);
        this.pix = ((DataBufferInt) buf.getRaster().getDataBuffer()).getData();
    }

    public static void main(String[] args) {


        SpaceGraph.window(new Sketch2DBitmap(256, 256)

                , 800, 800);
    }

    /**
     * must call this to re-generate texture so it will display
     */
    private void update() {
        bmp.set(buf);
    }

    @Override
    public Surface finger(Finger finger) {

        if (finger.pressed(0)) {
            v2 hitPoint = finger.posRelative(this);
            if (hitPoint.inUnit()) {

                int ax = Math.round(hitPoint.x * (float) pw);

                int ay = Math.round((1f - hitPoint.y) * (float) ph);


                float w = this.brushWidth * this.brushWidth;
                float a = brushAlpha * brushAlpha * 10.0F;
                for (int i = 0; (float) i < a; i++) {
                    int px = (int) ((double) ax + rng.nextGaussian() * (double) w);
                    if (px >= 0 && px < pw) {
                        int py = (int) ((double) ay + rng.nextGaussian() * (double) w);
                        if (py >= 0 && py < ph) {
                            mix(pix, py * pw + px);
                        }
                    }
                }


                update();
                return this;
            }
        }

        return null;
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
    protected void paint(GL2 gl, ReSurface reSurface) {
        if (gl == null) {

            //bmp.profile = gl.getGLProfile();
            update();
        }

        bmp.paint(gl, bounds);
    }

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
        ButtonSet<ColorToggle> colorMenu = new ButtonSet<ColorToggle>(ButtonSet.Mode.One,
                new ColorToggle(0f, (float) 0, (float) 0),
                new ColorToggle(1f, (float) 0, (float) 0),
                new ColorToggle(1f, 0.5f, (float) 0),
                new ColorToggle(0.75f, 0.75f, (float) 0),
                new ColorToggle(0f, 1.0F, (float) 0),
                new ColorToggle(0f, (float) 0, 1.0F),
                new ColorToggle(1f, (float) 0, 1.0F),
                new ColorToggle(0.5f, 0.5f, 0.5f),
                new ColorToggle(1f, 1.0F, 1.0F)
        );
        colorMenu.on(new ObjectBooleanProcedure<ColorToggle>() {
            @Override
            public void value(ColorToggle cc, boolean e) {
                if (e) {
                    Sketch2DBitmap.this.color(cc.r, cc.g, cc.b);
                }
            }
        });

        Surface toolMenu = grid(
                new XYSlider().on(new FloatFloatProcedure() {
                    @Override
                    public void value(float _width, float _alpha) {
                        brushWidth = Util.lerp(_width, 0.1f, 4f);
                        brushAlpha = Util.lerp(_alpha, 0.1f, 4f);
                    }
                }).set(0.5f, 0.75f)
        );

        return grid(colorMenu, toolMenu);
    }
}






































































































































































































