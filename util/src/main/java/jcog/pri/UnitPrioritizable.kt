package jcog.pri

import jcog.util.FloatFloatToFloatFunction

/** marker interface for implementations which limit values to the range of 0..1.0 (and deleted, NaN)  */
interface UnitPrioritizable : Prioritizable {

    /**
     * assumes 1 max value (Plink not NLink)
     */
    @kotlin.jvm.JvmDefault
    fun priAddOverflow(inc: Float /* float upperLimit=1 */): Float {

        if (inc <= jcog.pri.ScalarValue.EPSILON)
            return 0.toFloat()

        val beforeAfter = priDelta(FloatFloatToFloatFunction { x, y -> (if (x != x) 0.toFloat() else x) + y }, inc)

        val after = beforeAfter[1]
        val before = beforeAfter[0]
        val delta = if (before != before) after else after - before
        return Math.max(0.toFloat(), inc - delta) //should be >= 0
    }
}
