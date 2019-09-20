package nars.sensor;

import com.google.common.collect.Iterables;
import jcog.func.IntIntToObjectFunction;
import jcog.signal.wave2d.Bitmap2D;
import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.game.Game;
import nars.game.sensor.ComponentSignal;
import nars.game.sensor.Signal;
import nars.game.sensor.VectorSensor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Int;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import static java.lang.Math.max;
import static nars.Op.BELIEF;

/** TODO generalize beyond any 2D specific that this class was originally designed for */
public class Bitmap2DSensor<P extends Bitmap2D> extends VectorSensor {

    public final Bitmap2DConcepts<P> concepts;
    public final P src;

    public final int width, height;
    private FloatFloatToObjectFunction<Truth> mode;

    public Bitmap2DSensor(@Nullable Term root, P src, NAR n) {
        this(src.height() > 1 ?
                        /* 2D default */ RadixProduct(root, src.width(), src.height(), /*RADIX*/1) :
                        /* 1D default */ (x, y) -> root != null ? $.inh(root,$.the(x)) : $.p(x) //y==1
                , src, n);
    }

    public Bitmap2DSensor(@Nullable IntIntToObjectFunction<nars.term.Term> pixelTerm, P src, NAR n) {
        this(pixelTerm, src, Float.NaN, n);
    }

    public Bitmap2DSensor(@Nullable IntIntToObjectFunction<nars.term.Term> pixelTerm, P src, float defaultFreq, NAR n) {
        super(pixelTerm.apply(0,1)
                    .replace(Map.of(
                        Int.the(0), $.func("range", Int.the(0), Int.the(src.width()-1)),
                        Int.the(1), $.func("range", Int.the(0), Int.the(src.height()-1)))
        ), n);
        this.width = src.width();
        this.height = src.height();

        this.concepts = new Bitmap2DConcepts<>(src, pixelTerm, defaultFreq, this);
        this.src = concepts.src;


        if (src instanceof PixelBag) {
            //HACK sub-pri the actions for this attn group
            ((PixelBag)src).actions.forEach(aa -> n.control.input(aa.pri, pri));
        }

        /** modes */
        SET = (p, v) ->
                Signal.SET.apply(() ->
                        nar.confDefault(BELIEF)).value(p, v);

        DIFF = (p, v) ->
                Signal.DIFF.apply(() ->
                        nar.confDefault(BELIEF)).value(p, v);

        mode = SET;
        n.start(this);
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
        int m = max(x.length, y.length);
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
        //link(g);
    }

//    public void link(Game g) {
//        float basePri = this.pri.pri();
//        double sur = surprise();
//        float pri = (float) sur * basePri;
//
//        AbstractTaskLink tl = newLink();
//        tl.priMerge(BELIEF, pri, NAL.tasklinkMerge); //TODO * preamp?
////            tl.priMax(QUEST, surprise);
////            tl.priMax(GOAL, surprise*(1/4f));
//        g.what().link(tl);
//
//    }

    public final TaskConcept get(int x, int y) {
        return concepts.get(x, y);
    }




    @Override
    public final Iterator<ComponentSignal> iterator() {
        return concepts.iterator();
    }

    @Override
    public Signal get(Random rng) {
        return concepts.get(rng);
    }

//    private class PixelSelectorTaskLink extends DynamicTaskLink {
//        PixelSelectorTaskLink() {
//            super(term());
//        }
//
//
//        @Override public Termed src(When<NAR> when) {
//            return Bitmap2DSensor.this.get(when.x.random());
//        }
//
//
//        /** saccade shape */
//        @Override public Term target(Task task, Derivation d) {
//            //return task.term();
//            //return randomPixel(d.random).term();
//
//            //random adjacent cell
//            Bitmap2DConcepts.PixelSignal ps = (Bitmap2DConcepts.PixelSignal) d.nar.concept(task.term());
//            if (ps!=null) {
//                int xx = clamp(ps.x + d.random.nextInt(3) - 1, 0, width-1),
//                        yy = clamp(ps.y + d.random.nextInt(3) - 1, 0, height-1);
//                return concepts.get(xx, yy).term();
//            } else
//                return null;
////
////        Term[] nn;
////        Term center = pixelTerm.apply(xx, yy);
////        if (linkNESW) {
////            List<Term> neighbors = new FasterList(4);
////            if (xx > 0)
////                neighbors.add(pixelTerm.apply(xx - 1, yy));
////            if (yy > 0)
////                neighbors.add(pixelTerm.apply(xx, yy - 1));
////            if (xx < width - 1)
////                neighbors.add(pixelTerm.apply(xx + 1, yy));
////            if (yy < height - 1)
////                neighbors.add(pixelTerm.apply(xx, yy + 1));
////
////            nn = neighbors.toArray(EmptyTermArray);
////        } else {
////            nn = EmptyTermArray;
////        }
////        return TemplateTermLinker.of(center, nn);
////    }
//        }
//    }

//    private class ConjunctionSuperPixelTaskLink extends DynamicTaskLink {
//
//        final static int MAX_WIDTH = 2, MAX_HEIGHT = 2;
//
//        ConjunctionSuperPixelTaskLink() {
//            super(term());
//        }
//
//        @Override
//        public Termed src(When<NAR> when) {
//            return superPixel(when.x.random());
//        }
//
//
//        @Override
//        public Term target(Task task, Derivation d) {
//            return task.term();
//        }
//
//
//        @Nullable
//        private Term superPixel(Random rng) {
//            int batchX = max(rng.nextInt(Math.min(width, MAX_WIDTH)+1),1),
//                batchY = max(rng.nextInt(Math.min(height, MAX_HEIGHT)+1), 1);
//            int px = rng.nextInt(concepts.width - batchX+1);
//            int py = rng.nextInt(concepts.height - batchY+1);
//            TermList subterms = (batchX*batchY > 1) ? new TermList(batchX * batchY) : null;
//            for (int i = px; i < px+batchX; i++) {
//                for (int j = py; j < py+batchY; j++) {
//                    Signal ij = concepts.get(i, j);
//                    Term ijt = ij.term();
//                    if (subterms==null)
//                        return ijt; //only one pixel, unnegated
//                    Task current = ((SensorBeliefTables) (ij.beliefs())).current();
//                    if (current!=null) {
//                        subterms.add(ijt.negIf(current.isNegative()));
//                    }
//                }
//            }
//            switch (subterms.size()) {
//                case 0: return null;
//                case 1: return subterms.get(0);
//                default: return CONJ.the(0, (Subterms) subterms);
//            }
//        }
//
//    }

}
