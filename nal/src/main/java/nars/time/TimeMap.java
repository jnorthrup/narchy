package nars.time;

import jcog.tree.rtree.RTree;
import jcog.tree.rtree.Split;
import jcog.tree.rtree.rect.RectDouble;
import jcog.tree.rtree.split.AxialSplitLeaf;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;


public class TimeMap extends RTree<Task> implements Consumer<Task> {

    private final static Split<Task> AxialSplit = new AxialSplitLeaf<>();

    public TimeMap() {
        super((task) -> new RectDouble(task.start(), task.end(), task.hashCode(), task.hashCode()),
                2, 8, AxialSplit);
    }

//    public TimeMap(@NotNull NAR n) {
//        this();
//        n.tasks(true, true, false, false).forEach(this);
//    }

    @Override
    public void accept(@NotNull Task task) {
        if (!task.isEternal()) {
            add(task);
        }
    }






}





































