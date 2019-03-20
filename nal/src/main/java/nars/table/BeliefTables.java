package nars.table;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.Task;
import nars.control.op.Remember;
import nars.task.util.Answer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.stream.Stream;


/**
 * composite of multiple sub-tables
 * the sub-tables and their rank can be modified at runtime.
 */
public class BeliefTables extends FasterList<BeliefTable> implements BeliefTable {

//    public BeliefTables(Object... beliefTablesOrSuppliersOfThem) {
//        super(beliefTablesOrSuppliersOfThem);
//    }

    public BeliefTables(BeliefTable... b) {
        super(b);
    }

    /** this is very important: the result of this may not necessarily correspond with testing size=0.
     * isEmpty() means something different in dynamic task table cases */
    @Override public boolean isEmpty() {
        return allSatisfy(TaskTable::isEmpty);
    }

    @Override
    public void match(Answer a) {
        int triesBefore = a.ttl;
        allSatisfyWith((t,aa) -> {
            //TODO better TTL distribution system
            aa.ttl = triesBefore; //restore for next
            t.match(aa);
            return aa.active();
        }, a);
    }

    @Override
    public void add(Remember r, NAR n) {

//        activateAll();

        //stops if one of the tables accepts it
        allSatisfyWith((t,N)->t.tryAdd(r,N), n);

//        if (Param.ETERNALIZE_FORGOTTEN_TEMPORALS) {
//            if (eternal != EternalTable.EMPTY && !r.forgotten.isEmpty() &&
//                    temporal.size() >= temporal.capacity() - 1 /* some tolerance for full test */) {
//
//                r.forgotten.forEach(t -> {
//                    if (!(t instanceof SignalTask) && !t.isEternal()) {
//                        //TODO maybe sort by evi decreasing
//                        Task e = eternal.eternalize(t, temporal.capacity(), temporal.tableDur(), n);
//                        if (e != null)
//                            eternal.addAt(r, n, e);
//                    }
//                });
//            }
//        }

    }

    @Override
    public Stream<? extends Task> taskStream() {
        return stream().flatMap(TaskTable::taskStream);
    }


    @Override
    public final boolean removeTask(Task x, boolean delete) {
        return count(t -> t.removeTask(x, delete)) > 0;
    }

    @Override
    public final void clear() {
        forEach(TaskTable::clear);
    }

    public final void delete() {
        clear();
        //super.clear();
        size = 0;
        items = null;
    }

    /** gets first matching table of the provided type */
    @Nullable
    public <X extends TaskTable> X tableFirst(Class<? extends X> t) {
        for (Object x : this)
            if (t.isInstance(x))
                return (X) x;
        return null;
    }

    @Override
    public void forEachTask(long minT, long maxT, Consumer<? super Task> x) {
        forEach(t -> t.forEachTask(minT, maxT, x));
    }

    @Override
    public void setTaskCapacity(int newCapacity) {
        throw new UnsupportedOperationException("can only setAt capacity to individual tables contained by this");
    }


    @Override
    public void forEachTask(Consumer<? super Task> action) {
        forEach(t -> t.forEachTask(action));
    }

//    @Override
//    protected final BeliefTable inactive() {
//        return BeliefTable.Empty;
//    }


    @Override
    public int taskCount() {
        return (int) sumOfInt(TaskTable::taskCount);
    }


    public static final BeliefTables Empty = new BeliefTables() {

        @Override
        public void add(Remember r, NAR n) {

        }

//        @Override
//        public boolean add(Supplier<BeliefTable> t) {
//            return false;
//        }

        @Override
        public boolean add(BeliefTable beliefTable) {
            return false;
        }

        @Override
        public void match(Answer a) {

        }

        @Override
        public int taskCount() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    };
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




//    @Override
//    public void sample(Answer a) {
//        int n = tables.size();
//        switch (n) {
//            case 0:
//                break;
//            case 1:
//                tables.get(0).sample(a);
//                break;
//            case 2: {
//                int i = a.random().nextInt(2);
//                tables.get(i).sample(a); //match(a);
//                if (a.active())
//                    tables.get(1 - i).sample(a);
//                break;
//            }
//            case 3: {
//                int i = a.random().nextInt(6);
//                int x, y, z;
//                switch (i) {
//                    case 0:
//                        x = 0;
//                        y = 1;
//                        z = 2;
//                        break;
//                    case 1:
//                        x = 0;
//                        y = 2;
//                        z = 1;
//                        break;
//                    case 2:
//                        x = 1;
//                        y = 0;
//                        z = 2;
//                        break;
//                    case 3:
//                        x = 1;
//                        y = 2;
//                        z = 0;
//                        break;
//                    case 4:
//                        x = 2;
//                        y = 0;
//                        z = 1;
//                        break;
//                    case 5:
//                        x = 2;
//                        y = 1;
//                        z = 0;
//                        break;
//                    default:
//                        throw new UnsupportedOperationException();
//                }
//                tables.get(x).sample(a);
//                if (a.active()) {
//                    tables.get(y).sample(a);
//                    if (a.active()) {
//                        tables.get(z).sample(a);
//                    }
//                }
//                break;
//            }
//            default:
//                int[] order = new int[n];
//                for (int i = 0; i < n; i++)
//                    order[i] = i;
//                ArrayUtils.shuffle(order, a.random());
//                for (int i = 0; i < n; i++) {
//                    tables.get(order[i]).sample(a);
//                    if (!a.active())
//                        break;
//                }
//                break;
//        }
//
//    }
