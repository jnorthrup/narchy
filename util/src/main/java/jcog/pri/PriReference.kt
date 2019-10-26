package jcog.pri

import jcog.Util
import jcog.math.FloatSupplier

import java.util.function.BiConsumer
import java.util.function.Supplier

/**
 * prioritized reference
 */
interface PriReference<X> : Prioritizable, Supplier<X>, FloatSupplier {

    @kotlin.jvm.JvmDefault
    override fun asFloat(): Float {
        return pri()
    }

    companion object {

        fun histogram(pp: Iterable<Prioritized>, x: FloatArray): FloatArray {
            val bins = x.size
            val total = doubleArrayOf(0.toDouble())

            for (y in pp) {
                if (y == null)
                    continue
                var p = y.priElseZero()
                if (p > 1f) p = 1f
                val b = Util.bin(p, bins)
                x[b]++
                total[0]++
            }

            val t = total[0]
            if (t > 0.toDouble()) {
                for (i in 0 until bins)
                    x[i] = (x[i].toDouble() / t).toFloat()
            }
            return x
        }

        /**
         * double[histogramID][bin]
         */
        fun <X, Y> histogram(pp: Iterable<PriReference<Y>>, each: BiConsumer<PriReference<Y>, Array<DoubleArray>>, d: Array<DoubleArray>): Array<DoubleArray> {

            for (y in pp) {
                each.accept(y, d)
            }

            for (e in d) {
                var total = 0.0
                for (v in e) {
                    total += v
                }
                if (total > 0.toDouble()) {
                    var i = 0
                    val eLength = e.size
                    while (i < eLength) {
                        val f = e[i]
                        e[i] /= total
                        i++
                    }
                }
            }

            return d
        }
    }
}

