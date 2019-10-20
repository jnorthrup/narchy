package nars.video;

import jcog.signal.wave2d.MonoBufImgBitmap2D;
import spacegraph.util.AWTCamera;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Captures a awt/swing component to a bitmap and scales it down, returning an image pixel by pixel
 */
public class SwingBitmap2D extends MonoBufImgBitmap2D implements Supplier<BufferedImage> /* HACK */ {

    private final Component component;

    /** dimensoins of the cropped sub-region of input image */
    public Rectangle in;


    public SwingBitmap2D(Component component) {
        super();
        this.component = component;
        input(0, 0, component.getWidth(), component.getHeight());
        source = ()-> AWTCamera.get(component, img, in);
        updateBitmap();
    }

    @Deprecated @Override public BufferedImage get() {
        return img;
    }

    public void input(int x, int y, int w, int h) {
        this.in = new Rectangle(x, y, w, h);
    }


    public int inWidth() {
        return component.getWidth();
    }

    public int inHeight() {
        return component.getHeight();
    }


    /** x and y in 0..1.0, w and h in 0..1.0 */
    public void input(float x, float y, float w, float h) {
        var px = Math.round(inWidth() * x);
        var py = Math.round(inHeight() * y);
        var pw = Math.round(inWidth() * w);
        var ph = Math.round(inWidth() * h);
        input(px, py, pw, ph);
    }

    public boolean inputTranslate(int dx, int dy) {
        var nx = (int) (in.getX() + dx);
        var sw = in.getWidth();
        if ((nx < 0) || (nx >= component.getWidth() - sw))
            return false;
        var ny = (int) (in.getY() + dy);
        var sh = in.getHeight();
        if ((ny < 0) || (ny >= component.getHeight() - sh))
            return false;
        this.in = new Rectangle(nx, ny, (int) sw, (int) sh);
        return true;
    }


    public boolean inputZoom(double scale, int minPixelsX, int minPixelsY) {
        var rw = (int) in.getWidth();
        var sw = max(minPixelsX, min(component.getWidth()-1, rw * scale));
        var rh = (int) in.getHeight();
        var sh = max(minPixelsY, min(component.getHeight()-1, rh * scale));

        var isw = (int)sw;
        var ish = (int)sh;
        if ((isw == rw) && (ish == rh))
            return false;

        var dx = (sw - rw)/2.0;
        var dy = (sh - rh)/2.0;
        this.in = new Rectangle(
                (int)(in.getX()+dx), (int)(in.getY()+dy),
                (int) sw, (int) sh);
        return true;
    }
}
