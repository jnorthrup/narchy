package nars.sensor;

import jcog.signal.Bitmap2D;
import jcog.util.Int2Function;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.agent.NAgent;
import nars.concept.signal.Signal;
import nars.control.DurService;
import nars.control.channel.BufferedCauseChannel;
import nars.exe.Causable;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

import static nars.Op.BELIEF;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class Bitmap2DSensor<P extends Bitmap2D> extends Bitmap2DConcepts<P> implements Iterable<Signal> {

    private final NAR nar;
    private FloatFloatToObjectFunction<Truth> mode;

    public Bitmap2DSensor(@Nullable Term root, P src, NAR n) {
        this(src.height() > 1 ?
                /* 2D default */ RadixProduct(root, src.width(), src.height(), /*RADIX*/1) :
                /* 1D default */ (x,y)-> $.p(root, $.the(x))
                , src, n);
    }

    public Bitmap2DSensor(@Nullable Int2Function<Term> pixelTerm, P src, NAR n) {
        super(src, pixelTerm, n);
        this.nar = n;

        /** modes */
        SET = (p, v) ->
                Signal.SET.apply(() ->
                        nar.confDefault(BELIEF)).value(p, v);

        DIFF = (p, v) ->
                Signal.DIFF.apply(() ->
                        nar.confDefault(BELIEF)).value(p, v);

        mode = SET;
    }



    public void input() {
        input(dur(), mode);
    }

    /** manually inputs the contents of the current frame */
    public void input(int dur, FloatFloatToObjectFunction<Truth> mode) {
        nar.input( stream(mode, dur, nar) );
    }

    /** attaches a reading service to the NAR */
    public Bitmap2DReader readAdaptively() {
        return new Bitmap2DReader(mode);
    }

    protected int dur() {
        return nar.dur();
    }

    public Bitmap2DReader readAdaptively(NAgent agent) {
        return new Bitmap2DReader(mode) {
            protected int dur() {
                return Math.max(super.dur(), agent.sensorDur);
            }
        };
    }

    public DurService readDirectEachDuration() {
        return readDirectEachDuration(mode);
    }

    public DurService readDirectEachDuration(FloatFloatToObjectFunction<Truth> mode) {
        return DurService.on(nar, (nn)-> input(nar.dur(), mode));
    }

    final FloatFloatToObjectFunction<Truth> SET;
    final FloatFloatToObjectFunction<Truth> DIFF;


    public static Int2Function<Term> XY(Term root) {
        return (x, y) -> $.inh($.p(x, y), root);
    }

    public static Int2Function<Term> XY(Term root, int radix, int width, int height) {
        return (x, y) ->
                
                $.p(root, $.pRadix(x, radix, width), $.pRadix(y, radix, height));
    }

    public static Int2Function<Term> RadixProduct(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term coords = radix > 1 ?
                    $.p(zipCoords(coord(x, width, radix), coord(y, height, radix))) :
                    
                    
                    $.p(x, y);
            return root == null ? coords : $.inh(coords, root);
        };
    }

    public static Int2Function<Term> RadixRecurse(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term coords = radix > 1 ?
                    $.pRecurse(true, zipCoords(coord(x, width, radix), coord(y, height, radix))) :
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
                    (char) ('a' + (m - 1 - i)); 
            

            if (i >= sx && i >= sy) {
                
                xy = $.p($.the(x[ix++]), $.the(levelPrefix), $.the(y[iy++]));
            } else if (i >= sx) {
                
                xy = $.p($.the(levelPrefix), $.the(x[ix++]));
            } else { 
                
                xy = $.p($.the(y[iy++]), $.the(levelPrefix));
            }
            r[i] = xy;
        }
        return r;
    }

    @NotNull
    public static Term coord(char prefix, int n, int max) {
        
        
        return $.p($.the(prefix), $.p($.radixArray(n, 2, max)));
    }

    @NotNull
    public static Term[] coord(int n, int max, int radix) {
        
        
        return $.radixArray(n, radix, max);
    }

    public Bitmap2DSensor<P> diff() {
        mode = DIFF;
        return this;
    }


    /** service for progressively (AIKR) reading this sensor */
    private class Bitmap2DReader extends Causable {

        private int lastPixel;
        private long lastUpdate;

        private int pixelsRemainPerUpdate; 

        final BufferedCauseChannel in;

        static final int minUpdateDurs = 1;


        float conf = Float.NaN;

        FloatFloatToObjectFunction<Truth> mode;

        public Bitmap2DReader(FloatFloatToObjectFunction<Truth> mode) {
            super(Bitmap2DSensor.this.nar);
            lastUpdate = Bitmap2DSensor.this.nar.time();
            pixelsRemainPerUpdate = area;

            int maxPendingHistory = 8;
            in = nar.newChannel(Bitmap2DSensor.this).buffered(maxPendingHistory * width*height /* plus extra? */);

            this.mode = mode;
                    //(p, v) -> mode.apply(() -> conf).value(p, v);
        }

        @Override
        public float value() {
            return in.value();
        }


        protected int dur() {
            return Bitmap2DSensor.this.dur();
        }



        @Override
        protected int next(NAR nar, int work) {

            if (in == null)
                return -1;

            int dur = this.dur();

            int totalPixels = area;


            //conf = Math.max(nar.confMin.floatValue(), w2cSafe(c2wSafe(nar.confDefault(BELIEF)) / totalPixels)); //evidence divided equally among pixels
            conf = nar.confDefault(BELIEF);

            long now = nar.time();
            if (now - this.lastUpdate >= nar.dur() * minUpdateDurs) {
                Bitmap2DSensor.this.update();
                pixelsRemainPerUpdate = totalPixels;
                this.lastUpdate = now;
            } else {
                if (pixelsRemainPerUpdate <= 0)
                    return -1; 
            }


            



            
            

            
            int pixelsToProcess = Math.min(pixelsRemainPerUpdate, workToPixels(work));

            
            






            

            

            if (pixelsToProcess <= 0) //0 or -1
                return pixelsToProcess;

            pixelsRemainPerUpdate -= pixelsToProcess;

            int start, end;

            
            
                
                
                


            start = this.lastPixel;
            end = (start + pixelsToProcess);
            Stream<Task> s;

            if (end > totalPixels) {
                
                int extra = end - totalPixels;
                s = Stream.concat(
                        stream(mode, start, totalPixels, dur, nar), 
                        stream(mode, 0, extra, dur, nar) 
                );
                this.lastPixel = extra;
            } else {
                s = Bitmap2DSensor.this.stream(mode, start, end, dur, nar);
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


























    
























































    /*private long nextStamp() {
        return stamp;
    }*/



















































































}
