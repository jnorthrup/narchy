package jcog

import com.google.common.escape.Escapers
import org.HdrHistogram.AbstractHistogram
import org.HdrHistogram.AtomicHistogram
import org.HdrHistogram.DoubleHistogram
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.BiConsumer

/**
 * Utilities for process Text & String input/output, ex: encoding/escaping and decoding/unescaping Terms
 */
object Texts {

    internal val NaN2 = "NaN"
    internal val NaN4 = "NNaaNN"


    internal val quoteEscaper: Escapers.Builder = Escapers.builder().addEscape('\"', "\\\"")


    /**
     * @author http:
     */
    fun levenshteinDistance(a: CharSequence, b: CharSequence): Int {
        if (a == b) return 0

        val len0 = a.length + 1
        val len1 = b.length + 1
        var cost = IntArray(len0)
        for (i in 0 until len0) {
            cost[i] = i
        }
        var newcost = IntArray(len0)
        for (j in 1 until len1) {
            newcost[0] = j
            val bj = b[j - 1]
            for (i in 1 until len0) {
                val match = if (a[i - 1].toInt() == bj.toInt()) 0 else 1
                val cost_replace = cost[i - 1] + match
                val cost_insert = cost[i] + 1
                val cost_delete = newcost[i - 1] + 1

                var c = cost_insert
                if (cost_delete < c) c = cost_delete
                if (cost_replace < c) c = cost_replace

                newcost[i] = c
            }
            val swap = cost
            cost = newcost
            newcost = swap
        }
        return cost[len0 - 1]
    }

    fun n1(x: Float): String = if (x != x) "NaN" else DoubleFormatUtil.formatDoubleFast(x.toDouble(), 1)

    fun n3(x: Float): String = if (x != x) "NaN" else DoubleFormatUtil.formatDoubleFast(x.toDouble(), 3)


    fun n4(x: Float): String = if (x != x) NaN4 else DoubleFormatUtil.formatDoubleFast(x.toDouble(), 4)

    fun n4(x: Double): String = if (x != x) NaN4 else DoubleFormatUtil.formatDoubleFast(x, 4)

    fun hundredths(d: Float): Long = (d * 100.0f + 0.5f).toLong()

    fun tens(d: Float): Int = (d * 10.0f + 0.5f).toInt()

    fun n2(x: Float): String {
        if (x != x)
            return NaN2


        if (x < 0.toFloat() || x > 1.0f) {

            return DoubleFormatUtil.formatDoubleFast(x.toDouble(), 2)
        }

        val hundredths = hundredths(x).toInt()
        when (hundredths) {

            100 -> return "1.0"
            99 -> return ".99"
            90 -> return ".90"
            50 -> return ".50"
            0 -> return "0.0"
        }

        if (hundredths > 9) {
            val tens = hundredths / 10
            return String(charArrayOf('.', ('0' + tens).toChar(), ('0' + hundredths % 10).toChar()))
        } else {
            return String(charArrayOf('.', '0', ('0' + hundredths).toChar()))
        }
    }


    fun n2(p: Double): String = if (p != p) NaN2 else n2(p.toFloat())

    /**
     * character to a digit, or -1 if it wasnt a digit
     */
    fun i(c: Char): Int = if (c >= '0' && c  <= '9') c  - '0' else -1

    /**
     * fast parse an int under certain conditions, avoiding Integer.parse if possible
     */
    @Throws(NumberFormatException::class)
    fun i(s: String): Int {

        when (s.length) {
            0 -> throw UnsupportedOperationException()
            1 -> {
                val c = s[0]
                val i = i(c)
                if (i != -1) return i
            }
            2 -> {
                val dig1 = i(s[1])
                if (dig1 != -1) {
                    val dig10 = i(s[0])
                    if (dig10 != -1)
                        return dig10 * 10 + dig1
                }
            }
        }

        return Integer.parseInt(s)

    }

    /**
     * fast parse an int under certain conditions, avoiding Integer.parse if possible
     */
    @Throws(NumberFormatException::class)
    fun l(s: String): Long {
        val sl = s.length
        when (sl) {
            1 -> {
                val c = s[0]
                val i = i(c)
                if (i != -1) return i.toLong()
            }
            2 -> {
                val dig1 = i(s[1])
                if (dig1 != -1) {
                    val dig10 = i(s[0])
                    if (dig10 != -1)
                        return (dig10 * 10 + dig1).toLong()
                }
            }
        }
        return java.lang.Long.parseLong(s)
    }

    /**
     * fast parse a non-negative int under certain conditions, avoiding Integer.parse if possible
     * TODO parse negative values with a leading '-'
     */
    fun i(s: String, ifMissing: Int): Int {
        when (s.length) {
            0 -> return ifMissing
            1 -> return i1(s, ifMissing)
            2 -> return i2(s, ifMissing)
            3 -> return i3(s, ifMissing)
            else -> {


                for (i in 0 until s.length)
                    if (i(s[i]) == -1)
                        return ifMissing


                try {
                    return Integer.parseInt(s)
                } catch (e: NumberFormatException) {
                    return ifMissing

                }

            }
        }
    }

    fun i(b: ByteArray, radix: Int): String = i(b, 0, b.size, radix)

    fun i(b: ByteArray, from: Int, to: Int, radix: Int): String {
        assert(radix == 16)
        val c = ByteArray((to - from) * 2)
        var i = 0
        for (j in from until to) {
            val x = b[j]
            c[i++] = (x  / radix + '0'.toInt()).toByte()
            c[i++] = (x  % radix + '0'.toInt()).toByte()
        }
        return String(c)
    }

    fun i(s: String, offset: Int, ifMissing: Int): Int {
        val sl = s.length - offset
        if (sl <= 0)
            return ifMissing

        when (sl) {
            1 -> return i1(s, offset, ifMissing)
            2 -> return i2(s, offset, ifMissing)
            3 -> return i3(s, offset, ifMissing)
            else -> try {
                return Integer.parseInt(if (offset != 0) s.substring(offset) else s)
            } catch (e: NumberFormatException) {
                return ifMissing
            }

        }
    }

    private fun i3(s: String, ifMissing: Int): Int = i3(s, 0, ifMissing)

    private fun i3(s: String, offset: Int, ifMissing: Int): Int {
        val dig100 = i(s[offset])
        if (dig100 == -1) return ifMissing

        val dig10 = i(s[offset + 1])
        if (dig10 == -1) return ifMissing

        val dig1 = i(s[offset + 2])
        return if (dig1 == -1) ifMissing else dig100 * 100 + dig10 * 10 + dig1

    }

    private fun i2(s: String, ifMissing: Int): Int = i2(s, 0, ifMissing)

    private fun i2(s: String, offset: Int, ifMissing: Int): Int {
        val dig10 = i(s[offset])
        if (dig10 == -1) return ifMissing

        val dig1 = i(s[offset + 1])
        return if (dig1 == -1) ifMissing else dig10 * 10 + dig1

    }

    private fun i1(s: String, ifMissing: Int): Int = i1(s, 0, ifMissing)

    private fun i1(s: String, offset: Int, ifMissing: Int): Int {
        val dig1 = i(s[offset])
        return if (dig1 == -1) ifMissing else dig1
    }

    /**
     * fast parse for float, checking common conditions
     */
    @JvmOverloads
    fun f(s: String, ifMissing: Float = java.lang.Float.NaN): Float {

        when (s) {
            "0", "0.00" -> return 0.toFloat()
            "1", "1.00" -> return 1.0f
            "0.90", "0.9" -> return 0.9f
            "0.5" -> return 0.5f
            else -> try {
                return java.lang.Float.parseFloat(s)
            } catch (e: NumberFormatException) {
                return ifMissing
            }

        }

    }

    fun f(s: String, min: Float, max: Float): Float {
        val x = f(s, java.lang.Float.NaN)
        return if (x < min || x > max) java.lang.Float.NaN else x
    }

    fun arrayToString(vararg signals: Any): String {
        if (signals == null) return ""
        val slen = signals.size
        if (slen > 1)
            return Arrays.toString(signals)
        return if (slen > 0) signals[0].toString() else ""
    }

    fun n(x: Float, decimals: Int): CharSequence {
        when (decimals) {
            1 -> return n1(x)
            2 -> return n2(x)
            3 -> return n3(x)
            4 -> return n4(x)
            else -> return if (x == x) DoubleFormatUtil.formatDoubleFast(x.toDouble(), decimals) else "NaN"
        }


    }

    fun countRows(s: String, x: Char): Int {
        val bound = s.length
        var count = 0L
        for (i in 0 until bound) {
            if (s[i].toInt() == x.toInt()) {
                count++
            }
        }

        return count.toInt()
    }

    fun countCols(next: String): Int {
        var cols = 0
        var n = 0
        var nn = 0
        while ((next.indexOf('\n', n).also{nn = it}) != -1) {
            cols = Math.max(cols, nn - n)
            n = nn
        }
        return cols
    }

    fun n2(vararg v: Float): String {
        assert(v.size > 0)
        val sb = StringBuilder(v.size * 4 + 2 /* approx */)
        val s = v.size
        for (i in 0 until s) {
            sb.append(n2(v[i]))
            if (i != s - 1) sb.append(' ')
        }
        return sb.toString()
    }

    /**
     * prints an array of numbers separated by tab, suitable for a TSV line
     */
    fun n4(vararg v: Double): String {
        val sb = StringBuilder(v.size * 6 + 2 /* approx */)
        val s = v.size
        for (i in 0 until s) {
            sb.append(n4(v[i]))
            if (i != s - 1) sb.append('\t')
        }
        return sb.toString()
    }

    /**
     * prints an array of numbers separated by tab, suitable for a TSV line
     */
    fun n4(vararg v: Float): String {
        val sb = StringBuilder(v.size * 6 + 2 /* approx */)
        val s = v.size
        for (i in 0 until s) {
            sb.append(n4(v[i]))
            if (i != s - 1) sb.append('\t')
        }
        return sb.toString()
    }

    /**
     * prints an array of numbers separated by tab, suitable for a TSV line
     */
    fun n2(vararg v: Byte): String {
        val s = v.size
        val sb = StringBuilder()
        for (i in 0 until s) {
            val s1 = Integer.toHexString(java.lang.Byte.toUnsignedInt(v[i])) + ' '
            sb.append(s1)
        }
        return sb.toString()
    }

    /**
     * Return formatted Date String: yyyy.MM.dd HH:mm:ss
     * Based on Unix's time() input in seconds
     *
     * @param timestamp seconds since start of Unix-time
     * @return String formatted as - yyyy.MM.dd HH:mm:ss
     * from: https:
     */
    fun dateStr(timestamp: Long): String {
        val date = Date(timestamp * 1000L)
        val formatter = SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        return formatter.format(date)
    }

    /**
     * string repr of an amount of nanoseconds
     * from: https:
     */
    fun timeStr(ns: Double): String {
        assert(java.lang.Double.isFinite(ns))
        val neg = ns < 0.toDouble()
        return (if (neg) "-" else "") + _timeStr(Math.abs(ns))
    }


    private fun _timeStr(ns: Double): String {
        if (ns < 1000.0) return n4(ns) + "ns"
        if (ns < 1_000_000.0) return n4(ns / 1_000.0) + "us"
        if (ns < 1_000_000_000.0) return n4(ns / 1_000_000.0) + "ms"

        if (ns < 1_000_000_000_000.0) return n2(ns / 1_000_000_000.0) + 's'
        val sec = Math.round(ns / 1_000_000_000.0)
        if (sec < (5 * 60).toLong()) return (sec / 60L).toString() + "m" + sec % 60L + 's'.toString()
        val min = sec / 60L
        if (min < 60L) return min.toString() + "m"
        val hour = min / 60L
        if (min < (24 * 60).toLong()) return hour.toString() + "h" + min % 60L + 'm'.toString()
        val day = hour / 24L
        return day.toString() + "d" + day % 24L + 'h'.toString()
    }

    /**
     * from: https:
     */
    fun byteCountString(size: Long): String {
        if (size < 2L * (1L shl 10)) return size.toString() + "b"
        if (size < 2L * (1L shl 20)) return String.format("%dKb", size / (1L shl 10))
        return if (size < 2L * (1L shl 30)) String.format("%dMb", size / (1L shl 20)) else String.format("%dGb", size / (1L shl 30))
    }

    /**
     * from: https:
     */
    fun repeat(s: String, n: Int): String {

        if (s.length == 1) {
            val c = s[0]
            if (c.toInt() < 0xff) {
                val bb = ByteArray(n)
                Arrays.fill(bb, c.toByte())
                return String(bb)
            }
        }

        return s.repeat(Math.max(0, n))
    }

    /**
     * pad with leading zeros
     * TODO can be made faster
     */
    fun iPad(v: Long, digits: Int): String {
        var s = v.toString()
        while (s.length < digits)
            s = " $s"
        return s
    }

    fun n2percent(rate: Float): String = n2(100f * rate) + '%'

    fun histogramDecode(h: AbstractHistogram, header: String, linearStep: Int, x: BiConsumer<String, Any>) {
        val digits = (1.0 + Math.log10(h.maxValue.toDouble())).toInt()
        for (p in h.linearBucketValues(linearStep.toLong())) {
            x.accept(header + " [" +
                    iPad(p.valueIteratedFrom, digits) + ".." + iPad(p.valueIteratedTo, digits) + ']'.toString(),
                    p.countAddedInThisIterationStep)
        }
    }

    fun histogramDecode(h: DoubleHistogram, header: String, linearStep: Double, x: BiConsumer<String, Any>) {
        val order = charArrayOf('a')
        for (p in h.linearBucketValues(linearStep)) {
            x.accept(header + ' '.toString() + order[0]++ +
                    '['.toString() + n4(p.valueIteratedFrom) + ".." + n4(p.valueIteratedTo) + ']'.toString(),
                    p.countAddedInThisIterationStep)
        }
    }

    fun histogramDecode(h: AbstractHistogram, header: String, x: BiConsumer<String, Any>) {
        for (p in h.percentiles(1)) {
            x.accept(header + " [" +
                    p.valueIteratedFrom + ".." + p.valueIteratedTo + ']'.toString(),
                    p.countAddedInThisIterationStep)
        }
    }

    fun histogramString(h: AbstractHistogram, percentiles: Boolean): String {
        val sb = StringBuilder(256)
        histogramPrint(h, percentiles, sb)
        return sb.toString()
    }

    fun histogramPrint(h: AbstractHistogram, out: Appendable) {
        histogramPrint(h, true, out)
    }

    fun histogramPrint(h: AbstractHistogram, percentiles: Boolean, out: Appendable) {
        var h = h
        if (h is AtomicHistogram)
            h = h.copy()

        try {
            out.append("[n=")
                    .append(h.totalCount.toString())
                    .append(" avg=").append(n4(h.mean))
                    .append(", min=").append(n4(h.minValue.toFloat()))
                    .append(", max=").append(n4(h.maxValue.toFloat()))
                    .append(", stdev=").append(n4(h.stdDeviation))
                    .append(']')

            if (percentiles) {

                out.append('\n')

                histogramDecode(h, "", BiConsumer { label, value ->
                    try {
                        out.append(label).append(' ').append(value.toString()).append('\n')
                    } catch (e: IOException) {
                        throw WTF(e)
                    }
                })
            }
        } catch (e: IOException) {
            throw WTF(e)
        }

    }


    fun quote(s: String): String {
        var s = s
        val length = s.length

        if (length == 0)
            return "\"\""

        if (s[0]  == '\"' && s[length - 1] == '\"') {
            if (length == 1) {
                s = "\"\\\"\""
            } else {

            }
        } else {
            s = '"'.toString() + quoteEscaper.build().escape(s) + '"'.toString()
        }

        return s
    }


    fun unquote(x: String): String {
        var x = x
        while (true) {
            val len = x.length
            if (len > 0 && x[0]  == '\"' && x[len - 1]  == '\"') {
                x = x.substring(1, len - 1)
            } else {
                return x
            }
        }
    }

    /**
     * 2 decimal representation of values between 0 and 1. only the tens and hundredth
     * decimal point are displayed - not the ones, and not a decimal point.
     * for compact display.
     * if the value=1.0, then 'aa' is the result
     */
    fun n2u(x: Float): String {
        if (x < 0.toFloat() || x > 1.0f) throw RuntimeException("values >=0 and <=1")
        val hundreds = hundredths(x).toInt()
        if (x == 100.0f) return "aa"
        return if (hundreds < 10) "0$hundreds" else Integer.toString(hundreds)
    }

    /**
     * returns lev distance divided by max(a.length(), b.length()
     */
    fun levenshteinFraction(a: CharSequence, b: CharSequence): Float {
        val len = Math.max(a.length, b.length)
        return if (len == 0) 0f else levenshteinDistance(a, b).toFloat() / len.toFloat()
    }

    fun indent(amount: Int) {
        for (i in 0 until amount)
            print(' ')
    }


    /**
     * This class implements fast, thread-safe format of a double value
     * with a given number of decimal digits.
     *
     *
     * The contract for the format methods is this one:
     * if the source is greater than or equal to 1 (in absolute value),
     * use the decimals parameter to define the number of decimal digits; else,
     * use the precision parameter to define the number of decimal digits.
     *
     *
     * A few examples (consider decimals being 4 and precision being 8):
     *
     *  * 0.0 should be rendered as "0"
     *  * 0.1 should be rendered as "0.1"
     *  * 1234.1 should be rendered as "1234.1"
     *  * 1234.1234567 should be rendered as "1234.1235" (note the trailing 5! Rounding!)
     *  * 1234.00001 should be rendered as "1234"
     *  * 0.00001 should be rendered as "0.00001" (here you see the effect of the "precision" parameter)
     *  * 0.00000001 should be rendered as "0.00000001"
     *  * 0.000000001 should be rendered as "0"
     *
     *
     *
     * Originally authored by Julien Aym.
     * https:
     */
    internal enum class DoubleFormatUtil {
        ;

        companion object {

            /**
             * Most used power of ten (to avoid the cost of Math.pow(10, n)
             */
            private val POWERS_OF_TEN_LONG = LongArray(19)
            private val POWERS_OF_TEN_DOUBLE = DoubleArray(30)

            init {
                POWERS_OF_TEN_LONG[0] = 1L
                for (i in 1 until POWERS_OF_TEN_LONG.size) {
                    POWERS_OF_TEN_LONG[i] = POWERS_OF_TEN_LONG[i - 1] * 10L
                }
                for (i in POWERS_OF_TEN_DOUBLE.indices) {
                    POWERS_OF_TEN_DOUBLE[i] = java.lang.Double.parseDouble("1e$i")
                }
            }

            /**
             * Rounds
             * Rounds the given source value at the given precision
             * and writes the rounded value into the given target
             *
             * @param source    the source value to round
             * @param decimals  the decimals to round at (use if abs(source)  1.0)
             * @param precision the precision to round at (use if abs(source) &lt; 1.0)
             * @param target    the buffer to write to
             */
            fun formatDouble(source: Double, decimals: Int, precision: Int, target: StringBuilder) {
                val scale = if (Math.abs(source) >= 1.0) decimals else precision
                if (tooManyDigitsUsed(source, scale) || tooCloseToRound(source, scale)) {
                    formatDoublePrecise(source, decimals, precision, target)
                } else {
                    formatDoubleFast(source, decimals, precision, target)
                }
            }

            /**
             * Rounds the given source value at the given precision
             * and writes the rounded value into the given target
             *
             *
             * This method internally uses the String representation of the source value,
             * in order to avoid any double precision computation error.
             *
             * @param source    the source value to round
             * @param decimals  the decimals to round at (use if abs(source)  1.0)
             * @param precision the precision to round at (use if abs(source) &lt; 1.0)
             * @param target    the buffer to write to
             */
            fun formatDoublePrecise(source: Double, decimals: Int, precision: Int, target: StringBuilder) {
                var source = source
                if (isRoundedToZero(source, decimals, precision)) {

                    target.append('0')
                    return
                } else if (!java.lang.Double.isFinite(source)) {

                    target.append(source)
                    return
                }
                val negative = source < 0.0
                if (negative) {
                    source = -source

                    target.append('-')
                }
                val scale = if (source >= 1.0) decimals else precision


                val s = java.lang.Double.toString(source)
                if (source >= 1e-3 && source < 1e7) {

                    val dot = s.indexOf('.')
                    var decS = s.substring(dot + 1)
                    var decLength = decS.length
                    if (scale >= decLength) {
                        if ("0" == decS) {

                            target.append(s, 0, dot)
                        } else {
                            target.append(s)

                            var l = target.length - 1
                            while (l >= 0 && target[l]  == '0') {
                                target.setLength(l)
                                l--
                            }
                        }
                        return
                    } else if (scale + 1 < decLength) {

                        decLength = scale + 1
                        decS = decS.substring(0, decLength)
                    }
                    val intP = java.lang.Long.parseLong(s.substring(0, dot))
                    val decP = java.lang.Long.parseLong(decS)
                    format(target, scale, intP, decP)
                } else {

                    val dot = s.indexOf('.')
                    assert(dot >= 0)
                    val exp = s.indexOf('E')
                    assert(exp >= 0)
                    var exposant = Integer.parseInt(s.substring(exp + 1))
                    val intS = s.substring(0, dot)
                    val decS = s.substring(dot + 1, exp)
                    val decLength = decS.length
                    if (exposant >= 0) {
                        val digits = decLength - exposant
                        if (digits <= 0) {


                            target.append(intS)
                            target.append(decS)
                            target.append("0".repeat(-digits))
                        } else if (digits <= scale) {


                            target.append(intS).append(decS, 0, exposant).append('.').append(decS.substring(exposant))
                        } else {


                            val intP = java.lang.Long.parseLong(intS) * tenPow(exposant) + java.lang.Long.parseLong(decS.substring(0, exposant))
                            val decP = java.lang.Long.parseLong(decS.substring(exposant, exposant + scale + 1))
                            format(target, scale, intP, decP)
                        }
                    } else {

                        exposant = -exposant
                        val digits = scale - exposant + 1
                        if (digits < 0) {
                            target.append('0')
                        } else if (digits == 0) {
                            val decP = java.lang.Long.parseLong(intS)
                            format(target, scale, 0L, decP)
                        } else if (decLength < digits) {
                            val decP = java.lang.Long.parseLong(intS) * tenPow(decLength + 1) + java.lang.Long.parseLong(decS) * 10L
                            format(target, exposant + decLength, 0L, decP)
                        } else {
                            val subDecP = java.lang.Long.parseLong(decS.substring(0, digits))
                            val decP = java.lang.Long.parseLong(intS) * tenPow(digits) + subDecP
                            format(target, scale, 0L, decP)
                        }
                    }
                }
            }

            /**
             * Returns true if the given source value will be rounded to zero
             *
             * @param source    the source value to round
             * @param decimals  the decimals to round at (use if abs(source)  1.0)
             * @param precision the precision to round at (use if abs(source) &lt; 1.0)
             * @return true if the source value will be rounded to zero
             */
            private fun isRoundedToZero(source: Double, decimals: Int, precision: Int): Boolean =
                    source == 0.0 || Math.abs(source) < 4.999999999999999 / tenPowDouble(Math.max(decimals, precision) + 1)

            /**
             * Returns ten to the power of n
             *
             * @param n the nth power of ten to get
             * @return ten to the power of n
             */
            fun tenPow(n: Int): Long {
                assert(n >= 0)
                return if (n < POWERS_OF_TEN_LONG.size) POWERS_OF_TEN_LONG[n] else Math.pow(10.0, n.toDouble()).toLong()
            }

            private fun tenPowDouble(n: Int): Double {
                assert(n >= 0)
                return if (n < POWERS_OF_TEN_DOUBLE.size) POWERS_OF_TEN_DOUBLE[n] else Math.pow(10.0, n.toDouble())
            }

            /**
             * Helper method to do the custom rounding used within formatDoublePrecise
             *
             * @param target the buffer to write to
             * @param scale  the expected rounding scale
             * @param intP   the source integer part
             * @param decP   the source decimal part, truncated to scale + 1 digit
             */
            private fun format(target: StringBuilder, scale: Int, intP: Long, decP: Long) {
                var scale = scale
                var intP = intP
                var decP = decP
                if (decP != 0L) {


                    decP += 5L
                    decP /= 10L
                    if (decP.toDouble() >= tenPowDouble(scale)) {
                        intP++
                        decP -= tenPow(scale)
                    }
                    if (decP != 0L) {

                        while (decP % 10L == 0L) {
                            decP /= 10L
                            scale--
                        }
                    }
                }
                target.append(intP)
                if (decP != 0L) {
                    target.append('.')




                    while (scale > 0 && if (scale > 18) decP.toDouble() < tenPowDouble(--scale) else decP < tenPow(--scale)) {

                        target.append('0')
                    }
                    target.append(decP)
                }
            }

            fun formatDoubleFast(source: Double, decimals: Int): String {
                val target = StringBuilder(decimals * 2 /* estimate */)
                formatDoubleFast(source, decimals, target)
                return target.toString()
            }

            fun formatDoubleFast(source: Double, decimals: Int, target: StringBuilder) {
                formatDouble(source, decimals, decimals, target)
            }

            /**
             * Rounds the given source value at the given precision
             * and writes the rounded value into the given target
             *
             *
             * This method internally uses double precision computation and rounding,
             * so the result may not be accurate (see formatDouble method for conditions).
             *
             * @param source    the source value to round
             * @param decimals  the decimals to round at (use if abs(source)  1.0)
             * @param precision the precision to round at (use if abs(source) &lt; 1.0)
             * @param target    the buffer to write to
             */
            fun formatDoubleFast(source: Double, decimals: Int, precision: Int, target: StringBuilder) {
                var source = source
                if (isRoundedToZero(source, decimals, precision)) {

                    target.append('0')
                    return
                } else if (java.lang.Double.isNaN(source) || java.lang.Double.isInfinite(source)) {

                    target.append(source)
                    return
                }
                val isPositive = source >= 0.0
                source = Math.abs(source)
                var scale = if (source >= 1.0) decimals else precision
                var intPart = Math.floor(source).toLong()
                val tenScale = tenPowDouble(scale)
                val fracUnroundedPart = (source - intPart.toDouble()) * tenScale
                var fracPart = Math.round(fracUnroundedPart)
                if (fracPart.toDouble() >= tenScale) {
                    intPart++
                    fracPart = Math.round(fracPart.toDouble() - tenScale)
                }
                if (fracPart != 0L) {

                    while (fracPart % 10L == 0L) {
                        fracPart /= 10L
                        scale--
                    }
                }
                if (intPart != 0L || fracPart != 0L) {

                    if (!isPositive) {

                        target.append('-')
                    }

                    target.append(intPart)
                    if (fracPart != 0L) {

                        target.append('.')

                        while (scale > 0 && fracPart.toDouble() < tenPowDouble(--scale)) {
                            target.append('0')
                        }
                        target.append(fracPart)
                    }
                } else {
                    target.append('0')
                }
            }

            /**
             * Returns the exponent of the given value
             *
             * @param value the value to get the exponent from
             * @return the value's exponent
             */
            fun getExponant(value: Double): Int {


                var exp = java.lang.Double.doubleToRawLongBits(value) and 0x7ff0000000000000L
                exp = exp shr 52
                return (exp - 1023L).toInt()
            }

            /**
             * Returns true if the rounding is considered to use too many digits
             * of the double for a fast rounding
             *
             * @param source the source to round
             * @param scale  the scale to round at
             * @return true if the rounding will potentially use too many digits
             */
            private fun tooManyDigitsUsed(source: Double, scale: Int): Boolean {

                val decExp = Math.log10(source)
                return scale >= 308 || decExp + scale.toDouble() >= 14.5
            }

            /**
             * Returns true if the given source is considered to be too close
             * of a rounding value for the given scale.
             *
             * @param source the source to round
             * @param scale  the scale to round at
             * @return true if the source will be potentially rounded at the scale
             */
            private fun tooCloseToRound(source: Double, scale: Int): Boolean {
                var source = source
                source = Math.abs(source)
                val intPart = Math.floor(source).toLong()
                val fracPart = (source - intPart.toDouble()) * tenPowDouble(scale)
                val decExp = Math.log10(source)
                val range = if (decExp + scale.toDouble() >= 12.0) .1 else .001
                val distanceToRound1 = Math.abs(fracPart - Math.floor(fracPart))
                val distanceToRound2 = Math.abs(fracPart - Math.floor(fracPart) - 0.5)
                return distanceToRound1 <= range || distanceToRound2 <= range


            }
        }
    }
}

