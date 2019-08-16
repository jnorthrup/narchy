package nars.table;

import jcog.data.list.FasterList;
import jcog.util.ArrayUtil;
import nars.Task;
import nars.control.op.Remember;
import nars.task.util.Answer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * composite of multiple sub-tables
 * the sub-tables and their rank can be modified at runtime.
 */
public class BeliefTables extends FasterList<BeliefTable> implements BeliefTable {

    public BeliefTables(int capacity) {
        super(0, new BeliefTable[capacity]);
    }

    public BeliefTables(BeliefTable... b) {
        super(b);
    }

    /** this is very important: the result of this may not necessarily correspond with testing size=0.
     * isEmpty() means something different in dynamic task table cases.
     * TODO if any of the tables are dynamic then this can be cached and cause isEmpty() to always return false
     * */
    @Override public boolean isEmpty() {
        return allSatisfy(TaskTable::isEmpty);
    }

    @Override
    public void match(Answer a) {
        int size = this.size;
        if (size == 0) return;
        else if (size == 1) { items[0].match(a); }
        else {
            //first come first serve
            ANDshuffled(a.random(), t -> {
                t.match(a);
                return a.ttl > 0;
            });


            //fair round robin
//        int ttlStart = a.ttl; assert(ttlStart > 0);
//        int ttlFair = Math.max(1,
//            //ttlStart /size
//            ttlStart
//        );
//        int[] ttlUsed = new int[1];
//        ANDshuffled(a.random(), t->{
//            a.ttl = ttlFair; //restore for next
//            t.match(a);
//            ttlUsed[0] += ttlFair - a.ttl;
//            return true;
//        });
//        a.ttl = Math.max(0, ttlStart - ttlUsed[0]);
        }
    }


    /** visit subtables in shuffled order, while predicate returns true */
    public boolean ANDshuffled(Random rng, Predicate<BeliefTable> e)  {
        BeliefTable[] items = this.items;
        if (items == null)
            return true; //?wtf

        int n = Math.min(size, items.length);
        switch (n) {
            case 0:
                return true;
            case 1:
                return e.test(items[0]);
            case 2: {
                int i = rng.nextInt(2);
                return e.test(items[i])  && e.test(items[1 - i]);
            }
            case 3: {
                int i = rng.nextInt(6);
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
                return e.test(items[x]) && e.test(items[y]) && e.test(items[z]);
            }
            default:
                byte[] order = new byte[n];
                for (byte i = 0; i < n; i++)
                    order[i] = i;
                ArrayUtil.shuffle(order, rng);
                for (int i = 0; i < n; i++) {
                    if (!e.test(items[order[i]]))
                        return false;
                }
                return true;
        }

    }

    /** stops after the first table accepts it */
    @Override public void remember(Remember r) {
        anySatisfy(t -> {
            t.remember(r);
            return r.done;
        });

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
    @Deprecated public final void clear() {
        forEach(TaskTable::clear);
    }

    public final void delete() {
        size = 0;
        forEach(TaskTable::delete);
        Arrays.fill(items, 0, size, null);
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





