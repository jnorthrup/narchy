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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.table.BeliefTable.eternalTaskValueWithOriginality;
import static nars.time.Tense.ETERNAL;


/**
 * Created by me on 5/7/16.
 */
public class EternalTable extends SortedArray<Task> implements BeliefTable, FloatFunction<Task> {


    public EternalTable(int initialCapacity) {
        super();
        setTaskCapacity(initialCapacity);
    }

    @Override
    public final void forEachTask(Consumer<? super Task> x) {
//        Task[] a = toArray();
//        for (int i = 0, aLength = Math.min(size, a.length); i < aLength; i++) {
//            Task y = a[i];
//            if (y == null)
//                break;
//            if (!y.isDeleted())
//                x.accept(y);
//        }
        this.forEach(x);
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
        whileEach(t::tryAccept);
    }


    public void setTaskCapacity(int c) {
        assert (c >= 0);

        int wasCapacity = this.capacity();
        if (wasCapacity != c) {

            List<Task> trash = null;
            synchronized (this) {
                if (wasCapacity != c) {

                    wasCapacity = capacity();

                    int s = size;
                    if (s > 0 && (s > c)) {


                        trash = new FasterList(s - c);
                        while (c < s--) {
                            trash.add(removeLast());
                        }
                    }

                    if (wasCapacity != c)
                        resize(c);
                }
            }


            if (trash != null) {


                trash.forEach(Task::delete);

            }

        }
    }


    @Override
    public Task[] toArray() {

        int s = this.size;
        if (s == 0)
            return Task.EmptyArray;
        else {
            Task[] list = this.items;
            return Arrays.copyOf(list, Math.min(s, list.length));

        }

    }


    //    @Override
    public synchronized void clear() {
//        forEach(ScalarValue::delete);
        super.clear();
    }

    /**
     * for ranking purposes.  returns negative for descending order
     */
    @Override
    public final float floatValueOf(Task w) {
        return -eternalTaskValueWithOriginality(w);
    }


    @Nullable
    private synchronized Task put(Task incoming) {


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
            incoming = ((TaskProxy)incoming).the();
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
        synchronized (this) {
            int index = indexOf(x, this);
            if (index != -1) {
                removed = remove(index);
                assert (removed != null);
            } else {
                removed = null;
            }
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

        if (!contains(r, nar)) {
            reviseOrTryInsertion(r, nar);
        }
    }

    private synchronized void reviseOrTryInsertion(Remember r, NAR nar) {
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

                    conclusion = $.t((x.freq() + input.freq()) / 2, xconf).dithered(nar);

                }

            } else {

                Truth xt = x.truth();

                final float newBeliefWeight = input.evi();

                aProp = newBeliefWeight / (newBeliefWeight + x.evi());

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

                yt = yt.dithered(nar);
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
            revised = Task.tryTask(newTerm, input.punc(), conclusion, (term, revisionTruth) ->
                    new NALTask(term,
                            input.punc(),
                            revisionTruth,
                            nar.time() /* creation time */,
                            ETERNAL, ETERNAL,
                            Stamp.merge(input.stamp(), finalOldBelief.stamp(), finalAProp, nar.random())
                    )
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
    }

    public boolean contains(Remember r, NAR nar) {

        Task input = r.input, existing = null;

        synchronized (this) {
            if (size == 0)
                return false;

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

        if (existing != null) {
            r.merge(existing, nar);
            return true;
        }

        int cap = capacity();
        if (cap == 0) {
            if (input.isInput())
                throw new RuntimeException("input task rejected by " + EternalTable.class +  " with 0 capacity): " + input);
            r.forget(input);
            return true;
        }

        return false;
    }

    @Override
    public final Truth truth(long start, long end, @Nullable Term template, Predicate<Task> filter, NAR n) {
        return truth();
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


    @Nullable
    public Truth strongestTruth() {
        Task e = first();
        return (e != null) ? e.truth() : null;
    }


    /**
     * TODO batch eternalize multiple removed tasks together as one attempted task
     */
    public Task eternalize(Task x, int tableCap, long tableDur, NAR nar) {

        assert (!x.isDeleted());
        float factor = Math.max((1f / tableCap), (float) Util.unitize((((double) x.range()) / (1 + tableDur))));

        float eviMin;
        //synchronized (this) {
        if (size() == capacity()) {
            Task w = last();
            eviMin = w != null ? w.evi() : 0;
        } else {
            eviMin = 0;
        }
        //}
        Task eternalized = Task.eternalized(x, factor, eviMin, nar);

        if (eternalized == null)
            return null;

        float xPri = x.priElseZero();

        eternalized.pri(xPri * factor);

        if (Param.DEBUG)
            eternalized.log("Eternalized Temporal");

        return eternalized;

    }

}
