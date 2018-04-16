package nars.table;

import jcog.Util;
import jcog.list.FasterList;
import jcog.pri.Priority;
import jcog.sort.SortedArray;
import jcog.util.ArrayIterator;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.control.Cause;
import nars.task.NALTask;
import nars.task.Revision;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.table.BeliefTable.*;
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
        public boolean add(/*@NotNull*/ Task input, TaskConcept c, /*@NotNull*/ NAR nar) {
            return false;
        }


        @Override
        public void setCapacity(int c) {

        }

        @Override
        public void forEachTask(Consumer<? super Task> action) {

        }

        /*@NotNull*/
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
                break; //null-terminator reached sooner than expected
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
                break; //null-terminator reached sooner than expected
            if (selector.test(x))
                return x;
        }
        return null;
    }

    @Override
    public Stream<Task> streamTasks() {
//        Task[] values = toArray();
//        if (values.length == 0) return Stream.empty();
//        else return Stream.of(values);

        Object[] list = this.list;
        int size = Math.min(list.length, this.size);
        if (size == 0)
            return Stream.empty();
        else {
            //TODO may not be null filtered properly for certain multithread cases of removal
            return ArrayIterator.stream((Task[]) list, size);
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

                wasCapacity = capacity(); //just to be sure

                int s = size;
                if (s > c) {

                    //TODO can be accelerated by batch/range remove operation

                    trash = new FasterList(s - c);
                    while (c < s--) {
                        trash.add(removeLast());
                    }
                }

                if (wasCapacity != c)
                    resize(c);
            }

            //do this outside of the synch
            if (trash != null) {
//                Task s = strongest();
//                if (s!=null) {
//                    TaskLink.GeneralTaskLink sl = new TaskLink.GeneralTaskLink(s, 0);
//                    trash.forEach(t -> ((NALTask)t).delete(sl));
//                } else {
                trash.forEach(Task::delete);
//                }
            }

        }
    }


    @Override
    public Task[] toArray() {
        //synchronized (this) {
        int s = this.size;
        if (s == 0)
            return Task.EmptyArray;
        else {
            Task[] list = this.list;
            return Arrays.copyOf(list, Math.min(s, list.length), Task[].class);
            //return ArrayUtils.subarray(list, 0, size, Task[]::new);
        }
        //}
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
        if (l.length == 0) return null;
        return (Task) l[size-1];

//        Object[] l = this.list;
//        if (l.length == 0) return null;
//        int n = size;
//        Task w = null;
//        while (n > 0 && (w = (Task) l[n]) == null) n--; //scan upwards for first non-null
//        return w;

    }

    /**
     * for ranking purposes.  returns negative for descending order
     */
    @Override
    public final float floatValueOf(/*@NotNull*/ Task w) {
        //return rankEternalByConfAndOriginality(w);
        return -eternalTaskValue(w);
    }




    @Deprecated
    void removeTask(/*@NotNull*/ Task t, @Nullable String reason) {
//        if (reason!=null && Param.DEBUG && t instanceof MutableTask)
//            ((MutableTask)t).log(reason);

//        if (t instanceof NALTask) //HACK
//            ((NALTask) t).delete(/*strongest()*/);
//        else
        t.delete();
    }

    /**
     * @return null: no revision could be applied
     * ==newBelief: existing duplicate found
     * non-null: revised task
     */
    @Nullable
    private /*Revision*/Task tryRevision(/*@NotNull*/ Task y /* input */,
                                                      @Nullable NAR nar) {

        Object[] list = this.list;
        int bsize = list.length;
        if (bsize == 0)
            return null; //nothing to revise with


        //Try to select a best revision partner from existing beliefs:
        Task oldBelief = null;
        Truth conclusion = null;

        Truth newBeliefTruth = y.truth();

        for (int i = 0; i < bsize; i++) {
            Task x = (Task) list[i];

            if (x == null) //the array has trailing nulls from having extra capacity
                break;

            if (x.equals(y)) {
                /*if (x!=y && x.isInput())
                    throw new RuntimeException("different input task instances with same stamp");*/
                return x;
            }


            //same conf, same stamp, both non-cyclic; interpolate to avoid one being preferred over another arbitrarily
            float xconf = x.conf();
            if (Util.equals(xconf, y.conf(), nar.confResolution.floatValue()) &&
                    (!x.isCyclic() && !y.isCyclic()) &&
                    Arrays.equals(x.stamp(), y.stamp())) {

                conclusion = new PreciseTruth(0.5f * (x.freq() + y.freq()), xconf);

            } else if (Stamp.overlapping(y, x)) {
                boolean FILTER_WEAKER_BUT_EQUAL = false;
                if (FILTER_WEAKER_BUT_EQUAL && !y.isInput() && xconf >= y.conf() &&
                        Util.equals(x.freq(), y.freq(), nar.freqResolution.floatValue()) &&
                        Arrays.equals(y.stamp(), x.stamp())) {
                    y.delete();
                    return null; //subsume by stronger belief with same freq and stamp
                }

                continue; //unrevisable

            } else {


                //
                //            float factor = tRel * freqMatch;
                //            if (factor < best) {
                //                //even with conf=1.0 it wouldnt be enough to exceed existing best match
                //                continue;
                //            }

                //            float minValidConf = Math.min(newBeliefConf, x.conf());
                //            if (minValidConf < bestConf) continue;
                //            float minValidRank = BeliefTable.rankEternalByOriginality(minValidConf, totalEvidence);
                //            if (minValidRank < bestRank) continue;

                Truth xt = x.truth();

                //TODO use overlappingFraction?

                Truth yt = Revision.revise(newBeliefTruth, xt, 1f, conclusion == null ? 0 : conclusion.evi());
                if (yt == null)
                    continue;

                yt = yt.dither(nar);
                if (yt == null || yt.equalsIn(xt, nar) || yt.equalsIn(newBeliefTruth, nar)) ////avoid a weak or duplicate truth
                    continue;

                conclusion = yt;
            }

            oldBelief = x;

        }

        if (oldBelief == null)
            return null;

        final float newBeliefWeight = y.evi();

        //TODO use Task.tryContent in building the task:

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
                        Stamp.zip(y.stamp(),prevBelief.stamp(), aProp)
                )
        );
        if (x != null) {
            x.priSet(Priority.fund(Math.max(prevBelief.priElseZero(), y.priElseZero()), false, prevBelief, y));
            ((NALTask) x).cause = Cause.sample(Param.causeCapacity.intValue(), y, prevBelief);

            if (Param.DEBUG)
                x.log("Insertion Revision");

//            ((NALTask)y).meta("@",x);
//            ((NALTask)prevBelief).meta("@",x);

        }

        return x;
    }

    @Nullable
    private Task put(final Task incoming) {
        Task displaced = null;

        synchronized (this) {
            if (size == capacity()) {
                Task weakestPresent = weakest();
                if (weakestPresent != null) {
                    if (eternalTaskValueWithOriginality(weakestPresent)
                            <=
                        eternalTaskValueWithOriginality(incoming)) {
                        displaced = removeLast();
                    } else {
                        return incoming; //insufficient confidence
                    }
                }
            }

            add(incoming, this);
        }

        return displaced;
    }

    public final Truth truth() {
        Task s = strongest();
        return s != null ? s.truth() : null;
    }


//    @Override
//    public void remove(/*@NotNull*/ Task belief, List<Task> displ) {
//        synchronized(builder) {
//            /* removed = */ remove(indexOf(belief, this));
//        }
//        TaskTable.removeTask(belief, null, displ);
//    }


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
    public boolean add(/*@NotNull*/ Task input, TaskConcept c, /*@NotNull*/ NAR nar) {

        int cap = capacity();
        if (cap == 0) {
            //may be deleted already
            /*if (input.isInput())
                throw new RuntimeException("input task rejected (0 capacity): " + input + " "+ this + " " + this.capacity());*/
            return false;
        }

//        if ((input.conf() >= 1f) && (cap != 1) && (isEmpty() || (first().conf() < 1f))) {
//            //AXIOMATIC/CONSTANT BELIEF/GOAL
//            synchronized (this) {
//                addEternalAxiom(input, this, nar);
//                return true;
//            }
//        } else {

            Task revised = tryRevision(input, nar);
            if (revised == null) {
                if (insert(input)) {
                    return true; //accepted input
                } else {
                    input.delete();
                    return false; //rejected
                }
            } else {

                if (revised.equals(input)) {
                    //input is a duplicate of existing item
                    if (revised!=input)
                        input.delete();
                    return true;
                } else {

                    if (insert(revised)) {
                        //no need to link task separately since the insertion succeeded whether or not the actual input was inserted since at least the revision was
                        //just emit the task event

                        if (input.equals(revised)) {
                            System.out.println("input=revised");
                        }

                        if (insert(input)) {
                            //accept input also
                        } else {
                            input.delete(); //delete the input, but got the revision
                        }

                    }

                    nar.eventTask.emit(revised);

                    return true; //accepted revision
                }

//                if (revised!=input && revised instanceof NALTask) {
//                    ((NALTask)revised).causeMerge(input);
//                }
//
//                //generated a revision
//                if (insert(revised)) {
////                    //link the revision
////                    Tasklinks.linkTask(revised, iPri, c, nar);
//                } else {
//                    revised.delete(); //rejected revision
//                }
//
//                if (!revised.equals(input)) {
//
//                    nar.eventTask.emit(revised); //separately
//
//                    //try to insert the original input also
//                    boolean inputIns = insert(input);
//                    if (inputIns) {
//                        return true; //accepted revision and accepted input
//                    } else {
//                        input.delete();
//                        return true; //accepted revision and rejected input
//                    }
//                } else {
//                    return !revised.isDeleted();
//                }

            }
//        }

    }


    /**
     * try to insert but dont delete the input task if it wasn't inserted (but delete a displaced if it was)
     * returns true if it was inserted, false if not
     */
    private boolean insert(/*@NotNull*/ Task input) {

        Task displaced = put(input);

        if (displaced == input) {
            //rejected
            return false;
        } else if (displaced != null) {
            removeTask(displaced,
                    "Displaced"
                    //"Displaced by " + incoming,
            );
        }
        return true;
    }

//    private void addEternalAxiom(/*@NotNull*/ Task input, /*@NotNull*/ EternalTable et, NAR nar) {
//        //lock incoming 100% confidence belief/goal into a 1-item capacity table by itself, preventing further insertions or changes
//        //1. clear the corresponding table, set capacity to one, and insert this task
//        et.forEachTask(t -> removeTask(t, "Overridden"));
//        et.clear();
//        et.setCapacity(1);
//
////        //2. clear the other table, set capcity to zero preventing temporal tasks
//        //TODO
////        TemporalBeliefTable otherTable = temporal;
////        otherTable.forEach(overridden);
////        otherTable.clear();
////        otherTable.capacity(0);
//
//        //NAR.logger.info("axiom: {}", input);
//
//        et.put(input);
//
//    }


    @Nullable public Truth strongestTruth() {
        Task e = strongest();
        return (e != null) ? e.truth() : null;
    }

}
