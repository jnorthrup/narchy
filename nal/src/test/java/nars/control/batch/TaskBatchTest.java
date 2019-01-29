package nars.control.batch;

import jcog.data.byt.DynBytes;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;

import static jcog.Texts.n2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskBatchTest {

    static final BiConsumer<String, DynBytes> KEYER = (x, v)->v.write(x.getBytes());

    final StringBuilder x = new StringBuilder();

    class MyTask extends SimpleBatchableTask {

        private final String param;

        protected MyTask(String param, float pri) {
            super(pri, KEYER, param);
            this.param = param;
        }

        @Override
        protected void run() {
            x.append(param).append('@').append(n2(pri)).append(' ');
        }
    }
    class MyContinuation extends BatchableTask {

        private final String param;

        protected MyContinuation(String param, float pri) {
            super(pri, KEYER, param);
            this.param = param;
        }

        @Override
        public void run(TaskBatch.TaskQueue next) {
            x.append("c_").append(param).append('@').append(n2(pri)).append(' ');
            next.put(new MyTask(param, pri));
        }
    }

    @Test
    void testSimpleBatch() {
        x.setLength(0);
        TaskBatch b = TaskBatch.get();
        b.add(new MyTask("x", 0.5f));
        b.add(new MyTask("x", 0.25f));
        b.add(new MyTask("y", 0.5f));
        b.run();
        assertEquals("x@.75 y@.50 ", x.toString());
        assertTrue(b.queue[0].isEmpty());
        assertTrue(b.queue[1].isEmpty());
    }

    @Test
    void test2LayerBatch() {
        x.setLength(0);
        TaskBatch b = TaskBatch.get();
        b.add(new MyContinuation("x", 0.5f));
        b.add(new MyContinuation("x", 0.25f));
        b.add(new MyContinuation("y", 0.5f));
        b.run();
        assertEquals("c_x@.75 c_y@.50 x@.75 y@.50 ", x.toString());
        assertTrue(b.queue[0].isEmpty());
        assertTrue(b.queue[1].isEmpty());
    }
}