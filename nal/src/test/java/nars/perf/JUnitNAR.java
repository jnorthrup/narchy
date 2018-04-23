package nars.perf;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.Util;
import jcog.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.util.NALTest;
import org.eclipse.collections.api.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
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

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * JUnit wrappers and runners
 */
public class JUnitNAR {
    public static void main(String[] args) {
        junit(NARTestBenchmark.tests);
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
                .build();

        Launcher launcher =
                //LauncherFactory.create();
                new BetterThanDefaultLauncher(request);

//        //SummaryGeneratingListener listener = new SummaryGeneratingListener();
        //LoggingListener listener = LoggingListener.forJavaUtilLogging();
        //launcher.registerTestExecutionListeners(listener);
//
//        //listener.getSummary().printTo(new PrintWriter(System.out));
    }


    static class BetterThanDefaultLauncher implements Launcher, EngineExecutionListener, TestExecutionListener {

        private static final Logger logger = LoggerFactory.getLogger(BetterThanDefaultLauncher.class);
        final Queue<Pair<TestDescriptor, JupiterEngineExecutionContext>> all = new ConcurrentLinkedQueue<>();
        final JupiterEngineExecutionContext ctx;
        //private final TestExecutionListenerRegistry listenerRegistry = new TestExecutionListenerRegistry();
        private final Iterable<TestEngine> testEngines;
        ThrowableCollector throwCollector = new ThrowableCollector() {
            @Override
            public void execute(Executable executable) {
                ForkJoinPool.commonPool().submit(() -> {
                    try {
                        System.out.println(Thread.currentThread() + " exe " + executable);
                        executable.execute();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });

            }
        };
        Node.DynamicTestExecutor dte = testDescriptor -> System.err.println("TODO: dynamic: " + testDescriptor);


        /**
         * Construct a new {@code DefaultLauncher} with the supplied test engines.
         *
         * @param testEngines the test engines to delegate to; never {@code null} or empty
         */
        BetterThanDefaultLauncher(LauncherDiscoveryRequest request) {
            this.testEngines = ServiceLoader.load(TestEngine.class, ClassLoaderUtils.getDefaultClassLoader());
            final ExtensionRegistry reg = ExtensionRegistry.createRegistryWithDefaultExtensions(request.getConfigurationParameters());

            BeforeEachCallback before = context -> System.out.println("before: " + context);

            AfterTestExecutionCallback after = context -> System.out.println("after: " + context);

            reg.registerExtension(before, before);
            reg.registerExtension(after, after);


            ctx = new JupiterEngineExecutionContext(this, request.getConfigurationParameters()) {

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

            execute(request/*, listener*/);
        }

        @Override
        public void registerTestExecutionListeners(TestExecutionListener... listeners) {
            throw new RuntimeException("ignored");
//            Preconditions.notEmpty(listeners, "listeners array must not be null or empty");
//            Preconditions.containsNoNullElements(listeners, "individual listeners must not be null");
            //this.listenerRegistry.registerListeners(listeners);
        }

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
                    discoverRoot(discoveryRequest, "execution"),
                    discoveryRequest.getConfigurationParameters());
        }

//        TestExecutionListenerRegistry getTestExecutionListenerRegistry() {
//            return listenerRegistry;
//        }

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

        private void executeTests(Collection<TestDescriptor> root, ConfigurationParameters configurationParameters) {

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

            all.removeIf(x -> {
                logger.info("deq {}", x);
                if (x.getOne() instanceof TestMethodTestDescriptor) {
                    exe.execute(() -> {
                        logger.info("exe {}", x);
                        JupiterEngineExecutionContext tctx = x.getTwo();
                        try {
                            tctx = ((JupiterTestDescriptor) x.getOne()).prepare(tctx);
                            ((Node<JupiterEngineExecutionContext>) x.getOne()).execute(tctx, dte);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    throw new UnsupportedOperationException("unknown test descriptor type: " + x);
                }
                return true;
            });



            exe.awaitQuiescence(10000, SECONDS);
            logger.info("done");

        }

        private static void flatten(Queue<Pair<TestDescriptor, JupiterEngineExecutionContext>> target, TestDescriptor t, JupiterEngineExecutionContext ctx) {
            if (t.isContainer()) {
                if (t instanceof ClassTestDescriptor) {
                    ctx = ((ClassTestDescriptor) t).prepare(ctx);
                }
                JupiterEngineExecutionContext subCTX = ctx;
                t.getChildren().forEach(z -> flatten(target, z, subCTX));
            } else {
                try {
                    target.add(pair(t, ((JupiterTestDescriptor) t).prepare(ctx)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
            logger.error("{} {}", throwable,String.format("TestEngine with ID '%s' failed to %s tests", testEngine.getId(), phase));
            BlacklistedExceptions.rethrowIfBlacklisted(throwable);
        }

        @Override
        public void dynamicTestRegistered(TestDescriptor testDescriptor) {

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
