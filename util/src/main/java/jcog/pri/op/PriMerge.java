package jcog.pri.op;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.pri.Prioritized;
import jcog.pri.Prioritizable;

import java.util.function.BiConsumer;

import static jcog.pri.op.PriMerge.PriMergeOp.*;

/**
 * Budget merge function, with input scale factor
 */
@FunctionalInterface
public interface PriMerge<E extends Prioritizable, I extends Prioritized> extends BiConsumer<E, I> {


    /**
     * merge 'incoming' budget (scaled by incomingScale) into 'existing'
     *
     * @return any resultng overflow priority which was not absorbed by the target, >=0
     */
    float merge(E existing, I incoming);

    @Override
    default void accept(E existing, I incoming) {
        merge(existing, incoming);
    }

    static void max(Prioritizable existing, Prioritized incoming) {
        float p = incoming.priElseZero();
        if (p > 0)
            existing.priMax(p);
    }

    enum PriMergeOp {
        PLUS,
        AVG,
        OR,
        AVG_GEO,

        /**
         * adds momentum by includnig the existing priority as a factor twice against the new value once
         */
        AVG_GEO_SLOW,
        AVG_GEO_FAST,

        MAX
    }

    /**
     * srcScale only affects the amount of priority adjusted; for the other components, the 'score'
     * calculations are used to interpolate
     *
     * @param exi existing budget
     * @param inc incoming budget
     */
    static float blend(Prioritizable exi, Prioritized inc, PriMerge.PriMergeOp priMerge) {

        final float i = inc.priElseZero();
        final float[] e = new float[1];
        float ePriAfter = exi.pri((x, y) -> {
            if (x != x)
                x = 0; //undelete

            e[0] = x;

            float nextPri;
            switch (priMerge) {
                case PLUS:
                    nextPri = x + y;
                    break;
                case OR:
                    nextPri = Util.or(x, y);
                    break;
                case MAX:
                    nextPri = Math.max(x, y);
                    break;
                case AVG:
                    nextPri = (x + y) / 2f;
                    break;
                case AVG_GEO:
                    nextPri = Util.aveGeo(x, y);
                    break;
                case AVG_GEO_SLOW:
                    nextPri = Util.aveGeo(x, x, y);
                    break;
                case AVG_GEO_FAST:
                    nextPri = Util.aveGeo(x, y, y);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            return nextPri;
        }, i);

        return i - (ePriAfter - e[0]);
    }


    /**
     * sum priority
     */
    PriMerge<Prioritizable,Prioritized> plus = (tgt, src) -> blend(tgt, src, PLUS);

    /**
     * avg priority
     */
    PriMerge<Prioritizable,Prioritized> avg = (tgt, src) -> blend(tgt, src, AVG);

    /**
     * geometric mean
     */
    PriMerge<Prioritizable,Prioritized> avgGeo = (tgt, src) -> blend(tgt, src, AVG_GEO);

    PriMerge<Prioritizable,Prioritized> avgGeoSlow = (tgt, src) -> blend(tgt, src, AVG_GEO_SLOW);
    PriMerge<Prioritizable,Prioritized> avgGeoFast = (tgt, src) -> blend(tgt, src, AVG_GEO_FAST);


    PriMerge<Prioritizable,Prioritized> or = (tgt, src) -> blend(tgt, src, OR);


    PriMerge<Prioritizable,Prioritized> max = (tgt, src) -> blend(tgt, src, MAX);

    /**
     * avg priority
     */
    PriMerge<Prioritizable,Prioritized> replace = (tgt, src) -> tgt.pri((FloatSupplier)()-> src.pri());



}
