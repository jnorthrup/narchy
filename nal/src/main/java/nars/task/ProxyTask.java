package nars.task;

import jcog.WTF;
import jcog.pri.ScalarValue;
import jcog.pri.UnitPri;
import nars.NAL;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** implementations are immutable but will usually have a different hash and
  * equality than the origin task. hashcode here is calculated lazily.
 * the only mutable components are the hashcode and the cyclic status which is optionally inherited from the source.
 * */
public class ProxyTask extends UnitPri implements Task {

    public final Task task;

    private int hash = 0;

    volatile long creation;
    private volatile boolean cyclic = false;

    public ProxyTask(Task task) {

        this.task = task;
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

        if (inheritCyclic() && task.isCyclic())
            setCyclic(true);
    }


    protected boolean inheritCyclic() {
        return true;
    }

    @Override
    public float freq(long start, long end) {
        return task.freq(start, end);
    }

    @Override
    public String toString() {
        return appendTo(null).toString();
    }

    @Override
    public final void setCyclic(boolean b) {
        this.cyclic = b;
    }

    /** produce a concrete, non-proxy clone */
    public Task clone() {
        return Task.clone(this);
    }

    @Override
    public final boolean isCyclic() {
        return cyclic;
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
        if (h == 0)
            return this.hash = Task.hash(term(), truth(), punc(), start(), end(), stamp());
        else
            return h;
    }

    @Override
    @Deprecated public Task next(Object w) {
        throw new WTF();
    }

    @Override
    public short[] why() {
        return task.why();
    }





    @Override
    public long creation() {
        //updated to the latest of this or the proxy'd task's creation (as it may change)
        long next = Math.max(task.creation(), creation);
        this.creation = next;
        return next;
    }
    @Override
    public void setCreation(long creation) {
        this.creation = creation;
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

    /** produce a concrete non-proxy clone of this */
    @Nullable public NALTask the() {
        return Task.clone(this);
    }
}
