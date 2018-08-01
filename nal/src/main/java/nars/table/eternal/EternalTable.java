package nars.table.eternal;

import jcog.Util;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FasterList;
import jcog.pri.Priority;
import jcog.sort.SortedArray;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.control.proto.Remember;
import nars.table.TaskTable;
import nars.task.NALTask;
import nars.task.Revision;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.table.BeliefTable.eternalTaskValueWithOriginality;
import static nars.time.Tense.ETERNAL;


/**
 * Created by me on 5/7/16.
 */
public class EternalTable extends SortedArray<Task> implements TaskTable, FloatFunction<Task> {

    public static final EternalTable EMPTY = new EternalTable(0) {


        @Override
        public boolean removeTask(Task x) {
            return false;
        }

        @Override
        public void add(Remember input, NAR nar) {
        }


        @Override
        public void setCapacity(int c) {

        }

        @Override
        public void forEachTask(Consumer<? super Task> action) {

        }


        @Override
        public Iterator<Task> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    };


    public EternalTable(int initialCapacity) {
        super();
        setCapacity(initialCapacity);
    }

    @Override
    public void forEachTask(Consumer<? super Task> x) {
        Task[] a = toArray();
        for (int i = 0, aLength = Math.min(size, a.length); i < aLength; i++) {
            Task y = a[i];
            if (y == null)
                break;
            if (!y.isDeleted())
                x.accept(y);
        }
    }

    public Task select(@Nullable Predicate<? super Task> selector) {
        if (selector == null)
            return first();

        Task[] a = toArray();
        for (int i = 0, aLength = Math.min(size, a.length); i < aLength; i++) {
            Task x = a[i];
            if (x == null)
                break;
            if (selector.test(x))
                return x;
        }
        return null;
    }

    @Override
    public Stream<Task> streamTasks() {


        Object[] list = this.items;
        int size = Math.min(list.length, this.size);
        if (size == 0)
            return Stream.empty();
        else {
            return ArrayIterator.stream((Task[]) list, size).filter(Objects::nonNull);
        }
    }

    @Override
    protected Task[] newArray(int s) {
        return new Task[s];
    }

    public void setCapacity(int c) {
        assert( c>= 0);

        int wasCapacity = this.capacity();
        if (wasCapacity != c) {

            List<Task> trash = null;
            synchronized (this) {

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
            return Arrays.copyOf(list, Math.min(s, list.length), Task[].class);

        }

    }


    @Override
    public void clear() {
        synchronized (this) {
            super.clear();
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
    private Task put(final Task incoming) {

        synchronized (this) {
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

            int r = add(incoming, this);
            assert (r != -1);

            return displaced;
        }

    }

    public final Truth truth() {
        Task s = first();
        return s != null ? s.truth() : null;
    }


    @Override
    public boolean removeTask(Task x) {


        synchronized (this) {
            x.delete();

            int index = indexOf(x, this);
            if (index == -1)
                return false;

            int findAgainToBeSure = indexOf(x, this);
            return (findAgainToBeSure != -1) && remove(findAgainToBeSure) != null;
        }


    }

    @Override
    public void add(Remember r, NAR nar) {

        Task input = r.input;

        add(r, nar, input);
    }

    public void add(Remember r, NAR nar, Task input) {
        int cap = capacity();
        if (cap == 0) {
            r.forget(input);
            /*if (input.isInput())
                throw new RuntimeException("input task rejected (0 capacity): " + input + " "+ this + " " + this.capacity());*/
            return;
        }


        /**
         * @return null: no revision could be applied
         * ==newBelief: existing duplicate found
         * non-null: revised task
         */

        Object[] list = this.items;

        Task oldBelief = null;
        Truth conclusion = null;

        Truth newBeliefTruth = input.truth();

        synchronized (this) {
            for (Object aList: list) {
                if (aList == null)
                    break;

                Task x = (Task) aList;
                if (x.equals(input)) {

                /*if (x!=y && x.isInput())
                    throw new RuntimeException("different input task instances with same stamp");*/
                    r.merge(x);
                    return;
                }
            }
        }

        for (Object aList: list) {
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

                } else {

                    continue;
                }

            } else {


                Truth xt = x.truth();


                Truth yt = Revision.revise(newBeliefTruth, xt, 1f, conclusion == null ? 0 : conclusion.evi());
                if (yt == null)
                    continue;

                yt = yt.dither(nar);
                if (yt == null || yt.equalsIn(xt, nar) || yt.equalsIn(newBeliefTruth, nar))
                    continue;

                conclusion = yt;
            }

            oldBelief = x;

        }

        Task revised;
        if (oldBelief != null && conclusion != null) {

            final float newBeliefWeight = input.evi();


            float aProp = newBeliefWeight / (newBeliefWeight + oldBelief.evi());
            Term t =
                    Revision.intermpolate(
                            input.term(), oldBelief.term(),
                            aProp,
                            nar
                    );


            Task prevBelief = oldBelief;
            revised = Task.tryTask(t, input.punc(), conclusion, (term, revisionTruth) ->
                    new NALTask(term,
                            input.punc(),
                            revisionTruth,
                            nar.time() /* creation time */,
                            ETERNAL, ETERNAL,
                            Stamp.zip(input.stamp(), prevBelief.stamp(), aProp)
                    )
            );
            if (revised != null) {
                revised.pri(Priority.fund(Math.max(prevBelief.priElseZero(), input.priElseZero()), false, prevBelief, input));
                ((NALTask) revised).cause = Cause.sample(Param.causeCapacity.intValue(), input, prevBelief);

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
        float factor = Math.max((1f/tableCap), (float)Util.unitize((((double)x.range())/(1+tableDur))));

        float eviMin;
        //synchronized (this) {
            if (size() == capacity()) {
                Task w = last();
                eviMin = w!=null ? w.evi() : 0;
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
