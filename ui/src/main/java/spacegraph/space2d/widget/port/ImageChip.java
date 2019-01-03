package spacegraph.space2d.widget.port;

import spacegraph.video.Tex;

import java.awt.image.BufferedImage;

public class ImageChip extends ConstantPort<BufferedImage> {

    public ImageChip(BufferedImage img) {
        super(img);
        set(Tex.view(img));
    }

}
