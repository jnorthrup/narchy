package nars.task;

import jcog.pri.ScalarValue;
import nars.NAL;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** implementations are immutable but will usually have a different hash and
  * equality than the origin task. hashcode here is calculated lazily.
 * the only mutable components are the hashcode and the cyclic status which is optionally inherited from the source.
 * */
public class ProxyTask extends AbstractTask {

    public final Task task;

    private int hash = 0;



    protected ProxyTask(Task task) {

        this.task = task;
        this.why = task.why();
//        if (task instanceof TaskProxy) {
//            //System.out.println(task.getClass() + " may be unwrapped for " + getClass());
//            throw new WTF(task.getClass() + " may be unwrapped for use as base of " + getClass());
//        }

        creation = task.creation();

        float p = task.pri();
        if (p!=p) {
            if (NAL.DELETE_PROXY_TASK_TO_DELETED_TASK)
                delete();
            else
                pri(ScalarValue.EPSILON);
        } else
            pri(p);

        if (task.isCyclic())
            setCyclic(true);
    }

    @Deprecated protected boolean validated() {
        return false;
    }

    @Override
    public float freq(long start, long end) {
        return task.freq(start, end);
    }

    @Override
    public String toString() {
        return appendTo(null).toString();
    }



    /** produce a concrete, non-proxy clone */
    public Task the() {
        if (validated()) {
            return NALTask.the(term(), punc(), truth(), creation(), start(), end(), stamp());
        } else {
            return Task.clone(this); //runs validation first
        }
    }


    @Override
    public Term term() {
        return task.term();
    }

    @Override
    public boolean equals(Object obj) {
        return Task.equal(this, obj);
    }

    @Override
    public int hashCode() {
        int h = this.hash;
        return h != 0 ? h :
            (this.hash = Task.hash(term(), truth(), punc(), start(), end(), stamp()));
    }

    @Override
    public long creation() {
        //updated to the latest of this or the proxy'd task's creation (as it may change)
        long next = Math.max(task.creation(), creation);
        this.creation = next;
        return next;
    }


    @Override
    public long start() {
        return task.start();
    }

    @Override
    public long end() {
        return task.end();
    }

    @Override
    public long[] stamp() {
        return task.stamp();
    }

    @Override
    public @Nullable Truth truth() {
        return task.truth();
    }

    @Override
    public byte punc() {
        return task.punc();
    }



}
