//package nars.table;
//
//import nars.NAR;
//import nars.Task;
//import nars.concept.TaskConcept;
//import nars.task.signal.SignalTask;
//import nars.truth.Truth;
//import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
//import org.eclipse.collections.api.tuple.primitive.LongLongPair;
//
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///** TODO not finished yet */
//public class CachedTemporalBeliefTable extends ProxyTemporalBeliefTable {
//
//    public CachedTemporalBeliefTable(TemporalBeliefTable ref) {
//        super(ref);
//    }
//
//    final Map<IntObjectPair<LongLongPair>,Truth> cache = new ConcurrentHashMap<>();
//
//    protected void modified() {
//        cache.clear();
//    }
//
//    @Override
//    public Truth truth(long start, long end, EternalTable eternal, int dur) {
//        if (!eternal.isEmpty())
//            throw new RuntimeException("does not support eternal beliefs");
//
//        //TODO
//
//        return super.truth(start, end, EternalTable.EMPTY, dur);
//    }
//
//    @Override
//    public boolean add(Task t, TaskConcept c, NAR n) {
//        boolean b = super.add(t, c, n);
//        if (b) {
//            modified();
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public boolean removeTask(Task x) {
//        if (super.removeTask(x)) {
//            modified();
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public void setCapacity(int temporals) {
//        if (size() > temporals) {
//            modified(); //assume something was/will have to be removed
//        }
//        super.setCapacity(temporals);
//    }
//
//    @Override
//    public void clear() {
//        super.clear();
//        modified();
//    }
//
//    @Override
//    public void update(SignalTask x, Runnable change) {
//        super.update(x, ()->{
//            change.run();
//            modified();
//        });
//    }
//}
