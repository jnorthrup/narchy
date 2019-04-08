package nars.sensor;

import com.google.common.collect.Iterables;
import jcog.func.IntIntToObjectFunction;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.NAR;
import nars.agent.Game;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.concept.sensor.Signal;
import nars.concept.sensor.VectorSensor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Int;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;

import static nars.Op.BELIEF;

/**
 * manages reading a camera to a pixel grid of SensorConcepts
 * monochrome
 */
public class Bitmap2DSensor<P extends Bitmap2D> extends VectorSensor {

    public final Bitmap2DConcepts<P> concepts;
    public final P src;

    public final int width, height;
    private FloatFloatToObjectFunction<Truth> mode;

    public Bitmap2DSensor(@Nullable Term root, P src, NAR n) {
        this(src.height() > 1 ?
                        /* 2D default */ RadixProduct(root, src.width(), src.height(), /*RADIX*/1) :
                        /* 1D default */ (x, y) -> root != null ? $.p(root,$.the(x)) : $.p(x) //y==1
                , src, n);
    }

    public Bitmap2DSensor(@Nullable IntIntToObjectFunction<nars.term.Term> pixelTerm, P src, NAR n) {
        super(pixelTerm.apply(0,1)
                    .replace(Map.of(
                        Int.the(0), $.varDep(1),
                        Int.the(1), $.varDep(2))
        ), n);
        this.width = src.width();
        this.height = src.height();
        this.concepts = new Bitmap2DConcepts<>(src, pixelTerm, res, n);
        this.src = concepts.src;


        if (src instanceof PixelBag) {
            //HACK steal the actions for this attn group
            ((PixelBag)src).actions.forEach(aa -> aa.attn.parent(n, attn));
        }

        /** modes */
        SET = (p, v) ->
                Signal.SET.apply(() ->
                        nar.confDefault(BELIEF)).value(p, v);

        DIFF = (p, v) ->
                Signal.DIFF.apply(() ->
                        nar.confDefault(BELIEF)).value(p, v);

        mode = SET;
    }

    public Bitmap2DSensor<P> mode(FloatFloatToObjectFunction<Truth> mode) {
        this.mode = mode;
        return this;
    }

    @Override
    public int size() {
        return width * height;
    }

    @Override
    public Iterable<Termed> components() {
        return Iterables.transform(concepts, Concept::term);
    }


//    public void input() {
//        input(mode);
//    }
//
//    /**
//     * manually inputs the contents of the current frame
//     */
//    public void input(FloatFloatToObjectFunction<Truth> mode) {
//        in.input(concepts.stream(mode, nar));
//    }

//    public Bitmap2DConcepts.Bitmap2DReader readAdaptively() {
//        return concepts.newReader(in, mode, nar);
//    }
//    public Bitmap2DConcepts.Bitmap2DReader readAdaptively(BooleanSupplier enable) {
//        return concepts.newReader(in, mode, enable, nar);
//    }

//    public DurService readDirectEachDuration() {
//        return readDirectEachDuration(mode);
//    }

//    public DurService readDirectEachDuration(FloatFloatToObjectFunction<Truth> mode) {
//        return DurService.on(nar, (nn)-> input(nar.dur(), mode));
//    }

    final FloatFloatToObjectFunction<Truth> SET;
    final FloatFloatToObjectFunction<Truth> DIFF;


    public static IntIntToObjectFunction<nars.term.Term> XY(Term root) {
        return (x, y) -> $.inh($.p(x, y), root);
    }

    public static IntIntToObjectFunction<nars.term.Term> XY(Term root, int radix, int width, int height) {
        return (x, y) ->
                $.p(root, $.pRadix(x, radix, width), $.pRadix(y, radix, height));
    }

    public static IntIntToObjectFunction<nars.term.Term> RadixProduct(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term coords = radix > 1 ?
                    $.p(zipCoords(coord(x, width, radix), coord(y, height, radix))) :
                    $.p(x, y);
            return root == null ? coords :
                    //$.p(root, coords);
                    $.inh(coords, root);
        };
    }

    public static IntIntToObjectFunction<nars.term.Term> RadixRecurse(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term coords = radix > 1 ?
                    $.pRecurse(true, zipCoords(coord(x, width, radix), coord(y, height, radix))) :
                    $.p(x, y);
            return root == null ? coords : $.p(root,coords);
        };
    }

    public static IntIntToObjectFunction<nars.term.Term> InhRecurse(@Nullable Term root, int width, int height, int radix) {
        return (x, y) -> {
            Term coords = radix > 1 ?
                    $.inhRecurse(zipCoords(coord(x, width, radix), coord(y, height, radix))) :
                    $.p(x, y);
            return root == null ? coords : $.p(root,coords);
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


    public static Term coord(char prefix, int n, int max) {
        return $.p($.the(prefix), $.p($.radixArray(n, 2, max)));
    }

    public static Term[] coord(int n, int max, int radix) {


        return $.radixArray(n, radix, max);
    }

    public Bitmap2DSensor<P> diff() {
        mode = DIFF;
        return this;
    }

    @Override
    public void update(Game g) {
        src.updateBitmap();
        super.update(g);
    }

    public final TaskConcept get(int x, int y) {
        return concepts.get(x, y);
    }


    @Override
    public final Iterator<Signal> iterator() {
        return concepts.iterator();
    }



























    
























































    /*private long nextStamp() {
        return stamp;
    }*/


}
