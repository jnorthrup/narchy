package jcog.pri.op;

import jcog.Util;
import jcog.pri.Prioritized;
import jcog.pri.Priority;

import java.util.function.BiConsumer;

import static jcog.pri.op.PriMerge.PriMergeOp.*;

/**
 * Budget merge function, with input scale factor
 */
@FunctionalInterface
public interface PriMerge extends BiConsumer<Priority, Prioritized> {






    /** merge 'incoming' budget (scaled by incomingScale) into 'existing'
     * @return any resultng overflow priority which was not absorbed by the target, >=0
     * */
    float merge(Priority existing, Prioritized incoming);

    @Override
    default void accept(Priority existing, Prioritized incoming) {
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

        /** adds momentum by includnig the existing priority as a factor twice against the new value once */
        AVG_GEO_SLOW,
        AVG_GEO_FAST,

        MAX
    }

    /** srcScale only affects the amount of priority adjusted; for the other components, the 'score'
     * calculations are used to interpolate
     * @param exi existing budget
     * @param inc incoming budget
     *
     
     * */
    static float blend(Priority exi, Prioritized inc, PriMerge.PriMergeOp priMerge) {

        float e = exi.priElseZero();
        float i = inc.priElseZero();

        float nextPri;
        switch (priMerge) {
            case PLUS:
                nextPri = e + i;
                break;
            case OR:
                nextPri = Util.or(e,i);
                break;
            case MAX:
                nextPri = Math.max(e, i);
                break;
            case AVG:
                nextPri = (i+e)/2f;
                break;
            case AVG_GEO:
                nextPri = Util.aveGeo(e, i);
                break;
            case AVG_GEO_SLOW:
                nextPri = Util.aveGeo(e, e, i);
                break;
            case AVG_GEO_FAST:
                nextPri = Util.aveGeo(e, i, i);
                break;

            
            default:
                throw new UnsupportedOperationException();
        }

        float ePriAfter = exi.priSet( nextPri );

        return i - (ePriAfter - e);
    }







































    /** sum priority */
    PriMerge plus = (tgt, src) -> blend(tgt, src, PLUS);

    /** avg priority */
    PriMerge avg = (tgt, src) -> blend(tgt, src, AVG);

    /** geometric mean */
    PriMerge avgGeo = (tgt, src) -> blend(tgt, src, AVG_GEO);

    PriMerge avgGeoSlow = (tgt, src) -> blend(tgt, src, AVG_GEO_SLOW);
    PriMerge avgGeoFast = (tgt, src) -> blend(tgt, src, AVG_GEO_FAST);


    PriMerge or = (tgt, src) -> blend(tgt, src, OR);


    PriMerge max = (tgt, src) -> blend(tgt, src, MAX);

    /** avg priority */
    PriMerge replace = (tgt, src) -> src.priElse(tgt.priElseZero());




















































































































































}
