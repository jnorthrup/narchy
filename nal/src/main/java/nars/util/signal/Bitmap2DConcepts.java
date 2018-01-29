package nars.util.signal;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.signal.Bitmap2D;
import jcog.util.Array2DIterable;
import jcog.util.Int2Function;
import nars.NAR;
import nars.concept.SensorConcept;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * rectangular region of pixels
 */
public class Bitmap2DConcepts<S extends Bitmap2D> implements Iterable<SensorConcept> {

    public final SensorConcept[][] matrix;
    public final int width, height;
    public final S src;
    //private final Int2Function<Term> pixelTerm;

    public final Array2DIterable<SensorConcept> iter;

    protected FloatFloatToObjectFunction<Truth> brightnessTruth;

    protected Bitmap2DConcepts(S src, @Nullable Int2Function<Term> pixelTerm, int width, int height, FloatSupplier pri, NAR n) {

        this.src = src;
        this.width = width;
        this.height = height;
        //this.pixelTerm = pixelTerm;
        this.matrix = new SensorConcept[width][height];

        FloatFloatToObjectFunction<Truth> b = (p, v) -> brightnessTruth.value(p, v);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int xx = x;
                int yy = y;

                FloatSupplier f = () -> Util.unitize(src.brightness(xx, yy));


                SensorConcept sss = new SensorConcept(pixelTerm.get(x, y), n,
                        f, b).pri(pri);

                n.on(matrix[x][y] = sss);
            }
        }

        this.iter = new Array2DIterable<>(matrix);
    }

    /** # of pixels */
    public final int area() {
        return width * height;
    }

    /** iterate columns (x) first, then rows (y) */
    @Override final public Iterator<SensorConcept> iterator() {
        return iter.iterator();
    }

    public void update(int i) {
        src.update(i);
    }

    public SensorConcept get(int i) {
        return iter.order.get(i);
    }


    /** crude ASCII text representation of the current pixel state */
    public void print(PrintStream out) {
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                float b = matrix[i][j].asFloat();
                out.print(b >= 0.5f ? '*' : ' ');
            }
            out.println();
        }
    }
}
