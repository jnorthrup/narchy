package nars


import com.google.common.base.Splitter
import jcog.TODO
import jcog.Texts
import jcog.Util
import jcog.data.list.FasterList
import jcog.util.ArrayUtil
import nars.Op.*
import nars.subterm.IntrinSubterms
import nars.subterm.Subterms
import nars.subterm.TermList
import nars.term.*
import nars.term.`var`.NormalizedVariable
import nars.term.`var`.UnnormalizedVariable
import nars.term.`var`.VarPattern
import nars.term.anon.Intrin
import nars.term.atom.Atom
import nars.term.atom.Atomic
import nars.term.atom.IdempotInt
import nars.term.atom.IdempotentBool
import nars.term.compound.LightCompound
import nars.term.control.LambdaPred
import nars.term.control.PREDICATE
import nars.term.obj.JsonTerm
import nars.term.util.SetSectDiff
import nars.term.util.TermException
import nars.truth.MutableTruth
import nars.truth.PreciseTruth
import nars.truth.func.TruthFunctions.c2wSafe
import org.apache.commons.math3.fraction.Fraction
import org.eclipse.collections.api.RichIterable
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction
import org.roaringbitmap.RoaringBitmap
import java.lang.Character.isDigit
import java.net.URI
import java.net.URL
import java.nio.file.Path
import java.util.*
import java.util.function.Function
import java.util.function.Predicate

/***
 * oooo       oo       .o.       ooooooooo.
 * `888b.      8'      .888.      `888   `Y88.
 * 88`88b.    88     .8"888.      888   .d88'  .ooooo oo oooo  oooo   .ooooo.  oooo d8b oooo    ooo
 * 88  `88b.  88    .8' `888.     888ooo88P'  d88' `888  `888  `888  d88' `88b `888""8P  `88.  .8'
 * 88    `88b.88   .88ooo8888.    888`88b.    888   888   888   888  888ooo888  888       `88..8'
 * 88      `8888  .8'     `888.   888  `88b.  888   888   888   888  888    .o  888        `888'
 * 8o        `88 o88o     o8888o o888o  o888o `V8bod888   `V88V"V8P' `Y8bod8P' d888b        .8'
 * 888.                                .o..P'
 * 8P'                                 `Y8P'
 * "
 *
 * NARquery
 * Core Utility Class
 */
object `$` {

    val emptyQuote = Atomic.the("\"\"") as Atom
    private val DIV = the("div")


    @Throws(Narsese.NarseseException::class)
    infix fun <T : Term> `$`(term: String): T = Narsese.term(term, true) as T

    /**
     * doesnt throw exception, but may throw RuntimeException
     */
    infix fun <T : Term> `$$`(term: String): T {
        try {
            return `$`(term)
        } catch (e: Narsese.NarseseException) {
            throw RuntimeException(e)
        }

    }

    /**
     * doesnt normalize, doesnt throw exception, but may throw RuntimeException
     */
    infix fun <T : Term> `$$$`(term: String): T {
        try {
            return Narsese.term(term, false) as T
        } catch (e: Narsese.NarseseException) {
            throw RuntimeException(e)
        }

    }

    infix fun quote(text: String): Atom = if (text.isEmpty()) emptyQuote else Atomic.the(Texts.quote(text)) as Atom

    infix fun quote(text: Any): Atom {
        val s = text.toString()
        return quote(s)
    }

    fun the(vararg id: String): Array<Term> = Util.map(Function { Atomic.the(it) }, arrayOfNulls(id.size), *id)


    infix fun the(c: Char): Atomic = if (isDigit(c)) IdempotInt.the(Character.digit(c, 10)) else Atomic.the(c.toString())

    /**
     * Op.INHERITANCE from 2 Terms: subj --> pred
     * returns a Term if the two inputs are equal to each other
     */
    fun <T : Term> inh(subj: Term, pred: Term): T = INH.the(subj, pred) as T

    fun <T : Term> inh(subj: Term, pred: String): T = inh(subj, the(pred))

    fun <T : Term> inh(subj: String, pred: Term): T = inh(the(subj), pred)

    @Throws(Narsese.NarseseException::class)
    fun <T : Term> inh(subj: String, pred: String): T = inh<Term>(`$`<Term>(subj), `$`<Term>(pred)) as T

    fun <T : Term> sim(subj: Term, pred: Term): T = SIM.the(subj, pred) as T

    fun func(opTerm: String, vararg arg: Term): Term = func(Atomic.the(opTerm), *arg)

    fun func(opTerm: String, arg: Subterms): Term = func(Atomic.the(opTerm), arg)

    @Throws(Narsese.NarseseException::class)
    fun func(opTerm: String, vararg arg: String): Term = func(Atomic.the(opTerm), *array(*arg))

    /**
     * function ((a,b)==>c) aka: c(a,b)
     */
    fun func(opTerm: Atomic, vararg arg: Term): Term = INH.the(PROD.the(*arg), opTerm)

    fun func(opTerm: Atomic, arg: Subterms): Term = INH.the(PROD.the(arg), opTerm)

    /**
     * use with caution
     */
    fun funcFast(opTerm: Atomic, vararg arg: Term): Term = inhFast(pFast(*arg), opTerm)

    fun funcFast(opTerm: String, vararg arg: Term): Term = funcFast(Atomic.the(opTerm), *arg)

    /**
     * use with caution
     */
    fun inhFast(subj: Term, pred: Term): Term =//return new LighterCompound(INH, subj, pred);
            LightCompound(INH, subj, pred)

    fun <T : Term> impl(a: Term, b: Term): T = IMPL.the(a, b) as T

    fun <T : Term> impl(a: Term, dt: Int, b: Term): T = IMPL.the(a, dt, b) as T

    infix fun p(t: Collection<Term>): Term = p(*t.toTypedArray())

    fun p(vararg t: Term): Term = PROD.the(*t)

    fun pOrOnly(vararg t: Term): Term = if (t.size == 1) t[0] else p(*t)

    /**
     * creates from a sublist of a list
     */

    fun p(l: List<Term>, from: Int, to: Int): Term = p(*l.subList(from, to).toTypedArray())

    fun p(x: String, y: Term): Term = p(the(x), y)

    fun p(x: Term, y: String): Term = p(x, the(y))

    fun p(vararg t: String): Term = if (t.size == 0) EmptyProduct else p(*the(*t))

    fun p(vararg t: Int): Term = if (t.size == 0) EmptyProduct else p(*ints(*t))

    /**
     * encodes a boolean bitvector as an Int target, or if larger than 31 bits, as an Atom string
     */
    fun p(vararg t: Boolean): Term {
        if (t.size == 0) return EmptyProduct

        if (t.size < 31) {
            var b = 0
            for (i in t.indices) {
                if (t[i]) {
                    val i1 = 1 shl i
                    b = b or i1
                }
            }
            return IdempotInt.the(b)
        } else {
            throw TODO()
        }
    }

    /**
     * warning: generic variable
     */
    fun v(/**/type: Op, name: String): Variable {
        //special case: interpret normalized variables
        when (name.length) {
            1 -> {
                val c0 = name[0]
                if (isDigit(c0))
                    return v(type, (c0.toInt() - '0'.toInt()).toByte())
            }
            2 -> {
                val d0 = name[0]
                if (isDigit(d0)) {
                    val d1 = name[1]
                    if (isDigit(d1))
                        return v(type, ((d0.toInt() - '0'.toInt()) * 10 + (d1.toInt() - '0'.toInt())).toByte())
                }
            }
        }
        return UnnormalizedVariable(type, type.ch + name)
    }

    infix fun varDep(i: Int): Variable = v(VAR_DEP, i.toByte())

    infix fun varDep(s: String): Variable = v(VAR_DEP, s)

    infix fun varIndep(i: Int): Variable = v(VAR_INDEP, i.toByte())

    infix fun varIndep(s: String): Variable = v(VAR_INDEP, s)

    infix fun varQuery(i: Int): Variable = v(VAR_QUERY, i.toByte())

    infix fun varQuery(s: String): Variable = v(VAR_QUERY, s)

    infix fun varPattern(i: Int): VarPattern = v(VAR_PATTERN, i.toByte()) as VarPattern

    /**
     * Try to make a new compound from two components. Called by the logic rules.
     *
     *
     * A -{- B becomes {A} --> B
     *
     * @param subj The first component
     * @param pred The second component
     * @return A compound generated or null
     */
    fun inst(subj: Term, pred: Term): Term = INH.the(SETe.the(subj), pred)

    fun <T : Term> instprop(subject: Term, predicate: Term): T = INH.the(SETe.the(subject), SETi.the(predicate)) as T

    fun <T : Term> prop(subject: Term, predicate: Term): T = INH.the(subject, SETi.the(predicate)) as T

    fun p(c: CharArray, f: CharToObjectFunction<Term>): Term {
        val list = ArrayList<Term>()
        for (i in c.indices) {
            val term = f.valueOf(c[i])
            list.add(term)
        }
        val x = list.toTypedArray()
        return p(*x)
    }

    fun <X> p(x: Array<X>, toTerm: Function<X, Term>): Term = p(*terms(x, toTerm))

    fun <X> terms(map: Array<X>, toTerm: Function<X, Term>): Array<Term> {
        val list = ArrayList<Term>()
        for (x in map) {
            val term = toTerm.apply(x)
            list.add(term)
        }
        return list.toTypedArray()
    }

    private infix fun array(t: Collection<Term>): Array<Term> = t.toTypedArray()

    @Throws(Narsese.NarseseException::class)
    private fun array(vararg s: String) = s.map { `$`(it) as Term }.toTypedArray()

    infix fun seti(t: Collection<Term>): Term = SETi.the(*array(t))

    infix fun sete(b: RoaringBitmap): Term = SETe.the(*ints(b))

    infix fun sete(b: RichIterable<Term>): Term = SETe.the(b.toSortedSet())

    infix fun p(b: RoaringBitmap): Term = PROD.the(*ints(b))

    infix fun ints(b: RoaringBitmap): Array<Term> {
        val size = b.cardinality
        val ii = b.intIterator
        val t = arrayOfNulls<Term>(size)
        var k = 0
        while (ii.hasNext())
            t[k++] = IdempotInt.the(ii.next())
        return t as Array<Term>
    }

    /**
     * unnormalized variable
     */
    fun v(ch: Char, name: String): Variable = v(NormalizedVariable.typeIndex(ch), name)

    /**
     * normalized variable
     */
    fun v(/**/type: Op, id: Byte): NormalizedVariable = NormalizedVariable.the(type, id)

    /**
     * parallel conjunction &| aka &&+0
     */
    fun parallel(vararg s: Term): Term = CONJ.the(0, *s)

    fun parallel(s: Collection<Term>): Term = CONJ.the(0, s)

    /**
     * alias for disjunction
     */
    fun or(vararg x: Term): Term = DISJ(*x)

    /**
     * alias for conjunction
     */
    fun and(vararg x: Term): Term = CONJ.the(*x)


    /**
     * create a literal atom from a class (it's name)
     */
    infix fun the(c: Class<*>): Atom = Atomic.the(c.name) as Atom

    /**
     * gets the atomic target of an integer, with specific radix (up to 36)
     */
    fun intRadix(i: Int, radix: Int): Atom = quote(Integer.toString(i, radix))

    fun the(v: Int): Atomic = IdempotInt.the(v)

    fun tt(f: Float, c: Float): MutableTruth = MutableTruth(f, c2wSafe(c).toDouble())

    fun t(f: Float, c: Float): PreciseTruth? = PreciseTruth.byConf(f, c.toDouble())

    fun t(f: Float, c: Double): PreciseTruth? = PreciseTruth.byConf(f, c)

    /**
     * negates each entry in the array
     */
    fun neg(array: Array<Term>): Array<Term> {
        Util.map(Function { it.neg() }, array, *array)
        return array
    }

    fun theAtomic(string: ByteArray): Atomic = Atomic.the(String(string))

    fun p(array: ByteArray): Term = p(*Util.bytesToInts(array))

    fun the(c: Byte): Atomic = theAtomic(byteArrayOf(c))

    fun ints(vararg i: Short): Array<Term> {
        val l = i.size
        val list = ArrayList<Atomic>()
        for (j in 0 until l) {
            val the = the(i[j].toInt())
            list.add(the)
        }
        return list.toTypedArray()
    }

    fun ints(vararg i: Int): Array<Term> {
        val l = i.size
        val list = ArrayList<Atomic>()
        for (i1 in i) {
            val the = the(i1)
            list.add(the)
        }
        return list.toTypedArray()
    }

    /**
     * use with caution
     */
    fun the(b: Boolean): Term = if (b) IdempotentBool.True else IdempotentBool.False

    fun the(x: Float): Term {
        if (x != x)
            throw TODO("NaN")

        val rx = Util.round(x, 1.0f).toInt()
        return if (Util.equals(rx.toFloat(), x)) IdempotInt.the(rx) else the(Fraction(x.toDouble()))
    }

    fun the(x: Double): Term {
        if (x != x)
            throw TODO("NaN")
        val rx = Util.round(x, 1.0).toInt()
        return if (Util.equals(rx.toDouble(), x, java.lang.Double.MIN_NORMAL)) IdempotInt.the(rx) else the(Fraction(x))
    }

    infix fun doubleValue(x: Term): Double = when {
        x.op() === INT -> (x as IdempotInt).i.toDouble()
        else -> if (x.op() === ATOM) {
            unquote(  x) .let {it.toDouble()    }

        } else {
            throw TODO()
        }
    }

    fun the(o: Fraction): Term = func(DIV, the(o.numerator), the(o.denominator))

    infix fun the(x: Any): Term {
        if (x is Term)
            return x

        if (x is Number)
            return the(x)

        if (x is String)
            return the(x)

        throw UnsupportedOperationException("$x termize fail")
    }

    infix fun the(x: String): Atomic = Atomic.the(x)

    fun the(file: Path): Term = the(file.toUri())

    fun file(x: Term): Path {
        throw TODO()
    }

    infix fun the(n: Number): Term {
        val result: Term
        if (n is Int) {
            result = IdempotInt.the(n)
        } else if (n is Long) {
            result = Atomic.the(java.lang.Long.toString(n))
        } else if (n is Short) {
            result = IdempotInt.the(n.toInt())
        } else if (n is Byte) {
            result = IdempotInt.the(n.toInt())
        } else if (n is Float) {
            val d = n.toFloat()
            val id = d.toInt()
            result = if (d == d && Util.equals(d, id.toFloat())) IdempotInt.the(id) else Atomic.the(n.toString())

        } else {
            val d = n.toDouble()
            val id = d.toInt()
            result = if (d == d && Util.equals(d, id.toDouble())) IdempotInt.the(id) else Atomic.the(n.toString())

        }
        return result
    }

    fun <X> newArrayList(): List<X> = FasterList(0)

    fun <X> newArrayList(capacity: Int): List<X> = FasterList(capacity)

    fun pRadix(x: Int, radix: Int, maxX: Int): Term = p(*radixArray(x, radix, maxX))


    fun radix(x: Int, radix: Int, maxValue: Int): IntArray {
        var x = x
        assert(x >= 0)
        x %= maxValue //auto-wraparound

        val decimals = Math.ceil(Math.log(maxValue.toDouble()) / Math.log(radix.toDouble())).toInt()
        val y = IntArray(decimals)
        var X = -x
        var yi = 0
        do {
            y[yi++] = -(X % radix)
            X /= radix
        } while (X <= -1)
        return y
    }

    /**
     * most significant digit first, least last. padded with zeros
     *
     */
    fun radixArray(x: Int, radix: Int, maxX: Int): Array<Term> {

        val xx = radix(x, radix, maxX)

        //$.the(BinTxt.symbols[xx[i]]);
        val list = ArrayList<IdempotInt>()
        for (i in xx) {
            val the = IdempotInt.the(i)
            list.add(the)
        }
        return list.toTypedArray()
    }


    fun pRecurse(innerStart: Boolean, vararg t: Term): Term {
        var j = t.size - 1
        var n = if (innerStart) 0 else j - 1
        val inner = t[n]
        var nextInner = if (inner.op() !== PROD) p(inner) else inner
        while (--j > 0) {
            n += if (innerStart) +1 else -1
            val next = t[n]


            val nextArray = ArrayUtil.add(next.subterms().arrayShared(), nextInner)

            nextInner = if (next.op() !== PROD)
                if (innerStart) p(nextInner, next) else p(next, nextInner)
            else
                p(*nextArray)

        }
        return nextInner
    }

    fun inhRecurse(vararg t: Term): Compound? {
        var tl = t.size
        val bottom = t[--tl]
        var nextInner: Compound? = inh(t[--tl], bottom)
        while (nextInner != null && tl > 0) {
            nextInner = inh(t[--tl], nextInner)
        }
        return nextInner
    }


    infix fun unquote(s: Term): String = Texts.unquote(s.toString())


    //    /**
    //     * instantiate new Javascript context
    //     */
    //    public static NashornScriptEngine JS() {
    //        return (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
    //    }

    fun <X> IF(t: Term, test: Predicate<X>): PREDICATE<X> = LambdaPred(t, test)

    fun <X> AND(a: PREDICATE<X>, b: PREDICATE<X>): PREDICATE<X> =
            LambdaPred(CONJ.the(a, b), Predicate { x -> a.test(x) && b.test(x) })

    fun <X> OR(a: PREDICATE<X>, b: PREDICATE<X>): PREDICATE<X> =
            LambdaPred(DISJ(a, b), Predicate { x -> a.test(x) || b.test(x) })

    @Throws(NumberFormatException::class)
    infix fun intValue(intTerm: Term): Int {
        if (intTerm is IdempotInt)
            return intTerm.i

        throw NumberFormatException()

    }

    fun intValue(intTerm: Term, ifNotInt: Int): Int =
            if (intTerm is IdempotInt && intTerm.op() === INT) intTerm.i else ifNotInt

    infix fun fromJSON(j: String): Term = JsonTerm.the(j)

    infix fun pFast(x: Subterms): Compound = if (x.subs() == 0) EmptyProduct else LightCompound(PROD, x)

    fun pFast(vararg x: Term): Compound = if (x.size == 0) EmptyProduct else LightCompound(PROD, *x)
    //new LighterCompound(PROD, x);

    infix fun sFast(x: Subterms): Compound {
        if (x.subs() == 0) throw UnsupportedOperationException()
        return LightCompound(SETe, x)
    }

    infix fun sFast(x: Array<Term>): Term = sFast(true, x)

    infix fun sFast(x: SortedSet<Term>): Compound = sFast(false, x.toTypedArray())

    infix fun sFast(x: Collection<Term>): Compound = sFast(false, Terms.commute(x))

    fun sFast(sort: Boolean, x: Array<Term>): Compound {
        var x = x
        if (x.size == 0) throw TermException("empty set")
        if (sort && x.size > 1)
            x = Terms.commute(*x)
        return LightCompound(SETe, *x)
        //new LighterCompound(Op.SETe, x);
    }

    infix fun sFast(b: RoaringBitmap): Term = sFast(true, ints(b))

    infix fun pFast(x: Collection<Term>): Term = pFast(*x.toTypedArray())

    infix fun vFast(t: Collection<Term>): Subterms = vFast(*t.toTypedArray())

    /**
     * on-stack/on-heap cheaply constructed Subterms
     */
    fun vFast(vararg t: Term): Subterms {
        when (t.size) {
            0 -> return EmptySubterms
            1 -> {
                val a = t[0]
                if (Intrin.intrin(a.unneg()))
                    return IntrinSubterms(a)
            }
            2 -> {
                val a = t[0]
                val b = t[1]
                if (Intrin.intrin(a.unneg()) && Intrin.intrin(b.unneg()))
                    return IntrinSubterms(a, b)
            }
            3 -> {
                val a = t[0]
                val b = t[1]
                val c = t[2]
                var result = true
                for (term in Arrays.asList(a, b, c)) {
                    if (!Intrin.intrin(term.unneg())) {
                        result = false
                        break
                    }
                }
                if (result)
                    return IntrinSubterms(a, b, c)
            }
        }
        //                return new UnitSubterm(tt);
        //            default:
        //                if (t.length < 3)
        //                    return new ArrayTermVector(t);
        //                else
        return TermList(*t)
        //}


    }


    infix fun `$`(s: Array<String>): Array<Term> = s.map { `$`(it) as Term }.toTypedArray()

    fun func(f: Atomic, args: List<Term>): Term = func(f, *args.toTypedArray())

    fun funcImg(f: String, vararg x: Term): Term = funcImg(the(f), *x)

    fun funcImg(f: Atomic, vararg x: Term): Term {
        //        if (x.length > 1) {
        val xx = ArrayUtil.insert(0, x, f)
        xx[x.size] = ImgExt
        return INH.the(x[x.size - 1], PROD.the(*xx))
        //        } else {
        //            return $.func(f, x);
        //        }
    }


    fun diff(a: Term, b: Term): Term {
        //throw new TODO("use setAt/sect methods");
        val aop = a.op()
        if (aop === b.op()) {
            when (aop) {
                SETi -> return SetSectDiff.differenceSet(SETi, a, b)
                SETe -> return SetSectDiff.differenceSet(SETe, a, b)
            }
        }
        //throw new UnsupportedOperationException();
        return CONJ.the(a, b.neg())
    }

    infix fun identity(x: Any): Term {

        if (x is Term)
            return x
        else if (x is Termed) {
            val u = x.term()
            if (u != null)
                return u
            //else: probably still in the constructor before x.term() has been set, continue:
        }

        if (x is String)
            return Atomic.the(x)
        else {
            val c = x.javaClass
            val idHash = intRadix(System.identityHashCode(x), 36)
            return if (!c.isSynthetic) p(quote(c.name), idHash) else p(quote(c.simpleName), idHash)
        }

    }

    @JvmOverloads
    fun uuid(prefix: String? = null): Atom {
        val u = Util.uuid64()
        return quote(if (prefix != null) prefix + u else u)
    }

    fun the(u: URL): Term {

        if (u.query != null)
            throw TODO()

        val schemeStr = u.protocol
        val authorityStr = u.authority
        val pathStr = u.path

        return URI(schemeStr, authorityStr, pathStr)
    }

    fun the(u: URI): Term {

        if (u.fragment != null || u.query != null)
            throw TODO()

        val schemeStr = u.scheme
        val authorityStr = u.authority
        val pathStr = u.path

        return URI(schemeStr, authorityStr, pathStr)
    }

    /** https://en.wikipedia.org/wiki/Uniform_Resource_Identifier
     *
     * the URI scheme becomes the inheritance subject of the operation. so path components are the pred as a product. query can be the final component wrapped in a set to distinguish it, and init can be a json-like set of key/value pairs. the authority username/password can be special fields in that set of pairs.
     *
     * TODO authority, query
     */
    fun URI(schemeStr: String, authority: String?, pathStr: String): Term {
        /*
        URI = scheme:[//authority]path[?query][#fragment]
        authority = [userinfo@]host[:port]

                  userinfo     host        port
          ┌─┴─┬──┴────┬┴┐
          https://john.doe@www.example.com:123/forum/questions/?tag=networking&order=newest#top
         └┬┘└──────────────┴────────┴───────┬────-┴┬┘
         scheme           authority                 path                   query          fragment

          ldap://[2001:db8::7]/c=GB?objectClass?one
          └─┬┘ └───────┬─────┘└─┬─┘ └──────┬──────┘
         scheme    authority  path       query

          mailto:John.Doe@example.com
          └──┬─┘ └─────────┬────────┘
          scheme         path

          news:comp.infosystems.www.servers.unix
          └─┬┘ └───────────────┬───────────────┘
         scheme              path

          tel:+1-816-555-1212
          └┬┘ └──────┬──────┘
        scheme     path

          telnet://192.0.2.16:80/
          └──┬─┘ └──────┬──────┘│
          scheme    authority  path

          urn:oasis:names:specification:docbook:dtd:xml:4.1.2
          └┬┘ └──────────────────────┬──────────────────────┘
        scheme                     path

        */

        val scheme = the(schemeStr) as Atom //TODO cache these commonly used

        //TODO use more reliable path parser
        val pathComponents = Splitter.on('/').omitEmptyStrings().splitToList(pathStr)

        val path = p(*pathComponents.toTypedArray())
        return if (authority == null || authority.isEmpty())
            inh(path, scheme)
        else
            inh(PROD.the(INH.the(path, /*TODO parse*/the(authority))), scheme)
    }
}

fun Any.identity(): Term = `$` identity this
fun Any.quote(): Atom = `$` quote this
fun Any.the(): Term = `$` the this
fun Array<String>.`$`(): Array<Term> = `$` `$` this
fun Array<Term>.sFast(): Term = `$`.sFast(this)
fun Char.the(): Atomic = `$`.the(this)
fun Collection<Term>.pFast(): Term = `$` pFast this
fun Collection<Term>.sFast(): Compound = `$` sFast this
fun Collection<Term>.vFast(): Subterms = `$` vFast this
fun Int.varDep(): Variable = `$` varDep this
fun Int.varIndep(): Variable = `$` varIndep this
fun Int.varPattern(): VarPattern = `$` varPattern this
fun Int.varQuery(): Variable = `$` varQuery(this)
fun Number.the(): Term = `$` the this
fun RoaringBitmap.sFast(): Term = `$` sFast this
fun SortedSet<Term>.sFast(): Compound = `$` sFast this
fun String.fromJSON(): Term = `$` fromJSON this
fun String.quote(): Atom = `$` quote this
fun String.the(): Term = `$` the this
fun String.varDep(): Variable = `$` varDep(this)
fun String.varIndep(): Variable = `$` varIndep(this)
fun String.varQuery(): Variable = `$` varQuery(this)
fun Subterms.pFast(): Compound = `$` pFast this
fun Subterms.sFast(): Compound = `$`  sFast(this)
fun URI.the(): Term = `$`.the(this)
fun URL.the(): Term = `$`.the(this)