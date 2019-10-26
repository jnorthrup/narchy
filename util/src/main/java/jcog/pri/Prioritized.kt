package jcog.pri


import jcog.Skill
import jcog.Texts
import org.fusesource.jansi.Ansi

/**
 * something which has a priority floating point value
 * reports a priority scalar value (32-bit float precision)
 * NaN means it is 'deleted' which is a valid and testable state
 */
@FunctionalInterface
@Skill("Demand", "Microeconomics", "Macroeconomics")
interface Prioritized : Deleteable {

    @kotlin.jvm.JvmDefault
    val budgetString: String get() = Prioritized.toString(this)

    /**
     * returns the local (cached) priority value
     * if the value is NaN, then it means this has been deleted
     */
    fun pri(): Float


    @kotlin.jvm.JvmDefault
    open fun priElse(valueIfDeleted: Float): Float {
        val p = pri()
        return if (p == p) p else valueIfDeleted
    }

    @kotlin.jvm.JvmDefault
    fun priElseZero(): Float {
        return priElse(0.toFloat())
    }

    @kotlin.jvm.JvmDefault
    fun priElseNeg1(): Float {
        return priElse(-1.0f)
    }

    /** deleted if pri()==NaN  */
    @kotlin.jvm.JvmDefault
    override val isDeleted : Boolean get(){
        val p = pri()
        return p != p
    }

    companion object {

        /**
         * common instance for a 'Deleted budget'.
         */
        val Deleted: Prioritized = PriRO(java.lang.Float.NaN)
        /**
         * common instance for a 'full budget'.
         */
        val One: Prioritized = PriRO(1f)
        /**
         * common instance for a 'half budget'.
         */
        val Half: Prioritized = PriRO(0.5f)
        /**
         * common instance for a 'zero budget'.
         */
        val Zero: Prioritized = PriRO(0.toFloat())


        fun toString(b: Prioritized): String {
            return toStringBuilder(null, Texts.n4(b.pri())).toString()
        }


        fun toStringBuilder(sb: StringBuilder?, priorityString: String): StringBuilder {
            var sb = sb
            val c = 1 + priorityString.length
            if (sb == null)
                sb = StringBuilder(c)
            else {
                sb.ensureCapacity(c)
            }

            return sb.append('$').append(priorityString)
        }


        fun budgetSummaryColor(tv: Prioritized): Ansi.Color {
            val s = Math.floor((tv.priElseZero() * 5.0f).toDouble()).toInt()
            when (s) {
                1 -> return Ansi.Color.MAGENTA
                2 -> return Ansi.Color.GREEN
                3 -> return Ansi.Color.YELLOW
                4 -> return Ansi.Color.RED
                else -> return Ansi.Color.DEFAULT
            }
        }
    }


    //    static float sum(Prioritized... src) {
    //        return Util.sum(Prioritized::priElseZero, src);
    //    }
    //    static float max(Prioritized... src) {
    //        return Util.max(Prioritized::priElseZero, src);
    //    }
    //
    //    static <X extends Prioritizable> void normalize(X[] xx, float target) {
    //        int l = xx.length;
    //        assert (target == target);
    //        assert (l > 0);
    //
    //        float ss = sum(xx);
    //        if (ss <= ScalarValue.EPSILON)
    //            return;
    //
    //        float factor = target / ss;
    //
    //        for (X x : xx)
    //            x.priMult(factor);
    //
    //    }
}
