//package jcog.optimize.util;
//
//import jcog.TODO;
//import org.openjdk.jmh.runner.Runner;
//import org.openjdk.jmh.runner.options.Options;
//import org.openjdk.jmh.runner.options.OptionsBuilder;
//
//import java.util.concurrent.Callable;
//import java.util.concurrent.ForkJoinPool;
//import java.util.concurrent.Future;
//import java.util.function.Function;
//
///** executes within a JMH runner, providing an isolated VM
// * not finished */
//public class JmhExecutor implements Function<Callable, Future> {
//
//    private final ForkJoinPool exe;
//
//    public JmhExecutor() {
//        this.exe = ForkJoinPool.commonPool();
//    }
//
//    @Override
//    public Future apply(Callable callable) {
//        return exe.submit(()->{
//            Options o = new OptionsBuilder();
//            //...
//            Runner r = new Runner(o);
//            throw new TODO();
//        });
//    }
//}
