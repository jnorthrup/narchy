package nars;

import com.google.common.reflect.Reflection;
import jcog.TODO;
import jcog.Texts;
import jcog.table.DataTable;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.RemoteNode;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.pitest.classinfo.ClassName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.columns.numbers.NumberColumnFormatter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.GZIPOutputStream;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static tech.tablesaw.aggregate.AggregateFunctions.countFalse;
import static tech.tablesaw.aggregate.AggregateFunctions.mean;

/** periodically, or event-triggered: runs unit tests and broadcasts results */
public class SelfTest {

    /** local launcher */
    public static void main(String[] args) {
        SelfTest s = new SelfTest();
//        s.unitTestsByPackage("nars.nal.nal1");
//        s.unitTestsByPackage("nars.nal.nal2");
//        s.unitTestsByPackage("nars.nal.nal3");
//        s.unitTestsByPackage("nars.nal.nal4");
        s.unitTestsByPackage("nars.nal.nal5");
        s.unitTestsByPackage("nars.nal.nal6");
//        s.unitTestsByPackage("nars.nal.nal7");
//        s.unitTestsByPackage("nars.nal.nal8");
        s.run(6);
    }


    //TODO make this a Bag
    final CopyOnWriteArrayList<Supplier<DataTable>> experiments = new CopyOnWriteArrayList();

    public SelfTest() {

    }

    private final JupiterTestEngine engine = new JupiterTestEngine();
    //https://junit.org/junit5/docs/current/user-guide/#launcher-api-listeners-reporting
    private final LauncherConfig launcherConfig = LauncherConfig.builder()
            .enableTestEngineAutoRegistration(false)
            //.enableTestExecutionListenerAutoRegistration(false)
            //.addTestEngines(new CustomTestEngine())
            .addTestEngines(engine)
            //new LegacyXmlReportGeneratingListener(reportsDir, out)
            .build();

    public void unitTestsByPackage(String... packages) {
        experiments.add(unitTests((b)->{
            for (String pkg : packages)
                b.selectors(selectPackage(pkg));
        }));
    }

    public Supplier<DataTable> unitTests(Consumer<LauncherDiscoveryRequestBuilder> selector) {


        LauncherDiscoveryRequestBuilder b = LauncherDiscoveryRequestBuilder.request();
        selector.accept(b);

        return ()->{
            Launcher lf = LauncherFactory.create(launcherConfig);


            MyTestExecutionListener exe = new MyTestExecutionListener();
            DataTable results = newTable();
            exe.setTable(results);

            lf.registerTestExecutionListeners(exe);
            lf.execute(b.build());

            return results;
        };
    }

//    static private int _jupiterPrefixToRemove = "[engine:junit-jupiter]/[class:".length();
//
//    protected static String sid(TestIdentifier id) {
//        String uid = id.getUniqueId();
//        try {
//            return "(" + uid.substring(_jupiterPrefixToRemove).replace("]/[method:", "/").replace("]", "").replace("()","")
//                    .replace("/",",").replace(".",",") + ")";
//        } catch (Exception e) {
//            return uid;
//        }
//
//    }


//    public static class TestMetrics implements Serializable {
//
//        /** unixtime */
//        public final long when;
//
//        public final String id;
//        public long wallTimeNS;
//
//        public boolean success, error;
//
//        public TestMetrics(TestIdentifier testIdentifier) {
//            this.when = System.currentTimeMillis();
//            this.id = sid(testIdentifier);
//
//        }
//
//        @Override
//        public String toString() {
//            return id +
//                    (success ? " sccs " : " fail ") + (error ? " err " : " ") +
//                    " @ " + new Date(when) + " .. " +
//                    Texts.timeStr(wallTimeNS);
//        }
//    }


    protected static DataTable newTable() {
        DataTable d = new DataTable();
        d.addColumns(
                StringColumn.create("package"),
                StringColumn.create("class"),
                StringColumn.create("method"),
                LongColumn.create("start"),
                BooleanColumn.create("success"),
                BooleanColumn.create("error"),
                LongColumn.create("wallTimeNS")
        );
        return d;
    }

    private static class MyTestExecutionListener implements TestExecutionListener {

        private DataTable out;
        transient long startNS, endNS;
        private long startUnixTime;
//        transient private TestMetrics m;

        public MyTestExecutionListener() {
        }

        public MyTestExecutionListener(DataTable results) {
            this();
            setTable(results);
        }

        public void setTable(DataTable out) {
            this.out = out;
        }

        public void testPlanExecutionStarted(TestPlan testPlan) {
            //this.testPlan = testPlan;
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {

            this.startUnixTime = System.currentTimeMillis();
            this.startNS = System.nanoTime();
        }

        @Override
        public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
            //System.out.println(testIdentifier +  " " + entry);
        }

        @Override
        public void executionFinished(TestIdentifier id, TestExecutionResult result) {

            this.endNS = System.nanoTime();

            boolean success = false, error = false;
            long wallTimeNS = (endNS - startNS);
            switch (result.getStatus()) {
                case SUCCESSFUL:
                    success = true;
                    error = false;
                    break;
                case FAILED:
                    success = false;
                    error = false;
                    break;
                case ABORTED:
                    success = false;
                    error = true;
                    break;
            }



            TestSource src = id.getSource().orElse(null);
            if (src==null)
                return;

            if (src instanceof MethodSource) {
                String pk, cl, me;
                MethodSource m = (MethodSource)src;
                cl = ClassName.fromString(m.getClassName()).getNameWithoutPackage().toString();
                assert(!cl.isEmpty());
                pk = Reflection.getPackageName(m.getClassName());
                assert(!pk.isEmpty());
                me = m.getMethodName() +
                        (!m.getMethodParameterTypes().isEmpty() ? ('(' + id.getDisplayName() + ')') : "");
                assert(!me.isEmpty());
                out.add(pk, cl, me, startUnixTime, success, error, wallTimeNS);
            } else {
                //TODO / ignore
            }



        }
    }






    void run(int repeats) {
        run(repeats, Runtime.getRuntime().availableProcessors());
    }

    void run(int repeats, int threads) {

        final DataTable all = newTable();

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            try {
                save(all, "/home/me/d/tests1.csv");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));


        ExecutorService exe = Executors.newFixedThreadPool(threads);

        experiments.forEach(experiment -> {
            for (int i = 0; i < repeats; i++) {
                exe.execute(() -> {
                    DataTable d = experiment.get();

                    //System.out.println(Thread.currentThread());
                    synchronized (all) {
                        int cc = 0;
                        for (Column c : d.columns()) {
                            all.column(cc++).append(c);
                        }
                        //d.doWithRows((Consumer<Row>) all::addRow);
                    }
                    d.clear();
                });
            }
        });

        try {
            exe.shutdown();
            exe.awaitTermination(3600, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    private static final Logger logger = LoggerFactory.getLogger(SelfTest.class);

    private void save(DataTable d, String filename) throws IOException {
        synchronized (d) {
            FileOutputStream fos = new FileOutputStream(filename, true);
            GZIPOutputStream os = new GZIPOutputStream(new BufferedOutputStream(fos, 64 * 1024));
            logger.info("appending {} rows to {} ({})", d.rowCount(), filename, os.getClass());
            d.write().csv(os);
            os.flush();
            os.close();
            logger.info("saved {}", filename);

            report(d);
        }

    }

    private void report(DataTable all) {

        String[] testID = all.columns(0, 1, 2).stream().map(x -> x.name()).toArray(String[]::new);

        //https://jtablesaw.github.io/tablesaw/userguide/reducing
        {

            Table walltimes = all.summarize("wallTimeNS", mean/*, stdDev*/).by(testID)
                    .sortDescendingOn("Mean [wallTimeNS]")
                    ;

            ((NumberColumn)walltimes.column(3)).setPrintFormatter(new NSTimeFormatter());

            System.out.println(walltimes.printAll());
        }

        {
            Table ff = all.summarize("success", countFalse).by(testID);

            ff = ff.where(((NumberColumn)ff.column(3)).isGreaterThan(0));

            Table fails = ff.sortDescendingOn(ff.column(3).name());

            System.out.println(fails.printAll());
            System.out.println("failures: " + ff.summary());
        }



//        RealDecisionTree tr = Optimize.tree(d, 2, 8);
//        System.out.println(tr);
//        tr.print();
//        tr.printExplanations();

//        NAR n = NARS.tmp();
//        n.log();
//        Atom SUCCESS = Atomic.atom("success");
//        d.forEach(r->{
//            String traw = r.getString(0);
//            //String t = traw.substring(1, traw.length()-1);
//            Term tt = $.$$(traw);
//            switch (r.getString(2)) {
//                case "Success":
//                    Task b = n.believe($.inh(tt, SUCCESS));
//                    System.out.println(b);
//                    break;
//                case "Fail":
//                    n.believe($.inh(tt, SUCCESS).neg());
//                    break;
//            }
//        });
//        n.run(1000);
    }


    public static class RemoteLauncher {
        public static void main(String[] args) {
            Cloud cloud = CloudFactory.createCloud();

            RemoteNode rn = RemoteNode.at(cloud.node("eus")).useSimpleRemoting();
            rn.setRemoteAccount("me");
            rn.setRemoteJavaExec("/home/me/jdk/bin/java");
            rn.setRemoteJarCachePath("/aux/me/tmp");
            rn.setProp("debug", "true");


            DataTable s = cloud.node(/*"**"*/ "eus").exec(new Callable<>() {
                @Override
                public DataTable call() {

                    //return new TestServer("nars.nal.nal1").unitTestsByPackage();
                    throw new TODO();
                }
            });

            s.write().csv(System.out);

            cloud.shutdown();

        }
    }

    private static class NSTimeFormatter extends NumberColumnFormatter {
        public String format(double value) {
            return Texts.timeStr(Math.round(value));
        }
    }
}
