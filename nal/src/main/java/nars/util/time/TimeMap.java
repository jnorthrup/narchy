package nars.util.time;

import jcog.tree.rtree.RTree;
import jcog.tree.rtree.Split;
import jcog.tree.rtree.rect.RectDouble2D;
import jcog.tree.rtree.split.AxialSplitLeaf;
import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;


public class TimeMap extends RTree<Task> implements Consumer<Task> {

    private final static Split<Task> AxialSplit = new AxialSplitLeaf<>();

    public TimeMap() {
        super((task) -> new RectDouble2D(task.start(), task.end(), task.hashCode(), task.hashCode()),
                2, 8, AxialSplit);
    }

    public TimeMap(@NotNull NAR n) {
        this();
        n.tasks(true, true, false, false).forEach(this);
    }

    @Override
    public void accept(@NotNull Task task) {
        if (!task.isEternal()) {
            add(task);
        }
    }

//    public void print() {
//        out.println(nar.time() + ": " + "Total tasks: " + size() + '\t' + keySetSorted().toString());
//    }


}


//package nars.util.data;
//
//        import jcog.time.IntervalTree;
//        import nars.NAR;
//        import nars.Task;
//        import org.jetbrains.annotations.NotNull;
//
//        import java.util.function.Consumer;
//
//        import static java.lang.System.out;
//
//
//public class TimeMap0 extends IntervalTree<Long, Task> implements Consumer<Task> {
//
//    @NotNull
//    private final NAR nar;
//
//    public TimeMap0(@NotNull NAR n) {
//        this.nar = n;
//        n.forEachTask(this, true, true, false, false);
//    }
//
//    @Override
//    public void accept(@NotNull Task task) {
//        if (!task.isEternal()) {
//            put(task.start(), task);
//        }
//    }
//
//    public void print() {
//        out.println(nar.time() + ": " + "Total tasks: " + size() + '\t' + keySetSorted().toString());
//    }
//
//
//}
