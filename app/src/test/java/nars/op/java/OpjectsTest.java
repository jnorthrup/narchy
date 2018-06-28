package nars.op.java;

import jcog.list.FasterList;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpjectsTest {

    public static class SimpleClass {
        protected int v;

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

        int dur = 1;
        int focus = 4;

        n.timeFocus.set(focus);
        n.time.dur(dur);



        List<Term> evokes = new FasterList();
        final Opjects objs = new Opjects(n) {


            @Override
            protected boolean evoked(Term method, Object instance, Object[] params) {
                evokes.add(method);
                return super.evoked(method, instance, params);
            }
        };
        n.run();

        final SimpleClass x = objs.the("x", new SimpleClass());
        StringBuilder sb = new StringBuilder();
        n.onTask(sb::append);
        

        n.input("set(x,1)! :|:");

        n.run(dur);

        assertEquals(1, evokes.size());

        n.input("get(x,#y)! :|:");

        n.run(dur);

        assertEquals(2, evokes.size());

        n.run(dur);

        assertEquals(2, evokes.size());
    }

   @Test
    public void testObjectMethods() throws Narsese.NarseseException {
        final NAR n = NARS.tmp();



        
        final Opjects objs = new Opjects(n);

        final SimpleClass x = objs.a("x", SimpleClass.class);
        StringBuilder sb = new StringBuilder();
        n.onTask(sb::append);

        
        n.input("hashCode(x,#h)! :|:");
        n.run(1);
        n.run(1);
        assert(!sb.toString().contains("voke"));
    }

    /** invoked externally (ex: by user) */
    @Test public void testInvokeInstanced() {
        final NAR n = NARS.tmp();

        

        final SimpleClass y = new Opjects(n).a("y", SimpleClass.class);
        testInvoke(n, y);
    }

    static void testInvoke(NAR n, SimpleClass y) {
        StringBuilder sb = new StringBuilder();
        n.onTask(sb::append);
        n.run(1);
        {
            y.get();
        }
        n.run(1);
        {
            y.set(1);
        }
        n.run(1);
        String s = sb.toString();
        System.out.println("log:\n" + s);
        assertTrue(s.contains("get(y,0)."));
        assertTrue(s.contains("set(y,1)."));
    }

    @Test public void testInvokeWrapped() {
        final NAR n = NARS.tmp();

        

        final SimpleClass y = new Opjects(n).the("y", new SimpleClass());
        testInvoke(n, y);
    }

    @Disabled
    @Test
    public void learnMethodGoal() throws Narsese.NarseseException {



        final NAR n = NARS.tmp();


        final Opjects objs = new Opjects(n);

        final SimpleClass x = objs.a("x", SimpleClass.class);



        n.beliefPriDefault.set(0.05f);
        n.questionPriDefault.set(0.05f);
        n.questPriDefault.set(0.05f);
        n.freqResolution.set(0.1f);
        n.time.dur(10);
        n.termVolumeMax.set(30);

        n.logPriMin(System.out, 0.02f);







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





                n.input("$1.0 x(get,(),2)!");

                n.run(50);
            }

            




            n.run(50);
        }










        






    }
}