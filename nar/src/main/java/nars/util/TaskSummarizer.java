package nars.util;

import jcog.Util;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.control.NARPart;
import nars.term.Term;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static jcog.Texts.n2;
import static nars.Op.*;

/** fast summarization of certain Task features used for control feedback.  gathers statistics concurrently
 *  and produces a snapshot on request */
public class TaskSummarizer extends NARPart implements Consumer<Task> {

    final AtomicReference<TaskSummary> summary = new AtomicReference<>();

    public TaskSummarizer(NAR n) {
        super(n);
    }

    public static class TaskSummary implements Consumer<Task> /* TODO: extends AtomicIntegerArray or AtomicTensor */ {

        static int puncToIndex(byte p) {
            switch (p) {
                case BELIEF: return 0;
                case GOAL: return 1;
                case QUESTION: return 2;
                case QUEST: return 3;
            }
            return -1;
        }
        static byte indexToPunc(int p) {
            switch (p) {
                case 0: return BELIEF;
                case 1: return GOAL;
                case 2: return QUESTION;
                case 3: return QUEST;
            }
            return -1;
        }

        static final int NUM = 0;
        static final int VOL = 1;
        static final int PRI = 2;

        static final int PRI_DIGITS = 16; //priority will be mapped to 0..PRI_DIGITS-1 integers

        final AtomicInteger[][][] val = new AtomicInteger[4][Op.values().length][3];

        TaskSummary() {
            for (AtomicInteger[][] a : val) {
                for (AtomicInteger[] b : a) {
                    for (int k = 0; k < 3; k++)
                        b[k] = new AtomicInteger(0);
                }
            }
        }

        private static int pri(float p) {
            return Util.bin(p,PRI_DIGITS);
        }

        @Override
        public void accept(Task task) {
            int p = puncToIndex(task.punc());
            if (p < 0) return;
            Term tt = task.term();
            int o = tt.op().ordinal();
            AtomicInteger[] v = val[p][o];
            v[NUM].incrementAndGet();
            v[VOL].addAndGet(tt.volume());
            v[PRI].addAndGet(1 + pri(task.priElseZero()));

            //TODO something with tt.structure() maybe using a lossy structure
        }

        public final void print() {
            print(System.out);
        }

        public void print(PrintStream out) {
            forEach((punc, num, o, volMean, priMean) ->
                out.println(o.str + ' ' + ((char) punc) + " x " + num + ", volMean=" + n2(volMean) + " priMean=" + n2(priMean))
            );
        }

        public void forEach(CellConsumer c) {
            for (int pi = 0, valLength = val.length; pi < valLength; pi++) {
                AtomicInteger[][] a = val[pi];
                byte punc = indexToPunc(pi);
                for (int oi = 0, aLength = a.length; oi < aLength; oi++) {
                    AtomicInteger[] x = a[oi];
                    int num = x[0].get();
                    if (num > 0) {
                        Op o = Op.the(oi);
                        float volMean = x[1].floatValue() / num;
                        float priMean = Math.max(0, ((x[2].floatValue()-1)/PRI_DIGITS) / num);
                        c.accept(punc, num, o, volMean, priMean);
                    }
                }
            }
        }

        @FunctionalInterface interface CellConsumer {
            void accept(byte punc, int num, Op o, float volMean, float priMean);
        }

    }

    @Override
    protected void starting(NAR nar) {
        summary.set(new TaskSummary());
        super.starting(nar);
    }

    @Override
    public void accept(Task task) {
        summary.get().accept(task);
    }

    public TaskSummary snapshot() {
        return summary.getAndSet(new TaskSummary());
    }

}
