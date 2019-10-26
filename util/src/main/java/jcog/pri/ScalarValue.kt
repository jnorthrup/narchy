package jcog.pri

import jcog.util.FloatFloatToFloatFunction

/**
 * general purpose value.  consumes and supplies 32-bit float numbers
 * supports certain numeric operations on it
 * various storage implementations are possible
 * as well as the operation implementations.
 *
 * see: NumericUtils.java (lucene)
 */
interface ScalarValue : Prioritized {

    /** setter
     * @return this instance
     */
    fun <P : ScalarValue> pri(p: Float): P

    @kotlin.jvm.JvmDefault
    open fun priSetAndGet(p: Float): Float {
        pri<ScalarValue>(p)
        return p
    }

    /** getter.  returns NaN to indicate deletion  */
    override fun pri(): Float

    @kotlin.jvm.JvmDefault
    open fun priComparable(): Int {
        return java.lang.Float.floatToIntBits(pri())
    }

    @kotlin.jvm.JvmDefault
    open fun pri(update: FloatFloatToFloatFunction, x: Float): Float {
        return priSetAndGet(update.apply(pri(), x))
    }

    /** implementations can provide a faster non-value-returning strategy  */
    @kotlin.jvm.JvmDefault
    open fun priUpdate(update: FloatFloatToFloatFunction, x: Float) {
        pri(update, x)
    }


    @kotlin.jvm.JvmDefault
    fun priDelta(update: FloatFloatToFloatFunction, x: Float): FloatArray {
        val beforeAfter = FloatArray(2)
        beforeAfter[1] = pri(FloatFloatToFloatFunction { xx, yy ->
            beforeAfter[0] = xx
            update.apply(xx, yy)
        }, x)
        return beforeAfter
    }

    /**
     * the result of this should be that pri() is not finite (ex: NaN)
     * returns false if already deleted (allowing overriding subclasses to know if they shold also delete)
     */
    @kotlin.jvm.JvmDefault
    open fun delete(): Boolean {
        val p = pri()
        if (p == p) {
            this.pri<ScalarValue>(java.lang.Float.NaN)
            return true
        }
        return false
    }

    /** doesnt return any value so implementations may be slightly faster than priAdd(x)  */
    @kotlin.jvm.JvmDefault
    open fun priAdd(a: Float) {
        priUpdate(priAddUpdateFunction, a)
    }

    //    @kotlin.jvm.JvmDefault default float priAddAndGet(float a) {
    //        return pri(priAddUpdateFunction, a);
    //    }

    @kotlin.jvm.JvmDefault
    fun priSub(toSubtract: Float) {
        assert(toSubtract >= 0.toFloat()) { "trying to subtract negative priority: $toSubtract" }

        priAdd(-toSubtract)
    }

    /**
     * @return value after set
     */
    @kotlin.jvm.JvmDefault
    open fun priMult(_y: Float): Float {
        return pri(priMulUpdateFunction, _y)
    }

    //    /** y should be in domain (0...1) - for decaying result */
    //    @kotlin.jvm.JvmDefault default float priMult(float _y, float applyIfGreaterThan) {
    //        return pri((x,y)-> (x == x) ?
    //                ( x > applyIfGreaterThan ? Math.max(applyIfGreaterThan, (x * y)) : x)
    //                :
    //                Float.NaN,
    //        _y);
    //    }

    @kotlin.jvm.JvmDefault
    open fun priGetAndSetZero(): Float {
        val p = pri()
        pri<ScalarValue>(0.toFloat())
        return p
    }

    @kotlin.jvm.JvmDefault
    open fun priGetAndSet(next: Float): Float {
        val p = pri()
        pri<ScalarValue>(next)
        return p
    }

    @kotlin.jvm.JvmDefault
    open fun priGetAndDelete(): Float {
        val p = pri()
        delete()
        return p
    }

    companion object {

        /**
         * global minimum difference necessary to indicate a significant modification in budget float number components
         * TODO find if there is a better number
         *
         * 32-bit float has about 7 decimal digits of precision
         * https://en.wikipedia.org/wiki/Floating-point_arithmetic#Internal_representation
         */
        val EPSILON = 0.000001f

        /** > 2 * EPSILON  */
        val EPSILONcoarse = Math.sqrt(EPSILON.toDouble()).toFloat()//EPSILON*8;

        //    @Deprecated private static float priElseZero(ScalarValue x) {
        //        float p = x.pri();
        //        return (p==p) ? p : 0;
        //    }


        val priAddUpdateFunction: FloatFloatToFloatFunction = FloatFloatToFloatFunction { x, y ->
            if (x != x)
            //remains deleted by non-positive addend
            //undeleted by positive addend
                if (y <= 0.toFloat()) java.lang.Float.NaN else y
            else
                x + y
        }

        val priMulUpdateFunction: FloatFloatToFloatFunction = FloatFloatToFloatFunction { x, y -> if (x == x) x * y else java.lang.Float.NaN }
    }


}
