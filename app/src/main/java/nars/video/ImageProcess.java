package nars.video;

import boofcv.struct.image.ImageBase;

/**
 * A manipulation or filtering of a Vision context.
 * They can be chained together to form a pipeline of transforms
 */
public interface ImageProcess {

    ImageBase apply(ImageBase input);


}
