package nars.sensor;

import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.signal.Bitmap2D;
import jcog.data.iterator.Array2DIterable;
import jcog.util.Int2Function;
import nars.NAR;
import nars.Task;
import nars.concept.signal.Signal;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.Op.BELIEF;

/**
 * rectangular region of pixels
 */
public class Bitmap2DConcepts<P extends Bitmap2D> implements Iterable<Signal> {

    public final Signal[][] matrix;
    public final int width, height, area;
    public final P src;

    public final Array2DIterable<Signal> iter;

    /** each pixel's belief task priority for next input */
    public final FloatRange pixelPri = new FloatRange(0, 0, 1f);

    protected Bitmap2DConcepts(P src, @Nullable Int2Function<Term> pixelTerm, NAR n) {

        this.width = src.width();
        this.height = src.height();
        this.area = width * height;
        assert(area > 0);

        this.src = src;
        this.pixelPri.set( n.priDefault(BELIEF)*(1/Math.sqrt(area) ));

        this.matrix = new Signal[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int xx = x, yy = y;

                FloatSupplier f = () -> src.brightness(xx, yy);

                Signal sss = new Signal(pixelTerm.get(x, y), f, n) {
//                    @Override
//                    protected TermlinkTemplates buildTemplates(Term term) {
//                        TermlinkTemplates t = super.buildTemplates(term);
//                        if (xx > 0)
//                            t.add(pixelTerm.get(xx-1, yy));
//                        if (yy > 0)
//                            t.add(pixelTerm.get(xx, yy-1));
//                        if (xx < width-1)
//                            t.add(pixelTerm.get(xx+1, yy));
//                        if (yy < height-1)
//                            t.add(pixelTerm.get(xx, yy+1));
//
//                        return t;
//                    }
//
//                    @Override
//                    public TermlinkTemplates templates() {
//                        return super.templates();
//                    }
                }.pri(pixelPri);

                matrix[x][y] = sss;
            }
        }

        this.iter = new Array2DIterable<>(matrix);
    }

    /** iterate columns (x) first, then rows (y) */
    @Override final public Iterator<Signal> iterator() {
        return iter.iterator();
    }

    public void update() {
        src.update();
    }

    public Signal get(int i) {
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

    public Bitmap2DConcepts resolution(float resolution) {
        forEach(p -> p.resolution.set(resolution));
        return this;
    }

    /** streams (potentially) all pixels */
    public final Stream<Task> stream(FloatFloatToObjectFunction<Truth> truther, int dur, NAR nar) {
        return stream(truther, 0, area, dur, nar);
    }

    /** stream of tasks containing changes in all updated pixels */
    public Stream<Task> stream(FloatFloatToObjectFunction<Truth> truther, int start, int end, int dur, NAR nar) {

        long now = nar.time();

        long tStart = now - dur/2;
        long tEnd = now + dur/2;
        return IntStream.range(start, end)
                .mapToObj(i -> get(i).update(tStart, tEnd, truther, dur, nar))
                
        ;
    }

    public Signal getSafe(int i, int j) {
        return matrix[i][j];
    }
    @Nullable public Signal get(int i, int j) {
        if ((i < 0) || (j < 0) || (i >= width || j >= height))
                return null;
        return getSafe(i, j);
    }

}
