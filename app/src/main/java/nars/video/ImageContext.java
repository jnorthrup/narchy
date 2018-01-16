package nars.video;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.InterleavedU8;

/**
 * Describes a dynamically editable pipeline for manipulating images
 * driven by sources, and transformed. the result may be utilized at any
 * sub-step for outside computation
 */
abstract public class ImageContext implements ImageProcess {


    public InterleavedU8 output = new InterleavedU8();

    public static ImageContext seq(ImageProcess... stages) {
        return new ImageContext() {

            @Override
            public ImageBase apply(ImageBase input) {
                ImageBase prev = null;
                for (ImageProcess ip : stages) {
                    prev = ip.apply(prev);
                }
                return output = (InterleavedU8) prev; //HACK
            }
        };
    }

    public InterleavedU8 output() {
        return output;
    }
}
