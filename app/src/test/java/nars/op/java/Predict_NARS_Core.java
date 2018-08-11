package nars.op.java;

import jcog.tree.interval.IntervalTree;
import nars.*;
import nars.op.in.ChangedTextInput;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import java.util.List;

import static java.lang.System.out;
import static nars.Op.QUESTION;

/**
 * https:
 * https:
 *
 * @author me
 */
public class Predict_NARS_Core {

    static float signal;

    static IntervalTree<Long, Task> predictions = new IntervalTree<>();
    private static int last = -1;

    public static void main(String[] args) throws Narsese.NarseseException {


        int duration = 8;
        float freq = 1.0f / duration * 0.1f;
        int thinkInterval = 24;
        float discretization = 7f;

        
        NAR n = new NARS().get();
        
        
        
        

        n.eventTask.on(t -> {
            if (!t.isDeleted() && t.isBelief() && t.op()== Op.PROD && t.term().volume()==2 &&  !t.isEternal() && t.start() > n.time() && t.expectation()>0.5) {

                long time = (int) t.start();
                String ts = t.term().toString();
                if (ts.startsWith("(y")) {
                    predict(time, t);
                }
            }

        });






















        

        n.log();
        n.run((int) discretization * 4);


        ChangedTextInput chg = new ChangedTextInput(n);
        double lastsignal = 0;
        double lasttime = 0;

        while (true) {

            try {
                n.run(thinkInterval);
            }
            catch (Exception e) {
                System.err.println(e);
                n.stop();
                
            }
            

            
            signal = (float) Math.sin(freq * n.time()) * 0.5f + 0.5f;
            
            

            int cols = 40;
            int colActual = Math.round(signal * cols);
            int val = (int) (((int) ((signal * discretization)) * (10.0 / discretization)));

            long windowStart = n.time();
            long windowEnd = n.time() + 2;

            predictions.removeContainedBy(0L, windowStart);

            List<Task> pp = predictions.searchContainedBy(windowStart, windowEnd);
            IntHashSet pi = new IntHashSet();
            for (Task tf : pp) {
                if (tf.isDeleted())
                    continue;

                char cc = tf.term().toString().charAt("(y".length());
                int f = cc - '0';
                
                
                
                

                pi.add(Math.round((f / discretization) * cols));
                if (Math.abs(f-val) > 1) {
                    System.err.println(f + " vs actual " + val );
                    System.err.println(tf.proof());
                } else {
                    System.out.println("OK: " + tf);
                    System.out.println(tf.proof());
                }
            }

            for (int i = 0; i <= cols; i++) {


                char c;

                if (pi.contains(i))
                    c = 'X';
                else if (i == colActual)
                    c = '#';
                else
                    c = '.';

                out.print(c);

            }

            out.println();







            lastsignal = signal;
            lasttime = n.time();

            
            int dt = 1;
            /*if (last!=val && chg.set("((y" + val + ") &&-" + dt + " (--,(y" + last + "))). :|:")) {
                last = val;
            }*/



            if (chg.set("(y" + val + "). :|:")) {
                if (last != -1) {
                    n.input("(y" + last + "). :|: %0.00;0.90%");
                }
                last = val;
            }

            
            /*} else if (cnt==1000){
                System.out.println("observation phase end, residual predictions follow");
            }*/

            
            n.que($.$("(?X)"), QUESTION, n.time() + thinkInterval / 2);
            

        }

    }

    private static void predict(long time, Task v) {
        
        predictions.put(time, v);
    }
}