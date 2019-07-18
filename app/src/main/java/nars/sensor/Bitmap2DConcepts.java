package nars.sensor;

import jcog.Util;
import jcog.data.iterator.Array2DIterable;
import jcog.func.IntIntToObjectFunction;
import jcog.math.FloatRange;
import jcog.signal.wave2d.Bitmap2D;
import nars.NAL;
import nars.NAR;
import nars.attention.AttnBranch;
import nars.concept.Concept;
import nars.concept.sensor.Signal;
import nars.table.eternal.EternalDefaultTable;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * rectangular region of pixels
 */
public class Bitmap2DConcepts<P extends Bitmap2D> implements Iterable<Signal> {

    /** [y][x] */
    public final Signal[][] matrix;
    public final int width, height, area;
    public final P src;

    public final Array2DIterable<Signal> iter;
    private final IntIntToObjectFunction<nars.term.Term> pixelTerm;
    private final float defaultFreq;
    private final short[] cause;
    private final FloatRange res;
    private final AttnBranch attn
            ;

    protected Bitmap2DConcepts(P src, @Nullable IntIntToObjectFunction<nars.term.Term> pixelTerm, FloatRange res, AttnBranch attn, float defaultFreq, NAR n) {

        this.width = src.width();
        this.height = src.height();
        this.area = width * height;
        assert (area > 0);

        this.res = res;
        this.attn = attn;
        this.src = src;

        this.matrix = new Signal[height][width];

        this.pixelTerm = pixelTerm;

        cause = new short[] { n.newCause(this).id };

        this.defaultFreq = defaultFreq;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                Term sid = pixelTerm.apply(x, y);
                Signal sc = new PixelSignal(sid, x,y, n);
                if (defaultFreq==defaultFreq) {
                    EternalDefaultTable.add(sc, defaultFreq, n);
                }

                matrix[y][x] = sc;
            }
        }

        this.iter = new Array2DIterable<>(matrix);
    }

    protected float nextValue(int x, int y) {

        float ff = src.brightness(x, y);
//                                        float prev = this.prev;
//                                        this.prev = ff;
//                                        if (Util.equals(ff, prev) && Util.equals(prev, defaultFreq))
//                                            return Float.NaN;
//                                        return ff;
        return Util.equals(ff, defaultFreq, NAL.truth.TRUTH_EPSILON)
                ? Float.NaN : ff;
    }


    /**
     * iterate columns (x) first, then rows (y)
     */
    @Override
    final public Iterator<Signal> iterator() {
        return iter.iterator();
    }


    public Signal get(int i) {
        return iter.get(i);
    }

    /**
     * crude ASCII text representation of the current pixel state
     */
    public void print(PrintStream out) {
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                float b = matrix[j][i].asFloat();
                out.print(b >= 0.5f ? '*' : ' ');
            }
            out.println();
        }
    }


    /**
     * range of pixels, selected by the sequential 1-d ID
     */
    public final Stream<Signal> pixels(int from, int to) {
        return IntStream.range(from, to).mapToObj(this::get);
    }

    public Signal getSafe(int x, int y) {
        return matrix[y][x];
    }

    @Nullable
    public Signal get(int x, int y) {
        if ((x < 0) || (y < 0) || (x >= width || y >= height))
            return null;
        return getSafe(x, y);
    }

    public final List<? extends Concept> order() {
        return iter.order;
    }

    public Signal get(Random random) {
        return get(random.nextInt(this.area));
    }

    public final int size() { return area; }

    class PixelSignal extends Signal {

        public final int x, y;

        PixelSignal(Term sid, int x, int y, NAR n) {
            super(sid, n);
            this.x = x; this.y = y;
        }

        @Override
        public float nextValue() {
            return Bitmap2DConcepts.this.nextValue(x, y);
        }

        @Override
        protected boolean autoTaskLink() {
            return false;
        }

        @Override
        public short[] cause() {
            return cause;
        }

        @Override
        public float pri() {
            return attn.pri();
        }

        @Override
        public FloatRange resolution() {
            return res;
        }
    }


}
