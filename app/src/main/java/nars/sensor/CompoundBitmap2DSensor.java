package nars.sensor;

import jcog.signal.wave2d.Bitmap2D;
import jcog.util.Int2Function;
import nars.term.Term;
import nars.util.TimeAware;
import org.jetbrains.annotations.Nullable;

/** TODO bitmap of bitmaps for hierarchical vision
 * see how this is somewhat achieved in Gradius.java
 * */
public class CompoundBitmap2DSensor<P extends Bitmap2D>  {

    public CompoundBitmap2DSensor(@Nullable Int2Function<Term> pixelTerm, P src, int divW, int divH, TimeAware n) {

    }
}
