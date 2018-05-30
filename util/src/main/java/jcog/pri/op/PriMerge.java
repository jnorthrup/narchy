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
        
        MAX
    }

    /** srcScale only affects the amount of priority adjusted; for the other components, the 'score'
     * calculations are used to interpolate
     * @param exi existing budget
     * @param inc incoming budget
     *
     
     * */
    static float blend(Priority exi, Prioritized inc, PriMerge.PriMergeOp priMerge) {

        float ePriBefore = exi.priElseZero();
        float iPri = inc.priElseZero();

        float nextPri;
        switch (priMerge) {
            case PLUS:
                nextPri = ePriBefore + iPri;
                break;
            case OR:
                nextPri = Util.or(ePriBefore,iPri);
                break;
            case MAX:
                nextPri = Math.max(ePriBefore, iPri);
                break;
            case AVG:
                nextPri = (iPri+ePriBefore)/2f;
                break;
            
            
            
            default:
                throw new UnsupportedOperationException();
        }

        float ePriAfter = exi.priSet( nextPri );

        return iPri - (ePriAfter - ePriBefore);
    }







































    /** sum priority, LERP other components in proportion to the priorities */
    PriMerge plus = (tgt, src) -> blend(tgt, src, PLUS);

    /** avg priority, LERP other components in proportion to the priorities */
    PriMerge avg = (tgt, src) -> blend(tgt, src, AVG);


    PriMerge or = (tgt, src) -> blend(tgt, src, OR);


    PriMerge max = (tgt, src) -> blend(tgt, src, MAX);

    /** avg priority, LERP other components in proportion to the priorities */
    PriMerge replace = (tgt, src) -> src.priElse(tgt.priElseZero());




















































































































































}
