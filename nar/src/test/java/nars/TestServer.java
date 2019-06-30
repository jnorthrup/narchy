package nars;

import jcog.Texts;
import jcog.data.list.FasterList;
import jcog.table.DataTable;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.nanocloud.RemoteNode;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.StringColumn;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

/** periodically, or event-triggered: runs unit tests and broadcasts results */
public class TestServer {

    public List<String> packages = new FasterList();

    public TestServer(String... packages) {
        for (String p : packages) this.packages.add(p);

    }

    protected DataTable test() {

        DataTable results = newTestTable();

        //https://junit.org/junit5/docs/current/user-guide/#launcher-api-listeners-reporting
        LauncherConfig launcherConfig = LauncherConfig.builder()
                .enableTestEngineAutoRegistration(false)
                .enableTestExecutionListenerAutoRegistration(false)
                //.addTestEngines(new CustomTestEngine())
                .addTestEngines(new JupiterTestEngine())
                .addTestExecutionListeners(new MyTestExecutionListener(results))
                //new LegacyXmlReportGeneratingListener(reportsDir, out)
                .build();



        LauncherDiscoveryRequestBuilder b = LauncherDiscoveryRequestBuilder.request();
        for (String pkg : this.packages)
            b.selectors(selectPackage(pkg));

        LauncherFactory.create(launcherConfig).execute(b.build());

        return results;
    }

    static private int _jupiterPrefixToRemove = "[engine:junit-jupiter]/[class:".length();

    protected static String sid(TestIdentifier id) {
        String uid = id.getUniqueId();
        try {
            return "(" + uid.substring(_jupiterPrefixToRemove).replace("]/[method:", "/").replace("]", "").replace("()","").replace("/",",").replace(".",",") + ")";
        } catch (Exception e) {
            return uid;
        }

    }


    public static class TestMetrics implements Serializable {
        public final LocalDateTime when;
        public final String id;
        public long wallTimeNS;

        public enum Status {
            Success, Fail, Error
        }
        public Status status;

        public TestMetrics(TestIdentifier testIdentifier) {
            this.when = LocalDateTime.now();
            this.id = sid(testIdentifier);
        }

        @Override
        public String toString() {
            return "test(" +
                    "id='" + id + '\'' +
                    ' ' + status +
                    " @ " + when +
                    " wallTimeNS=" + Texts.timeStr(wallTimeNS) +
                    '}';
        }
    }


    protected static DataTable newTestTable() {
        DataTable d = new DataTable();
        d.addColumns(
                StringColumn.create("test"),
                DateTimeColumn.create("start"),
                StringColumn.create("status"),
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
        public void executionFinished(TestIdentifier id, TestExecutionResult result) {
            this.endNS = System.nanoTime();
            m.wallTimeNS = (endNS - startNS);
            switch (result.getStatus()) {
                case SUCCESSFUL:
                    m.status = TestMetrics.Status.Success;
                    break;
                case FAILED:
                    m.status = TestMetrics.Status.Fail;
                    break;
                case ABORTED:
                    m.status = TestMetrics.Status.Error;
                    break;

            }

            out.add(m.id, m.when, m.status.name(), m.wallTimeNS);
        }
    }


    /** local launcher */
    public static void main(String[] args) {
        TestServer s = new TestServer(
                "nars.nal.nal1"
        );
        DataTable d = s.test();
//        ((TextColumn)d.column(0)).setPrintFormatter(new StringColumnFormatter() {
//            @Override
//            public String format(String value) {
//                return "\"" + super.format(value) + "\"";
//            }
//        });
        d.write().csv(System.out);

//        RealDecisionTree tr = Optimize.tree(d, 2, 8);
//        System.out.println(tr);
//        tr.print();
//        tr.printExplanations();

        NAR n = NARS.tmp();
        n.log();
        Atom SUCCESS = Atomic.atom("success");
        d.forEach(r->{
            String traw = r.getString(0);
            //String t = traw.substring(1, traw.length()-1);
            Term tt = $.$$(traw);
            switch (r.getString(2)) {
                case "Success":
                    Task b = n.believe($.inh(tt, SUCCESS));
                    System.out.println(b);
                    break;
                case "Fail":
                    n.believe($.inh(tt, SUCCESS).neg());
                    break;
            }
        });
        n.run(1000);
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
                    return new TestServer("nars.nal.nal1").test();
                }
            });

            s.write().csv(System.out);

            cloud.shutdown();

        }
    }

}
