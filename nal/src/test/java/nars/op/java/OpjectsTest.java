package nars.op.java;

import nars.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpjectsTest {

    static class SimpleClass {
        private int v;

        public void set(int x) {
            System.err.println("set: " + x);
            this.v = x;
        }

        public int get() {
            return v;
        }
    }


    /** self invocation */
    @Test public void testEvoke() throws Narsese.NarseseException {
        final NAR n = NARS.tmp();
        n.time.dur(3);

        Param.DEBUG = true;

        Set<Task> evokes = new HashSet();
        final Opjects objs = new Opjects(n) {
            @Override
            protected boolean evoked(Task task, Object[] args) {
                evokes.add(task);
                return super.evoked(task, args);
            }
        };

        final SimpleClass x = objs.a("x", SimpleClass.class);
        StringBuilder sb = new StringBuilder();
        n.onTask(sb::append);
        n.log();

        n.input("set(x,1)! :|:");
        n.run(1);
        n.run(1);

        n.input("get(x,#y)! :|:");
        n.run(4);

        assertEquals(2, evokes.size());

        n.run(1);
    }

   @Test
    public void testObjectMethods() throws Narsese.NarseseException {
        final NAR n = NARS.tmp();

        Param.DEBUG = true;

        n.log();
        final Opjects objs = new Opjects(n);

        final SimpleClass x = objs.a("x", SimpleClass.class);
        StringBuilder sb = new StringBuilder();
        n.onTask(sb::append);

        //n.input("x(getClass,(),#y)! :|:");
        n.input("hashCode(x,#h)! :|:");
        n.run(1);
        n.run(1);
        assert(!sb.toString().contains("voke"));
    }

    /** invoked externally (ex: by user) */
    @Test public void testInvoke() {
        final NAR n = NARS.tmp();

        n.log();
        final Opjects objs = new Opjects(n);

        final SimpleClass x = objs.a("x", SimpleClass.class);
        StringBuilder sb = new StringBuilder();
        n.onTask(sb::append);
        n.run(1);
        {
            x.get();
        }
        n.run(1);
        {
            x.set(1);
        }
        n.run(1);
        String s = sb.toString();
        assertTrue(s.contains("$.50 get(x,0)."));
        assertTrue(s.contains("$.50 set(x,1)."));
    }

    @Disabled
    @Test
    public void learnMethodGoal() throws Narsese.NarseseException {
//         StringBuilder sb = new StringBuilder();
//        n.onTask(sb::append);
        Param.DEBUG = true;
        final NAR n = NARS.tmp();


        final Opjects objs = new Opjects(n);

        final SimpleClass x = objs.a("x", SimpleClass.class);


        n.priDefault(BELIEF, 0.05f);
        n.priDefault(QUESTION, 0.05f);
        n.priDefault(QUEST, 0.05f);
        n.freqResolution.set(0.1f);
        n.time.dur(10);
        n.termVolumeMax.set(30);

        n.logPriMin(System.out, 0.02f);
//        n.onTask(xx -> {
//           if (xx instanceof DerivedTask) {
//               if (xx.isGoal())
//                System.out.println(xx);
//           }
//        });

        int N = 2;

        n.clear();

        int loops = 0, trainingRounds = 4;
        while (x.v != 2) {

            if (loops++ < trainingRounds) {
                for (int i = 0; i < 2; i++) {


                    x.set(i % N);

                    n.run(1);

                    x.get();

                    n.run(1);
                }



//                n.input("$0.5 (0<->2)?");
//                n.input("$0.5 (1<->2)?");
                n.input("$1.0 x(get,(),2)!");

                n.run(50);
            }

            //n.input("$1.0 x(set,2)! :|:");
//            n.input("$1.0 SimpleClass(get,x,(),2)! :|:");
//            n.input("$1.0 SimpleClass(get,x,(),_)! :|:");
//        n.input("$1.0 --SimpleClass(get,x,(),0)! :|:");
//        n.input("$1.0 --SimpleClass(get,x,(),1)! :|:");
            n.run(50);
        }

//        while (x.v!=3) {
//
//        }

//        n.input("$0.5 (0<->1)?");
//        n.input("$0.5 (1<->2)?");
//        n.input("$0.5 (2<->3)?");
//        n.input("$0.5 (3<->4)?");
//        n.input("$1.0 (SimpleClass(set,x,$x) ==> SimpleClass(get,x,(),$x))?");
        //n.run(100);


//        n.tasks().forEachOrdered(z -> {
//            System.out.println(z);
//        });

    }
}