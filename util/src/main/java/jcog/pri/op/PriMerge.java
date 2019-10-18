package jcog.pri.op;

import jcog.Util;
import jcog.pri.Prioritizable;
import jcog.pri.Prioritized;

import java.util.function.BiConsumer;

/**
 * Budget merge function, with input scale factor
 */

public enum PriMerge implements BiConsumer<Prioritizable, Prioritized> {

    plus {
        @Override public float merge(float e, float i) {  return e + i; }
    },
    minus {
        @Override public float merge(float e, float i) {  return e - i; }
    },
    avg {
        @Override public float merge(float e, float i) {  return (e + i)/2; }
    },
    and {
        @Override public float merge(float e, float i) {  return e * i; }

    @Override
        protected boolean undelete() {
            return false;
        }
    },
    or {
        @Override public float merge(float e, float i) {  return Util.or(e, i); }
    },
    min {
        @Override public float merge(float e, float i) {  return Math.min(e, i); }
    },
    max {
        @Override public float merge(float e, float i) {  return Math.max(e, i); }
    },
    replace {
        @Override public float merge(float e, float i) {  return i; }

        @Override
        protected boolean ignoreDeletedIncoming() {
            return false;
        }
    }
    //    AVG_GEO,
    //    AVG_GEO_SLOW, //adds momentum by includnig the existing priority as a factor twice against the new value once
    //    AVG_GEO_FAST,

    ;





    public abstract float merge(float e, float i);


    @Override public final void accept(Prioritizable existing, Prioritized incoming) {
        merge(existing, incoming.pri());
    }

    public final void merge(Prioritizable existing, float incoming) {
        merge(existing, incoming, PriReturn.Post);
    }

    /**
     * merge 'incoming' budget (scaled by incomingScale) into 'existing'
     */
    public final float merge(Prioritizable existing, float incoming, PriReturn mode) {

        if (incoming!=incoming && ignoreDeletedIncoming())
            return 0;

        final float[] _pBefore = new float[1];
        float pAfter = existing.pri((x, y) -> {

            float x1 = x;
            if (x1 != x1) {
                if (!undelete())
                    return Float.NaN;

                x1 = 0; //undelete
            }

            _pBefore[0] = x1;

            return merge(x1, y);
        }, incoming);


        float pBefore = _pBefore[0];

        return mode!=null ? mode.apply(incoming, pBefore, pAfter) : Float.NaN;
    }

    protected boolean ignoreDeletedIncoming() {
        return true;
    }

    /** if the existing value is deleted, whether to undelete (reset to zero) */
    protected boolean undelete() {
        return true;
    }

//    protected boolean commutative() {
//        throw new TODO();
//    }

    /** merges for non-NaN 0..1.0 range */
    public final float mergeUnitize(float existing, float incoming) {
        if (existing != existing)
            existing = 0;
        float next = merge(existing, incoming);
        if (next == next) {
            if (next > 1) next = 1;
            else if (next < 0) next = 0;
        } else
            next = 0;
        return next;
    }


//
//    /**
//     * sum priority
//     */
//    PriMerge<Prioritizable,Prioritized> plus = (tgt, src) -> merge(tgt, src, PLUS);
//
//    /**
//     * avg priority
//     */
//    PriMerge<Prioritizable,Prioritized> avg = (tgt, src) -> merge(tgt, src, AVG);
//
//    PriMerge<Prioritizable,Prioritized> or = (tgt, src) -> merge(tgt, src, OR);
//
//
//    PriMerge<Prioritizable,Prioritized> max = (tgt, src) -> merge(tgt, src, MAX);
//
//    /**
//     * avg priority
//     */
//    PriMerge<Prioritizable,Prioritized> replace = (tgt, src) -> tgt.pri((FloatSupplier)()-> src.pri());


//    PriMerge<Prioritizable,Prioritized> avgGeoSlow = (tgt, src) -> merge(tgt, src, AVG_GEO_SLOW);
//    PriMerge<Prioritizable,Prioritized> avgGeoFast = (tgt, src) -> merge(tgt, src, AVG_GEO_FAST);
//    PriMerge<Prioritizable,Prioritized> avgGeo = (tgt, src) -> merge(tgt, src, AVG_GEO); //geometric mean


}
