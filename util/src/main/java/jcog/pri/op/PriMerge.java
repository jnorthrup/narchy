package jcog.pri.op;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.pri.Prioritized;
import jcog.pri.Priority;

import java.util.function.BiConsumer;

import static jcog.pri.op.PriMerge.PriMergeOp.*;

/**
 * Budget merge function, with input scale factor
 */
@FunctionalInterface
public interface PriMerge<E extends Priority, I extends Prioritized> extends BiConsumer<E, I> {


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

    static void max(Priority existing, Prioritized incoming) {
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
    static float blend(Priority exi, Prioritized inc, PriMerge.PriMergeOp priMerge) {

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
    PriMerge<Priority,Prioritized> plus = (tgt, src) -> blend(tgt, src, PLUS);

    /**
     * avg priority
     */
    PriMerge<Priority,Prioritized> avg = (tgt, src) -> blend(tgt, src, AVG);

    /**
     * geometric mean
     */
    PriMerge<Priority,Prioritized> avgGeo = (tgt, src) -> blend(tgt, src, AVG_GEO);

    PriMerge<Priority,Prioritized> avgGeoSlow = (tgt, src) -> blend(tgt, src, AVG_GEO_SLOW);
    PriMerge<Priority,Prioritized> avgGeoFast = (tgt, src) -> blend(tgt, src, AVG_GEO_FAST);


    PriMerge<Priority,Prioritized> or = (tgt, src) -> blend(tgt, src, OR);


    PriMerge<Priority,Prioritized> max = (tgt, src) -> blend(tgt, src, MAX);

    /**
     * avg priority
     */
    PriMerge<Priority,Prioritized> replace = (tgt, src) -> tgt.pri((FloatSupplier)()-> src.pri());



}
