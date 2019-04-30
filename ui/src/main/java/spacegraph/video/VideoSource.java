package spacegraph.video;

import jcog.signal.tensor.TensorTopic;
import jcog.signal.wave2d.RGBBufImgBitmap2D;

import java.awt.image.BufferedImage;

abstract public class VideoSource implements AutoCloseable {
    public int width, height;
    public final TensorTopic<RGBBufImgBitmap2D> tensor = new TensorTopic();
    //@Deprecated public final Topic<WebcamEvent> eventChange = new ListTopic();
    public volatile BufferedImage image;

    protected synchronized BufferedImage setAndGet(BufferedImage image) {
        if (image == null) {
            this.width = this.height = 1;
            return this.image = null;
        } else {
            this.width = image.getWidth();
            this.height = image.getHeight();
            tensor.emit(new RGBBufImgBitmap2D(this.image = image));
            return image;
        }
    }

    public float aspect() {
        int w = width, h = height;
        if (w == 0 || h == 0)
            return 1;
        else
            return ((float)height)/width;
    }
}
