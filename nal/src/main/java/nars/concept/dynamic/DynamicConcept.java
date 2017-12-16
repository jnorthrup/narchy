package nars.concept.dynamic;

import nars.NAR;
import nars.concept.TaskConcept;
import nars.concept.builder.DefaultConceptBuilder;
import nars.table.BeliefTable;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;


/** just a marker class for a TaskConcept with DynamicBeliefTables */
public final class DynamicConcept extends TaskConcept {


    public DynamicConcept(Term term, BeliefTable beliefs, BeliefTable goals, DefaultConceptBuilder conceptBuilder) {
        super(term, beliefs, goals, conceptBuilder);
    }


}

//might be dangerous because the task will get indexed but never deleted by its concept (this):
//        @Override
//        public TruthDelta add(@NotNull Task input, @NotNull QuestionTable questions, @NotNull CompoundConcept<?> concept, @NotNull NAR nar) {
//            if (input.isInput()) {
//                return super.add(input, questions, concept, nar);
//            } else {
//                //do not insert but report that it was
//                Truth current = ((BeliefTable)tableFor(input.punc())).truth(input.occurrence(), nar.time());
//                return new TruthDelta(current, current);
//            }
//        }

//        @Override
//        public TruthDelta add(@NotNull Task input, @NotNull QuestionTable questions, @NotNull CompoundConcept<?> concept, @NotNull NAR nar) {
//            //only allow input and dynamic belief tasks to be inserted; otherwise process a new dynamic result
//            if (!input.isInput() && (!(input instanceof DynamicBeliefTask))) {
//                DynamicBeliefTask d = generate(input.term(), input.occurrence(), input.budget());
//                if (d!=null) {
//                    input.delete(); //necessary to cause NAR to replace the Task in the index, so as not to seem as a duplicate
//                    nar.inputLater(d);
//                    return null;
//                }
//            }
//
////            //only allow input tasks
////            if (!input.isInput())
////                return null;
//
//            return super.add(input, questions, concept, nar);
//        }

//package nars.util.signal;
//
//import com.google.common.collect.TreeMultimap;
//import nars.NAR;
//import DefaultBeliefTable;
//import EternalTable;
//import TemporalBeliefTable;
//import nars.nal.Tense;
//import nars.task.Task;
//import nars.truth.Truth;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.function.Consumer;
//import java.util.function.Predicate;
//
///**
// * customized belief table for sensor and motor concepts
// */
//public class SensorBeliefTable extends DefaultBeliefTable {
//
//    public SensorBeliefTable(int eCap, int tCap) {
//        super(eCap, tCap);
//    }
//
//    @Override
//    protected Task addTemporal(@NotNull Task goal, List<Task> displaced, @NotNull NAR nar) {
//
//        if (!goal.isInput() && nar.taskPast.test(goal)) {
//            //this is a goal for a past time, reject
//            return null;
//        }
//
//        if (!isEmpty()) {
//            TemporalBeliefTable temporals = temporal;
//            if (temporals.isFull()) {
//                //remove any past goals
//                temporals.removeIf(nar.taskPast);
//            }
//        }
//
//        return super.addTemporal(goal, displaced, nar);
//    }
//
////    @Override
////    protected TemporalBeliefTable newTemporalBeliefTable(Map<Task, Task> mp, int initialTemporalCapacity) {
////        return new SensorTemporalBeliefTable(mp, initialTemporalCapacity);
////    }
//
//    public static class SensorTemporalBeliefTable implements TemporalBeliefTable {
//
//        //TODO
//
//
//        private int capacity;
//        @NotNull
//        private final TreeMultimap<Long, Task> map;
//
//        public SensorTemporalBeliefTable(@Deprecated Map<Task, Task> outerMap, int capacity) {
//            map = TreeMultimap.create(Long::compare, (Task ta, Task tb) -> {
//                return Float.compare(ta.conf(), tb.conf());
//            });
//            this.capacity = capacity;
//
//        }
//
//        @Override
//        public void clear() {
//            map.clear();
//        }
//
//        @Nullable
//        @Override
//        public Task get(@NotNull Object key) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Nullable
//        @Override
//        public Object remove(@NotNull Task key) {
//            return map.remove(key.occurrence(), key);
//        }
//
//        @Nullable
//        @Override
//        public Task put(@NotNull Task task, @NotNull Task task2) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public int size() {
//            return map.size();
//        }
//
//        @Override
//        public void forEachKey(@NotNull Consumer<? super Task> each) {
//            map.values().forEach(each);
//        }
//
//        @Override
//        public int capacity() {
//            return capacity;
//        }
//
//        @Override
//        public void setCapacity(int i) {
//            this.capacity = i;
//        }
//
//        @Override
//        public void topWhile(@NotNull Predicate<? super Task> each, int n) {
//            //TODO choose a radius of n around the current nar.time()
//
//
//            for (Long aLong : map.keySet()) {
//                for (Task task : map.get(aLong)) {
//                    if (!each.test(task))
//                        break;
//                    if (n-- == 0)
//                        break;
//                }
//
//            }
//        }
//
//        public int compare(@NotNull Task o1, @NotNull Task o2) {
//            return Long.compare(o1.occurrence(), o2.occurrence());
//        }
//
//        @Override
//        public @Nullable Task strongest(long when, long now, Task against) {
//            //map.keySet().higher(when);
//            return null;
//        }
//
//        @Override
//        public @Nullable Truth truth(long when, long now, EternalTable eternal) {
//             return strongest(when, now, null).projectTruth(when, now, false);
//        }
//
//        @Override
//        public Task add(@NotNull Task input, EternalTable eternal, List<Task> displ, @NotNull NAR nar) {
//            return null;
//        }
//
//        @Override
//        public void removeIf(@NotNull Predicate<Task> o) {
//
//        }
//
//        @Override
//        public long minTime() {
//            //TODO
//            return Tense.ETERNAL;
//        }
//
//        @Override
//        public long maxTime() {
//            //TODO
//            return Tense.ETERNAL;
//        }
//
//        @Override
//        public void minTime(long minT) {
//
//        }
//
//        @Override
//        public void maxTime(long maxT) {
//
//        }
//
//        @Nullable
//        @Override
//        public Iterator<Task> iterator() {
//            return null;
//        }
//    }
//}
