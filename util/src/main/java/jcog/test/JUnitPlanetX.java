package jcog.test;

import jcog.TODO;
import jcog.Util;
import jcog.io.arff.ARFF;
import jcog.list.FasterList;
import org.eclipse.collections.api.tuple.Pair;
import org.junit.jupiter.api.Disabled;
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

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * JUnit5 Planet_X
 * Parallel Test Runner with Advanced Statistics collection
 * based on JUnit "Jupiter"
 */
public class JUnitPlanetX implements Launcher, EngineExecutionListener, TestExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(JUnitPlanetX.class);

    public final ARFF.ARFFObject<TestRun> results = new ARFF.ARFFObject<>(TestRun.class);

    private final Queue<Pair<TestDescriptor, JupiterEngineExecutionContext>> all = new ConcurrentLinkedQueue<>();

    
    private final Node.DynamicTestExecutor dte = testDescriptor -> {
        
    };
    private final Iterable<TestEngine> testEngines;
    private final LauncherDiscoveryRequestBuilder request;

    /**
     * Construct a new {@code DefaultLauncher} with the supplied test engines.
     *
     * @param testEngines the test engines to delegate to; never {@code null} or empty
     */
    public JUnitPlanetX() {
        this.request = LauncherDiscoveryRequestBuilder.request();
        this.testEngines = ServiceLoader.load(TestEngine.class, ClassLoaderUtils.getDefaultClassLoader());

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
                
                
                
                return true;
            }
        }
        return false;
    }


    public ARFF.ARFFObject<TestRun> report(File out) throws FileNotFoundException {
        return report(new PrintStream(new FileOutputStream(out)));
    }

    public ARFF.ARFFObject<TestRun> report(PrintStream out) {
        try {
            results.write(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        out.flush();
        return results;
    }

    public JUnitPlanetX run() {
        LauncherDiscoveryRequest req = request.build();

        

        final ExtensionRegistry reg = ExtensionRegistry
                .createRegistryWithDefaultExtensions(req.getConfigurationParameters());
        










        JupiterEngineExecutionContext ctx = new JupiterEngineExecutionContext(this, req.getConfigurationParameters()) {

            final ThrowableCollector throwCollector = new ThrowableCollector() {
                @Override
                public void execute(Executable executable) {
                    throw new UnsupportedOperationException("unused");










                }
            };

            




















            @Override
            public EngineExecutionListener getExecutionListener() {
                return JUnitPlanetX.this;
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




            }
        };

        
        



        

        


        




        

        for (TestDescriptor t : discoverRoot(req, "execution")) {
            flatten(all, t, ctx);
        }


        ForkJoinPool exe = new ForkJoinPool(Util.concurrency());


        do {
            all.removeIf(x -> {
                
                TestDescriptor test = x.getOne();
                exe.execute(() -> {
                    
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
                        
                        fail = true;
                    }
                    long end = System.nanoTime();

                    try {
                        t.after(tctx);

                        
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





                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    boolean success = !fail && !tctx.getThrowableCollector().isNotEmpty();
                    results.put(new TestRun(klass,
                            test.getParent().get() instanceof TestTemplateTestDescriptor ?
                                    "\"" + method + ":" + test.getDisplayName() + "\"" : 
                                    "\"" + method + "\"",
                            
                            
                            
                            
                            start, end - start, success));

                });

                return true;
            });

            if (all.isEmpty())
                exe.awaitQuiescence(1000, TimeUnit.SECONDS);

        } while (!all.isEmpty() || !exe.isQuiescent());


        return this;
    }

    public JUnitPlanetX test(String packagePattern) {
        request.selectors(DiscoverySelectors.selectPackage(packagePattern));
        return this;
    }

    public JUnitPlanetX test(Class... testClasses) {
        for (Class testClass : testClasses)
            request.selectors(DiscoverySelectors.selectClass(testClass));
        return this;
    }


    @Override
    public void registerTestExecutionListeners(TestExecutionListener... listeners) {
        throw new RuntimeException("ignored");


        
    }

    @Override
    public TestPlan discover(LauncherDiscoveryRequest discoveryRequest) {
        Preconditions.notNull(discoveryRequest, "LauncherDiscoveryRequest must not be null");
        return TestPlan.from(discoverRoot(discoveryRequest, "discovery"));
    }





    @Override
    public void execute(LauncherDiscoveryRequest discoveryRequest, TestExecutionListener... listeners) {
    }

    private Collection<TestDescriptor> discoverRoot(LauncherDiscoveryRequest discoveryRequest, String phase) {
        List<TestDescriptor> root = new FasterList<>();

        for (TestEngine testEngine : this.testEngines) {
            
            boolean engineIsExcluded = discoveryRequest.getEngineFilters().stream()
                    .map(engineFilter -> engineFilter.apply(testEngine))
                    .anyMatch(FilterResult::excluded);
            

            if (engineIsExcluded) {



                continue;
            }




            discoverEngineRoot(testEngine, discoveryRequest).ifPresent(root::add);
        }
        
        
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

    private void handleThrowable(TestEngine testEngine, String phase, Throwable throwable) {
        logger.error("{} {}", throwable, String.format("TestEngine with ID '%s' failed to %s tests", testEngine.getId(), phase));
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
        
    }

    @Override
    public void executionFinished(TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
        
    }

    @Override
    public void reportingEntryPublished(TestDescriptor testDescriptor, ReportEntry entry) {

    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {

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


}
