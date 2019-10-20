package nars.op.java;

import com.google.common.collect.Sets;
import jcog.Paper;
import jcog.Skill;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.data.map.CustomConcurrentHashMap;
import jcog.math.FloatRange;
import jcog.memoize.CaffeineMemoize;
import jcog.memoize.Memoize;
import jcog.util.ArrayUtil;
import nars.*;
import nars.attention.What;
import nars.concept.Operator;
import nars.control.channel.CauseChannel;
import nars.subterm.Subterms;
import nars.task.NALTask;
import nars.term.Functor;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.part.DurLoop;
import nars.truth.PreciseTruth;
import nars.util.AtomicOperations;
import nars.util.Timed;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.matcher.ElementMatchers;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.Math.round;
import static jcog.data.map.CustomConcurrentHashMap.*;
import static nars.Op.*;
import static nars.truth.func.TruthFunctions.c2wSafe;
import static org.eclipse.collections.impl.tuple.Tuples.pair;


/**
 * Opjects - Operable Objects
 * Transparent JVM Metaprogramming Interface for Non-Axiomatic Logic Reasoners
 * <p>
 * Generates dynamic proxy classes for any POJO that intercepts specific
 * methods and generates reasoner events which can be
 * stored, input to one or more reasoners, etc..
 * <p>
 * An Opjects instance manages a set of ("opject") proxy instances and their activity,
 * whether either by a user (ex: while training) or the NAR itself (ex: after trained).
 * <p>
 * Invoke -  invoked internally, deliberately as a result of NAR activity.
 * <p>
 * Evoke -  externally caused execution (by user or other computer process).
 * in a sense the NAR is puppeted by these actions, because it perceives them,
 * to some degree, as causing them itself.  however, from a user's perspective,
 * we clearly distinguish between Evoked and Invoked procedures.
 * <p>
 * Evocation trains the NAR how to invoke.  During on-line use, it provides
 * asynchronously triggered feedback which can inform and trigger the NAR
 * for what it has learned, or any other task.
 * <p>
 * TODO option to record stack traces
 * TODO the results need to be buffered each cycle to avoid inputting multiple boolean-returning tasks that contradict each other
 */
@Paper
@Skill({"Metaprogramming", "Reinforcement_learning"})
public class Opjects extends DefaultTermizer {

    static final org.slf4j.Logger logger = LoggerFactory.getLogger(Opjects.class);
    static final ClassLoadingStrategy classLoadingStrategy =

            ClassLoadingStrategy.Default.WRAPPER;

    final ByteBuddy bb = new ByteBuddy();

    public final FloatRange exeThresh = new FloatRange(0.75f, 0.5f, 1f);


    /**
     * determines evidence weighting for reporting specific feedback values
     */
    float beliefEviFactor = 1f;
    float invokeEviFactor = beliefEviFactor;
    float beliefFreq = 1f;
    float invokeFreq = 1f;


    /**
     * determines evidence weighting for reporting assumed feedback assumptions
     */
    float doubtEviFactor = 0.75f;
    float uninvokeEviFactor = 1;
    float doubtFreq = 0.5f;
    float uninvokeFreq = 1f - invokeFreq;

    public final FloatRange pri = new FloatRange(1f, 0f, 1f);

    /**
     * cached; updated at most each duration
     */
    private float beliefEvi = 0;
    private float beliefPri = 0;
    private float invokeEvi;

    /**
     * set of operators in probing mode which are kept here for batched execution
     * should be a setAt.   using ConcurrentFastIteratingHashMap instead of the Set because it has newer code updates
     */
    final ConcurrentFastIteratingHashSet<MethodExec> probing =
            new ConcurrentFastIteratingHashSet<>(new MethodExec[0]);


    public static final Set<String> methodExclusions = Sets.newConcurrentHashSet(java.util.Set.of(
            "hashCode",
            "notify",
            "notifyAll",
            "wait",
            "finalize",
            "stream",
            "iterator",
            "getHandler",
            "setHandler",
            "toString",
            "equals"
    ));

    final Map<Class, Class> proxyCache = new CustomConcurrentHashMap(STRONG, EQUALS, SOFT, IDENTITY, 64);

    final Map<Class, Boolean> clCache = new CustomConcurrentHashMap(STRONG, EQUALS, STRONG, IDENTITY, 64);
    final Map<String, MethodExec> opCache = new CustomConcurrentHashMap(STRONG, EQUALS, STRONG, IDENTITY, 64);


    /**
     * TODO maybe use a stack to track invocations inside of evocations inside of invokations etc
     */
    static final ThreadLocal<AtomicBoolean> evoking = ThreadLocal.withInitial(AtomicBoolean::new);


    public final NAR nar;

    /**
     * for externally-puppeted method invocation goals
     */


    protected final CauseChannel<Task> in;
    private final What what;

    private final Memoize<Pair<Pair<Class, Term>, List<Class<?>>>, MethodHandle> methodCache =
            CaffeineMemoize.build(
            //new SoftMemoize<>(
        x -> {

            var c = x.getOne().getOne();
            var methodTerm = x.getOne().getTwo();
            var types = x.getTwo();

            var mName = methodTerm.toString();
            var cc = types.isEmpty() ? ArrayUtil.EMPTY_CLASS_ARRAY : ((FasterList<Class<?>>) types).array();
            var m = findMethod(c, mName, cc);
            if (m == null || !methodEvokable(m))
                return null;

            m.setAccessible(true);
            try {
                var mh = MethodHandles.lookup()
                        .unreflect(m)
                        .asFixedArity()
                ;
                return mh;

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        },
    -1 /* soft */, false);
    //, 512, STRONG, SOFT);


    /**
     * whether NAR can envoke the method internally
     */
    public static boolean methodEvokable(Method m) {
        return isPublic(m);
    }


    static boolean isPublic(Method m) {
        var mm = m.getModifiers();
        return Modifier.isPublic(mm);
    }

    @Deprecated public Opjects(NAR n) {
        this(n.main);
    }
    public Opjects(What w) {
        this.what = w;
        in = (nar = w.nar).newChannel(this);
        update(w.nar);
        var on = w.nar.onDur(this::update);
    }

    /**
     * called every duration to update all the operators in one batch, so they dont register events individually
     */
    protected void update(NAR nar) {
        var cMin = (float) c2wSafe(nar.confMin.evi());
        var cMax = c2wSafe(nar.confDefault(BELIEF));
        beliefEvi = Util.lerp(beliefEviFactor, cMin, cMax);
        var doubtEvi = Util.lerp(doubtEviFactor, cMin, cMax);
        invokeEvi = Util.lerp(invokeEviFactor, cMin, cMax);
        var uninvokeEvi = Util.lerp(uninvokeEviFactor, cMin, cMax);
        var invokePri = beliefPri = pri.floatValue() * nar.priDefault(BELIEF);

        probing.forEachWith(AtomicOperations::update, nar);
    }

    @Override
    protected Term classInPackage(Term classs, Term packagge) {
        var t = $.inst(classs, packagge);


        return t;
    }


    /**
     * registers an alias/binding shortcut target rewrite macro
     */
    public void alias(String op, Term instance, String method) {
        nar.add(Functor.f(op, s ->
            $.func(method, instance, s.subs() == 1 ? s.sub(0) : PROD.the(s))
        ));
    }


    @FunctionalInterface
    interface InstanceMethodValueModel {

        void update(Term instance, Object obj, Method method, Object[] args, Object nextValue, NAR nar);
    }


    final InstanceMethodValueModel pointTasks = new PointMethodValueModel();
    final Function<Term, InstanceMethodValueModel> valueModel = (x) -> pointTasks /* memoryless */;

    public class PointMethodValueModel implements InstanceMethodValueModel {


        @Override
        public void update(Term instance, Object obj, Method method, Object[] args, Object nextValue, NAR nar) {


            var now = nar.time();
            var dur = nar.dur();
            long start = round(now - dur / 2);
            long end = round(now + dur / 2);

            var f = beliefFreq;

            var methodReturnType = method.getReturnType();
            var isVoid = methodReturnType == void.class;

            Task value;

            if (!isVoid) {
                var t = opTerm(instance, method, args, nextValue);
                value = value(t, f, start, end, nar);
            } else {
                value = null;
            }

            var evokedOrInvoked = evoking.get().getOpaque();

            Task feedback;
            if (isVoid || evokedOrInvoked) {
                feedback = feedback(opTerm(instance, method, args, isVoid ? null : $.varDep(1)), start, end, nar);
            } else {
                feedback = null;
            }

            if (feedback == null && value!=null)
                in.accept(value, what);
            else if (value==null && feedback != null)
                in.accept(feedback, what);
            else if (value!=null && feedback!=null)
                in.acceptAll(new Task[]{feedback, value}, what);


        }

        public Task feedback(Term nt, long start, long end, NAR nar) {

            Task feedback =
//                new TruthletTask(nt, BELIEF,
//                    Truthlet.step(
//                            uninvokeFreq, start,
//                            invokeFreq, invokeEvi,
//                            end, uninvokeFreq,
//                            uninvokeEvi
//                    ), nar);
                    NALTask.the(nt, BELIEF, PreciseTruth.byEvi(invokeFreq, invokeEvi), start, start, end, nar.evidence());
            //if (NAL.DEBUG) {
            //}
            //feedback.setCyclic(true); //prevent immediate structural transforms
            feedback.priMax(beliefPri);
            return feedback;
        }


        public Task value(Term nextTerm, float freq, long start, long end, NAR nar) {
            var nt = nextTerm;
            if (nt.op() == NEG) {
                nt = nt.unneg();
                freq = 1 - freq;
            }

            Task value =
//                new TruthletTask(nt, BELIEF,
//                    Truthlet.step(
//                            doubtFreq, start,
//                            f, beliefEvi,
//                            end, doubtFreq,
//                            doubtEvi
//                    ),
//                    nar);
                    NALTask.the(nt, BELIEF, PreciseTruth.byEvi(freq, beliefEvi), start, start, end, nar.evidence());


            value.priMax(beliefPri);
            return value;
        }
    }


    /**
     * this target should not be used in constructing terms that will leave this class.
     * this is so it wont pollute the NAR's index and possibly interfere with other
     * identifiers that it may be equal to (ex: NAR.self())
     */
    private class Instance extends ProxyTerm {

        /**
         * reference to the actual object
         */
        public final Object object;

        final InstanceMethodValueModel belief;

        /**
         * for VM-caused invocations: if true, inputs a goal task since none was involved. assists learning the interface
         */

        public Instance(Term id, Object object) {
            super(id);
            this.object = object;
            this.belief = valueModel.apply(id);
        }

        public Object update(Object obj, Method method, Object[] args, Object nextValue) {


            belief.update(ref, obj, method, args, nextValue, nar);

            return nextValue;
        }


    }

    Term opTerm(Term instance, Method method, Object[] args, Object result) {


        var returnType = method.getReturnType();
        var isVoid = result == null && returnType == void.class;
        var isBoolean = returnType == boolean.class || (returnType == Boolean.class && result!=null);

        var xn = 3;
        if (args.length == 0) {
            xn--;
        }
        if (isVoid || isBoolean) {
            xn--;
        }

        var x = new Term[xn];
        var resultTerm = xn - 1;

        x[0] = instance;

        if (method.isVarArgs() && args.length == 1) {
            args = (Object[]) args[0];
        }
        if (args.length > 0) {
            switch (args.length) {
                case 0:
                    break;
                case 1:
                    x[1] = term(args[0]);
                    break; /* unwrapped singleton */
                default:
                    x[1] = PROD.the(terms(args));
                    break;
            }
            assert (x[1] != null) : "could not termize: " + Arrays.toString(args);
        }

        var negate = false;

        if (result instanceof Term) {
            var tr = (Term) result;
            if (tr.op() == NEG) {
                tr = tr.unneg();
                negate = true;
            }
            x[resultTerm] = tr;
        } else {
            if (isBoolean) {

                boolean b = (Boolean) result;
                if (!b) {
                    negate = true;
                }
                result = null;
            }

            if (!isVoid && !isBoolean) {
                x[resultTerm] = Opjects.this.term(result);
                assert (x[resultTerm] != null) : "could not termize: " + result;
            }
        }

        return $.func(methodName(method), x).negIf(negate).normalize();

    }

    static String methodName(Method method) {
        var n = method.getName();
        var i = n.indexOf("$accessor$");
        if (i != -1) {
            return n.substring(0, i);
        } else {
            return n;
        }
    }

    private class MethodExec extends AtomicOperations implements BiConsumer<Term, Timed> {
        private final Term methodName;
        public Operator operator;

        /**
         * TODO invalidate any entries from here if an instance is registered after it has
         * been running which may cause it to be ignored once it becomes available.
         * maybe use a central runCache to make this easier
         */
        final Memoize<Term, Runnable> runCache;


        MethodExec(String _methodName) {
            super(null, Opjects.this.exeThresh);

            this.methodName = $.the(_methodName);

            runCache = CaffeineMemoize.build(term -> {

                var args = validArgs(Functor.args(term));
                if (args == null)
                    return null;

                var instanceTerm = args.sub(0);
                var instance = termToObj.get(instanceTerm);
                if (instance == null)
                    return null;

                var as = args.subs();
                var methodArgs = as > 1 && (as > 2 || !args.sub(as - 1).op().var) ? args.sub(1) : Op.EmptyProduct;

                var maWrapped = methodArgs.op() == PROD;

                var aa = maWrapped ? methodArgs.subs() : 1;

                Object[] instanceAndArgs;
                List<Class<?>> types;
                if (aa == 0) {
                    instanceAndArgs = new Object[]{instance};
                    types = Collections.emptyList();
                } else {
                    instanceAndArgs = object(instance, maWrapped ? methodArgs.subterms().arrayShared() : new Term[]{methodArgs});
                    types = Util.typesOf(instanceAndArgs, 1 /* skip leading instance value */, instanceAndArgs.length);
                }


                Pair<Pair<Class, Term>, List<Class<?>>> key = pair(pair(instance.getClass(), methodName), types);
                var mh = methodCache.apply(key);
                if (mh == null) {
                    return null;
                }

                return () -> {

                    var flag = evoking.get();
                    flag.set(true);

                    try {
                        mh.invokeWithArguments(instanceAndArgs);
                        evoked(methodName, instance, instanceAndArgs);
                    } catch (Throwable throwable) {
                        logger.error("{} execution {}", term, throwable);
                    } finally {
                        flag.set(false);
                    }


                };

            }, -1, false);
            //, 512, STRONG, SOFT);
        }

        @Override
        public int hashCode() {
            return methodName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        protected void enable(NAR n) {
            probing.add(this);
        }

        @Override
        protected void disable(NAR n) {
            probing.remove(this);
        }

        @Override
        protected Task exePrefilter(Task x) {

            if (!x.isCommand() && x.freq() <= 0.5f + NAL.truth.TRUTH_EPSILON)
                return null;

            if (runCache.apply(x.term()) == null)
                return null;

            return x;


        }


        @Override
        public void accept(Term term, Timed timed) {

            runCache.apply(term).run();

        }
    }


    public final <T> T the(String id, T instance, Object... args) {
        return the($.the(id), instance, args);
    }

    /**
     * wraps a provided instance in an intercepting proxy class
     * not as efficient as the Opject.a(...) method since a custom proxy class will
     * be created, and method invocation is slower, needing to use java reflection.
     */
    public <T> T the(Term id, T instance, Object... args) {

        reflect(instance.getClass());

        try {
            var cl = bb
                    .with(TypeValidation.DISABLED)
                    .subclass(instance.getClass())
                    .method(ElementMatchers.isPublic().and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class))))

                    .intercept(InvocationHandlerAdapter.of((objWrapper, method, margs) ->
                            invoke(objWrapper, instance, method, margs)))
                    .make()
                    .load(
                            Thread.currentThread().getContextClassLoader(),

                            classLoadingStrategy
                    )
                    .getLoaded();
            var instWrapped = (T) cl.getConstructor(Util.typesOfArray(args)).newInstance(args);

            register(id, instWrapped);

            return instWrapped;


        } catch (Throwable e) {
            throw new RuntimeException(e);
        }


    }

    private void reflect(Class<?> cl) {
        clCache.computeIfAbsent(cl, (clazz) -> {
            for (var m: clazz.getMethods())
                reflect(m);
            return true;
        });
    }

    private MethodExec reflect(Method m) {
        if (!validMethod(m))
            return null;

        m.setAccessible(true);

        var n = m.getName();
        return opCache.computeIfAbsent(n, (mn) -> {
            var methodExec = new MethodExec(mn);
            var op = nar.setOp(Atomic.atom(mn), methodExec);
            methodExec.operator = op;
            return methodExec;
        });
    }

    public final <T> T a(String id, Class<? extends T> cl, Object... args) {
        return a($.the(id), cl, args);
    }

    /**
     * creates a new instance to be managed by this
     */
    public <T> T a(Term id, Class<? extends T> cl, Object... args) {

        var ccc = proxyCache.computeIfAbsent(cl, (baseClass) -> {

            var cc = bb
                    .with(TypeValidation.DISABLED)
                    .subclass(baseClass)


                    .method(ElementMatchers.isPublic().and(ElementMatchers.not(ElementMatchers.isDeclaredBy(Object.class))))
                    .intercept(MethodDelegation.to(this))
                    .make()
                    .load(
                            Thread.currentThread().getContextClassLoader(),

                            classLoadingStrategy
                    )
                    .getLoaded();

            reflect(cc);

            return cc;


        });


        try {
            var inst = (T) ccc.getConstructor(Util.typesOfArray(args)).newInstance(args);
            return register(id, inst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    @RuntimeType
    public final Object intercept(@AllArguments Object[] args, @SuperMethod Method method, @SuperCall Callable supercall, @This final Object obj) {
//        try {
            try {
                final var returned = supercall.call();
                return this.tryInvoked(obj, method, args, returned);
            } catch (final Exception e) {
                //e.printStackTrace();
                throw new RuntimeException(e);
            }
//
//        } catch (InvocationTargetException | IllegalAccessException e) {
//            logger.error("{} args={}", obj, args);
//            return null;
//        }
    }


    private <T> T register(final Term id, final T wrappedInstance) {

        this.put(new Instance(id, wrappedInstance), wrappedInstance);

        return wrappedInstance;
    }


    protected boolean evoked(final Term method, final Object instance, final Object[] params) {
        return true;
    }

    protected static Subterms validArgs(final Subterms args) {
        if (args.sub(0).op() != ATOM)
            return null;


        final var a = args.subs();
        switch (a) {

            case 1:
                return args;

            case 2: {
                final var o1 = args.sub(1).op();
                if (Opjects.validParamTerm(o1)) {
                    return args;
                }
                break;
            }

            case 3: {

                final var o1 = args.sub(1).op();
                if (Opjects.validParamTerm(o1)) {
                    final var o2 = args.sub(2).op();
                    if (o2 == VAR_DEP)
                        return args;
                }
                break;
            }
        }

        return null;
    }

    static final int VALID_PARAM_TERM = or(ATOM, INT, VAR_DEP, PROD, BOOL);
    private static boolean validParamTerm(final Op o1) {
        return o1.isAny(Opjects.VALID_PARAM_TERM);
    }


    protected static boolean validMethod(final Method m) {
        if (Opjects.methodExclusions.contains(m.getName()))
            return false;
        else {
            final var mm = m.getModifiers();

            return Modifier.isPublic(mm) && !Modifier.isStatic(mm);
        }
    }


    private static Method findMethod(final Class<?> clazz, final Predicate<Method> predicate) {


        for (var current = clazz; current != null; current = current.getSuperclass()) {


            final var methods = current.isInterface() ? current.getMethods() : current.getDeclaredMethods();
            for (final var method: methods) {
                if (predicate.test(method)) {
                    return method;
                }
            }


            for (final var ifc: current.getInterfaces()) {
                final var m = Opjects.findMethod(ifc, predicate);
                if (m != null)
                    return m;
            }
        }

        return null;
    }

    /**
     * Determine if the supplied candidate method (typically a method higher in
     * the type hierarchy) has a signature that is compatible with a method that
     * has the supplied name and parameter types, taking method sub-signatures
     * and generics into account.
     */
    private static boolean hasCompatibleSignature(final Method candidate, final String method, final Class<?>[] parameterTypes) {

        if (!method.equals(candidate.getName())) {
            return false;
        }


        if (parameterTypes.length > 0 && candidate.isVarArgs()) {
            return true;
        }
        if (parameterTypes.length != candidate.getParameterCount()) {
            return false;
        }


        final var ctp = candidate.getParameterTypes();
        if (Arrays.equals(parameterTypes, ctp)) {
            return true;
        }


        for (var i = 0; i < parameterTypes.length; i++) {
            final var lowerType = parameterTypes[i];
            final var upperType = ctp[i];
            if (!upperType.isAssignableFrom(lowerType)) {
                return false;
            }
        }


        return true;


    }


    public final Object invoke(final Object wrapper, final Object obj, final Method method, final Object[] args) {

        Object result;
        try {
            result = method.invoke(obj, args);
        } catch (final Throwable t) {
            Opjects.logger.error("{} args={}: {}", obj, args, t);
            result = t;
        }

        return this.tryInvoked(wrapper, method, args, result);
    }


    protected final @Nullable Object tryInvoked(final Object obj, final Method m, final Object[] args, final Object result) {
        if (Opjects.methodExclusions.contains(m.getName()))
            return result;


        return this.invoked(obj, m, args, result);


    }


    protected Object invoked(final Object obj, final Method m, final Object[] args, final Object result) {
        final var in = (Instance) this.objToTerm.get(obj);
        return (in == null) ?
                result :
                in.update(obj, m, args, result);

    }


    /**
     * @see org.junit.platform.commons.support.ReflectionSupport#findMethod(Class, String, Class...)
     */
    static Method findMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        Preconditions.notNull(clazz, "Class must not be null");
        Preconditions.notNull(parameterTypes, "Parameter types array must not be null");
        Preconditions.containsNoNullElements(parameterTypes, "Individual parameter types must not be null");

        return Opjects.findMethod(clazz, method -> Opjects.hasCompatibleSignature(method, methodName, parameterTypes));
    }
}
