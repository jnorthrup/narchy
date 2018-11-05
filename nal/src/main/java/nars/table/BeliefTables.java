package nars.table;

import jcog.data.list.FasterList;
import jcog.data.list.LimitedFasterList;
import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
import nars.task.util.Answer;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.stream.Stream;


/**
 * composite of multiple sub-tables
 * the sub-tables and their rank can be modified at runtime.
 */
public class BeliefTables implements BeliefTable {

    public static final BeliefTables Empty = new BeliefTables(LimitedFasterList.Empty) {
        @Override
        public void match(Answer r) {

        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    };

    public final FasterList<BeliefTable> tables;

    public BeliefTables(BeliefTable... tables) {
        this(new FasterList<>(tables));
    }

    public BeliefTables(FasterList<BeliefTable> tables) {
        this.tables = tables;
    }


    /** this is very important: the result of this may not necessarily correspond with testing size=0.
     * isEmpty() means something different in dynamic task table cases */
    @Override public boolean isEmpty() {
        return tables.allSatisfy(TaskTable::isEmpty);
    }

    @Override
    public void match(Answer r) {
        tables.allSatisfyWith((t,rr) -> {
            if (rr.active()) {
                t.match(rr);
                return true;
            }
            return false;
        }, r);
    }

    @Override
    public void sample(Answer a) {
        int n = tables.size();
        switch (n) {
            case 0:
                break;
            case 1:
                tables.get(0).match(a);
                break;
            case 2: {
                int i = a.random().nextInt(2);
                tables.get(i).match(a);
                if (a.active())
                    tables.get(1 - i).match(a);
                break;
            }
            case 3: {
                int i = a.random().nextInt(6);
                int x, y, z;
                switch (i) {
                    case 0:
                        x = 0;
                        y = 1;
                        z = 2;
                        break;
                    case 1:
                        x = 0;
                        y = 2;
                        z = 1;
                        break;
                    case 2:
                        x = 1;
                        y = 0;
                        z = 2;
                        break;
                    case 3:
                        x = 1;
                        y = 2;
                        z = 0;
                        break;
                    case 4:
                        x = 2;
                        y = 0;
                        z = 1;
                        break;
                    case 5:
                        x = 2;
                        y = 1;
                        z = 0;
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                tables.get(x).match(a);
                if (a.active()) {
                    tables.get(y).match(a);
                    if (a.active()) {
                        tables.get(z).match(a);
                    }
                }
                break;
            }
            default:
                int[] order = new int[n];
                for (int i = 0; i < n; i++)
                    order[i] = i;
                ArrayUtils.shuffle(order, a.random());
                for (int i = 0; i < n; i++) {
                    tables.get(order[i]).match(a);
                    if (!a.active())
                        break;
                }
                break;
        }

    }

    @Override
    public void add(Remember r, NAR n) {
        tables.allSatisfyWith((t,rr) -> {
            t.add(rr, n);
            return !rr.done(); //if one of the tables cancelled it, stop here
        }, r);

//        if (Param.ETERNALIZE_FORGOTTEN_TEMPORALS) {
//            if (eternal != EternalTable.EMPTY && !r.forgotten.isEmpty() &&
//                    temporal.size() >= temporal.capacity() - 1 /* some tolerance for full test */) {
//
//                r.forgotten.forEach(t -> {
//                    if (!(t instanceof SignalTask) && !t.isEternal()) {
//                        //TODO maybe sort by evi decreasing
//                        Task e = eternal.eternalize(t, temporal.capacity(), temporal.tableDur(), n);
//                        if (e != null)
//                            eternal.add(r, n, e);
//                    }
//                });
//            }
//        }

    }



    @Override
    public Stream<? extends Task> streamTasks() {
        return tables.stream().flatMap(TaskTable::streamTasks);
    }


    @Override
    public boolean removeTask(Task x, boolean delete) {
        return tables.count(t -> t.removeTask(x, delete)) > 0;
    }

    @Override
    public void clear() {
        tables.forEach(TaskTable::clear);
    }

    public final void delete() {
        clear();
        //tables.clear(); //<- not necessary and can interrupt iterations in progress
    }

    /** gets first matching table of the provided type */
    @Nullable
    public <X extends TaskTable> X tableFirst(Class<? extends X> t) {
        for (TaskTable x : tables) {
            if (t.isInstance(x))
                return (X) x;
        }
        return null;
    }

    @Override
    public void forEachTask(long minT, long maxT, Consumer<? super Task> x) {
        tables.forEach(t -> t.forEachTask(minT, maxT, x));
    }

    @Override
    public void setTaskCapacity(int newCapacity) {
        throw new UnsupportedOperationException("can only set capacity to individual tables contained by this");
    }


    @Override
    public void forEachTask(Consumer<? super Task> action) {
        tables.forEach(t -> t.forEachTask(action));
    }

    @Override
    public int size() {
        return (int) tables.sumOfInt(TaskTable::size);
    }


//    @Override
//    public Task sample(long start, long end, Term template, NAR nar) {
//        Task ete = eternal.sample(start, end, template, nar);
//        if (ete != null && start == ETERNAL)
//            return ete; //eternal sought
//
//        Task tmp = temporal.sample(start, end, template, nar);
//        if (ete == null || tmp != null && tmp.contains(start, end))
//            return tmp; //interval found
//
//        if (tmp == null) return ete;
//        float e = TruthIntegration.eviInteg(ete, start, end, 1);
//        float t = TruthIntegration.eviInteg(tmp, start, end, 1);
//        return nar.random().nextFloat() < (t / Math.max(Float.MIN_NORMAL, (e + t))) ? tmp : ete;
//    }


}



