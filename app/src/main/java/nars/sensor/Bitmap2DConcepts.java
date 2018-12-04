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
import java.util.function.BooleanSupplier;
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
    //private final FloatRange priPixel = new FloatRange(0, 0, 1);

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
                    @Override
                    protected CauseChannel<ITask> newChannel(NAR n) {
                        return null;
                    }
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
                }.setPri(pri).setResolution(res);


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
    public final Stream<ITask> stream(FloatFloatToObjectFunction<Truth> truther, NAR nar) {
        return stream(truther, 0, area, nar);
    }

    private long last;

    /**
     * stream of tasks containing changes in all updated pixels
     */
    public Stream<ITask> stream(FloatFloatToObjectFunction<Truth> truther, int pixelStart, int pixelEnd, NAR nar) {

        long now = nar.time();

        long tStart = last; //now - dur / 2;
        long tEnd = now;// + Math.max(0, dur / 2 - 1);
        last = now;
        float dur = nar.dur() * area;
        return pixels(pixelStart, pixelEnd).map(p -> p.update(tStart, tEnd, truther, dur, nar));
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

    public Bitmap2DReader newReader(CauseChannel<ITask> in, FloatFloatToObjectFunction<Truth> mode, BooleanSupplier enable, NAR nar) {
        return new Bitmap2DReader(in, mode, nar) {
            @Override
            protected void next(NAR nar, BooleanSupplier kontinue) {
                if (!enable.getAsBoolean()) {
                    sleeping(nar);
                    return;
                }
                super.next(nar, kontinue);
            }
        };
    }

    public Bitmap2DReader newReader(CauseChannel<ITask> in, FloatFloatToObjectFunction<Truth> mode, NAR nar) {
        return new Bitmap2DReader(in, mode, nar);
    }

    /**
     * service for progressively (AIKR) reading this sensor
     */
    protected class Bitmap2DReader extends Causable {

        private int lastPixel;
        private long lastFrameStart;


        final BufferedCauseChannel<ITask> in;


        float conf = Float.NaN;

        FloatFloatToObjectFunction<Truth> mode;
        private volatile int pixelsSinceLastStart = 0;

        public Bitmap2DReader(CauseChannel<ITask> in, FloatFloatToObjectFunction<Truth> mode, NAR nar) {
            super();
            lastFrameStart = nar.time();


            int maxPendingHistory = 8;
            this.in = in.buffered(maxPendingHistory * width * height /* plus extra? */);


            this.mode = mode;
            //(p, v) -> mode.apply(() -> conf).value(p, v);

            nar.on(this);
        }

        @Override
        public float value() {
            return in.value();
        }

        @Override
        protected void next(NAR nar, BooleanSupplier kontinue) {




            int totalPixels = area;


            //conf = Math.max(nar.confMin.floatValue(), w2cSafe(c2wSafe(nar.confDefault(BELIEF)) / totalPixels)); //evidence divided equally among pixels
            conf = nar.confDefault(BELIEF);

            long now = nar.time();
            Bitmap2DConcepts.this.update();

            long sinceLastFrameStart = now - lastFrameStart;

            //priPixel.set(priPixel(pri.floatValue()));

//            int pixelsToProcess = Math.min(pixelsRemainPerUpdate, workToPixels(work));
//
//
//            if (pixelsToProcess <= 0) //0 or -1
//                return;

//            pixelsRemainPerUpdate -= pixelsToProcess;




            int firstPixel = this.lastPixel;
            int lastPixel = (firstPixel + totalPixels);
            Stream<ITask> s;
            int dur = nar.dur();

            if (lastPixel > totalPixels) {
                s = Stream.concat(
                        stream(mode, firstPixel, totalPixels, nar),
                        stream(mode, 0, lastPixel - totalPixels, nar)
                );
            } else {
                s = Bitmap2DConcepts.this.stream(mode, firstPixel, lastPixel, nar);
            }

            //TODO stop using Stream<> its not necessary here
//            int beforeStart = in.size();
//            int pixelsRead =
//                    //(int) in.input(s.takeWhile((z) -> beforeStart == in.size() || kontinue.getAsBoolean() ) );
            final int[] pixelsRead = {0};
            s.forEach(z -> {
                if (z != null) {
                    ITask.run(z, nar); //inline
                }
                pixelsRead[0]++;
            });

            if (sinceLastFrameStart > dur) {
                pixelsSinceLastStart = 0;
                lastFrameStart = now;
            } else {
                pixelsSinceLastStart += pixelsRead[0];
                if (pixelsSinceLastStart >= area) {
                    long untilNext = Math.max(1, dur - 1) + lastFrameStart;
                    if (untilNext > now)
                        sleepUntil(untilNext);
                }
            }

            if (pixelsRead[0] > 0) {
                this.lastPixel = (pixelsRead[0] + this.lastPixel ) % totalPixels;
                in.commit();
            }
        }

//        /**
//         * how many pixels to process for the given work amount
//         * can be 1:1 or some other amount
//         */
//        protected int workToPixels(int work) {
//            return work;
//        }


    }

//    private float priPixel(float pri) {
//        return pri;
//        //return pri/area;
//        //return (float) (pri / Math.sqrt(area));
//    }


}
