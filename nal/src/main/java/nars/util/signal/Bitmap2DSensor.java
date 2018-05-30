package nars.util.signal;

import jcog.signal.Bitmap2D;
import jcog.util.Int2Function;
import nars.$;
import nars.NAR;
import nars.NAgent;
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
        input(dur(), SET);
    }

    /** manually inputs the contents of the current frame */
    public void input(int dur, FloatFloatToObjectFunction<Truth> mode) {
        nar.input( stream(mode, dur, nar) );
    }

    /** attaches a reading service to the NAR */
    public Bitmap2DReader readAdaptively() {
        return new Bitmap2DReader();
    }

    protected int dur() {
        return nar.dur();
    }

    public Bitmap2DReader readAdaptively(NAgent agent) {
        return new Bitmap2DReader() {
            protected int dur() {
                return Math.max(super.dur(), agent.sensorDur);
            }
        };
    }

    public DurService readDirectEachDuration() {
        return readDirectEachDuration(SET);
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



    /** service for progressively (AIKR) reading this sensor */
    private class Bitmap2DReader extends Causable {

        private int lastPixel;
        private long lastUpdate;

        private int pixelsRemainPerUpdate; 

        final CauseChannel<ITask> in;

        static final int minUpdateDurs = 1;


        /** to calculate avg number pixels processed per duration */
        private final DescriptiveStatistics pixelsProcessed;

        










        float conf;

        FloatFloatToObjectFunction<Truth> mode;

        public Bitmap2DReader() {
            super(Bitmap2DSensor.this.nar);
            lastUpdate = Bitmap2DSensor.this.nar.time();
            pixelsRemainPerUpdate = area;
            in = nar.newChannel(Bitmap2DSensor.this);
            pixelsProcessed = new DescriptiveStatistics(8);
            conf = nar.confDefault(BELIEF);
            mode = (p, v) -> Scalar.SET.apply(() -> conf).value(p, v);
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

            int dur = this.dur();
            conf = nar.confDefault(BELIEF);

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
                    return -1; 
            }


            



            
            

            
            int pixelsToProcess = Math.min(pixelsRemainPerUpdate, workToPixels(work));

            
            






            

            

            if (pixelsToProcess == 0)
                return 0;

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







            in.input(s);

            


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


























    
























































    /*private long nextStamp() {
        return stamp;
    }*/



















































































}
