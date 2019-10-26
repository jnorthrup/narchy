package jcog.pri

import jcog.Texts
import jcog.Util
import jcog.pri.op.PriMerge
import jcog.pri.op.PriReturn
import org.eclipse.collections.api.block.function.primitive.FloatFunction
import java.util.function.Function

/**
 * Mutable Prioritized
 * implementations need only implement priSet() and pri()
 */
interface Prioritizable : Prioritized, ScalarValue {

    @kotlin.jvm.JvmDefault
    open fun pri(p: Prioritized): Prioritizable {
        if (this !== p)
            pri<ScalarValue>(p.pri())
        return this
    }

    /** set priority at-least this value  */
    @kotlin.jvm.JvmDefault
    fun priMax(p: Float) {
        priMax(p, PriReturn.Post)
    }

    @kotlin.jvm.JvmDefault
    fun priMin(p: Float) {
        priMin(p, PriReturn.Post)
    }

    @kotlin.jvm.JvmDefault
    fun priMax(p: Float, mode: PriReturn): Float {
        return PriMerge.max.merge(this, p, mode)
    }

    @kotlin.jvm.JvmDefault
    fun priMin(p: Float, mode: PriReturn): Float {
        return PriMerge.min.merge(this, p, mode)
    }

    @kotlin.jvm.JvmDefault
    fun take(source: Prioritizable, p: Float, amountOrFraction: Boolean, copyOrMove: Boolean): Float {

        if (p != p || p < ScalarValue.EPSILON)
            return 0.toFloat() //amount is insignificant

        assert(this !== source)

        val amount: Float
        if (amountOrFraction) {
            val s = source.pri()
            if (s != s || s < ScalarValue.EPSILON)
                return 0.toFloat() //source is depleted

            amount = Math.min(s, p)
        } else {
            assert(p <= 1f)
            amount = source.priElseZero() * p
            if (amount < ScalarValue.EPSILON)
                return 0.toFloat() //request*source is insignificant
        }

        val before = FloatArray(1)

        val after = pri({ x, a ->
            var x1 = x
            if (x1 != x1)
                x1 = 0.toFloat()
            before[0] = x1
            x1 + a
        }, amount)

        var b = before[0]
        if (b != b)
            b = 0.toFloat()

        val taken = after - b

        if (!copyOrMove) {
            //            float taken = source.priDelta((exist,subtracting)->{
            //
            //            }, amount);
            //TODO verify source actually had it.  this would involve somehow combining the 2 atomic ops
            source.priSub(taken)
        }

        return taken
    }


    @kotlin.jvm.JvmDefault
    open fun <P : Prioritizable> withPri(p: Float): P {
        pri<ScalarValue>(p)
        return this as P
    }


    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    @kotlin.jvm.JvmDefault
    fun toBudgetStringExternal(): Appendable {
        return toBudgetStringExternal(null)
    }

    @kotlin.jvm.JvmDefault
    fun toBudgetStringExternal(sb: StringBuilder?): StringBuilder {
        return Prioritized.toStringBuilder(sb, Texts.n2(pri()))
    }

    @kotlin.jvm.JvmDefault
    fun toBudgetString(): String {
        return toBudgetStringExternal().toString()
    }

    companion object {


        //    /**
        //     * normalizes the current value to within: min..(range+min), (range=max-min)
        //     */
        //    @kotlin.jvm.JvmDefault default void normalizePri(float min, float range, float lerp) {
        //        float p = priElseNeg1();
        //        if (p < 0) return;
        //
        //        priLerp((p - min) / range, lerp);
        //    }

        //    @kotlin.jvm.JvmDefault default Priority priLerp(float target, float speed) {
        //        float pri = pri();
        //        if (pri == pri)
        //            pri(lerp(speed, pri, target));
        //        return this;
        //    }

        fun fund(maxPri: Float, copyOrTransfer: Boolean, vararg src: Prioritizable): Float {
            return fund(maxPri, copyOrTransfer, Function { x -> x }, *src)
        }

        /**
         * X[] may contain nulls
         */
        @SafeVarargs
        fun <X> fund(maxPri: Float, copyOrTransfer: Boolean, getPri: Function<X, Prioritizable>, vararg src: X): Float {

            assert(src.size > 0)

            val priTarget = Math.min(maxPri.toDouble(), Util.sumDouble(FloatFunction { s -> if (s == null) 0.toFloat() else getPri.apply(s).priElseZero() }, *src))

            val u = UnitPri()

            if (priTarget > ScalarValue.EPSILON.toDouble()) {
                val perSrc = (priTarget / src.size.toDouble()).toFloat()
                //TODO random visit order if not copying (transferring)
                for (t in src) {
                    if (t != null) {
                        val v = u.take(getPri.apply(t), perSrc, true, copyOrTransfer)
                        if (Util.equals(v, 1f, ScalarValue.EPSILON))
                            break //done
                    }
                }
            }
            //assert (u.priElseZero() <= maxPri + ScalarValue.EPSILON): "not: " + u.priElseZero() + " <= " + maxPri + EPSILON;
            return u.pri()
        }
    }


}
