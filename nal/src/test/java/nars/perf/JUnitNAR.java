package nars.perf;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.Util;
import nars.NAR;
import nars.Param;
import nars.util.NALTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.LoggingListener;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/** JUnit wrappers and runners */
public class JUnitNAR {

    /** HACK runs all Junit test methods, summing the scores.
     * TODO use proper JUnit5 test runner api but it is a mess to figure out right now */
    public static float tests(Executor exe, Supplier<NAR> s, Class<? extends NALTest>... c) {


        List<Method> methods = Stream.of(c)
                .flatMap(cc -> Stream.of(cc.getMethods())
                .filter(x -> x.getAnnotation(Test.class) != null))
                .collect(toList());

        final CountDownLatch remain = new CountDownLatch(methods.size());
        final AtomicDouble sum = new AtomicDouble(0);
        methods.forEach(m -> {
            exe.execute(()->{
                try {
                    sum.addAndGet(test(s, m));
                } finally {
                    remain.countDown();
                }
            });
        });
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
                return 0f; //fatal during test
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0f;
        }
    }

    /** alternate, less flexible due to JUnit 5's unfortunately contaminated unworkable API */
    public static void junit(Class... testClasses) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        //selectPackage("com.example.mytests"),
                        (ClassSelector[])Util.map(
                                DiscoverySelectors::selectClass,
                                new ClassSelector[testClasses.length], testClasses)

                        //selectClass(FastCompoundNAL1Test.class)
                )
                // .filters( includeClassNamePatterns(".*Tests")  )
                .build();


        Launcher launcher = LauncherFactory.create();


        //SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LoggingListener listener = LoggingListener.forJavaUtilLogging();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request, listener);

        //listener.getSummary().printTo(new PrintWriter(System.out));
    }
}
