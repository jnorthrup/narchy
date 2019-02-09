package nars.link;

import jcog.math.FloatSupplier;
import jcog.memoize.HijackMemoize;
import jcog.pri.OverflowDistributor;
import jcog.pri.PriBuffer;
import jcog.pri.Prioritizable;
import jcog.pri.ScalarValue;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.BufferedBag;
import jcog.pri.op.PriMerge;
import jcog.sort.TopN;
import nars.NAR;
import nars.Param;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class TaskLinkBag extends BufferedBag.SimpleBufferedBag<TaskLink,TaskLink> {

    private final FloatSupplier forgetRate;

    public TaskLinkBag(Bag<TaskLink, TaskLink> activates, FloatSupplier forgetRate, boolean concurrent) {
        super(activates, new TaskLinkBuffer(Param.tasklinkMerge, concurrent));
        this.forgetRate = forgetRate;
    }

    @Override
    protected final TaskLink keyInternal(TaskLink c) {
        return c;
    }

    public void forget(NAR nar) {

        commit(nar.attn.forgetting.forget(this, 1f, forgetRate.asFloat()));

        tangentAtoms.clear();

    }



    private static class TaskLinkBuffer extends PriBuffer<TaskLink> {

        public TaskLinkBuffer(PriMerge merge, boolean concurrent) {
            super(merge, concurrent);
        }

        @Override
        protected void merge(Prioritizable existing, TaskLink incoming, float pri, OverflowDistributor<
                TaskLink> overflow) {
            //super.merge(existing, incoming, pri, overflow);
            ((TaskLink) existing).merge(incoming, merge);
        }
    }

    /** tangent ranking cache; cleared on each update
     *  TODO store a sampler, not an entire TopN<TaskLink>
     * */
    private final HijackMemoize<Atomic,TopN<TaskLink>> tangentAtoms = new HijackMemoize<Atomic,TopN<TaskLink>>((Atomic x)->{
        TopN<TaskLink> match = null;

        for (TaskLink t : this) {
            if (t == null) continue; //HACK
            float xp = t.priElseZero();
            if (match == null || xp > match.minValueIfFull()) {
                Term y = atomOther(x, t);
                if (y != null) {
                    if (match==null) {
                        //TODO pool
                        int cap = Math.max(3, (int) Math.ceil(Math.sqrt(capacity()))); //heuristic
                        match = new TopN<>(new TaskLink[cap], (FloatFunction<TaskLink>) ScalarValue::pri);
                        match.setCapacity(cap);
                    }
                    match.add(t);
                }
            }
        }

        if(match!=null) {
            match.compact(0.75f);
            return match;
        } else
            return TopN.Empty;
    }, 96, 3);



    /** acts as a virtual tasklink bag associated with an atom concept allowing it to otherwise act as a junction between tasklinking compounds which share it */
    @Nullable public Term atomTangent(Atomic src, TaskLink except, Random rng) {
        TopN<TaskLink> match = tangentAtoms.apply(src);
        TaskLink l;
        switch (match.size()) {
            case 0:
                l = except;
                break;
            case 1:
                l = match.get(0);
                break;
            case 2:
                TaskLink a = match.get(0), b = match.get(1);
                l = a.equals(except) ? b : a;
                break;
            default:
                l = match.getRoulette(rng, (t) -> !t.equals(except));
                break;
        }

        return l!=null ? l.other(src) : null;
    }


    @Nullable
    static private Term atomOther(Term include, TaskLink t) {
        Term tSrc = t.source();
        if (include.equals(tSrc)) {
            Term y = t.target();
            return y; //!t.isSelf() ? y : null;
        }
//        if (include.equals(t.target())) {
//            Term y = tSrc;
//            return y; //!t.isSelf() ? y : null;
//        }
        return null;
    }


}
