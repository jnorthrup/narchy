package nars.table;

import jcog.Util;
import jcog.list.FasterList;
import jcog.pri.Priority;
import jcog.sort.SortedArray;
import jcog.util.ArrayIterator;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.control.Cause;
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
        public Task strongest() {
            return null;
        }

        @Override
        public Task weakest() {
            return null;
        }

        @Override
        public boolean removeTask(Task x) {
            return false;
        }

        @Override
        public boolean add(Task input, TaskConcept c, NAR nar) {
            return false;
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
            return strongest();

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


        Object[] list = this.list;
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
        int wasCapacity = this.capacity();
        if (wasCapacity != c) {

            List<Task> trash = null;
            synchronized (this) {

                wasCapacity = capacity();

                int s = size;
                if (s > c) {


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
            Task[] list = this.list;
            return Arrays.copyOf(list, Math.min(s, list.length), Task[].class);

        }

    }


    @Override
    public void clear() {
        synchronized (this) {
            super.clear();
        }
    }

    public Task strongest() {
        Object[] l = this.list;
        return (l.length == 0) ? null : (Task) l[0];
    }

    public Task weakest() {
        int s = size;
        if (s == 0) return null;
        Object[] l = this.list;
        int ll = Math.min(s, l.length);
        return ll == 0 ? null : (Task) l[ll - 1];
    }

    /**
     * for ranking purposes.  returns negative for descending order
     */
    @Override
    public final float floatValueOf(Task w) {

        return -eternalTaskValueWithOriginality(w);
    }


    @Deprecated
    void removeTask(Task t, @Nullable String reason) {
        t.delete();
    }

    /**
     * @return null: no revision could be applied
     * ==newBelief: existing duplicate found
     * non-null: revised task
     */
    @Nullable
    private /*Revision*/Task tryRevision(Task y /* input */,
                                         NAR nar) {

        Object[] list = this.list;
        int bsize = list.length;

        Task oldBelief = null;
        Truth conclusion = null;

        Truth newBeliefTruth = y.truth();

        for (int i = 0; i < bsize; i++) {
            Task x = (Task) list[i];

            if (x == null)
                break;

            if (x.equals(y)) {
                /*if (x!=y && x.isInput())
                    throw new RuntimeException("different input task instances with same stamp");*/
                return x;
            }

            float xconf = x.conf();
            if (Stamp.overlapsAny(y, x)) {

                //HACK interpolate truth if only freq differs
                if ((!x.isCyclic() && !y.isCyclic()) &&
                        Arrays.equals(x.stamp(), y.stamp()) &&
                        Util.equals(xconf, y.conf(), nar.confResolution.floatValue())) {

                    conclusion = $.t((x.freq() + y.freq()) / 2, xconf).dither(nar);

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

        if (oldBelief == null || conclusion == null)
            return null;

        final float newBeliefWeight = y.evi();


        float aProp = newBeliefWeight / (newBeliefWeight + oldBelief.evi());
        Term t =
                Revision.intermpolate(
                        y.term(), oldBelief.term(),
                        aProp,
                        nar
                );


        Task prevBelief = oldBelief;
        Task x = Task.tryTask(t, y.punc(), conclusion, (term, revisionTruth) ->
                new NALTask(term,
                        y.punc(),
                        revisionTruth,
                        nar.time() /* creation time */,
                        ETERNAL, ETERNAL,
                        Stamp.zip(y.stamp(), prevBelief.stamp(), aProp)
                )
        );
        if (x != null) {
            x.priSet(Priority.fund(Math.max(prevBelief.priElseZero(), y.priElseZero()), false, prevBelief, y));
            ((NALTask) x).cause = Cause.sample(Param.causeCapacity.intValue(), y, prevBelief);

            if (Param.DEBUG)
                x.log("Insertion Revision");


        }

        return x;
    }

    @Nullable
    private Task put(final Task incoming) {

        synchronized (this) {
            Task displaced = null;

            if (size == capacity()) {
                Task weakestPresent = weakest();
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
        Task s = strongest();
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
    public boolean add(Task input, TaskConcept c, NAR nar) {

        int cap = capacity();
        if (cap == 0) {
            
            /*if (input.isInput())
                throw new RuntimeException("input task rejected (0 capacity): " + input + " "+ this + " " + this.capacity());*/
            return false;
        }


        Task revised = tryRevision(input, nar);
        if (revised == null) {


            return insert(input);
        } else {

            if (revised.equals(input)) {


                return true;
            } else {

                if (insert(revised)) {


                    if (insert(input)) {

                    } /*else {
                            input.delete(); 
                        }*/

                }

                nar.eventTask.emit(revised);

                return true;
            }


        }


    }


    /**
     * try to insert but dont delete the input task if it wasn't inserted (but delete a displaced if it was)
     * returns true if it was inserted, false if not
     */
    private boolean insert(Task input) {

        Task displaced = put(input);

        if (displaced == input) {

            return false;
        } else if (displaced != null) {
            removeTask(displaced,
                    "Displaced"

            );
        }
        return true;
    }


    @Nullable
    public Truth strongestTruth() {
        Task e = strongest();
        return (e != null) ? e.truth() : null;
    }

}
