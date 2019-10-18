package nars.sensor;

import jcog.Util;
import jcog.data.iterator.Array2DIterable;
import jcog.func.IntIntToObjectFunction;
import jcog.signal.wave2d.Bitmap2D;
import nars.NAL;
import nars.NAR;
import nars.game.Game;
import nars.game.sensor.ComponentSignal;
import nars.game.sensor.Signal;
import nars.table.eternal.EternalDefaultTable;
import nars.term.Term;
import nars.truth.Truth;
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
public class Bitmap2DConcepts<P extends Bitmap2D> implements Iterable<ComponentSignal> {

    /** [y][x] */
    public final ComponentSignal[][] matrix;
    public final int width;
    public final int height;
    public final int area;
    public final P src;

    public final Array2DIterable<ComponentSignal> iter;
    private final float defaultFreq;

    protected Bitmap2DConcepts(P src, @Nullable IntIntToObjectFunction<nars.term.Term> pixelTerm, float defaultFreq, Bitmap2DSensor<?> s) {

        this.width = src.width();
        this.height = src.height();
        this.area = width * height;
        assert (area > 0);

        this.src = src;

        this.matrix = new ComponentSignal[height][width];


        this.defaultFreq = defaultFreq;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                Term sid = pixelTerm.apply(x, y);
                PixelSignal sc = new PixelSignal(sid, x,y, s);
                if (defaultFreq==defaultFreq) {
                    EternalDefaultTable.add(sc, defaultFreq, s.nar);
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
    public final Iterator<ComponentSignal> iterator() {
        return iter.iterator();
    }


    public Signal get(int i) {
        return iter.get(i);
    }

    /**
     * crude ASCII text representation of the current pixel state
     */
    public void print(long when, PrintStream out, NAR nar) {
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                Truth b = matrix[j][i].beliefs().truth(when, nar);
                out.print(b!=null ? (b.freq() >= 0.5f ? '+' : '-') : ' ' );
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

    public @Nullable Signal get(int x, int y) {
        if ((x < 0) || (y < 0) || (x >= width || y >= height))
            return null;
        return getSafe(x, y);
    }

    public final List<? extends ComponentSignal> order() {
        return iter.order;
    }

    public Signal get(Random random) {
        return get(random.nextInt(this.area));
    }

    public final int size() { return area; }



    class PixelSignal extends ComponentSignal {

        public final int x;
        public final int y;

        PixelSignal(Term sid, int x, int y, Bitmap2DSensor s) {
            super(sid, s);
            this.x = x; this.y = y;
        }

        @Override
        protected float value(Game g) {
            return Bitmap2DConcepts.this.nextValue(x, y);
        }

    }


}
