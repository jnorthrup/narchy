package nars.table.eternal;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.pri.Prioritizable;
import jcog.sort.SortedArray;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.CauseMerge;
import nars.control.op.Remember;
import nars.table.BeliefTable;
import nars.task.NALTask;
import nars.task.Revision;
import nars.task.TaskProxy;
import nars.task.util.Answer;
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

    private final StampedLock lock = new StampedLock();

    public EternalTable(int initialCapacity) {
        super();
        setTaskCapacity(initialCapacity);
    }

    @Override
    public final void forEachTask(Consumer<? super Task> x) {
//        long r = lock.readLock();
//        try {
            this.forEach(x);
//        } finally {
//            lock.unlockRead(r);
//        }
    }


    @Override
    public final Stream<? extends Task> streamTasks() {


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
    public void match(Answer t) {
        //long r = lock.readLock();
        //try {
            whileEach(t::tryAccept);
//        } finally {
//            lock.unlockRead(r);
//        }
    }

    @Override
    public void setTaskCapacity(int c) {
        assert (c >= 0);

        if (capacity() == c)
            return;

        List<Task> trash = null;

        long r = lock.writeLock();
        try {
            int wasCapacity = this.capacity();

            //synchronized (this) {
            if (wasCapacity != c) {

                //r = lock.tryConvertToWriteLock(r); //TODO

                int s = size;
                if (s > 0 && (s > c)) {
                    trash = new FasterList(s - c);
                    while (c < s--) {
                        trash.add(removeLast());
                    }
                }

                resize(c);
            }
            //}
        } finally {
            lock.unlock(r);
        }

        if (trash != null)
            trash.forEach(Task::delete);
    }


    @Override
    public Task[] toArray() {

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

        if (size() > 0) {
            //long l = lock.readLock();
            long l = lock.writeLock();
            try {
                if (size() > 0) {
                    //        forEach(ScalarValue::delete);
                    super.clear();
                }
            } finally {
                lock.unlock(l);
            }
        }

    }

    /**
     * for ranking purposes.  returns negative for descending order
     */
    @Override
    public final float floatValueOf(Task w) {
        return -eternalTaskValueWithOriginality(w);
    }


    @Nullable
    private Task put(Task incoming) {


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


        if (incoming instanceof TaskProxy) {
            incoming = ((TaskProxy) incoming).the();
        }

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

        Task removed;

        long r = lock.readLock();
        try {

            int index = indexOf(x, this);
            if (index != -1) {
                long rr = lock.tryConvertToWriteLock(r);
                if (rr==0) { lock.unlockRead(r); r = lock.writeLock(); } else { r = rr; }
                removed = remove(index);
                assert (removed != null);
            } else {
                removed = null;
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
    public void forEachTask(long minT, long maxT, Consumer<? super Task> x) {
        forEachTask(x);
    }

    @Override
    public final void add(Remember r, NAR nar) {
        if (!r.input.isEternal())
            return;

        tryAdd(r, nar);
    }

    /** lock begins as read */
    private long reviseOrTryInsertion(Remember r, NAR nar, long lock) {
        Object[] list = this.items;

        Task input = r.input;

        Task oldBelief = null;
        Truth conclusion = null;

        Term newTerm = null;
        Term inputTerm = input.term();
        float aProp = Float.NaN;

        for (Object aList : list) {
            if (aList == null)
                break;
            Task x = (Task) aList;
            float xconf = x.conf();
            if (Stamp.overlapsAny(input, x)) {

                //HACK interpolate truth if only freq differs
                if ((!x.isCyclic() && !input.isCyclic()) &&
                        Arrays.equals(x.stamp(), input.stamp()) &&
                        Util.equals(xconf, input.conf(), nar.confResolution.floatValue())) {

                    conclusion = $.t((x.freq() + input.freq()) / 2, xconf).dither(nar);

                }

            } else {

                Truth xt = x.truth();

                final double newBeliefWeight = input.evi();

                aProp = (float) (newBeliefWeight / (newBeliefWeight + x.evi()));

                Term xTerm = x.term();
                Term nt;
                if (inputTerm.equals(xTerm)) {
                    nt = inputTerm;
                } else {
                    nt =
                            Intermpolate.intermpolate(
                                    inputTerm, xTerm,
                                    aProp,
                                    nar
                            );

                    if (!nt.op().taskable)
                        continue;
                    if (!nt.concept().equals(inputTerm.concept()))
                        continue;
                }

                Truth newBeliefTruth = input.truth();

                Truth yt = Revision.revise(newBeliefTruth, xt);
                if (yt == null)
                    continue;

                yt = yt.dither(nar);
                if (yt == null || (nt.equals(inputTerm) && ((yt.equalsIn(xt, nar) || yt.equalsIn(newBeliefTruth, nar)))))
                    continue;

                newTerm = nt;
                conclusion = yt;
                oldBelief = x;
            }

        }

        NALTask revised;
        if (oldBelief != null && conclusion != null) {


            Task finalOldBelief = oldBelief;
            float finalAProp = aProp;
            revised = Task.tryTask(newTerm, input.punc(), conclusion, (term, revisionTruth) -> NALTask.the(term, input.punc(), revisionTruth, nar.time(), ETERNAL, ETERNAL, Stamp.merge(input.stamp(), finalOldBelief.stamp(), finalAProp, nar.random()))
            );
            if (revised != null) {
                //TODO maybe based on relative evidence
                revised.pri(Prioritizable.fund(Math.max(finalOldBelief.priElseZero(), input.priElseZero()), false, finalOldBelief, input));
                revised.cause(CauseMerge.AppendUnique.merge(Param.causeCapacity.intValue(), input, finalOldBelief));


                if (Param.DEBUG)
                    revised.log("Insertion Revision");


            }
        } else {
            revised = null;
        }

        long ll = this.lock.tryConvertToWriteLock(lock);
        if (ll==0) {
            this.lock.unlockRead(lock);
            lock = this.lock.writeLock();
        } else {
            lock = ll;
        }

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

    public void tryAdd(Remember r, NAR nar) {

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

            if (existing==null) {
                l = reviseOrTryInsertion(r, nar, l);
                return;
            }
        } finally {
            lock.unlock(l);
        }

        if (existing != null) {
            r.merge(existing, nar);
            return;
        }

        int cap = capacity();
        if (cap == 0) {
            if (input.isInput())
                throw new RuntimeException("input task rejected by " + EternalTable.class + " with 0 capacity): " + input);
            r.forget(input);
        }

    }


    /**
     * try to insert but dont delete the input task if it wasn't inserted (but delete a displaced if it was)
     * returns true if it was inserted, false if not
     */
    private boolean tryPut(Task x, Remember r) {

        Task displaced = put(x);

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
