package nars.table.eternal;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.pri.Prioritizable;
import jcog.sort.SortedArray;
import jcog.util.LambdaStampedLock;
import nars.$;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.control.CauseMerge;
import nars.control.op.Remember;
import nars.table.BeliefTable;
import nars.task.NALTask;
import nars.task.util.Answer;
import nars.task.util.Revision;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Intermpolate;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.table.BeliefTable.eternalTaskValueWithOriginality;
import static nars.time.Tense.ETERNAL;


/**
 * Created by me on 5/7/16.
 */
public class EternalTable extends SortedArray<Task> implements BeliefTable, FloatFunction<Task> {

    private final LambdaStampedLock lock = new LambdaStampedLock();

    public EternalTable(int initialCapacity) {
        super();
        setTaskCapacity(initialCapacity);
    }

    @Override
    public final void forEachTask(long minT, long maxT, Consumer<? super Task> x) {
        forEachTask(x);
    }

    @Override
    public final void forEachTask(Consumer<? super Task> x) {
        forEach(x);
    }

    @Override
    public void forEach(Consumer<? super Task> x) {
        lock.readOptimistic(()->super.forEach(x));
    }


    @Override
    public final int taskCount() {
        return size();
    }

    @Override
    public final Stream<? extends Task> taskStream() {


//        Object[] list = this.items;
//        int size = Math.min(list.length, this.size);
//        if (size == 0)
//            return Stream.empty();
//        else {
//            return ArrayIterator.streamNonNull((Task[]) list, size);
//        }
        return stream();
    }

//    @Override protected final Task[] copyOfArray(Task[] x, int s) {
//        Task[] y = new Task[s];
//        System.arraycopy(x, 0, y, 0, Math.min(y.length, x.length));
//        return y;
//    }

    @Override
    public void match(Answer a) {
        //long r = lock.readLock();
        //try {
        lock.readOptimistic(()->whileEach(a));
//        } finally {
//            lock.unlockRead(r);
//        }
    }

    @Override
    public void setTaskCapacity(int c) {
        if (capacity() == c)
            return;

        assert (c >= 0);

        List<Task> trash = null;

        long r = lock.writeLock();
        try {
            int wasCapacity = this.capacity();
            if (wasCapacity != c) {

                int s = size;
                if (s > c) {
                    trash = new FasterList(s - c);
                    while (c < s--) {
                        trash.add(removeLast());
                    }
                }

                resize(c);
            }
        } finally {
            lock.unlockWrite(r);
        }

        if (trash != null)
            trash.forEach(Task::delete);
    }


    @Override
    public Task[] taskArray() {

        //long l = lock.readLock();
//        try {
        int s = this.size;
        if (s == 0)
            return Task.EmptyArray;
        else {
            Task[] list = this.items;
            return Arrays.copyOf(list, Math.min(s, list.length));
        }
//        } finally {
//            lock.unlock(l);
//        }

    }


    //    @Override
    public void clear() {

        int sizeEstimate = size();
        long l = sizeEstimate == 0 ? lock.readLock() /* may not need to acquire write lock */: lock.writeLock();
        try {
            if (size() > 0) {

                if (StampedLock.isReadLockStamp(l))
                    l = Util.readToWrite(l, lock);

                //forEach(ScalarValue::delete);
                super.delete();
            }
        } finally {
            lock.unlock(l);
        }

    }

    /**
     * for ranking purposes.  returns negative for descending order
     */
    @Override
    public final float floatValueOf(Task w) {
        return (float) -eternalTaskValueWithOriginality(w);
    }


    /** direct insert; not ordinarily invoked from external */
    @Nullable
	public Task insert(Task incoming) {


        Task displaced = null;

        if (size == capacity()) {
            Task weakestPresent = last();
            if (weakestPresent != null) {
                if (eternalTaskValueWithOriginality(weakestPresent)
                        <=
                        eternalTaskValueWithOriginality(incoming)) {
                    displaced = removeLast();
                } else {
                    return incoming; //rejected
                }
            }
        }

//        if (incoming instanceof ProxyTask) {
//            incoming = ((ProxyTask) incoming).the();
//        }

        int r = add(incoming, this);
        assert (r != -1);

        return displaced;

    }

    public final Truth truth() {
        Task s = first();
        return s != null ? s.truth() : null;
    }


    @Override
    public boolean removeTask(Task x, boolean delete) {

        if (!x.isEternal())
            return false;

        Task removed = null;

        //TODO use optimistic read here
        long r = lock.readLock();
        try {

            Task[] items = this.items;

            int index = indexOf(x, this);

            if (index != -1) {
                Task xx = items[index];

                r = Util.readToWrite(r, lock);

                if (items[index] != xx) { //moved while waiting for lock, retry:
                    index = indexOf(x, this);
                }

                if (index != -1) {
                    boolean wasRemoved = removeFast(xx, index);
                    assert (wasRemoved);
                    removed = xx;
                }
            }

        } finally {
            lock.unlock(r);
        }

        if (removed != null) {
            if (delete)
                removed.delete();
            return true;
        } else {
            return false;
        }
    }



    @Override
    public final void remember(Remember r) {
        if (!r.input.isEternal())
            return;

        Task input = r.input, existing = null;

        long l = lock.readLock();
        try {
            if (size > 0) {

                //scan list for existing equal task
                Object[] list = this.items;
                for (Object aList : list) {
                    if (aList == null)
                        break;

                    Task x = (Task) aList;
                    if (x.equals(input)) {
                        existing = x;
                        break;
                    }
                }
            }

            if (existing == null) {
                l = reviseOrTryInsertion(r, l);
                return;
            }
        } finally {
            lock.unlock(l);
        }

        r.merge(existing);

    }

    /**
     * lock begins as read
     */
    private long reviseOrTryInsertion(Remember r, long lock) {
        Object[] list = this.items;

        Task input = r.input;

        Task oldBelief = null;
        Truth conclusion = null;

        Term newTerm = null;
        Term inputTerm = input.term();
        float aProp = Float.NaN;
        final double ie = input.evi();

        NAR nar = r.nar;
        long[] inputStamp = input.stamp();

        for (Object _x : list) {
            if (_x == null) break; //HACK
            Task x = (Task) _x;

            Truth yt = null;

            Term nt;
            Truth xt = x.truth();

            if (Stamp.overlapsAny(inputStamp, x.stamp())) {

                //HACK interpolate truth if only freq differs
                if ((!x.isCyclic() && !input.isCyclic()) &&
                        Arrays.equals(x.stamp(), input.stamp()) &&
                        Util.equals(x.conf(), input.conf(), nar.confResolution.floatValue())) {

                    yt = $.t((x.freq() + input.freq()) / 2, x.conf()).dither(nar);
                }

            } else {


                yt = Revision.revise(input.truth(), xt);
            }

            if (yt != null) {

                float _aProp = (float) (ie / (ie + x.evi()));
                if (inputTerm instanceof Compound) {
                    nt =
                            Intermpolate.intermpolate(
                                    (Compound)inputTerm, (Compound)(x.term()),
                                    _aProp,
                                    nar
                            );

                    if (!nt.op().taskable)
                        continue;
                    if (!nt.concept().equals(inputTerm.concept()))
                        continue;
                } else {
                    nt = inputTerm;
                }

                yt = yt.dither(nar);

                if (yt == null || (nt.equals(inputTerm) && ((yt.equalTruth(xt, nar) || yt.equalTruth(input.truth(), nar)))))
                    continue;

                if (conclusion != null) {
                    //a previous conclusion exists; try if by originality this one is stronger
                    if (conclusion.evi() * oldBelief.originality() >= yt.evi() * x.originality()) {
                        continue;
                    }
                }

                newTerm = nt;
                conclusion = yt;
                oldBelief = x;
                aProp = _aProp;
            }
        }

        NALTask revised;
        if (oldBelief != null) {


            Task finalOldBelief = oldBelief;
            float finalAProp = aProp;
            revised = Task.tryTask(newTerm, input.punc(), conclusion, (term, revisionTruth) -> NALTask.the(term, input.punc(), revisionTruth, nar.time(), ETERNAL, ETERNAL, Stamp.merge(input.stamp(), finalOldBelief.stamp(), finalAProp, nar.random()))
            );
            if (revised != null) {
                //TODO maybe based on relative evidence
                revised.pri(Prioritizable.fund(Math.max(finalOldBelief.priElseZero(), input.priElseZero()), false, finalOldBelief, input));
                revised.cause(CauseMerge.AppendUnique.merge(NAL.causeCapacity.intValue(), input, finalOldBelief));

            }
        } else {
            revised = null;
        }

        lock = Util.readToWrite(lock, this.lock);

        if (revised == null) {
            tryPut(input, r);
        } else {
            if (tryPut(revised, r)) {
                if (tryPut(input, r)) {
                    //inserted both
                }
                //inserted only the revision
            }
            //could not insertion revision
        }

        return lock;
    }


    /**
     * try to insert but dont delete the input task if it wasn't inserted (but delete a displaced if it was)
     * returns true if it was inserted, false if not
     */
    private boolean tryPut(Task x, Remember r) {

        Task displaced = insert(x);

        if (displaced == x) {
            r.forget(x);
        } else {
            if (displaced != null) {
                r.forget(displaced);
            }

            r.remember(x);
        }

        return true;
    }


//    @Nullable
//    public Truth strongestTruth() {
//        Task e = first();
//        return (e != null) ? e.truth() : null;
//    }


//    /**
//     * TODO batch eternalize multiple removed tasks together as one attempted task
//     */
//    public Task eternalize(Task x, int tableCap, long tableDur, NAR nar) {
//
//        assert (!x.isDeleted());
//        float factor = Math.max((1f / tableCap), (float) Util.unitize((((double) x.range()) / (1 + tableDur))));
//
//        float eviMin;
//        //synchronized (this) {
//        if (size() == capacity()) {
//            Task w = last();
//            eviMin = w != null ? w.evi() : 0;
//        } else {
//            eviMin = 0;
//        }
//        //}
//        Task eternalized = Task.eternalized(x, factor, eviMin, nar);
//
//        if (eternalized == null)
//            return null;
//
//        float xPri = x.priElseZero();
//
//        eternalized.pri(xPri * factor);
//
//        if (Param.DEBUG)
//            eternalized.log("Eternalized Temporal");
//
//        return eternalized;
//
//    }


}
