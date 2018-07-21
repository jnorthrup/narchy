package nars.sensor;

import jcog.signal.Bitmap2D;
import jcog.util.Int2Function;
import nars.$;
import nars.NAR;
import nars.agent.NAgent;
import nars.concept.TaskConcept;
import nars.concept.sensor.AbstractSensor;
import nars.concept.sensor.Signal;
import nars.control.DurService;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.BELIEF;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class Bitmap2DSensor<P extends Bitmap2D> extends AbstractSensor {

    public final Bitmap2DConcepts<P> concepts;
    public final P src;
    private final NAR nar;
    private final CauseChannel<ITask> in;
    public final int width, height;
    private FloatFloatToObjectFunction<Truth> mode;

    public Bitmap2DSensor(@Nullable Term root, P src, NAR n) {
        this(src.height() > 1 ?
                /* 2D default */ RadixProduct(root, src.width(), src.height(), /*RADIX*/1) :
                /* 1D default */ (x,y)-> $.p(root, $.the(x))
                , src, n);
    }

    public Bitmap2DSensor(@Nullable Int2Function<Term> pixelTerm, P src, NAR n) {
        super(n);
        this.width = src.width();
        this.height = src.height();
        this.concepts = new Bitmap2DConcepts<>(src, pixelTerm, pri(), resolution(), n);
        this.src = concepts.src;
        this.nar = n;

        /** modes */
        SET = (p, v) ->
                Signal.SET.apply(() ->
                        nar.confDefault(BELIEF)).value(p, v);

        DIFF = (p, v) ->
                Signal.DIFF.apply(() ->
                        nar.confDefault(BELIEF)).value(p, v);

        mode = SET;
        this.in = nar.newChannel(this);
    }


    @Override
    public void update(long last, long now, NAR nar) {
        //..
    }

    public void input() {
        input(dur(), mode);
    }

    /** manually inputs the contents of the current frame */
    public void input(int dur, FloatFloatToObjectFunction<Truth> mode) {
        nar.input( concepts.stream(mode, dur, nar) );
    }

    protected int dur() {
        return nar.dur();
    }

    public Bitmap2DConcepts.Bitmap2DReader readAdaptively(NAgent agent) {
        return concepts.newReader(in, mode, nar);
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


    public final TaskConcept get(int x, int y) {
        return concepts.get(x, y);
    }



























    
























































    /*private long nextStamp() {
        return stamp;
    }*/



















































































}
