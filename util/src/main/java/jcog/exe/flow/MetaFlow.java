package jcog.exe.flow;

import jcog.data.byt.DynBytes;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.list.FasterList;
import jcog.memoize.QuickMemoize;
import org.HdrHistogram.ConcurrentDoubleHistogram;
import org.eclipse.collections.impl.map.mutable.primitive.ByteObjectHashMap;

import java.io.PrintStream;

/** instrumented CFG with cost/benefit analysis measured at decision points
 * stored in call trie
 * https://download.java.net/java/early_access/jdk11/docs/api/java.base/java/lang/StackWalker.html
 * https://www.sitepoint.com/deep-dive-into-java-9s-stack-walking-api/#exceptionvsstackwalker
 * */
public class MetaFlow {

    private final StackWalker.StackFrame base;

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

    class StackCursor extends DynBytes {

        final StackWalker.StackFrame base;
        private StackWalker.StackFrame at = null;
        final FasterList<StackWalker.StackFrame> buffer = new FasterList(256);

        public StackCursor(StackWalker.StackFrame base) {
            super(2048);
            this.base = base;
        }

        /** position the cursor for the current stack state */
        public StackCursor reset() {


            //TODO align with previous buffer rather than clearing it, at least some of it is common
            //store the character position of each frame so that on ascent, it can just reposition to that previous location
            buffer.clear();
            walkerSummary.walk((s) -> {
                s.skip(1).takeWhile(f -> {
//                    if (f == at) {
//                        //no change
//                        return false;
//                    } else { //TODO
                        return f!=base; //descending
//                    }
                }).filter(MetaFlow.this::frameFilter).forEach(buffer::add);
                return null;
            });

            at = buffer.getLast();

//            Optional<Class<?>> callerClass = walker.walk(s ->
//                    s.map(StackWalker.StackFrame::getDeclaringClass)
//                            .filter(interestingClasses::contains)
//                            .findFirst());
            clear();
            for (int i = buffer.size()-1; i>=0; i--) {
                write(buffer.get(i));
                if (i != 0) {
                    char SEPARATOR = '/';
                    writeByte((byte)SEPARATOR);
                }
            }
            return this;
        }


        protected void write(StackWalker.StackFrame f) {
            //TODO elide re-writing package and class if unchanged from previous
            writeUTF(f.getClassName());
            writeByte((byte)':');
            writeUTF(f.getMethodName());
            write(compactDescriptor(f.getDescriptor()));
        }


        public void print() {
            print(System.out);
        }
        public void print(PrintStream out) {
            out.println(toStringFromBytes());
            buffer.reverseForEach(b -> {
                out.println(b);
            });
            out.println();
        }
    }

    static final QuickMemoize<String,byte[]> compactMethodDescriptors = new QuickMemoize<>(1024, (descriptor)->{
        //TODO use byteseek automaton
        descriptor = descriptor.replace("java/lang/","");
        descriptor = descriptor.substring(0, descriptor.length()-1); //remove trailing ';'
        return descriptor.getBytes();
    });

    static byte[] compactDescriptor(String descriptor) {
        return compactMethodDescriptors.apply(descriptor);
    }

    /** shared by all cursors */
    protected boolean frameFilter(StackWalker.StackFrame stackFrame) {
        return true;
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

    final ThreadLocal<StackCursor> cursors;

    public MetaFlow() {
        base = walkerSummary.walk(s->s.dropWhile(x->
                x.getClassName().equals("ThreadLocal"))
                .findFirst().get());
        cursors = ThreadLocal.withInitial(()->new StackCursor(base));
    }


    static final ThreadLocal<MetaFlow> meta = ThreadLocal.withInitial(MetaFlow::new);

    public static MetaFlow exe() {
        return meta.get();
    }

    public static StackCursor stack() {
        return exe().threadLocalStackCursor();
    }

    /** gets thread-local cursor */
    public StackCursor threadLocalStackCursor() {
        return cursors.get();
    }


    public static void aMethodToCall() {
        stack().reset().print();
    }

    public static void main(String[] args) {
        MetaFlow m = exe();
        stack().reset().print();
        System.out.println();
        aMethodToCall();
    }
}
