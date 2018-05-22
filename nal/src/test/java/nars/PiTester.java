package nars;

import jcog.Util;
import jcog.list.FasterList;
import nars.term.TermTest;
import nars.term.compound.CachedCompound;
import org.pitest.classinfo.ClassInfo;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.classpath.CodeSource;
import org.pitest.classpath.PathFilter;
import org.pitest.classpath.ProjectClassPaths;
import org.pitest.coverage.CoverageDatabase;
import org.pitest.coverage.CoverageGenerator;
import org.pitest.coverage.execute.CoverageOptions;
import org.pitest.coverage.execute.DefaultCoverageGenerator;
import org.pitest.coverage.export.NullCoverageExporter;
import org.pitest.functional.FCollection;
import org.pitest.functional.prelude.Prelude;
import org.pitest.mutationtest.ClassMutationResults;
import org.pitest.mutationtest.EngineArguments;
import org.pitest.mutationtest.MutationConfig;
import org.pitest.mutationtest.MutationResult;
import org.pitest.mutationtest.build.*;
import org.pitest.mutationtest.config.DefaultDependencyPathPredicate;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.config.TestPluginArguments;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationEngine;
import org.pitest.mutationtest.engine.gregor.config.GregorEngineFactory;
import org.pitest.mutationtest.execute.MutationAnalysisExecutor;
import org.pitest.mutationtest.incremental.DefaultCodeHistory;
import org.pitest.mutationtest.incremental.IncrementalAnalyser;
import org.pitest.mutationtest.incremental.ObjectOutputStreamHistoryStore;
import org.pitest.mutationtest.incremental.WriterFactory;
import org.pitest.mutationtest.statistics.MutationStatisticsListener;
import org.pitest.mutationtest.tooling.JarCreatingJarFinder;
import org.pitest.process.DefaultJavaExecutableLocator;
import org.pitest.process.JavaAgent;
import org.pitest.process.LaunchOptions;
import org.pitest.util.IsolationUtils;
import org.pitest.util.Timings;

import java.io.Reader;
import java.util.*;
import java.util.function.Predicate;

/** executes pitest
 * https://github.com/hcoles/pitest/blob/master/pitest-entry/src/test/java/org/pitest/mutationtest/TestMutationTesting.java
 * */
public class PiTester {

    public static void main(String[] args) {
        //TestResultCollector resultCollector = findTestsIn(TestClassWithTestAnnotation.class);

//        Collection<TestResult> fb = new ConcurrentLinkedQueue<>();
//        ConcreteResultCollector rc = new ConcreteResultCollector(fb);
//        new JUnit5TestUnitFinder().findTestUnits(NAL1Test.class)
//                .stream()
//                .forEach(testUnit -> testUnit.execute(rc));
//        fb.forEach(System.out::println);

        //Predicate<String> testFilter = t -> t.startsWith("nars.nal");
        Predicate<String> testFilter =
                t ->
                        //t.equals(NAL1Test.class.getName()) ||
                        t.equals(TermTest.class.getName());
        run(CachedCompound.SimpleCachedCompound.class, testFilter);
    }


    static void run(final Class<?> clazz, final Predicate<String> testFilter) {

        int concurrency = Util.concurrencyDefault(1);

        //MetaDataExtractor metaDataExtractor = new MetaDataExtractor();

        final Collection<MutationResult> results = new FasterList().asSynchronized();
        MutationStatisticsListener stats = new MutationStatisticsListener() {
            @Override
            public void handleMutationResult(ClassMutationResults r) {
                results.addAll(r.getMutations());
                super.handleMutationResult(r);
            }
        };

        MutationAnalysisExecutor mae = new MutationAnalysisExecutor(concurrency,
                List.of(
                        stats
                )
        );
//                Collections
//                        .singletonList(metaDataExtractor));

        final ReportOptions data = new ReportOptions();

        data.setNumberOfThreads(concurrency);
        data.setReportDir("/tmp/pitest");
        //data.setVerbose(true);



//        final Set<Predicate<String>> tests = Collections.singleton(Prelude
//                .isEqualTo(test.getName()));

        data.setTargetTests(Set.of(testFilter));
        data.setDependencyAnalysisMaxDistance(-1);

        final Set<String> mutees = Collections.singleton(clazz.getName() + "*");
        data.setTargetClasses(mutees);

        data.setTimeoutConstant(PercentAndConstantTimeoutStrategy.DEFAULT_CONSTANT);
        data.setTimeoutFactor(PercentAndConstantTimeoutStrategy.DEFAULT_FACTOR);

        final JavaAgent agent = new JarCreatingJarFinder();

        try {

            TestPluginArguments config = TestPluginArguments.
                    defaults().withTestPlugin("junit5");

            CoverageOptions covopt = new CoverageOptions(
                    data.getTargetClasses(),
                    data.getExcludedClasses(),
                    config,
                    data.isVerbose(), data.getDependencyAnalysisMaxDistance());

            // data.setConfiguration(this.config);
            final CoverageOptions coverageOptions = covopt;

            final LaunchOptions launchOptions = new LaunchOptions(agent,
                    new DefaultJavaExecutableLocator(), data.getJvmArgs(),
                    new HashMap<>());

            final PathFilter pf = new PathFilter(
                    Prelude.not(new DefaultDependencyPathPredicate()),
                    Prelude.not(new DefaultDependencyPathPredicate()));
            final ProjectClassPaths cps = new ProjectClassPaths(data.getClassPath(),
                    data.createClassesFilter(), pf);

            final Timings timings = new Timings();
            final CodeSource code = new CodeSource(cps);

            final CoverageGenerator coverageGenerator = new DefaultCoverageGenerator(
                    null, coverageOptions, launchOptions, code,
                    new NullCoverageExporter(),
                    timings, false);

            final CoverageDatabase coverageData = coverageGenerator.calculateCoverage();

            final Collection<ClassName> codeClasses = FCollection.map(code.getCode(),
                    ClassInfo.toClassName());

            final EngineArguments arguments = EngineArguments.arguments()
                    .withMutators(null);//Arrays.asList(mutators));

            final MutationEngine engine = new GregorEngineFactory()
                    .createEngine(arguments);

            final MutationConfig mutationConfig = new MutationConfig(engine,
                    launchOptions);

            final ClassloaderByteArraySource bas = new ClassloaderByteArraySource(
                    IsolationUtils.getContextClassLoader());

            final MutationInterceptor emptyIntercpetor =
                    CompoundMutationInterceptor.nullInterceptor();

            final MutationSource source = new MutationSource(mutationConfig,
                    new DefaultTestPrioritiser(coverageData),
                    bas, emptyIntercpetor);


            final WorkerFactory wf = new WorkerFactory(null,
                    coverageOptions.getPitConfig(), mutationConfig, arguments,
                    new PercentAndConstantTimeoutStrategy(data.getTimeoutFactor(),
                            data.getTimeoutConstant()),
                    data.isVerbose(), data.getClassPath()
                    .getLocalClassPath());


            final Optional<Reader> reader = data.createHistoryReader();
            final WriterFactory historyWriter = data.createHistoryWriter();


            final MutationTestBuilder builder = new MutationTestBuilder(wf,
                    new IncrementalAnalyser(new DefaultCodeHistory(code, new ObjectOutputStreamHistoryStore(historyWriter, reader)), coverageData) {

                        @Override
                        public Collection<MutationResult> analyse(Collection<MutationDetails> mutation) {
                            Collection<MutationResult> r = super.analyse(mutation);
                            return r;
                        }
                    },
                    //new NullAnalyser(),
                    source, new DefaultGrouper(0));

            final List<MutationAnalysisUnit> tus = builder
                    .createMutationTestUnits(codeClasses);

            mae.run(tus);




            results.forEach(m -> {

                System.out.println(m);
            });
            stats.getStatistics().report(System.out);
            System.out.println("coverage=" + coverageData.createSummary().getCoverage() + "%");

        } finally {
            agent.close();
        }
    }


}
