package jcog.exe.flow;

import jcog.data.byt.DynBytes;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import org.HdrHistogram.ConcurrentDoubleHistogram;
import org.eclipse.collections.impl.map.mutable.primitive.ByteObjectHashMap;

import java.lang.reflect.Method;

/** instrumented CFG with cost/benefit analysis measured at decision points
 * stored in call trie
 * https://download.java.net/java/early_access/jdk11/docs/api/java.base/java/lang/StackWalker.html
 * https://www.sitepoint.com/deep-dive-into-java-9s-stack-walking-api/#exceptionvsstackwalker
 * */
public class MetaFlow {

    static class Quality {
        final String name;
        private final int id;

        /** adjustable factor: positive=gain, negative=loss;  0 = no effect in value decision*/
        volatile float value = 0f;

        Quality(String name, int id) {
            this.name = name;
            this.id = id;
        }

        Quality value(float newValue) {
            this.value = newValue;
            return this;
        }
    }

    /** labels for cost/value qualities */
    final ConcurrentFastIteratingHashMap<String,Quality> qualia = new ConcurrentFastIteratingHashMap<>(new Quality[0]);

    class Node extends ByteObjectHashMap<ConcurrentDoubleHistogram> {

    }

    public static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    public static final StackWalker walkerSummary = StackWalker.getInstance();

    static class StackCursor extends DynBytes {

        private Method at = null;

        public StackCursor() {
            super(1024);
        }

        /** position the cursor for the current stack state */
        public void reset(StackWalker.StackFrame base) {
            //StackWalker.getInstance()

//            Optional<Class<?>> callerClass = walker.walk(s ->
//                    s.map(StackWalker.StackFrame::getDeclaringClass)
//                            .filter(interestingClasses::contains)
//                            .findFirst());
            clear();

        }
    }

    //MyConcurrentRadixTree<Node> calls

    public Quality quality(String q) {
        return qualia.computeIfAbsent(q, qq -> {
            synchronized (qualia) {
                int id = qualia.size();
                return new Quality(qq, id);
            }
        });
    }
}
