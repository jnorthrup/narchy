package nars;

import jcog.data.list.FasterList;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

/** periodically, or event-triggered: runs unit tests and broadcasts results */
public class TestServer {

    public List<String> packages = new FasterList();

    public TestServer(String... packages) {
        for (String p : packages) this.packages.add(p);

    }

    protected SortedMap<String, TestExecutionResult> test() {

        SortedMap<String,TestExecutionResult> results = new TreeMap();

        //https://junit.org/junit5/docs/current/user-guide/#launcher-api-listeners-reporting
        LauncherConfig launcherConfig = LauncherConfig.builder()
                .enableTestEngineAutoRegistration(false)
                .enableTestExecutionListenerAutoRegistration(false)
                //.addTestEngines(new CustomTestEngine())
                .addTestEngines(new JupiterTestEngine())
                .addTestExecutionListeners(
                        new TestExecutionListener() {
                            @Override
                            public void executionFinished(TestIdentifier id, TestExecutionResult result) {
                                //System.out.println(id + " " + result);
                                results.put(sid(id), result);
                            }
                        }
                )
                //new LegacyXmlReportGeneratingListener(reportsDir, out)
//                    .addTestExecutionListeners(new CustomTestExecutionListener())
                .build();


        Launcher launcher = LauncherFactory.create(launcherConfig);

        LauncherDiscoveryRequestBuilder b = LauncherDiscoveryRequestBuilder.request();
        for (String pkg : this.packages)
            b.selectors(selectPackage(pkg));
        launcher.execute(b.build());

        return results;
    }

    static private int _jupiterPrefixToRemove = "[engine:junit-jupiter]/[class:".length();
    protected static String sid(TestIdentifier id) {
        return id.getUniqueId().substring(_jupiterPrefixToRemove).replace("]/[method:","/").replace("]", "");
    }

    public static void main(String[] args) {
        TestServer s = new TestServer(
    "nars.nal.nal1"
        );

        s.test().forEach((k,v)->System.out.println(k + "\t" + v));

    }
}
