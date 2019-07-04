package nars;

import jcog.TODO;
import jcog.Texts;
import jcog.data.list.FasterList;
import jcog.table.DataTable;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.RemoteNode;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.numbers.NumberColumnFormatter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static tech.tablesaw.aggregate.AggregateFunctions.*;

/** periodically, or event-triggered: runs unit tests and broadcasts results */
public class TestServer {

    //TODO make this a Bag
    final List<Supplier<DataTable>> experiments = new FasterList();

    public TestServer() {

    }

    private final JupiterTestEngine engine = new JupiterTestEngine();

    public void unitTestsByPackage(String... packages) {
        experiments.add(unitTests((b)->{
            for (String pkg : packages)
                b.selectors(selectPackage(pkg));
        }));
    }

    public Supplier<DataTable> unitTests(Consumer<LauncherDiscoveryRequestBuilder> selector) {
        return ()->{
            DataTable results = newTable();

            //https://junit.org/junit5/docs/current/user-guide/#launcher-api-listeners-reporting
            LauncherConfig launcherConfig = LauncherConfig.builder()
                    .enableTestEngineAutoRegistration(false)
                    .enableTestExecutionListenerAutoRegistration(false)
                    //.addTestEngines(new CustomTestEngine())
                    .addTestEngines(engine)
                    .addTestExecutionListeners(new MyTestExecutionListener(results))
                    //new LegacyXmlReportGeneratingListener(reportsDir, out)
                    .build();

            LauncherDiscoveryRequestBuilder b = LauncherDiscoveryRequestBuilder.request();
            selector.accept(b);

            LauncherFactory.create(launcherConfig).execute(b.build());

            return results;
        };
    }

    static private int _jupiterPrefixToRemove = "[engine:junit-jupiter]/[class:".length();

    protected static String sid(TestIdentifier id) {
        String uid = id.getUniqueId();
        try {
            return "(" + uid.substring(_jupiterPrefixToRemove).replace("]/[method:", "/").replace("]", "").replace("()","")
                    .replace("/",",").replace(".",",") + ")";
        } catch (Exception e) {
            return uid;
        }

    }


    public static class TestMetrics implements Serializable {

        /** unixtime */
        public final long when;

        public final String id;
        public long wallTimeNS;

        public boolean success, error;

        public TestMetrics(TestIdentifier testIdentifier) {
            this.when = System.currentTimeMillis();
            this.id = sid(testIdentifier);
        }

        @Override
        public String toString() {
            return id +
                    (success ? " sccs " : " fail ") + (error ? " err " : " ") +
                    " @ " + new Date(when) + " .. " +
                    Texts.timeStr(wallTimeNS);
        }
    }


    protected static DataTable newTable() {
        DataTable d = new DataTable();
        d.addColumns(
                StringColumn.create("test"),
                LongColumn.create("start"),
                BooleanColumn.create("success"),
                BooleanColumn.create("error"),
                LongColumn.create("wallTimeNS")
        );
        return d;
    }

    private static class MyTestExecutionListener implements TestExecutionListener {

        private final DataTable out;
        transient long startNS, endNS;
        transient private TestMetrics m;

        public MyTestExecutionListener(DataTable out) {
            this.out = out;
        }

        public void testPlanExecutionStarted(TestPlan testPlan) {
            //this.testPlan = testPlan;
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            this.m = new TestMetrics(testIdentifier);
            this.startNS = System.nanoTime();
        }

        @Override
        public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
            System.out.println(testIdentifier +  " " + entry);
        }

        @Override
        public void executionFinished(TestIdentifier id, TestExecutionResult result) {

            boolean success = false, error = false;
            this.endNS = System.nanoTime();
            m.wallTimeNS = (endNS - startNS);
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

            out.add(m.id, m.when, success, error, m.wallTimeNS);
        }
    }


    /** local launcher */
    public static void main(String[] args) {
        TestServer s = new TestServer();
        s.unitTestsByPackage("nars.nal.nal1");
        s.unitTestsByPackage("nars.nal.nal2");
        s.unitTestsByPackage("nars.nal.nal3");
//        ((TextColumn)d.column(0)).setPrintFormatter(new StringColumnFormatter() {
//            @Override
//            public String format(String value) {
//                return "\"" + super.format(value) + "\"";
//            }
//        });

        s.run();


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


    final static int maxRows = 128*1024;

    void run() {

        final DataTable all = newTable();

        while (true) {
            for (Supplier<DataTable> experiment : experiments) {
                DataTable d = experiment.get();
                //d.write().csv(System.out);
                d.doWithRows((Consumer<Row>) all::addRow);
            }

            report(all);

            if (all.rowCount() >= maxRows)
                break; //TODO
        }
    }

    private void report(DataTable all) {
        //https://jtablesaw.github.io/tablesaw/userguide/reducing
        {
            Table walltimes = all.summarize("wallTimeNS", mean/*, stdDev*/).by("test")
                    .sortDescendingOn("Mean [wallTimeNS]")
                    ;

            ((NumberColumn)walltimes.column(1)).setPrintFormatter(new NSTimeFormatter());

            System.out.println(walltimes.printAll());
        }

        {
            Table ff = all.summarize("success", countTrue).by("test");

            ff = ff.where(((NumberColumn)ff.column(1)).isGreaterThan(0));

            Table fails = ff.sortDescendingOn(ff.column(1).name());

            System.out.println(fails.printAll());
        }
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
