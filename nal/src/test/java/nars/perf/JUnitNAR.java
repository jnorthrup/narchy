package nars.perf;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.TODO;
import jcog.Util;
import jcog.io.ARFF;
import jcog.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.nal.nal4.NAL4Test;
import nars.util.NALTest;
import org.eclipse.collections.api.tuple.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.engine.descriptor.*;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.execution.ThrowableCollector;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.commons.util.BlacklistedExceptions;
import org.junit.platform.commons.util.ClassLoaderUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.*;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.hierarchical.Node;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.eclipse.collections.impl.tuple.Tuples.pair;
import static org.junit.jupiter.engine.Constants.EXTENSIONS_AUTODETECTION_ENABLED_PROPERTY_NAME;

/**
 * JUnit wrappers and runners
 */
public class JUnitNAR {

    public static void main(String[] args) throws IOException {
        //junit(NARTestBenchmark.tests);
        junit(NAL4Test.class);
    }

    /**
     * HACK runs all Junit test methods, summing the scores.
     * TODO use proper JUnit5 test runner api but it is a mess to figure out right now
     */
    public static float tests(Executor exe, Supplier<NAR> s, Class<? extends NALTest>... c) {


        List<Method> methods = Stream.of(c)
                .flatMap(cc -> Stream.of(cc.getMethods())
                        .filter(x -> x.getAnnotation(Test.class) != null))
                .collect(toList());

        final CountDownLatch remain = new CountDownLatch(methods.size());
        final AtomicDouble sum = new AtomicDouble(0);
        methods.forEach(m -> exe.execute(() -> {
            try {
                sum.addAndGet(test(s, m));
            } finally {
                remain.countDown();
            }
        }));
        try {
            remain.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return sum.floatValue();
    }

    private static float test(Supplier<NAR> s, Method m) {
        try {
            NALTest t = (NALTest) m.getDeclaringClass().getConstructor().newInstance();
            t.test.set(s.get()); //overwrite NAR with the supplier
            t.test.nar.random().setSeed(
                    System.nanoTime()
                    //1 //should change on each iteration so constant value wont work
            );
            try {
                m.invoke(t);
            } catch (Throwable ee) {
                return -1; //fatal setup
            }

            Param.DEBUG = false;

            try {
                t.test.test(false);
                return t.test.score;
                //return 1 + t.test.score; //+1 for successful completion
            } catch (Throwable ee) {
                //return -2f;
                //return -1f;
                return 0.0f; //fatal during test
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0f;
        }
    }

    /**
     * alternate, less flexible due to JUnit 5's unfortunately contaminated unworkable API
     */
    public static void junit(Class... testClasses) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()

                .selectors(
                        //selectPackage("com.example.mytests"),
                        (ClassSelector[]) Util.map(
                                DiscoverySelectors::selectClass,
                                new ClassSelector[testClasses.length], testClasses)

                        //selectClass(FastCompoundNAL1Test.class)
                )

                // .filters( includeClassNamePatterns(".*Tests")  )

                //.selectors(DiscoverySelectors.selectPackage("nars"))

                .configurationParameter(EXTENSIONS_AUTODETECTION_ENABLED_PROPERTY_NAME, "true")
                .build();

        PlanetXLauncher launcher =
                //LauncherFactory.create();
                new PlanetXLauncher(request);


//        //SummaryGeneratingListener listener = new SummaryGeneratingListener();
        //LoggingListener listener = LoggingListener.forJavaUtilLogging();
        //launcher.registerTestExecutionListeners(listener);
//
//        //listener.getSummary().printTo(new PrintWriter(System.out));
    }

    static class TestRun {
        public final String klass;
        public final String method;
        public final long startedAt;
        public final long wallTime;
        public final boolean success;

        TestRun(String klass, String method, long startedAt, long wallTime, boolean success) {
            this.klass = klass;
            this.method = method;
            this.startedAt = startedAt;
            this.wallTime = wallTime;
            this.success = success;
        }
    }

    static class PlanetXLauncher implements Launcher, EngineExecutionListener, TestExecutionListener {

        private static final Logger logger = LoggerFactory.getLogger(PlanetXLauncher.class);

        public final ARFF.ARFFObject<TestRun> results = new ARFF.ARFFObject<>(TestRun.class);

        final Queue<Pair<TestDescriptor, JupiterEngineExecutionContext>> all = new ConcurrentLinkedQueue<>();
        final JupiterEngineExecutionContext ctx;

        //private final TestExecutionListenerRegistry listenerRegistry = new TestExecutionListenerRegistry();
        final Node.DynamicTestExecutor dte = testDescriptor -> {
            System.err.println("TODO: dynamic: " + testDescriptor);
        };
        private final Iterable<TestEngine> testEngines;


        /**
         * Construct a new {@code DefaultLauncher} with the supplied test engines.
         *
         * @param testEngines the test engines to delegate to; never {@code null} or empty
         */
        PlanetXLauncher(LauncherDiscoveryRequest request) {
            this.testEngines = ServiceLoader.load(TestEngine.class, ClassLoaderUtils.getDefaultClassLoader());

            final ExtensionRegistry reg = ExtensionRegistry
                    .createRegistryWithDefaultExtensions(request.getConfigurationParameters());
            //reg.registerExtension(ParameterizedTestExtension.class);
//            reg.registerExtension(new ParameterizedTestExtension(), this);

//            BeforeEachCallback before =
//                    context -> System.out.println("before: " + context);
//            reg.registerExtension(before, before);
//
//            AfterTestExecutionCallback after =
//                    context -> System.out.println("after: " + context);
//            reg.registerExtension(after, after);


            ctx = new JupiterEngineExecutionContext(this, request.getConfigurationParameters()) {
                final ThrowableCollector throwCollector = new ThrowableCollector() {
                    @Override
                    public void execute(Executable executable) {
                        throw new UnsupportedOperationException("unused");
//                        ForkJoinPool.commonPool().submit(() -> {
//                            try {
//                                //System.out.println(Thread.currentThread() + " exe " + executable);
//                                executable.execute();
//                                //onSuccess(executable)
//                            } catch (Throwable throwable) {
//                                throwable.printStackTrace();
//                            }
//                        });

                    }
                };

                //                @Override
//                public TestInstanceProvider getTestInstanceProvider() {
//                    return new TestInstanceProvider() {
//                        @Override
//                        public Object getTestInstance(Optional<ExtensionRegistry> reg) {
//                            TestSource t = x.getSource().get();
//                            if (t instanceof org.junit.platform.engine.support.descriptor.MethodSource) {
//                                try {
//
//                                    Object inst = Class.forName(((org.junit.platform.engine.support.descriptor.MethodSource) t).getClassName())
//                                            .newInstance();
//                                    return inst;
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                            throw new UnsupportedOperationException();
//                        }
//                    };
//                }

                @Override
                public EngineExecutionListener getExecutionListener() {
                    return PlanetXLauncher.this;
                }

                @Override
                public ThrowableCollector getThrowableCollector() {
                    return throwCollector;
                }

                @Override
                public ExtensionRegistry getExtensionRegistry() {
                    return reg;
                }


                @Override
                public ExtensionContext getExtensionContext() {
                    return null;
//                            return new MethodExtensionContext(null,
//                                    getExecutionListener(), (TestMethodTestDescriptor)x,
//                                    getConfigurationParameters(), getTestInstanceProvider().getTestInstance(null),
//                                    getThrowableCollector());
                }
            };

            execute(request);
        }

        private static void flatten(Queue<Pair<TestDescriptor, JupiterEngineExecutionContext>> target, TestDescriptor t, JupiterEngineExecutionContext ctx) {


            if (t.isContainer()) {

                if (t instanceof ClassTestDescriptor && isDisabled(((ClassTestDescriptor) t).getTestClass().getAnnotations()))
                    return;

                JupiterEngineExecutionContext subCTX;
                if (t instanceof JupiterTestDescriptor) {
                    try {
                        subCTX = ((JupiterTestDescriptor) t).prepare(ctx);
                    } catch (Exception e) {
                        e.printStackTrace();
                        subCTX = ctx;
                    }
                } else {
                    subCTX = ctx;
                }

                if (t instanceof TestTemplateTestDescriptor && ctx.getExtensionContext() != null) {
                    target.add(pair(t, subCTX));
//                    Stream<TestTemplateInvocationContext> s = ParameterizedTestExtension.getTestTemplateInvocationContexts(
//                            ((TestTemplateTestDescriptor)t).getTestMethod(), subCTX.getExtensionContext());
//                    List<TestTemplateInvocationContext> dyn = s.collect(toList());
//                    if (!dyn.isEmpty()) {
//                        //System.out.println(dyn);
//                        dyn.forEach(ic -> {
//                            t.ex
//                        });
//                    }
                }

                JupiterEngineExecutionContext finalSubCTX = subCTX;
                t.getChildren().forEach(z -> flatten(target, z, finalSubCTX));
            } else {

                if (t instanceof TestMethodTestDescriptor && isDisabled(((TestMethodTestDescriptor) t).getTestMethod().getAnnotations()))
                    return;

                try {
                    target.add(pair(t, t instanceof JupiterTestDescriptor ? ((JupiterTestDescriptor) t).prepare(ctx) : ctx));
                } catch (Exception e) {
                    e.printStackTrace();
                }


                Set<? extends TestDescriptor> descendents = t.getDescendants();
                if (!descendents.isEmpty()) {
                    throw new TODO();
                }
                if (t.mayRegisterTests()) {
                    throw new TODO();
                }
            }

        }

        private static boolean isDisabled(Annotation[] annotations) {
            for (Annotation a : annotations) {
                if (a.annotationType() == Disabled.class) {
                    //@Disabled
                    // "When applied at the method level, the presence of this annotation does not
                    // prevent the test class from being instantiated."
                    return true;
                }
            }
            return false;
        }

        @Override
        public void registerTestExecutionListeners(TestExecutionListener... listeners) {
            throw new RuntimeException("ignored");
//            Preconditions.notEmpty(listeners, "listeners array must not be null or empty");
//            Preconditions.containsNoNullElements(listeners, "individual listeners must not be null");
            //this.listenerRegistry.registerListeners(listeners);
        }

//        TestExecutionListenerRegistry getTestExecutionListenerRegistry() {
//            return listenerRegistry;
//        }

        @Override
        public TestPlan discover(LauncherDiscoveryRequest discoveryRequest) {
            Preconditions.notNull(discoveryRequest, "LauncherDiscoveryRequest must not be null");
            return TestPlan.from(discoverRoot(discoveryRequest, "discovery"));
        }

        @Override
        public void execute(LauncherDiscoveryRequest discoveryRequest, TestExecutionListener... listeners) {
            Preconditions.notNull(discoveryRequest, "LauncherDiscoveryRequest must not be null");
            //Preconditions.notNull(listeners, "TestExecutionListener array must not be null");
            if (listeners.length > 0) {
                throw new RuntimeException("ignoring listeners: " + Arrays.toString(listeners));
            }
            //Preconditions.containsNoNullElements(listeners, "individual listeners must not be null");
            executeTests(
                    discoverRoot(discoveryRequest, "execution"));
        }

        private Collection<TestDescriptor> discoverRoot(LauncherDiscoveryRequest discoveryRequest, String phase) {
            List<TestDescriptor> root = new FasterList<>();

            for (TestEngine testEngine : this.testEngines) {
                // @formatter:off
                boolean engineIsExcluded = discoveryRequest.getEngineFilters().stream()
                        .map(engineFilter -> engineFilter.apply(testEngine))
                        .anyMatch(FilterResult::excluded);
                // @formatter:on

                if (engineIsExcluded) {
//                    logger.debug(() -> String.format(
//                            "Test discovery for engine '%s' was skipped due to an EngineFilter in phase '%s'.",
//                            testEngine.getId(), phase));
                    continue;
                }

//                logger.debug(() -> String.format("Discovering tests during Launcher %s phase in engine '%s'.", phase,
//                        testEngine.getId()));

                Optional<TestDescriptor> engineRoot = discoverEngineRoot(testEngine, discoveryRequest);
                engineRoot.ifPresent(root::add);
            }
            //root.applyPostDiscoveryFilters(discoveryRequest);
            //root.prune();
            return root;
        }

        private Optional<TestDescriptor> discoverEngineRoot(TestEngine testEngine,
                                                            EngineDiscoveryRequest discoveryRequest) {

            UniqueId uniqueEngineId = UniqueId.forEngine(testEngine.getId());
            try {
                TestDescriptor engineRoot = testEngine.discover(discoveryRequest, uniqueEngineId);
                Preconditions.notNull(engineRoot,
                        () -> String.format(
                                "The discover() method for TestEngine with ID '%s' must return a non-null root TestDescriptor.",
                                testEngine.getId()));
                return Optional.of(engineRoot);
            } catch (Throwable throwable) {
                handleThrowable(testEngine, "discover", throwable);
                return Optional.empty();
            }
        }

        private void executeTests(Collection<TestDescriptor> root) {

//            TestExecutionListenerRegistry listenerRegistry = buildListenerRegistryForExecution(listeners);
//            TestPlan testPlan = TestPlan.from(root);//.getEngineDescriptors());
//            TestExecutionListener testExecutionListener = this; //listenerRegistry.getCompositeTestExecutionListener();
            //testExecutionListener.testPlanExecutionStarted(testPlan);
//            ExecutionListenerAdapter engineExecutionListener = new ExecutionListenerAdapter(testPlan,
//                    testExecutionListener);


            //TestDescriptor testDescriptor = root;//.getTestDescriptorFor(testEngine);

            for (TestDescriptor t : root) {
                flatten(all, t, ctx);
            }


            ForkJoinPool exe = new ForkJoinPool(Util.concurrencyDefault());


            do {
                all.removeIf(x -> {
                    //logger.info("deq {}", x);
                    TestDescriptor test = x.getOne();
                    exe.execute(() -> {
                        //logger.info("exe {}", x);
                        JupiterEngineExecutionContext tctx = x.getTwo();


                        Class<?> c = tctx.getExtensionContext().getTestClass().get();
                        String klass = c.getName();

                        Method m = tctx.getExtensionContext().getTestMethod().get();

                        String method = m.getName();


                        boolean fail = false;
                        Node<JupiterEngineExecutionContext> t = (Node<JupiterEngineExecutionContext>) test;
                        try {
                            t.before(tctx);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                        long start = System.nanoTime();
                        try {
                            t.execute(tctx, dte);


                        } catch (Throwable e) {
                            //e.printStackTrace();
                            fail = true;
                        }
                        long end = System.nanoTime();

                        try {
                            t.after(tctx);

                            //dynamic tests are descendants
                            if (!((TestDescriptor) t).getDescendants().isEmpty()) {
                                ((TestDescriptor) t).getDescendants().forEach(d -> {
                                    try {
                                        JupiterEngineExecutionContext tctx2 = ((TestTemplateInvocationTestDescriptor) d).prepare(tctx);
                                        all.add(pair(d, tctx2));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                });
                            }

//                            if (!((TestDescriptor)t).getChildren().isEmpty())
//                                System.out.println(((TestDescriptor)t).getChildren());


                            //t.cleanUp(tctx);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        boolean success = !fail && !tctx.getThrowableCollector().isNotEmpty();
                        results.put(new TestRun(klass,
                                test.getUniqueId() + " " + test.getDisplayName(),
                                //test.getUniqueId().toString(),
                                //test.getDisplayName() + "/" + tctx.getExtensionContext().getDisplayName(), //getUniqueId().toString(),
                                //test.getDisplayName() + " "  + tctx.getExtensionContext().getUniqueId(),
                                start, end - start, success));

                    });

                    return true;
                });

                if (all.isEmpty())
                    exe.awaitQuiescence(5000, TimeUnit.SECONDS);

            } while (!all.isEmpty() || !exe.isQuiescent());



            try {
                results.write(System.out);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

//        private TestExecutionListenerRegistry buildListenerRegistryForExecution(TestExecutionListener... listeners) {
//            if (listeners.length == 0) {
//                return this.listenerRegistry;
//            }
//            TestExecutionListenerRegistry registry = new TestExecutionListenerRegistry(this.listenerRegistry);
//            registry.registerListeners(listeners);
//            return registry;
//        }

//        private void execute(TestEngine testEngine, ExecutionRequest executionRequest) {
//            try {
//                testEngine.execute(executionRequest);
//            }
//            catch (Throwable throwable) {
//                handleThrowable(testEngine, "execute", throwable);
//            }
//        }

        private void handleThrowable(TestEngine testEngine, String phase, Throwable throwable) {
            logger.error("{} {}", throwable, String.format("TestEngine with ID '%s' failed to %s tests", testEngine.getId(), phase));
            BlacklistedExceptions.rethrowIfBlacklisted(throwable);
        }

        @Override
        public void dynamicTestRegistered(TestDescriptor testDescriptor) {
            System.err.println("dyn: " + testDescriptor);
        }

        @Override
        public void executionSkipped(TestDescriptor testDescriptor, String reason) {

        }

        @Override
        public void executionStarted(TestDescriptor testDescriptor) {
            System.out.println("started: " + testDescriptor);
        }

        @Override
        public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
            System.out.println("finished : " + testDescriptor);
        }

        @Override
        public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {

        }

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {

        }


    }

}
