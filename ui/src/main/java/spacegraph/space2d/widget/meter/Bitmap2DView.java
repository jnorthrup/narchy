package spacegraph.space2d.widget.meter;

import jcog.signal.wave2d.Bitmap2D;
import spacegraph.video.Draw;

public class Bitmap2DView extends BitmapMatrixView  {

    public Bitmap2DView(Bitmap2D bmp) {
        super(bmp.width(), bmp.height(), (x,y)->{
            float a = bmp.brightness(x, y);
            return Draw.rgbInt(a,a,a);
        });
    }

}
