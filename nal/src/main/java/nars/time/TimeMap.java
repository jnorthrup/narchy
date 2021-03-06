package nars.time;

import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.RTree;
import jcog.tree.rtree.Split;
import jcog.tree.rtree.rect.RectDouble;
import jcog.tree.rtree.split.AxialSplit;
import nars.Task;

import java.util.function.Consumer;
import java.util.function.Function;


public class TimeMap extends RTree<Task> implements Consumer<Task> {

    private static final Split<Task> AxialSplit = new AxialSplit<>();

    public TimeMap() {
        super(new Function<Task, HyperRegion>() {
                  @Override
                  public HyperRegion apply(Task task) {
                      return new RectDouble((double) task.start(), (double) task.end(), (double) task.hashCode(), (double) task.hashCode());
                  }
              },
                8, AxialSplit);
    }

//    public TimeMap(@NotNull NAR n) {
//        this();
//        n.tasks(true, true, false, false).forEach(this);
//    }

    @Override
    public void accept(Task task) {
        if (!task.isEternal()) {
            add(task);
        }
    }






}





































