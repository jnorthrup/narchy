package nars;

//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.LoggerContext;
//import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
//import ch.qos.logback.core.ConsoleAppender;

import com.google.common.base.Strings;
import com.google.common.escape.Escapers;
import jcog.Util;
import jcog.list.FasterList;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import nars.derive.LambdaPred;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.task.TaskBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.compound.CompoundLight;
import nars.term.obj.JsonTerm;
import nars.term.pred.PrediTerm;
import nars.term.var.NormalizedVariable;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.VarPattern;
import nars.term.var.Variable;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.fraction.Fraction;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

import javax.script.ScriptEngineManager;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;

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
public enum $ {
    ;

    static {
        Thread.currentThread().setName("$");

        //System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");  //http://logging.apache.org/log4j/2.x/log4j-core/apidocs/org/apache/logging/log4j/core/async/AsyncLogger.html

        //System.setProperty("org.slf4j.simpleLogger.cacheOutputStream", "true");

//        ServiceLoader.loadInstalled(DoesntSuckLoggerFactory.class);
//
//        DoesntSuckLoggerFactory f = (DoesntSuckLoggerFactory) LoggerFactory.getILoggerFactory();

//        SimpleLogger LOG = (SimpleLogger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
//        System.out.println(LOG);


//            //http://logback.qos.ch/manual/layouts.html
//
//            Logger LOG = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
//            LoggerContext loggerContext = LOG.getLoggerContext();
//            // we are not interested in auto-configuration
//            loggerContext.reset();
//
//            PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
//            logEncoder.setContext(loggerContext);
//            //logEncoder.setPattern("\\( %highlight(%level),%green(%thread),%yellow(%logger{0}) \\): \"%message\".%n");
//            logEncoder.setPattern("\\( %green(%thread),%highlight(%logger{0}) \\): \"%message\".%n");
//
//            logEncoder.start();
//
//
//            ConsoleAppender c = new ConsoleAppender();
//            c.setContext(loggerContext);
//            c.setEncoder(logEncoder);
//            c.setImmediateFlush(false);
//            //c.setWithJansi(true);
//            c.start();
//
//            LOG.addAppender(c);

////            SyslogAppender syslog = new SyslogAppender();
////            syslog.setPort(5000);
////            syslog.setFacility("LOCAL6");
////            syslog.setContext(loggerContext);
////            syslog.setCharset(Charset.forName("UTF8"));
////            syslog.start();
////            LOG.addAppender(syslog);
//
////            SocketAppender sa = new SocketAppender();
////            sa.setName("socketlog");
////            sa.setContext(loggerContext);
////            sa.setQueueSize(1);
////            sa.setEventDelayLimit(Duration.buildByMilliseconds(100));
////            sa.setRemoteHost("localhost");
////            sa.setPort(4560);
////            sa.setIncludeCallerData(true);
////            sa.setReconnectionDelay(Duration.buildByMilliseconds(200));
////            sa.start();
////            LOG.addAppender(sa);
//
////        logRoot.debug("Message 1");
////        logRoot.info("Message 1");
////        logRoot.warn("Message 2");
////        logRoot.error("Message 2");
//
//
////        } catch (Throwable t) {
////            System.err.println("Logging Disabled: " + t);
////        }
//        }

    }

    public static <T extends Term> T $(String term) throws Narsese.NarseseException {
        return (T) Narsese.term(term, true);
    }

    public static <T extends Term> T $safe(String term) {
        try {
            return $(term);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }

//    public static @NotNull <O> ObjRef<O> ref(String term, O instance) {
//        return new ObjRef(term, instance);
//    }

    static final Atom emptyQuote = (Atom) Atomic.the("\"\"");

    static final Escapers.Builder quoteEscaper = Escapers.builder().addEscape('\"', "\\\"");


    public static Atomic quote(Object text) {
        String s = text.toString();

        int length = s.length();

        if (length == 0)
            return emptyQuote;

        if (s.charAt(0) == '\"' && s.charAt(length - 1) == '\"') {
            if (length == 1) {
                s = "\"\\\"\"";
            } else {
                //already quoted the empty string
            }
        } else {
            s = ("\"" + quoteEscaper.build().escape(s) + '"');
        }

        return Atomic.the(s);
    }


    public static Term[] the(String... id) {
        int l = id.length;
        Term[] x = new Term[l];
        for (int i = 0; i < l; i++)
            x[i] = Atomic.the(id[i]);
        return x;
    }


    public static Atom the(char c) {
        return (Atom) Atomic.the(String.valueOf(c));
    }

    /**
     * Op.INHERITANCE from 2 Terms: subj --> pred
     * returns a Term if the two inputs are equal to each other
     */
    public static <T extends Term> T inh(Term subj, Term pred) {
        return (T) INH.the(DTERNAL, subj, pred);
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


    public static Term func(String opTerm, String... arg) throws Narsese.NarseseException {
        return func(Atomic.the(opTerm), $.array(arg));
    }

    /**
     * function ((a,b)==>c) aka: c(a,b)
     */
    public static Term func(Atomic opTerm, Term... arg) {
        return INH.the($.p(arg), opTerm);
    }




    public static <T extends Term> T impl(Term a, Term b) {
        return (T) IMPL.the(DTERNAL, a, b);
    }


    public static <T extends Term> T impl(Term a, int dt, Term b) {
        return (T) IMPL.the(dt, a, b);
    }


    public static Term p(Collection<Term> t) {
        return $.p(t.toArray(new Term[t.size()]));
    }


    public static Term p(Term... t) {
        return PROD.the(t);
    }


    /**
     * creates from a sublist of a list
     */
    @NotNull
    public static Term p(List<Term> l, int from, int to) {
        Term[] x = new Term[to - from];

        for (int j = 0, i = from; i < to; i++)
            x[j++] = l.get(i);

        return $.p(x);
    }

    public static Term p(String... t) {
        return $.p((Term[]) $.the(t));
    }

    public static Term p(int... t) {
        return $.p((Term[]) $.the(t));
    }

    /**
     * warning: generic variable
     */
    public static Variable v(/*@NotNull*/ Op type, String name) {

//        if (name.length()==1) {
//            char c = name.charAt(0);
//            if (c >= '1' && c <= '9')
//                return $.v(type, c-'0'); //explicit use of normalized var
//        }

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
    public static <T extends Term> T inst(@NotNull Term subj, Term pred) {
        return (T) INH.the(SETe.the(subj), pred);
    }

    public static <T extends Term> T instprop(@NotNull Term subject, @NotNull Term predicate) {
        return (T) INH.the(SETe.the(subject), SETi.the(predicate));
    }

    public static <T extends Term> T prop(Term subject, Term predicate) {
        return (T) INH.the(subject, SETi.the(predicate));
    }

    //    @NotNull
//    public static TaskBuilder goal(@NotNull Term term, float freq, float conf) {
//        return task(term, GOAL, freq, conf);
//    }

    @NotNull
    public static TaskBuilder task(@NotNull String term, byte punct, float freq, float conf) throws Narsese.NarseseException {
        return task($.$(term), punct, freq, conf);
    }

    public static TaskBuilder task(@NotNull Term term, byte punct, float freq, float conf) {
        return task(term, punct, t(freq, conf));
    }

    public static TaskBuilder task(@NotNull Term term, byte punct, Truth truth) {
        return new TaskBuilder(term, punct, truth);
    }

    public static Term sete(Collection<? extends Term> t) {
        return SETe.the(DTERNAL, (Collection) t);
    }

//    /**
//     * construct set_ext of key,value pairs from a Map
//     */
//    public static Term seteMap(@NotNull Map<Term, Term> map) {
//        return $.sete(
//                map.entrySet().stream().map(
//                        e -> $.p(e.getKey(), e.getValue()))
//                        .collect(Collectors.toSet())
//        );
//    }

    public static Term p(char[] c, CharToObjectFunction<Term> f) {
        Term[] x = new Term[c.length];
        for (int i = 0; i < c.length; i++) {
            x[i] = f.valueOf(c[i]);
        }
        return $.p(x);
    }

    public static <X> Term p(@NotNull X[] x, @NotNull Function<X, Term> toTerm) {
        return $.p((Term[]) terms(x, toTerm));
    }

    public static <X> Term[] terms(@NotNull X[] map, @NotNull Function<X, Term> toTerm) {
        return Stream.of(map).map(toTerm).toArray(Term[]::new);
    }

//    public static <X> Term seteMap(@NotNull Map<Term, ? extends X> map, @NotNull Function<X, Term> toTerm) {
//        return $.sete(
//                map.entrySet().stream().map(
//                        e -> $.p(e.getKey(), toTerm.apply(e.getValue())))
//                        .collect(Collectors.toSet())
//        );
//    }

    private static Term[] array(@NotNull Collection<? extends Term> t) {
        return t.toArray(new Term[t.size()]);
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
        return SETe.the(ints(b)); //non-interned
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
    public static NormalizedVariable v(/*@NotNull*/ Op type, byte id) {
        return NormalizedVariable.the(type, id);
    }

    public static Term conj(@NotNull Collection<Term> collection, @NotNull Term... append) {
        if (append.length == 0)
            throw new RuntimeException("unnecessary append");
        int cs = collection.size();
        Term[] ca = new Term[cs + append.length];
        collection.toArray(ca);
        int i = cs;
        for (Term t : append) {
            ca[i++] = t;
        }
        return CONJ.the(DTERNAL, ca);
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

    public static Term disj(Term... a) {
        Term[] b = a.clone();
        neg(b);
        return CONJ.the(DTERNAL, b).neg();
    }


    public static Term secte(SortedSet<Term> x) {
        return SECTe.the(DTERNAL, x);
    }


    /**
     * create a literal atom from a class (it's name)
     */
    public static Atom the(Class c) {
        return (Atom) Atomic.the(c.getName());
    }


    /**
     * gets the atomic term of an integer, with specific radix (up to 36)
     */
    public static Atom the(int i, int radix) {
        return (Atom) Atomic.the(Integer.toString(i, radix));
    }


    public static Atomic the(int v) {
        return Int.the(v);
    }

    public static Atomic the(float v) {
        if (Util.equals((float) Math.floor(v), v, Float.MIN_VALUE * 2)) {
            //close enough to be an int, so it doesnt need to be quoted
            return the((int) v);
        }
        //return Atom.the(Utf8.toUtf8(name));

        return quote(Float.toString(v));

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
    }


    public static PreciseTruth t(float f, float c) {
        return new PreciseTruth(f, c);
    }


    /**
     * negates each entry in the array
     */
    public static void neg(Term[] array) {
        Util.map(Term::neg, array, array);
    }

    public static Atomic the(byte[] id) {
        return Atomic.the(new String(id));
    }

    public static Atomic the(byte c) {
        return the(new byte[]{c});
    }

    public static Term[] the(int... i) {
        int l = i.length;
        Term[] x = new Term[l];
        for (int j = 0; j < l; j++) {
            x[j] = the(i[j]);
        }
        return x;
    }

    public static Term the(boolean b) {
        return b ? Op.True : Op.False;
    }

    public static Term the(double x) {
        int rx = (int)Util.round(x, 1);
        if (Util.equals(rx, x, Double.MIN_NORMAL)) {
            return Int.the(rx);
        } else {
            return the(new Fraction(x));
        }
    }

    public static Term the(Fraction o) {
        return $.func("div", $.the(o.getNumerator()), $.the(o.getDenominator()));
    }

    public static Term the(Object o) {
        if (o instanceof Term)
            return ((Term) o);

        if (o instanceof Number) {
            if (o instanceof Integer)
                return Int.the((Integer) o);
        }

        return Atomic.the(o.toString());
    }

    /**
     * conjunction sequence (2-ary)
     */
    public static Term seq(Term x, int dt, Term y) {
        return CONJ.the(dt, x, y); //must be a vector, not set
    }


    public static <K, V> Map<K, V> newHashMap() {
        return newHashMap(0);
    }

    public static <K, V> Map<K, V> newHashMap(int capacity) {
        return new HashMap<>(capacity);

        //return new UnifiedMap(capacity);
        //return new UnifriedMap(capacity /*, loadFactor */);

        //return new FasterHashMap(capacity);
        //return new FastMap<>(); //javolution http://javolution.org/apidocs/javolution/util/FastMap.html

        //return new LinkedHashMap(capacity);
    }

    public static <X> List<X> newArrayList() {
        return new FasterList<>(0);
        //return new ArrayList();
    }

    public static <X> List<X> newArrayList(int capacity) {
        return new FasterList(capacity);
        //return new ArrayList(capacity);
    }

    public static <X> Set<X> newHashSet(int capacity) {
//        if (capacity < 4) {
//            return new UnifiedSet(0);
//        } else {
        //return new UnifiedSet(capacity);
        //return new SimpleHashSet(capacity);
        return new HashSet(capacity);
        //return new LinkedHashSet(capacity);
//        }
    }

//    @NotNull
//    public static <X> Set<X> newHashSet(@NotNull Collection<X> values) {
//        Set<X> s = newHashSet(values.size());
//        s.addAll(values);
//        return s;
//    }

//    public static @Nullable <C> Reference<C> reference(@Nullable C s) {
//        return s == null ? null :
//                //new SoftReference<>(s);
//                //new WeakReference<>(s);
//                Param.DEBUG ? new SoftReference<>(s) : new WeakReference<>(s);
//    }

//    @Nullable
//    public static <C> Reference<C>[] reference(@Nullable C[] s) {
//        int l = Util.lastNonNull((Object[]) s);
//        if (l > -1) {
//            l++;
//            Reference<C>[] rr = new Reference[l];
//            for (int i = 0; i < l; i++) {
//                rr[i] = reference(s[i]);
//            }
//            return rr;
//        }
//        return null;
//    }

//    public static void dereference(@NotNull Reference[] p) {
//        for (int i = 0; i < p.length; i++) {
//            Reference x = p[i];
//            if (x != null)
//                x.clear();
//            p[i] = null;
//        }
//    }
//
//    @Nullable
//    public static <C> C dereference(@Nullable Reference<C> s) {
//        return s == null ? null : s.get();
//    }
//
//    @Nullable
//    public static <C> C dereference(@Nullable Reference<C>[] s, int index) {
//        if (s == null || index >= s.length) return null;
//        return dereference(s[index]);
//    }


    public static Term pRadix(int x, int radix, int maxX) {
        Term[] tt = radixArray(x, radix, maxX);
        return $.p(tt);
    }


    /**
     * most significant digit first, least last. padded with zeros
     */
    public static @NotNull Term[] radixArray(int x, int radix, int maxX) {
        String xs = Integer.toString(x, radix);
        String xx = Integer.toString(maxX, radix);
        Term[] tt = new Term[xx.length()];
        int ttl = tt.length;
        int xsl = xs.length();
        int p = ttl - xsl;
        int j = 0;
        for (int i = 0; i < ttl; i++) {
            Term n;
            if (p-- > 0) {
                n = $.the(0); //pad with zeros
            } else {
                n = $.the(xs.charAt(j++) - '0');
            }
            tt[i] = n;
        }
        return tt;
    }


    public static @NotNull Term pRecurseIntersect(char prefix, @NotNull Term... t) {
        final int[] index = {0};
        return SECTe.the($.terms(t, x -> Atomic.the(Strings.repeat(String.valueOf(prefix), ++index[0]) + x)));
    }

    public static @NotNull Term pRecurse(@NotNull Term... t) {
        int tl = t.length;
        Term inner = t[--tl];
        Term nextInner = inner.op() != PROD ? $.p(inner) : inner; //wrap innermost item in product too, for fairness
        while (tl > 0) {
            Term next = t[--tl];
            nextInner = next.op() != PROD ? $.p(next, nextInner) : $.p(ArrayUtils.add(next.subterms().arrayShared(), nextInner));
        }
        return nextInner;
    }

    public static @Nullable Compound inhRecurse(@NotNull Term... t) {
        int tl = t.length;
        @NotNull Term bottom = t[--tl];
        Compound nextInner = $.inh(t[--tl], bottom); //wrap innermost item in product too, for fairness
        while (nextInner != null && tl > 0) {
            nextInner = $.inh(t[--tl], nextInner);
        }
        return nextInner;
    }

//    public static void logLevel(Class logClass, Level l) {
//        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(logClass)).setLevel(l);
//    }

//    @NotNull
//    public static TaskBuilder command(@NotNull Compound op) {
//        //TODO use lightweight CommandTask impl without all the logic metadata
//        TaskBuilder t = new TaskBuilder(op, COMMAND, null);
//        t.setPri(1f);
//        return t;
//    }
//
//    @NotNull
//    public static TaskBuilder command(@NotNull String functor, Term... args) {
//        //TODO use lightweight CommandTask impl without all the logic metadata
//        return command(func(functor, args));
//    }

    @NotNull
    public static String unquote(Term s) {
        return unquote(s.toString());
    }

    @NotNull
    public static String unquote(String x) {
        while (true) {
            int len = x.length();
            if (len > 0 && x.charAt(0) == '\"' && x.charAt(len - 1) == '\"') {
                x = x.substring(1, len - 1);
            } else {
                return x;
            }
        }
    }


    /**
     * instantiate new Javascript context
     */
    public static NashornScriptEngine JS() {
        return (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
    }

    public static <X> PrediTerm<X> IF(Term t, Predicate<X> test) {
        return new LambdaPred<>(t, test);
    }

    public static <X> PrediTerm<X> AND(PrediTerm<X> a, PrediTerm<X> b) {
        return new LambdaPred<>(CONJ.the(a, b), (X x) -> a.test(x) && b.test(x));
    }

    public static <X> PrediTerm<X> OR(PrediTerm<X> a, PrediTerm<X> b) {
        return new LambdaPred<>($.disj(a, b), (X x) -> a.test(x) || b.test(x));
    }

    public static int intValue(Term intTerm) throws NumberFormatException {
        if (intTerm instanceof Int && intTerm.op()==INT)
            return ((Int) intTerm).id;
//        if (intTerm instanceof Atom) {
//            String xs = intTerm.toString();
//            return Texts.i(xs);
//        } else {
        throw new NumberFormatException();
        //   }
    }

    public static int intValue(Term intTerm, int ifNotInt) {
        if (intTerm instanceof Int && intTerm.op()==INT)
            return ((Int) intTerm).id;
        else
            return ifNotInt;
    }

    public static Term fromJSON(String j) {
        return JsonTerm.the(j);
    }

    public static Term pFast(Subterms x) {
        return new CompoundLight(Op.PROD,x);
    }
    public static Term pFast(Term... x) {
        return new CompoundLight(Op.PROD, $.vFast(x));
    }

    public static Term sFast(Term... x) {
        return new CompoundLight(Op.SETe, $.vFast(Terms.sorted(x)));
    }

    public static Term sFast(RoaringBitmap b) {
        return sFast(ints(b)); //non-interned
    }

    public static Term pFast(Collection<? extends Term> x) {
        return new CompoundLight(Op.PROD, $.vFast(x.toArray(new Term[x.size()])));
    }

    public static Subterms vFast(Term[] t) {
        return new TermList(t);
    }


    //TODO add this to a '$.printree' command

    /*
     * Copyright 2006-2009 Odysseus Software GmbH
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */

//    /**
//     * Node pretty printer for debugging purposes.
//     *
//     * @author Christoph Beck
//     */
//    public static class NodePrinter {
//        private static boolean isLastSibling(Node node, Node parent) {
//            if (parent != null) {
//                return node == parent.getChild(parent.getCardinality() - 1);
//            }
//            return true;
//        }
//
//        private static void dump(PrintWriter writer, Node node, Stack<Node> predecessors) {
//            if (!predecessors.isEmpty()) {
//                Node parent = null;
//                for (Node predecessor: predecessors) {
//                    if (isLastSibling(predecessor, parent)) {
//                        writer.print("   ");
//                    } else {
//                        writer.print("|  ");
//                    }
//                    parent = predecessor;
//                }
//                writer.println("|");
//            }
//            Node parent = null;
//            for (Node predecessor: predecessors) {
//                if (isLastSibling(predecessor, parent)) {
//                    writer.print("   ");
//                } else {
//                    writer.print("|  ");
//                }
//                parent = predecessor;
//            }
//            writer.print("+- ");
//            writer.println(node.toString());
//
//            predecessors.push(node);
//            for (int i = 0; i < node.getCardinality(); i++) {
//                dump(writer, node.getChild(i), predecessors);
//            }
//            predecessors.pop();
//        }
//
//        public static void dump(PrintWriter writer, Node node) {
//            dump(writer, node, new Stack<Node>());
//        }
//    }
}
