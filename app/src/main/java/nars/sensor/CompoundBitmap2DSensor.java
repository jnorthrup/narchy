package nars.sensor;

import jcog.signal.wave2d.Bitmap2D;
import jcog.func.IntIntToObjectFunction;
import nars.util.Timed;
import org.jetbrains.annotations.Nullable;

/** TODO bitmap of bitmaps for hierarchical vision
 * see how this is somewhat achieved in Gradius.java
 * */
public class CompoundBitmap2DSensor<P extends Bitmap2D>  {

    public CompoundBitmap2DSensor(@Nullable IntIntToObjectFunction<nars.term.Term> pixelTerm, P src, int divW, int divH, Timed n) {

    }
}
