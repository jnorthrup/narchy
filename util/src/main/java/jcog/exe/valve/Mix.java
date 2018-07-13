package jcog.exe.valve;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.ScalarValue;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.Nullable;

/**
 * base class; does nothing (token indicator)
 */
public class Mix<Who, What, X extends Share<Who, What>> extends ArrayBag<Who, X> {

    public final What what;


    protected Mix(What what) {
        super(PriMerge.max /* unused */, Sharing.MAX_CUSTOMERS);
        this.what = what;
    }

    /**
     * prepare for next cycle
     */
    @Override
    public Mix<Who, What, X> commit() {

        if (isEmpty())
            return this;

        final float[] min = {Float.POSITIVE_INFINITY}, max = {Float.NEGATIVE_INFINITY};
        forEach(s -> {
            float need = s.need;

            min[0] = Math.min(min[0], need);
            max[0] = Math.max(max[0], need);
        });


        float range = max[0] - min[0];
        int n = size();
        if (range < ScalarValue.EPSILON * n) {
            float each = 1f / n;
            commit(s -> s.pri(each));
        } else {
            float minn = min[0];
            commit(s -> s.pri(Util.clamp(((s.need - minn) / range),0.1f/n,1f/n)));

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
