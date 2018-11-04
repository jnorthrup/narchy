package nars.op.java;

import com.google.common.collect.Sets;
import jcog.Paper;
import jcog.Skill;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.data.map.CustomConcurrentHashMap;
import jcog.math.FloatRange;
import jcog.memoize.SoftMemoize;
import nars.*;
import nars.concept.Concept;
import nars.concept.Operator;
import nars.control.DurService;
import nars.control.channel.CauseChannel;
import nars.subterm.Subterms;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.util.AtomicExec;
import nars.util.Timed;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.junit.platform.commons.util.Preconditions;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static jcog.data.map.CustomConcurrentHashMap.*;
import static nars.Op.*;
import static nars.truth.TruthFunctions.c2w;
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
 */
@Paper
@Skill({"Metaprogramming", "Reinforcement_learning"})
public class Opjects extends DefaultTermizer {

    final static org.slf4j.Logger logger = LoggerFactory.getLogger(Opjects.class);
    final static ClassLoadingStrategy classLoadingStrategy =

            ClassLoadingStrategy.Default.WRAPPER;

    final ByteBuddy bb = new ByteBuddy();

    public final FloatRange exeThresh = new FloatRange(0.75f, 0.5f, 1f);
    private final DurService on;

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

    /**
     * cached; updated at most each duration
     */
    private float beliefEvi = 0, doubtEvi = 0, beliefPri = 0, invokeEvi, uninvokeEvi, invokePri;

    /**
     * set of operators in probing mode which are kept here for batched execution
     * should be a set.   using ConcurrentFastIteratingHashMap instead of the Set because it has newer code updates
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
    final static ThreadLocal<AtomicBoolean> evoking = ThreadLocal.withInitial(AtomicBoolean::new);


    public final NAR nar;

    /**
     * for externally-puppeted method invocation goals
     */


    protected final CauseChannel<ITask> in;

    private final SoftMemoize<Pair<Pair<Class, Term>, List<Class<?>>>, MethodHandle> methodCache = new SoftMemoize<>((x) -> {

        Class c = x.getOne().getOne();
        Term methodTerm = x.getOne().getTwo();
        List<Class<?>> types = x.getTwo();

        String mName = methodTerm.toString();
        Class<?>[] cc = types.isEmpty() ? ArrayUtils.EMPTY_CLASS_ARRAY : ((FasterList<Class<?>>) types).array();
        Method m = findMethod(c, mName, cc);
        if (m == null || !methodEvokable(m))
            return null;

        m.setAccessible(true);
        try {
            MethodHandle mh = MethodHandles.lookup().unreflect(m);
            return mh;

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }, 512, STRONG, SOFT);


    /**
     * whether NAR can envoke the method internally
     */
    public boolean methodEvokable(Method m) {
        return isPublic(m);
    }


    static boolean isPublic(Method m) {
        int mm = m.getModifiers();
        return Modifier.isPublic(mm);
    }

    public Opjects(NAR n) {
        in = (nar = n).newChannel(this);
        update(n);
        this.on = DurService.on(n, this::update);
    }

    /**
     * called every duration to update all the operators in one batch, so they dont register events individually
     */
    protected void update(NAR nar) {
        float cMin = c2w(nar.confMin.floatValue());
        float cMax = c2w(nar.confDefault(BELIEF));
        beliefEvi = Util.lerp(beliefEviFactor, cMin, cMax);
        doubtEvi = Util.lerp(doubtEviFactor, cMin, cMax);
        invokeEvi = Util.lerp(invokeEviFactor, cMin, cMax);
        uninvokeEvi = Util.lerp(uninvokeEviFactor, cMin, cMax);
        invokePri = beliefPri = nar.priDefault(BELIEF);

        probing.forEach(p -> p.update(nar));
    }

    @Override
    protected Term classInPackage(Term classs, Term packagge) {
        Term t = $.inst(classs, packagge);


        return t;
    }


    /**
     * registers an alias/binding shortcut term rewrite macro
     */
    public Concept alias(String op, Term instance, String method) {
        return nar.on(op, (s) ->
                $.func(method, instance, s.subs() == 1 ? s.sub(0) : PROD.the(s))
        );
    }


    interface InstanceMethodValueModel {

        void update(Term instance, Object obj, Method method, Object[] args, Object nextValue, NAR nar);
    }


    final InstanceMethodValueModel pointTasks = new PointMethodValueModel();
    final Function<Term, InstanceMethodValueModel> valueModel = (x) -> pointTasks /* memoryless */;

    public class PointMethodValueModel implements InstanceMethodValueModel {


        @Override
        public void update(Term instance, Object obj, Method method, Object[] args, Object nextValue, NAR nar) {



            long now = nar.time();
            int dur = nar.dur();
            long start = now - dur / 2;
            long end = now + dur / 2;

            float f = beliefFreq;

            Class<?> methodReturnType = method.getReturnType();
            boolean isVoid = methodReturnType == void.class;

            NALTask value;

            if (!isVoid) {
                Term t = opTerm(instance, method, args, nextValue);
                value = value(t, f, start, end, nar);
            } else {
                value = null;
            }

            boolean evokedOrInvoked = evoking.get().get();

            NALTask feedback;
            if (isVoid || evokedOrInvoked) {
                feedback = feedback(opTerm(instance, method, args, isVoid ? null : $.varDep(1)), start, end, nar);
            } else {
                feedback = null;
            }


            if (feedback == null)
                in.input(value);
            else
                in.input(feedback, value);


        }

        public NALTask feedback(Term nt, long start, long end, NAR nar) {

            NALTask feedback =
//                new TruthletTask(nt, BELIEF,
//                    Truthlet.step(
//                            uninvokeFreq, start,
//                            invokeFreq, invokeEvi,
//                            end, uninvokeFreq,
//                            uninvokeEvi
//                    ), nar);
                new NALTask(nt, BELIEF, PreciseTruth.byEvi(invokeFreq, invokeEvi), start, start, end, nar.evidence());
            if (Param.DEBUG) feedback.log("Invoked");
            feedback.priMax(beliefPri);
            return feedback;
        }


        public NALTask value(Term nextTerm, float freq, long start, long end, NAR nar) {
            Term nt = nextTerm;
            if (nt.op() == NEG) {
                nt = nt.unneg();
                freq = 1 - freq;
            }

            NALTask value =
//                new TruthletTask(nt, BELIEF,
//                    Truthlet.step(
//                            doubtFreq, start,
//                            f, beliefEvi,
//                            end, doubtFreq,
//                            doubtEvi
//                    ),
//                    nar);
                    new NALTask(nt, BELIEF, PreciseTruth.byEvi(freq, beliefEvi), start, start, end, nar.evidence());

            if (Param.DEBUG) value.log("Invoke Result");

            value.priMax(beliefPri);
            return value;
        }
    }


    /**
     * this term should not be used in constructing terms that will leave this class.
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


        Class<?> returnType = method.getReturnType();
        boolean isVoid = result == null && returnType == void.class;
        boolean isBoolean = returnType == boolean.class || (returnType == Boolean.class && result!=null);

        int xn = 3;
        if (args.length == 0) {
            xn--;
        }
        if (isVoid || isBoolean) {
            xn--;
        }

        Term[] x = new Term[xn];
        int resultTerm = xn - 1;

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

        boolean negate = false;

        if (result instanceof Term) {
            Term tr = (Term) result;
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
        String n = method.getName();
        int i = n.indexOf("$accessor$");
        if (i != -1) {
            return n.substring(0, i);
        } else {
            return n;
        }
    }

    private class MethodExec extends AtomicExec implements BiConsumer<Term, Timed> {
        private final Term methodName;
        public Operator operator;

        /**
         * TODO invalidate any entries from here if an instance is registered after it has
         * been running which may cause it to be ignored once it becomes available.
         * maybe use a central runCache to make this easier
         */
        final SoftMemoize<Term, Runnable> runCache;


        public MethodExec(String _methodName) {
            super(null, Opjects.this.exeThresh);

            this.methodName = $.the(_methodName);

            runCache = new SoftMemoize<>((term) -> {
                Subterms args = validArgs(Operator.args(term));
                if (args == null)
                    return null;

                Term instanceTerm = args.sub(0);
                Object instance = termToObj.get(instanceTerm);
                if (instance == null)
                    return null;

                int as = args.subs();
                Term methodArgs = as > 1 && (as > 2 || !args.sub(as - 1).op().var) ? args.sub(1) : Op.EmptyProduct;

                boolean maWrapped = methodArgs.op() == PROD;

                int aa = maWrapped ? methodArgs.subs() : 1;

                Object[] instanceAndArgs;
                List<Class<?>> types;
                if (aa == 0) {
                    instanceAndArgs = new Object[]{instance};
                    types = Collections.emptyList();
                } else {
                    instanceAndArgs = object(instance, maWrapped ? methodArgs.subterms().arrayShared() : new Term[]{methodArgs});
                    types = Util.typesOf(instanceAndArgs, 1 /* skip leading instance value */, instanceAndArgs.length);
                }


                Class c = instance.getClass();

                Pair<Pair<Class, Term>, List<Class<?>>> key = pair(pair(c, methodName), types);
                MethodHandle mh = methodCache.apply(key);
                if (mh == null) {

                    return null;
                }

                return () -> {

                    AtomicBoolean flag = evoking.get();
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

            }, 512, STRONG, SOFT);
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
        protected void disable() {
            probing.remove(this);
        }

        @Override
        protected Task exePrefilter(Task x) {

            if (!x.isCommand() && x.freq() <= 0.5f + Param.TRUTH_EPSILON)
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
     */
    public <T> T the(Term id, T instance, Object... args) {

        reflect(instance.getClass());

        try {
            Class cl = bb
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
            T instWrapped = (T) cl.getConstructor(Util.typesOfArray(args)).newInstance(args);

            register(id, instWrapped);

            return instWrapped;


        } catch (Throwable e) {
            throw new RuntimeException(e);
        }


    }

    private void reflect(Class<?> cl) {

        clCache.computeIfAbsent(cl, (clazz) -> {

            for (Method m: clazz.getMethods()) {
                reflect(m);
            }

            return true;
        });
    }

    private MethodExec reflect(Method m) {
        if (!validMethod(m))
            return null;

        m.setAccessible(true);

        String n = m.getName();
        return opCache.computeIfAbsent(n, (mn) -> {
            MethodExec methodExec = new MethodExec(mn);
            Operator op = nar.onOp(mn, methodExec);
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

        Class ccc = proxyCache.computeIfAbsent(cl, (baseClass) -> {

            Class cc = bb
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
            T inst = (T) ccc.getConstructor(Util.typesOfArray(args)).newInstance(args);
            return register(id, inst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    @RuntimeType
    public Object intercept(@AllArguments Object[] args, @SuperMethod Method method, @This Object obj) {
        try {
            return tryInvoked(obj, method, args, method.invoke(obj, args));
        } catch (InvocationTargetException | IllegalAccessException e) {
            logger.error("{} args={}", obj, args);
            return null;
        }
    }


    private <T> T register(Term id, T wrappedInstance) {

        put(new Instance(id, wrappedInstance), wrappedInstance);

        return wrappedInstance;
    }


    protected boolean evoked(Term method, Object instance, Object[] params) {
        return true;
    }

    protected Subterms validArgs(Subterms args) {
        if (args.sub(0).op() != ATOM)
            return null;


        int a = args.subs();
        switch (a) {
            case 1:
                return args;
            case 2: {
                Op o1 = args.sub(1).op();
                if (validParamTerm(o1)) {


                    return args;
                }
                break;
            }

            case 3: {

                Op o1 = args.sub(1).op();
                Op o2 = args.sub(2).op();
                if (validParamTerm(o1) && o2 == VAR_DEP)
                    return args;
                break;
            }
        }

        return null;
    }

    private boolean validParamTerm(Op o1) {
        return o1 == VAR_DEP || o1 == PROD || (o1.atomic && !o1.var);
    }


    protected boolean validMethod(Method m) {
        if (methodExclusions.contains(m.getName()))
            return false;

        int mm = m.getModifiers();
        if (!Modifier.isPublic(mm))
            return false;
        return !Modifier.isStatic(mm);
    }


    private static Method findMethod(Class<?> clazz, Predicate<Method> predicate) {


        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {


            Method[] methods = current.isInterface() ? current.getMethods() : current.getDeclaredMethods();
            for (Method method: methods) {
                if (predicate.test(method)) {
                    return method;
                }
            }


            for (Class<?> ifc: current.getInterfaces()) {
                Method m = findMethod(ifc, predicate);
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
    private static boolean hasCompatibleSignature(Method candidate, String method, Class<?>[] parameterTypes) {

        if (!method.equals(candidate.getName())) {
            return false;
        }


        if (parameterTypes.length > 0 && candidate.isVarArgs()) {
            return true;
        }
        if (parameterTypes.length != candidate.getParameterCount()) {
            return false;
        }


        Class<?>[] ctp = candidate.getParameterTypes();
        if (Arrays.equals(parameterTypes, ctp)) {
            return true;
        }


        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> lowerType = parameterTypes[i];
            Class<?> upperType = ctp[i];
            if (!upperType.isAssignableFrom(lowerType)) {
                return false;
            }
        }


        return true;


    }


    public final Object invoke(Object wrapper, Object obj, Method method, Object[] args) {

        Object result;
        try {
            result = method.invoke(obj, args);
        } catch (Throwable t) {
            logger.error("{} args={}: {}", obj, args, t);
            result = t;
        }

        return tryInvoked(wrapper, method, args, result);
    }


    @Nullable
    protected final Object tryInvoked(Object obj, Method m, Object[] args, Object result) {
        if (methodExclusions.contains(m.getName()))
            return result;


        return invoked(obj, m, args, result);


    }


    protected Object invoked(Object obj, Method m, Object[] args, Object result) {
        Instance in = (Instance) objToTerm.get(obj);
        return (in == null) ?
                result :
                in.update(obj, m, args, result);

    }


    /**
     * @see org.junit.platform.commons.support.ReflectionSupport#findMethod(Class, String, Class...)
     */
    static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Preconditions.notNull(clazz, "Class must not be null");
        Preconditions.notNull(parameterTypes, "Parameter types array must not be null");
        Preconditions.containsNoNullElements(parameterTypes, "Individual parameter types must not be null");

        return findMethod(clazz, method -> hasCompatibleSignature(method, methodName, parameterTypes));
    }
}
