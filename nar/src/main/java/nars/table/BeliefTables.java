package nars.table;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtil;
import nars.Task;
import nars.control.op.Remember;
import nars.task.util.Answer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;


/**
 * composite of multiple sub-tables
 * the sub-tables and their rank can be modified at runtime.
 */
public class BeliefTables extends FasterList<BeliefTable> implements BeliefTable {

    static final int ORDERED = 0;
    static final int SHUFFLE_FIRST_COME_FIRST_SERVE = 1;
    static final int SHUFFLE_ROUND_ROBIN = 2;

    protected int matchMode = 2;

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
        if (size != 0) {
            BeliefTable[] items = this.items;
            if (size == 1) {
                items[0].match(a);
            } else {
                IntArrayList nonEmpty = new IntArrayList(size);
                for (int i = 0; i < size; i++) {
                    if (!items[i].isEmpty())
                        nonEmpty.add(i);
                }
                int N = nonEmpty.size();
                if (N ==1) {
                    items[nonEmpty.get(0)].match(a);
                } else {
                    switch (matchMode) {
                        case ORDERED: {
                            for (int i = 0; i < N; i++) {
                                items[nonEmpty.get(i)].match(a);
                                if (a.ttl <= 0)
                                    break;
                            }
                            break;
                        }

                        case SHUFFLE_FIRST_COME_FIRST_SERVE: {
                            int[] ne = nonEmpty.toIntArray();
                            ArrayUtil.shuffle(ne, a.random());
                            for (int i = 0; i < N; i++) {
                                items[ne[i]].match(a);
                                if (a.ttl <= 0)
                                    break;
                            }
                            break;
                        }

                        case SHUFFLE_ROUND_ROBIN: {
                            //fair round robin
                            int ttlStart = a.ttl;
                            assert (ttlStart > 0);
                            int ttlFair = Math.max(1,
                                (int)Math.ceil(((float)ttlStart) / N)
                                //ttlStart
                            );
                            int[] ne = nonEmpty.toIntArray();
                            ArrayUtil.shuffle(ne, a.random());
                            int ttlUsed = 0;
                            for (int i = 0; i < N; i++) {
                                a.ttl = ttlFair;
                                items[ne[i]].match(a);
                                ttlUsed += ttlFair - a.ttl;
                                if (ttlUsed >= ttlStart)
                                    break;
                            }
                            break;
                        }
                        default:
                            throw new UnsupportedOperationException();
                    }
                }

            }
        }
    }


//    /** visit subtables in shuffled order, while predicate returns true */
//    public boolean ANDshuffled(Random rng, Predicate<BeliefTable> e)  {
//        BeliefTable[] items = this.items;
//        if (items == null)
//            return true; //?wtf
//
//        int n = Math.min(size, items.length);
//        switch (n) {
//            case 0:
//                return true;
//            case 1:
//                return e.test(items[0]);
//            case 2: {
//                int i = rng.nextInt(2);
//                return e.test(items[i])  && e.test(items[1 - i]);
//            }
//            case 3: {
//                int i = rng.nextInt(6);
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
//                return e.test(items[x]) && e.test(items[y]) && e.test(items[z]);
//            }
//            default:
//                byte[] order = new byte[n];
//                for (byte i = 0; i < n; i++)
//                    order[i] = i;
//                ArrayUtil.shuffle(order, rng);
//                for (int i = 0; i < n; i++) {
//                    if (!e.test(items[order[i]]))
//                        return false;
//                }
//                return true;
//        }
//
//    }

    /** stops after the first table accepts it */
    @Override public void remember(Remember r) {
        BeliefTable[] items = this.items;
        for (int i = 0, thisSize = this.size; i < thisSize; i++) {
            items[i].remember(r);
            if (r.complete())
                break;
        }

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
    public @Nullable <X extends TaskTable> X tableFirst(Class<? extends X> t) {
        for (BeliefTable beliefTable : this) {
            if (t.isInstance(beliefTable)) {
                return (X) beliefTable;
            }
        }
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
        forEachWith(TaskTable::forEachTask, action);
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





