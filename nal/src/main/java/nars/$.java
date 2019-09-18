package nars;


import com.google.common.base.Splitter;
import jcog.TODO;
import jcog.Texts;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtil;
import nars.subterm.IntrinSubterms;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Variable;
import nars.term.*;
import nars.term.anon.Intrin;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.compound.LightCompound;
import nars.term.control.LambdaPred;
import nars.term.control.PREDICATE;
import nars.term.obj.JsonTerm;
import nars.term.util.SetSectDiff;
import nars.term.var.NormalizedVariable;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.VarPattern;
import nars.truth.MutableTruth;
import nars.truth.PreciseTruth;
import org.apache.commons.math3.fraction.Fraction;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Character.isDigit;
import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.truth.func.TruthFunctions.c2wSafe;

/***
 *     oooo       oo       .o.       ooooooooo.
 *    `888b.      8'      .888.      `888   `Y88.
 *     88`88b.    88     .8"888.      888   .d88'  .ooooo oo oooo  oooo   .ooooo.  oooo d8b oooo    ooo
 *     88  `88b.  88    .8' `888.     888ooo88P'  d88' `888  `888  `888  d88' `88b `888""8P  `88.  .8'
 *     88    `88b.88   .88ooo8888.    888`88b.    888   888   888   888  888ooo888  888       `88..8'
 *     88      `8888  .8'     `888.   888  `88b.  888   888   888   888  888    .o  888        `888'
 *     8o        `88 o88o     o8888o o888o  o888o `V8bod888   `V88V"V8P' `Y8bod8P' d888b        .8'
 *                                                      888.                                .o..P'
 *                                                      8P'                                 `Y8P'
 *                                                      "
 *
 *                                              NARquery
 *                                          Core Utility Class
 */
public enum $ { ;

    static final Atom emptyQuote = (Atom) Atomic.the("\"\"");
    private static final Atomic DIV = $.the("div");


    public static <T extends Term> T $(String term) throws Narsese.NarseseException {
        return (T) Narsese.term(term, true);
    }

    /**
     * doesnt throw exception, but may throw RuntimeException
     */
    public static <T extends Term> T $$(String term) {
        try {
            return $(term);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * doesnt normalize, doesnt throw exception, but may throw RuntimeException
     */
    public static <T extends Term> T $$$(String term) {
        try {
            return (T) Narsese.term(term, false);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }

    public static Atom quote(Object text) {
        String s = text.toString();

        return s.isEmpty() ? emptyQuote : (Atom) Atomic.the(Texts.quote(s));
    }

    public static Term[] the(String... id) {
        return Util.map(Atomic::the, new Term[id.length], id);
    }




    public static Atomic the(char c) {
        if (Character.isDigit(c))
            return Int.the(Character.digit(c, 10));

        return Atomic.the(String.valueOf(c));
    }

    /**
     * Op.INHERITANCE from 2 Terms: subj --> pred
     * returns a Term if the two inputs are equal to each other
     */
    public static <T extends Term> T inh(Term subj, Term pred) {
        return (T) INH.the(subj, pred);
    }

    public static <T extends Term> T inh(Term subj, String pred) {
        return $.inh(subj, $.the(pred));
    }

    public static <T extends Term> T inh(String subj, Term pred) {
        return $.inh($.the(subj), pred);
    }

    public static <T extends Term> T inh(String subj, String pred) throws Narsese.NarseseException {
        return (T) inh($(subj), $(pred));
    }

    public static <T extends Term> T sim(Term subj, Term pred) {
        return (T) SIM.the(subj, pred);
    }

    public static Term func(String opTerm, Term... arg) {
        return func(Atomic.the(opTerm), arg);
    }

    public static Term func(String opTerm, Subterms arg) {
        return func(Atomic.the(opTerm), arg);
    }

    public static Term func(String opTerm, String... arg) throws Narsese.NarseseException {
        return func(Atomic.the(opTerm), $.array(arg));
    }

    /**
     * function ((a,b)==>c) aka: c(a,b)
     */
    public static Term func(Atomic opTerm, Term... arg) {
        Term a = PROD.the(arg);
        return a == Null ? Null : INH.the(a, opTerm);
    }

    public static Term func(Atomic opTerm, Subterms arg) {
        Term a = PROD.the(arg);
        return a == Null ? Null : INH.the(a, opTerm);
    }

    /**
     * use with caution
     */
    public static Term funcFast(Atomic opTerm, Term... arg) {
        return $.inhFast($.pFast(arg), opTerm);
    }

    public static Term funcFast(String opTerm, Term... arg) {
        return $.funcFast(Atomic.the(opTerm), arg);
    }

    /**
     * use with caution
     */
    public static Term inhFast(Term subj, Term pred) {
        //return new LighterCompound(INH, subj, pred);
        return new LightCompound(INH, subj, pred);
    }

    public static <T extends Term> T impl(Term a, Term b) {
        return (T) IMPL.the(a, b);
    }

    public static <T extends Term> T impl(Term a, int dt, Term b) {
        return (T) IMPL.the(a, dt, b);
    }

    public static Term p(Collection<Term> t) {
        return $.p(t.toArray(Op.EmptyTermArray));
    }

    public static Term p(Term... t) {
        return PROD.the(t);
    }
    public static Term pOrOnly(Term... t) {
        if (t.length == 1)
            return t[0];
        else
            return p(t);
    }

    /**
     * creates from a sublist of a list
     */

    public static Term p(List<Term> l, int from, int to) {
        if (from == to)
            return Op.EmptyProduct;

        Term[] x = new Term[to - from];

        for (int j = 0, i = from; i < to; i++)
            x[j++] = l.get(i);

        return $.p(x);
    }

    public static Term p(String x, Term y) {
        return p($.the(x), y);
    }

    public static Term p(String... t) {
        return t.length == 0 ? Op.EmptyProduct : $.p($.the(t));
    }

    public static Term p(int... t) {
        return t.length == 0 ? Op.EmptyProduct : $.p($.the(t));
    }

    /**
     * encodes a boolean bitvector as an Int target, or if larger than 31 bits, as an Atom string
     */
    public static Term p(boolean... t) {
        if (t.length == 0) return Op.EmptyProduct;

        if (t.length < 31) {
            int b = 0;
            for (int i = 0; i < t.length; i++) {
                if (t[i])
                    b |= 1 << i;
            }
            return Int.the(b);
        } else {
            throw new TODO();
        }
    }

    /**
     * warning: generic variable
     */
    public static Variable v(/**/ Op type, String name) {
        //special case: interpret normalized variables
        switch (name.length()) {
            case 1:
                char c0 = name.charAt(0);
                if (isDigit(c0))
                    return $.v(type, (byte) (c0 - '0'));
                break;
            case 2:
                char d0 = name.charAt(0);
                if (isDigit(d0)) {
                    char d1 = name.charAt(1);
                    if (isDigit(d1))
                        return $.v(type, (byte) ((d0 - '0') * 10 + (d1 - '0')));
                }
                break;
        }
        return new UnnormalizedVariable(type, type.ch + name);
    }

    public static Variable varDep(int i) {
        return v(VAR_DEP, (byte) i);
    }

    public static Variable varDep(String s) {
        return v(VAR_DEP, s);
    }

    public static Variable varIndep(int i) {
        return v(VAR_INDEP, (byte) i);
    }

    public static Variable varIndep(String s) {
        return v(VAR_INDEP, s);
    }

    public static Variable varQuery(int i) {
        return v(VAR_QUERY, (byte) i);
    }

    public static Variable varQuery(String s) {
        return v(VAR_QUERY, s);
    }

    public static VarPattern varPattern(int i) {
        return (VarPattern) v(VAR_PATTERN, (byte) i);
    }

    /**
     * Try to make a new compound from two components. Called by the logic rules.
     * <p>
     * A -{- B becomes {A} --> B
     *
     * @param subj The first component
     * @param pred The second component
     * @return A compound generated or null
     */
    public static Term inst(Term subj, Term pred) {
        return INH.the(SETe.the(subj), pred);
    }

    public static <T extends Term> T instprop(Term subject, Term predicate) {
        return (T) INH.the(SETe.the(subject), SETi.the(predicate));
    }

    public static <T extends Term> T prop(Term subject, Term predicate) {
        return (T) INH.the(subject, SETi.the(predicate));
    }

    public static Term p(char[] c, CharToObjectFunction<Term> f) {
        Term[] x = new Term[c.length];
        for (int i = 0; i < c.length; i++) {
            x[i] = f.valueOf(c[i]);
        }
        return $.p(x);
    }

    public static <X> Term p(X[] x, Function<X, Term> toTerm) {
        return $.p(terms(x, toTerm));
    }

    public static <X> Term[] terms(X[] map, Function<X, Term> toTerm) {
        return Stream.of(map).map(toTerm).toArray(Term[]::new);
    }

    private static Term[] array(Collection<? extends Term> t) {
        return t.toArray(Op.EmptyTermArray);
    }

    private static Term[] array(String... s) throws Narsese.NarseseException {
        int l = s.length;
        Term[] tt = new Term[l];
        for (int i = 0; i < l; i++)
            tt[i] = $.$(s[i]);

        return tt;
    }

    public static Term seti(Collection<Term> t) {
        return SETi.the(array(t));
    }

    public static Term sete(RoaringBitmap b) {
        return SETe.the(ints(b));
    }

    public static Term sete(RichIterable<Term> b) {
        return SETe.the(b.toSortedSet());
    }

    public static Term p(RoaringBitmap b) {
        return PROD.the(ints(b));
    }

    public static Term[] ints(RoaringBitmap b) {
        int size = b.getCardinality();
        PeekableIntIterator ii = b.getIntIterator();
        Term[] t = new Term[size];
        int k = 0;
        while (ii.hasNext()) {
            t[k++] = Int.the(ii.next());
        }
        return t;
    }

    /**
     * unnormalized variable
     */
    public static Variable v(char ch, String name) {
        return v(NormalizedVariable.typeIndex(ch), name);
    }

    /**
     * normalized variable
     */
    public static NormalizedVariable v(/**/ Op type, byte id) {
        return NormalizedVariable.the(type, id);
    }

    /**
     * parallel conjunction &| aka &&+0
     */
    public static Term parallel(Term... s) {
        return CONJ.the(0, s);
    }

    public static Term parallel(Collection<Term> s) {
        return CONJ.the(0, s);
    }

    /**
     * alias for disjunction
     */
    public static Term or(Term... x) {
        return DISJ(x);
    }

    /**
     * alias for conjunction
     */
    public static Term and(Term... x) {
        return CONJ.the(x);
    }


    /**
     * create a literal atom from a class (it's name)
     */
    public static Atom the(Class c) {
        return (Atom) Atomic.the(c.getName());
    }

    /**
     * gets the atomic target of an integer, with specific radix (up to 36)
     */
    public static Atom intRadix(int i, int radix) {
        return $.quote(Integer.toString(i, radix));
    }

    public static Atomic the(int v) {
        return Int.the(v);
    }

    public static MutableTruth tt(float f, float c) {
        return new MutableTruth(f, c2wSafe(c));
    }

    public static PreciseTruth t(float f, float c) {
        return PreciseTruth.byConf(f, c);
    }
    public static PreciseTruth t(float f, double c) {
        return PreciseTruth.byConf(f, c);
    }

    /**
     * negates each entry in the array
     */
    public static Term[] neg(Term[] array) {
        Util.map(Term::neg, array, array);
        return array;
    }

    public static Atomic theAtomic(byte[] string) {
        return Atomic.the(new String(string));
    }

    public static Term p(byte[] array) {
        return p(Util.bytesToInts(array));
    }

    public static Atomic the(byte c) {
        return theAtomic(new byte[]{c});
    }

    public static Term[] the(int... i) {
        int l = i.length;
        Term[] x = new Term[l];
        for (int j = 0; j < l; j++) {
            x[j] = the(i[j]);
        }
        return x;
    }

    /**
     * use with caution
     */
    public static Term the(boolean b) {
        return b ? Bool.True : Bool.False;
    }

    public static Term the(float x) {
        if (x != x)
            throw new TODO("NaN");

        int rx = (int) Util.round(x, 1);
        if (Util.equals(rx, x)) {
            return Int.the(rx);
        } else {
            return the(new Fraction(x));

        }

        //return quote(Float.toString(v));
    }

    public static Term the(double x) {
        if (x != x)
            throw new TODO("NaN");
        int rx = (int) Util.round(x, 1);
        if (Util.equals(rx, x, Double.MIN_NORMAL)) {
            return Int.the(rx);
        } else {
            return the(new Fraction(x));
        }
    }

    public static double doubleValue(Term x) {
        if (x.op() == INT) {
            return ((Int) x).i;
        } else if (x.op() == ATOM) {
            return Double.parseDouble($.unquote(x));
        } else {
            throw new TODO();
        }
    }

    public static Term the(Fraction o) {
        return $.func(DIV, $.the(o.getNumerator()), $.the(o.getDenominator()));
    }

    public static Term the(Object x) {
        if (x instanceof Term)
            return ((Term) x);

        if (x instanceof Number)
            return the((Number) x);

        if (x instanceof String)
            return the((String) x);

        throw new UnsupportedOperationException(x + " termize fail");
    }

    public static Atomic the(String x) {
        return Atomic.the(x);
    }

    public static Term the(Path file) {
        return the(file.toUri());
    }

    public static Path file(Term x) {
        throw new TODO();
    }

    public static Term the(Number n) {
        if (n instanceof Integer) {
            return Int.the((Integer) n);
        } else if (n instanceof Long) {
            return Atomic.the(Long.toString((Long) n));
        } else if (n instanceof Short) {
            return Int.the((Short) n);
        } else if (n instanceof Byte) {
            return Int.the((Byte) n);
        } else if (n instanceof Float) {
            float d = n.floatValue();
            int id = (int) d;
            if (d == d && Util.equals(d, id))
                return Int.the(id);

            return Atomic.the(n.toString());
        } else {
            double d = n.doubleValue();
            int id = (int) d;
            if (d == d && Util.equals(d, id))
                return Int.the(id);

            return Atomic.the(n.toString());
        }
    }

    public static <X> List<X> newArrayList() {
        return new FasterList<>(0);

    }

    public static <X> List<X> newArrayList(int capacity) {
        return new FasterList(capacity);

    }

    public static Term pRadix(int x, int radix, int maxX) {
        return $.p(radixArray(x, radix, maxX));
    }


    public static int[] radix(int x, int radix, int maxValue) {
        assert(x >= 0);
        x = x % maxValue; //auto-wraparound

        int decimals = (int) Math.ceil(Math.log(maxValue)/Math.log(radix));
        int[] y = new int[decimals];
        int X = -x;
        int yi = 0;
        do {
            y[yi++] = -(X % radix);
            X /= radix;
        } while(X <= -1);
        return y;
    }

    /**
     * most significant digit first, least last. padded with zeros
     *
     */
    public static Term[] radixArray(int x, int radix, int maxX) {

        int[] xx = radix(x, radix, maxX);

        Term[] tt = new Term[xx.length];
        for (int i = 0; i < xx.length; i++) {
            tt[i] =
                //$.the(BinTxt.symbols[xx[i]]);
                Int.the(xx[i]);
        }
        return tt;
    }




    public static Term pRecurse(boolean innerStart, Term... t) {
        int j = t.length - 1;
        int n = innerStart ? 0 : j - 1;
        Term inner = t[n];
        Term nextInner = inner.op() != PROD ? $.p(inner) : inner;
        while (--j > 0) {
            n += innerStart ? +1 : -1;
            Term next = t[n];


            Term[] nextArray = ArrayUtil.add(next.subterms().arrayShared(), nextInner);

            nextInner = next.op() != PROD ?
                    (innerStart ? $.p(nextInner, next) : $.p(next, nextInner)) : $.p(nextArray);

        }
        return nextInner;
    }

    public static @Nullable Compound inhRecurse(Term... t) {
        int tl = t.length;
        Term bottom = t[--tl];
        Compound nextInner = $.inh(t[--tl], bottom);
        while (nextInner != null && tl > 0) {
            nextInner = $.inh(t[--tl], nextInner);
        }
        return nextInner;
    }


    public static String unquote(Term s) {
        return Texts.unquote(s.toString());
    }


//    /**
//     * instantiate new Javascript context
//     */
//    public static NashornScriptEngine JS() {
//        return (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
//    }

    public static <X> PREDICATE<X> IF(Term t, Predicate<X> test) {
        return new LambdaPred<>(t, test);
    }

    public static <X> PREDICATE<X> AND(PREDICATE<X> a, PREDICATE<X> b) {
        return new LambdaPred<>(CONJ.the(a, b), (X x) -> a.test(x) && b.test(x));
    }

    public static <X> PREDICATE<X> OR(PREDICATE<X> a, PREDICATE<X> b) {
        return new LambdaPred<>(DISJ(a, b), (X x) -> a.test(x) || b.test(x));
    }

    public static int intValue(Term intTerm) throws NumberFormatException {
        if (intTerm instanceof Int && intTerm.op() == INT)
            return ((Int) intTerm).i;


        throw new NumberFormatException();

    }

    public static int intValue(Term intTerm, int ifNotInt) {
        return intTerm instanceof Int && intTerm.op() == INT ? ((Int) intTerm).i : ifNotInt;
    }

    public static Term fromJSON(String j) {
        return JsonTerm.the(j);
    }


    public static Compound pFast(Subterms x) {
        if (x.subs() == 0) return Op.EmptyProduct;
        else return new LightCompound(PROD, x);
    }

    public static Compound pFast(Term... x) {
        if (x.length == 0) return Op.EmptyProduct;
        else return new LightCompound(Op.PROD, x);
        //return new LighterCompound(PROD, x);
    }

    public static Compound sFast(Subterms x) {
        if (x.subs() == 0) throw new UnsupportedOperationException();
        return new LightCompound(Op.SETe, x);
    }

    public static Term sFast(Term[] x) {
        return sFast(true, x);
    }

    public static Compound sFast(SortedSet<Term> x) {
        return sFast(false, x.toArray(EmptyTermArray));
    }

    public static Compound sFast(Collection<Term> x) {
        return sFast(false, Terms.commute(x));
    }

    public static Compound sFast(boolean sort, Term[] x) {
        if (x.length == 0) throw new UnsupportedOperationException();
        if (sort && x.length > 1)
            x = Terms.commute(x);
        return new LightCompound(Op.SETe, x);
        //return new LighterCompound(Op.SETe, x);
    }

    public static Term sFast(RoaringBitmap b) {
        return sFast(true, ints(b));
    }

    public static Term pFast(Collection<? extends Term> x) {
        return pFast(x.toArray(EmptyTermArray));
    }

    public static Subterms vFast(Collection<Term> t) {
        return vFast(t.toArray(EmptyTermArray));
    }

    /**
     * on-stack/on-heap cheaply constructed Subterms
     */
    public static Subterms vFast(Term... t) {
        switch (t.length) {
            case 0:
                return Op.EmptySubterms;
            case 1: {
                Term a = t[0];
                if (Intrin.intrin(a.unneg()))
                    return new IntrinSubterms(a);
                break;
            }
            case 2: {
                Term a = t[0], b = t[1];
                if (Intrin.intrin(a.unneg()) && Intrin.intrin(b.unneg()))
                    return new IntrinSubterms(a, b);
                break;
            }
            case 3: {
                Term a = t[0], b = t[1], c = t[2];
                if (Intrin.intrin(a.unneg()) && Intrin.intrin(b.unneg()) && Intrin.intrin(c.unneg()))
                    return new IntrinSubterms(a, b, c);
                break;
            }
        }
//                return new UnitSubterm(tt);
//            default:
//                if (t.length < 3)
//                    return new ArrayTermVector(t);
//                else
        return new TermList(t);
        //}


    }


    public static Term[] $(String[] s) {
        return Util.map((String x) -> {
            try {
                return $.$(x);
            } catch (Narsese.NarseseException e) {
                throw new RuntimeException(e);
            }
        }, Term[]::new, s);
    }

    public static Term func(Atomic f, List<Term> args) {
        return $.func(f, args.toArray(Op.EmptyTermArray));
    }

    public static Term funcImg(String f, Term... x) {
        return funcImg($.the(f), x);
    }

    public static Term funcImg(Atomic f, Term... x) {
//        if (x.length > 1) {
        Term[] xx = ArrayUtil.insert(0, x, f);
        xx[x.length] = ImgExt;
        return INH.the(x[x.length - 1], PROD.the(xx));
//        } else {
//            return $.func(f, x);
//        }
    }


    public static Term diff(Term a, Term b) {
        //throw new TODO("use setAt/sect methods");
        Op aop = a.op();
        if (aop == b.op()) {
            if (aop == SETi) {
                return SetSectDiff.differenceSet(SETi, a, b);
            } else if (aop == SETe) {
                return SetSectDiff.differenceSet(SETe, a, b);
            }
        }
        //throw new UnsupportedOperationException();
        return CONJ.the(a, b.neg());
    }

    public static Term identity(Object x) {

        if (x instanceof Term)
            return ((Term) x);
        else if (x instanceof Termed) {
            Term u = ((Termed) x).term();
            if (u != null)
                return u;
            //else: probably still in the constructor before x.term() has been set, continue:
        }

        if (x instanceof String)
            return Atomic.the((String) x);
        else {
            Class<?> c = x.getClass();
            Term idHash = $.intRadix(System.identityHashCode(x), 36);
            if (!c.isSynthetic())
                return $.p($.quote(c.getName()), idHash);
            else
                return $.p($.quote(c.getSimpleName()), idHash);
        }

    }

    public static Atom uuid() {
        return uuid(null);
    }

    public static Atom uuid(@Nullable String prefix) {
        String u = Util.uuid64();
        return $.quote(prefix != null ? prefix + u : u);
    }

    public static Term the(URL u) {

        if (u.getQuery()!=null)
            throw new TODO();

        String schemeStr = u.getProtocol();
        String authorityStr = u.getAuthority();
        String pathStr = u.getPath();

        return URI(schemeStr, authorityStr, pathStr);
    }

    public static Term the(URI u) {

        if (u.getFragment()!=null || u.getQuery()!=null)
            throw new TODO();

        String schemeStr = u.getScheme();
        String authorityStr = u.getAuthority();
        String pathStr = u.getPath();

        return URI(schemeStr, authorityStr, pathStr);
    }

    /** https://en.wikipedia.org/wiki/Uniform_Resource_Identifier
     *
     * the URI scheme becomes the inheritance subject of the operation. so path components are the pred as a product. query can be the final component wrapped in a set to distinguish it, and init can be a json-like set of key/value pairs. the authority username/password can be special fields in that set of pairs.
     *
     * TODO authority, query
     * */
    public static Term URI(String schemeStr, @Nullable String authority, String pathStr) {
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

        Atom scheme = (Atom) the(schemeStr); //TODO cache these commonly used

        //TODO use more reliable path parser
        List<String> pathComponents = Splitter.on('/').omitEmptyStrings().splitToList(pathStr);

        Term path = p(pathComponents.toArray(ArrayUtil.EMPTY_STRING_ARRAY));
        return (authority == null || authority.isEmpty()) ?
                inh(path, scheme) : inh( PROD.the(INH.the(path, /*TODO parse*/the(authority))), scheme);
    }


}
