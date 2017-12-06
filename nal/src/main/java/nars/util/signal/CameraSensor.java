package nars.util.signal;

import jcog.Util;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.Task;
import nars.concept.SensorConcept;
import nars.control.CauseChannel;
import nars.task.ITask;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.Op.BELIEF;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class CameraSensor<P extends Bitmap2D> extends Sensor2D<P> implements Iterable<CameraSensor<P>.PixelConcept> {


    public static final int RADIX = 1;

    public final List<PixelConcept> pixels;
    public final CauseChannel<ITask> in;

    float resolution = 0.01f;//Param.TRUTH_EPSILON;

    final int numPixels;


    transient int w, h;
    transient float conf;
    private int lastPixel;


    private long lastUpdate;
    int pixelsRemainPerUpdate = 0;

    //private long stamp;


    public CameraSensor(@Nullable Term root, P src, NAgent a) {
        this(root, src, a.nar);
    }
    public CameraSensor(@Nullable Term root, P src, NAR n) {
        super(src, src.width(), src.height(), n);

        this.w = src.width();
        this.h = src.height();
        numPixels = w * h;

        this.in = n.newCauseChannel(this);


        pixels = encode(
                RadixProduct(root, w, h, RADIX)
                //RadixRecurse(root, w, h, RADIX)
                //InhRecurse(root, w, h, RADIX)
                , n);

        lastUpdate = n.time();
    }

    @NotNull
    @Override
    public Iterator<PixelConcept> iterator() {
        return pixels.iterator();
    }


    private final FloatToObjectFunction<Truth> brightnessTruth =
            (v) -> $.t(v, conf);


    public static Int2Function<Compound> XY(Term root, int width, int height) {
        return (x, y) -> $.inh($.p(x, y), root);
    }

    public static Int2Function<Compound> XY(Term root, int radix, int width, int height) {
        return (x, y) -> $.inh($.p($.pRadix(x, radix, width), $.pRadix(y, radix, height)), root);
    }

    private static Int2Function<Term> RadixProduct(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term coords = radix > 1 ?
                    $.p(zipCoords(coord(x, width, radix), coord(y, height, radix))) :
                    //$.p(new Term[]{coord('x', x, width), coord('y', y, height)}) :
                    //new Term[]{coord('x', x, width), coord('y', y, height)} :
                    $.p(x, y);
            return root == null ? coords : $.inh(coords, root);
        };
    }

    private static Int2Function<Term> RadixRecurse(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term coords = radix > 1 ?
                    $.pRecurse(zipCoords(coord(x, width, radix), coord(y, height, radix))) :
                    $.p(x, y);
            return root == null ? coords : $.inh(coords, root);
        };
    }

    private static Int2Function<Term> InhRecurse(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term coords = radix > 1 ?
                    $.inhRecurse(zipCoords(coord(x, width, radix), coord(y, height, radix))) :
                    $.p(x, y);
            return root == null ? coords : $.inh(coords, root);
        };
    }

    private static Term[] zipCoords(Term[] x, Term[] y) {
        int m = Math.max(x.length, y.length);
        Term[] r = new Term[m];
        int sx = m - x.length;
        int sy = m - y.length;
        int ix = 0, iy = 0;
        for (int i = 0; i < m; i++) {
            Term xy;
            char levelPrefix =
                    (char) ('a' + (m - 1 - i)); //each level given a different scale prefix
            //'p';

            if (i >= sx && i >= sy) {
                //xy = Atomic.the(levelPrefix + x[ix++].toString() + y[iy++]);
                xy = $.p($.the(x[ix++]), $.the(levelPrefix), $.the(y[iy++]));
            } else if (i >= sx) {
                //xy = Atomic.the(levelPrefix + x[ix++].toString() + "_");
                xy = $.p($.the(levelPrefix), $.the(x[ix++]));
            } else { //if (i < y.length) {
                //xy = Atomic.the(levelPrefix + "_" + y[iy++]);
                xy = $.p($.the(y[iy++]), $.the(levelPrefix));
            }
            r[i] = xy;
        }
        return r;
    }

    @NotNull
    public static Term coord(char prefix, int n, int max) {
        //return $.pRecurseIntersect(prefix, $.radixArray(n, radix, max));
        //return $.pRecurse($.radixArray(n, radix, max));
        return $.p($.the(prefix), $.p($.radixArray(n, 2, max)));
    }

    @NotNull
    public static Term[] coord(int n, int max, int radix) {
        //return $.pRecurseIntersect(prefix, $.radixArray(n, radix, max));
        //return $.pRecurse($.radixArray(n, radix, max));
        return $.radixArray(n, radix, max);
    }


    public List<PixelConcept> encode(Int2Function<Term> cellTerm, NAR nar) {
        List<PixelConcept> l = $.newArrayList();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                //TODO support multiple coordinate termizations
                Term cell = cellTerm.get(x, y);


                PixelConcept sss = new PixelConcept(cell, x, y, nar);
                nar.on(sss);


                l.add(sss);

                matrix[x][y] = sss;
            }
        }
        return l;
    }

//    private float distToResolution(float dist) {
//
//        float r = Util.lerp(minResolution, maxResolution, dist);
//
//        return r;
//    }

    public CameraSensor resolution(float resolution) {
        this.resolution = resolution;
        return this;
    }

//    public PixelConcept concept(int x, int y) {
//        if (x < 0)
//            x += w;
//        if (y < 0)
//            y += h;
//        if (x >= w)
//            x -= w;
//        if (y >= h)
//            y -= h;
//        return (PixelConcept) matrix[x][y]; //pixels.get(x * width + y);
//    }

    @Override
    public float value() {
        return in.value();
    }


    @Override
    protected int next(NAR nar, int work) {


        int totalPixels = pixels.size();

        long now = nar.time();
        if (this.lastUpdate != now) {
            src.update(1);
            pixelsRemainPerUpdate = totalPixels;
            this.lastUpdate = now;
        } else {
            if (pixelsRemainPerUpdate<=0)
                return -1; //done for this cycle
        }


        //stamp = nar.time.nextStamp();

        this.conf = nar.confDefault(BELIEF);

        //adjust resolution based on value - but can cause more noise in doing so
        //resolution(Util.round(Math.min(0.01f, 0.5f * (1f - this.in.amp())), 0.01f));

        //frame-rate timeslicing

        int pixelsToProcess = Math.min(pixelsRemainPerUpdate, workToPixels(work));
        if (pixelsToProcess == 0)
            return 0;

        pixelsRemainPerUpdate -= pixelsToProcess;

        int start, end;

//        float pixelPri =

//                (float) (nar.priDefault(BELIEF) / (Math.sqrt(numPixels)));
//                ///((float)Math.sqrt(end-start));

        pixelPriCurrent =
                nar.priDefault(BELIEF);
//                (float) (nar.priDefault(BELIEF) / pixelsToProcess);

        start = this.lastPixel;
        end = (start + pixelsToProcess);
        Stream<Task> s;


        if (end > totalPixels) {
            //wrap around
            int extra = end - totalPixels;
            s = Stream.concat(
                update(start, totalPixels, nar), //last 'half'
                update(0, extra, nar) //first half after wrap around
            );
        } else {
            s = update(start, end, nar);
        }

        in.input(s);

        //System.out.println(value + " " + fraction + " "+ start + " " + end);


        return pixelsToProcess;
    }

    /** how many pixels to process for the given work amount */
    protected int workToPixels(int work) {
        return work;
    }

    private Stream<Task> update(int start, int end, NAR nar) {
        long now = nar.time();
        int dur = nar.dur();

        this.lastPixel = end;

        return IntStream.range(start, end).mapToObj(i -> {
            PixelConcept p = pixels.get(i);
            return p.update(now, dur, nar);
        }).filter(Objects::nonNull);
    }


    interface Int2Function<T> {
        T get(int x, int y);
    }

//    private long nextStamp;
//    private void frameStamp() {
//        nextStamp = nar.time.nextStamp();
//    }


    float pixelPriCurrent = 0;

    final FloatSupplier pixelPri = ()->pixelPriCurrent;

    public class PixelConcept extends SensorConcept {

//        //private final int x, y;
        //private final TermContainer templates;

        PixelConcept(Term cell, int x, int y, NAR nar) {
            super(cell, nar, null, brightnessTruth);
            setSignal(() -> Util.unitize(src.brightness(x, y)));
            this.resolution = () -> CameraSensor.this.resolution;
            sensor.pri(pixelPri);

            //            this.x = x;
//            this.y = y;

            //                List<Term> s = $.newArrayList(4);
//                //int extraSize = subs.size() + 4;
//
//                if (x > 0) s.add( concept(x-1, y) );
//                if (x < w-1) s.add( concept(x+1, y) );
//                if (y > 0) s.add( concept(x, y-1) );
//                if (y < h-1) s.add( concept(x, y+1) );
//
//                return TermVector.the(s);

            //this.templates = new PixelNeighborsXYRandom(x, y, w, h, 1);
        }


        //        @Override
//        public TermContainer templates() {
//            return templates;
//        }


        //        @Override
//        protected LongSupplier update(Truth currentBelief, @NotNull NAR nar) {
//            return ()->nextStamp;
//        }


//        @Override
//        protected LongSupplier nextStamp(@NotNull NAR nar) {
//            return CameraSensor.this::nextStamp;
//        }
    }

    /*private long nextStamp() {
        return stamp;
    }*/


//    /** links only to the 'id' of the image, and N random neighboring pixels */
//    private class PixelNeighborsXYRandom implements TermContainer {
//
//        private final int x;
//        private final int y;
//        private final int w;
//        private final int h;
//        private final int extra;
//
//        public PixelNeighborsXYRandom(int x, int y, int w, int h, int extra) {
//            this.extra = extra;
//            this.x = x;
//            this.y = y;
//            this.w = w;
//            this.h = h;
//        }
//
//        @Override
//        public int size() {
//            return extra + 1;
//        }
//
//        @Override
//        public @NotNull Term sub(int i) {
//
//            if (i == 0) {
//                return id;
//            } else {
//                //extra
//                Random rng = nar.random();
//                return concept(
//                        x + (rng.nextBoolean() ? -1 : +1),
//                        y + (rng.nextBoolean() ? -1 : +1)
//                ).term();
//            }
//
//
//
//        }
//    }

//    private class PixelNeighborsManhattan implements TermContainer {
//
//        private final int x;
//        private final int y;
//        private final int w;
//        private final int h;
//        private final TermContainer subs;
//
//        public PixelNeighborsManhattan(TermContainer subs, int x, int y, int w, int h) {
//            this.subs = subs;
//            this.x = x;
//            this.y = y;
//            this.w = w;
//            this.h = h;
//        }
//
//        @Override
//        public int size() {
//            return 4 + subs.size();
//        }
//
//        @Override
//        public @NotNull Term sub(int i) {
//
//
//            switch (i) {
//                case 0:
//                    return (x == 0) ? sub(1) : concept(x - 1, y).sub(0);
//                case 1:
//                    return (x == w - 1) ? sub(0) : concept(x + 1, y).sub(0);
//                case 2:
//                    return (y == 0) ? sub(3) : concept(x, y - 1).sub(0);
//                case 3:
//                    return (y == h - 1) ? sub(2) : concept(x, y + 1).sub(0);
//                default:
//                    return subs.sub(i - 4);
//            }
//
//        }
//    }
}
