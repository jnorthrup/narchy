package nars.sensor;

import jcog.data.iterator.Array2DIterable;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.signal.wave2d.Bitmap2D;
import jcog.util.Int2Function;
import nars.NAR;
import nars.concept.sensor.Signal;
import nars.control.channel.BufferedCauseChannel;
import nars.control.channel.CauseChannel;
import nars.exe.Causable;
import nars.task.ITask;
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

    private final FloatRange pri;
    private final FloatRange priPixel = new FloatRange(0, 0, 1);

    public final Array2DIterable<Signal> iter;

    protected Bitmap2DConcepts(P src, @Nullable Int2Function<Term> pixelTerm, FloatRange pri, FloatRange res, NAR n) {

        this.pri = pri;
        last = n.time();
        this.width = src.width();
        this.height = src.height();
        this.area = width * height;
        assert (area > 0);

        this.src = src;

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
                }.setPri(priPixel).setResolution(res);

                matrix[x][y] = sss;
            }
        }

        this.iter = new Array2DIterable<>(matrix);
    }

    /**
     * iterate columns (x) first, then rows (y)
     */
    @Override
    final public Iterator<Signal> iterator() {
        return iter.iterator();
    }

    public void update() {
        src.update();
    }

    public Signal get(int i) {
        return iter.order.get(i);
    }

    /**
     * crude ASCII text representation of the current pixel state
     */
    public void print(PrintStream out) {
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                float b = matrix[i][j].asFloat();
                out.print(b >= 0.5f ? '*' : ' ');
            }
            out.println();
        }
    }


    /**
     * streams (potentially) all pixels
     */
    public final Stream<ITask> stream(FloatFloatToObjectFunction<Truth> truther, int dur, NAR nar) {
        return stream(truther, 0, area, dur, nar);
    }

    private long last;

    /**
     * stream of tasks containing changes in all updated pixels
     */
    public Stream<ITask> stream(FloatFloatToObjectFunction<Truth> truther, int pixelStart, int pixelEnd, int dur, NAR nar) {

        long now = nar.time();

        long tStart = last; //now - dur / 2;
        long tEnd = now;// + Math.max(0, dur / 2 - 1);
        last = now;
        return pixels(pixelStart, pixelEnd).map(p -> p.update(tStart, tEnd, truther, nar));
    }

    /**
     * range of pixels, selected by the sequential 1-d ID
     */
    public final Stream<Signal> pixels(int from, int to) {
        return IntStream.range(from, to).mapToObj(this::get);
    }

    public Signal getSafe(int i, int j) {
        return matrix[i][j];
    }

    @Nullable
    public Signal get(int i, int j) {
        if ((i < 0) || (j < 0) || (i >= width || j >= height))
            return null;
        return getSafe(i, j);
    }

    public Bitmap2DReader newReader(CauseChannel<ITask> in, FloatFloatToObjectFunction<Truth> mode, NAR nar) {
        return new Bitmap2DReader(in, mode, nar);
    }

    /**
     * service for progressively (AIKR) reading this sensor
     */
    protected class Bitmap2DReader extends Causable {

        private int lastPixel;
        private long lastUpdate;

        private int pixelsRemainPerUpdate;

        final BufferedCauseChannel<ITask> in;

        static final int minUpdateDurs = 1;


        float conf = Float.NaN;

        FloatFloatToObjectFunction<Truth> mode;

        public Bitmap2DReader(CauseChannel<ITask> in, FloatFloatToObjectFunction<Truth> mode, NAR nar) {
            super(nar);
            lastUpdate = nar.time();
            pixelsRemainPerUpdate = area;

            int maxPendingHistory = 8;
            this.in = in.buffered(maxPendingHistory * width * height /* plus extra? */);


            this.mode = mode;
            //(p, v) -> mode.apply(() -> conf).value(p, v);
        }

        @Override
        public float value() {
            return in.value();
        }

        @Override
        protected int next(NAR nar, int work) {

            if (in == null)
                return 0; //return -1;

            priPixel.set(priPixel(pri.floatValue()));

            int dur = nar.dur();

            int totalPixels = area;


            //conf = Math.max(nar.confMin.floatValue(), w2cSafe(c2wSafe(nar.confDefault(BELIEF)) / totalPixels)); //evidence divided equally among pixels
            conf = nar.confDefault(BELIEF);

            long now = nar.time();
            if (now - this.lastUpdate >= nar.dur() * minUpdateDurs) {
                Bitmap2DConcepts.this.update();
                pixelsRemainPerUpdate = totalPixels;
                this.lastUpdate = now;
            } else {
                if (pixelsRemainPerUpdate <= 0)
                    return 0; //return -1;
            }


            int pixelsToProcess = Math.min(pixelsRemainPerUpdate, workToPixels(work));


            if (pixelsToProcess <= 0) //0 or -1
                return pixelsToProcess;

            pixelsRemainPerUpdate -= pixelsToProcess;

            int start, end;


            start = this.lastPixel;
            end = (start + pixelsToProcess);
            Stream<ITask> s;

            if (end > totalPixels) {

                int extra = end - totalPixels;
                s = Stream.concat(
                        stream(mode, start, totalPixels, dur, nar),
                        stream(mode, 0, extra, dur, nar)
                );
                this.lastPixel = extra;
            } else {
                s = Bitmap2DConcepts.this.stream(mode, start, end, dur, nar);
                this.lastPixel = end;
            }

            //TODO stop using Stream<> its not necessary here
            int pixelsGenerated = (int) in.input(s);
            if (pixelsGenerated > 0)
                in.commit();

            return pixelsGenerated;
        }

        /**
         * how many pixels to process for the given work amount
         * can be 1:1 or some other amount
         */
        protected int workToPixels(int work) {
            return work;
        }


    }

    private float priPixel(float pri) {
        //return pri/area;
        return (float) (pri / Math.sqrt(area));
    }


}
