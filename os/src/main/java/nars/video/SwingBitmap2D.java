package nars.video;

import jcog.signal.wave2d.MonoBufImgBitmap2D;
import spacegraph.util.AWTCamera;

import java.awt.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Captures a awt/swing component to a bitmap and scales it down, returning an image pixel by pixel
 */
public class SwingBitmap2D extends MonoBufImgBitmap2D {

    private final Component component;

    /** dimensoins of the cropped sub-region of input image */
    public Rectangle in;


    public SwingBitmap2D(Component component) {
        super();
        this.component = component;
        input(0, 0, component.getWidth(), component.getHeight());
        source = ()->{
            return AWTCamera.get(component, sourceImage, in);
        };
        update();
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
        int px = Math.round(inWidth() * x);
        int py = Math.round(inHeight() * y);
        int pw = Math.round(inWidth() * w);
        int ph = Math.round(inWidth() * h);
        input(px, py, pw, ph);
    }

    public boolean inputTranslate(int dx, int dy) {
        int nx = (int) (in.getX() + dx);
        double sw = in.getWidth();
        if ((nx < 0) || (nx >= component.getWidth() - sw))
            return false;
        int ny = (int) (in.getY() + dy);
        double sh = in.getHeight();
        if ((ny < 0) || (ny >= component.getHeight() - sh))
            return false;
        this.in = new Rectangle(nx, ny, (int) sw, (int) sh);
        return true;
    }


    public boolean inputZoom(double scale, int minPixelsX, int minPixelsY) {
        int rw = (int) in.getWidth();
        double sw = max(minPixelsX, min(component.getWidth()-1, rw * scale));
        int rh = (int) in.getHeight();
        double sh = max(minPixelsY, min(component.getHeight()-1, rh * scale));

        int isw = (int)sw;
        int ish = (int)sh;
        if ((isw == rw) && (ish == rh))
            return false; 

        double dx = (sw - rw)/2.0;
        double dy = (sh - rh)/2.0;
        this.in = new Rectangle(
                (int)(in.getX()+dx), (int)(in.getY()+dy),
                (int) sw, (int) sh);
        return true;
    }
}
