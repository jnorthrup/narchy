package nars.util.signal;

import jcog.signal.Bitmap2D;
import jcog.util.Int2Function;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.scalar.Scalar;
import nars.control.DurService;
import nars.control.channel.CauseChannel;
import nars.exe.Causable;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

import static nars.Op.BELIEF;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class Bitmap2DSensor<P extends Bitmap2D> extends Bitmap2DConcepts<P> implements Iterable<Scalar> {

    private final NAR nar;

    public Bitmap2DSensor(@Nullable Term root, P src, NAR n) {
        this(src.height() > 1 ?
                /* 2D default */ RadixProduct(root, src.width(), src.height(), /*RADIX*/1) :
                /* 1D default */ (x,y)->$.p(x)
                , src, n);
    }

    public Bitmap2DSensor(@Nullable Int2Function<Term> pixelTerm, P src, NAR n) {
        super(src, pixelTerm, n);
        this.nar = n;

        /** modes */
        SET = (p, v) ->
                Scalar.SET.apply(() ->
                        nar.confDefault(BELIEF)).value(p, v);

        DIFF = (p, v) ->
                Scalar.DIFF.apply(() ->
                        nar.confDefault(BELIEF)).value(p, v);

    }


    public void input() {
        input(SET);
    }

    /** manually inputs the contents of the current frame */
    public void input(FloatFloatToObjectFunction<Truth> mode) {
        nar.input( stream(mode, nar) );
    }

    /** attaches a reading service to the NAR */
    public Bitmap2DReader readAdaptively() {
        return new Bitmap2DReader(nar);
    }


    public DurService readDirectEachDuration() {
        return readDirectEachDuration(SET);
    }

    public DurService readDirectEachDuration(FloatFloatToObjectFunction<Truth> mode) {
        return DurService.on(nar, (nn)->{
            input(mode);
        });
    }

    final FloatFloatToObjectFunction<Truth> SET;
    final FloatFloatToObjectFunction<Truth> DIFF;


    public static Int2Function<Term> XY(Term root) {
        return (x, y) -> $.inh($.p(x, y), root);
    }

    public static Int2Function<Term> XY(Term root, int radix, int width, int height) {
        return (x, y) ->
                //$.inh($.p($.pRadix(x, radix, width), $.pRadix(y, radix, height)), root);
                $.p(root, $.pRadix(x, radix, width), $.pRadix(y, radix, height));
    }

    public static Int2Function<Term> RadixProduct(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term coords = radix > 1 ?
                    $.p(zipCoords(coord(x, width, radix), coord(y, height, radix))) :
                    //$.p(new Term[]{coord('x', x, width), coord('y', y, height)}) :
                    //new Term[]{coord('x', x, width), coord('y', y, height)} :
                    $.p(x, y);
            return root == null ? coords : $.inh(coords, root);
        };
    }

    public static Int2Function<Term> RadixRecurse(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term coords = radix > 1 ?
                    $.pRecurse(zipCoords(coord(x, width, radix), coord(y, height, radix))) :
                    $.p(x, y);
            return root == null ? coords : $.inh(coords, root);
        };
    }

    public static Int2Function<Term> InhRecurse(@Nullable Term root, int width, int height, int radix) {
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



    /** service for progressively (AIKR) reading this sensor */
    private class Bitmap2DReader extends Causable {

        private final NAR n;
        private int lastPixel;
        private long lastUpdate;

        private int pixelsRemainPerUpdate; //initial value

        final CauseChannel<ITask> in;

        static final int minUpdateDurs = 1;


        /** to calculate avg number pixels processed per duration */
        private final DescriptiveStatistics pixelsProcessed;

        //        /** sets difference mode */
//        public Bitmap2DSensor modeDiffer() {
//            brightnessTruth = Signal.DIFF.apply(()->conf);
//            return this;
//        }
//
//        public Bitmap2DSensor modeUpdate() {
//            brightnessTruth = Signal.SET.apply(()->conf);
//            return this;
//        }

        float conf;

        FloatFloatToObjectFunction<Truth> mode;

        public Bitmap2DReader(NAR n) {
            super(n);
            this.n = n;
            lastUpdate = n.time();
            pixelsRemainPerUpdate = area;
            in = n.newChannel(Bitmap2DSensor.this);
            pixelsProcessed = new DescriptiveStatistics(8);
            conf = n.confDefault(BELIEF);
            mode = (p, v) -> Scalar.SET.apply(() -> conf).value(p, v);
        }

        @Override
        public float value() {
            return in.value();
        }


        @Override
        protected int next(NAR nar, int work) {

            conf = n.confDefault(BELIEF);

            int totalPixels = area;

            long now = nar.time();
            if (now - this.lastUpdate >= nar.dur() * minUpdateDurs) {
                int pixelsProcessedInLastDur = totalPixels - pixelsRemainPerUpdate;
                pixelsProcessed.addValue(pixelsProcessedInLastDur);
                Bitmap2DSensor.this.update();
                pixelsRemainPerUpdate = totalPixels;
                this.lastUpdate = now;
            } else {
                if (pixelsRemainPerUpdate <= 0)
                    return -1; //done for this cycle
            }


            //stamp = nar.time.nextStamp();



            //adjust resolution based on value - but can cause more noise in doing so
            //resolution(Util.round(Math.min(0.01f, 0.5f * (1f - this.in.amp())), 0.01f));

            //frame-rate timeslicing
            int pixelsToProcess = Math.min(pixelsRemainPerUpdate, workToPixels(work));

            //confidence proportional to the amount of the frame processed per duration, calculated in aggregate because
            //each call will only proceed some of the image potentially but multiple times per duration
//                float meanPixelsProcessedPerDuration = (float) pixelsProcessed.getMean();
//                if (meanPixelsProcessedPerDuration!=meanPixelsProcessedPerDuration)
//                    meanPixelsProcessedPerDuration = 0;

//                this.conf =
//                        nar.confDefault(BELIEF)
            //w2c(Util.lerp( (meanPixelsProcessedPerDuration) / numPixels, c2w(nar.confMin.floatValue()), c2w(nar.confDefault(BELIEF))))

            //System.out.println(meanPixelsProcessedPerDuration + "/" + numPixels + " -> " + conf + "%");

            if (pixelsToProcess == 0)
                return 0;

            pixelsRemainPerUpdate -= pixelsToProcess;

            int start, end;

            //pixelPri.set(nar.priDefault(BELIEF));
            //                (float) (nar.priDefault(BELIEF) / pixelsToProcess);
                //        float pixelPri =
                //                (float) (nar.priDefault(BELIEF) / (Math.sqrt(numPixels)));
                //                ///((float)Math.sqrt(end-start));


            start = this.lastPixel;
            end = (start + pixelsToProcess);
            Stream<Task> s;

            if (end > totalPixels) {
                //wrap around
                int extra = end - totalPixels;
                s = Stream.concat(
                        stream(mode, start, totalPixels, nar), //last 'half'
                        stream(mode, 0, extra, nar) //first half after wrap around
                );
                this.lastPixel = extra;
            } else {
                s = Bitmap2DSensor.this.stream(mode, start, end, nar);
                this.lastPixel = end;
            }

//            float priEach =
//                    //1f/totalPixels;
//                    (float) (1/Math.sqrt(totalPixels));
//            Consumer<Task> pixelBudgeter = x -> x.pri(priEach);
//            s = (s.peek(pixelBudgeter));

            in.input(s);

            //System.out.println(value + " " + fraction + " "+ start + " " + end);


            return pixelsToProcess;
        }

        /**
         * how many pixels to process for the given work amount
         * can be 1:1 or some other amount
         */
        protected int workToPixels(int work) {
            return work;
        }


    }


//    private float distToResolution(float dist) {
//
//        float r = Util.lerp(minResolution, maxResolution, dist);
//
//        return r;
//    }


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





    //    private long nextStamp;
//    private void frameStamp() {
//        nextStamp = nar.time.nextStamp();
//    }





//    static public class PixelConcept extends SensorConcept {
//
//        private final int x, y;
//        //private final TermContainer templates;
//
//        PixelConcept(Term cell, int x, int y, NAR nar) {
//            super(cell, nar, null, brightnessTruth);
//
//            sensor.pri(pixelPri);
//
//            this.x = x;
//            this.y = y;
//
//            //                List<Term> s = $.newArrayList(4);
////                //int extraSize = subs.size() + 4;
////
////                if (x > 0) s.add( concept(x-1, y) );
////                if (x < w-1) s.add( concept(x+1, y) );
////                if (y > 0) s.add( concept(x, y-1) );
////                if (y < h-1) s.add( concept(x, y+1) );
////
////                return TermVector.the(s);
//
////            this.templates = new PixelNeighborsXYRandom(x, y, w, h, 1);
//
////            List<Termed> l = templates();
////            for (int i = x - 1; i <= x + 1; i++) {
////                for (int j = y - 1; j <= y + 1; j++) {
////                    if (i == x && j == y) continue;
////                    if (i < 0 || j < 0 || i >= w || j >= h) continue;
////                    l.add(pixelTerm.get(i, j));
////                }
////            }
//        }
//
//
//        //        @Override
////        protected LongSupplier update(Truth currentBelief, @NotNull NAR nar) {
////            return ()->nextStamp;
////        }
//
//
////        @Override
////        protected LongSupplier nextStamp(@NotNull NAR nar) {
////            return CameraSensor.this::nextStamp;
////        }
//    }

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
