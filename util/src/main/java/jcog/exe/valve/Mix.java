package jcog.exe.valve;

import com.google.common.base.Joiner;
import jcog.bag.impl.ArrayBag;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.Nullable;

/** base class; does nothing (token indicator) */
public class Mix<Who, What, X extends Share<Who,What>> extends ArrayBag<Who, X> {

    public final What what;

    protected Mix(What what) {
        super(PriMerge.max /* unused */, Sharing.MAX_CUSTOMERS);
        this.what = what;
    }

    /** prepare for next cycle */
    @Override public Mix<Who, What, X> commit() {
        
        if (isEmpty())
            return this;

        final float[] sum = {0};
        forEach(s -> {
            sum[0] += s.need;
        });
        float summ = sum[0];
        if (summ < Float.MIN_NORMAL) {
            
            commit(s -> s.priSet(0.5f));
        } else {
            commit(s -> s.priSet(s.need / summ)); 
            
        }

        return this;
    }

    @Override
    public String toString() {
        return Joiner.on(",").join(this);
    }

    @Nullable
    @Override
    public Who key(X rShare) {
        return rShare.who;
    }
}
