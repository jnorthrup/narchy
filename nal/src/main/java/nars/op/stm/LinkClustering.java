package nars.op.stm;

import jcog.TODO;
import jcog.Util;
import jcog.pri.MultiLink;
import jcog.pri.PLink;
import jcog.pri.VLink;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

public class LinkClustering extends ChainClustering {

    public LinkClustering(@NotNull NAR nar, FloatFunction<Task> accept, int centroids, int capacity, int minConjSize, int maxConjSize) {
        super(nar, accept, centroids, capacity);
    }
  protected void linkClustersMulti(Stream<VLink<Task>> group, NAR nar) {
        Task[] tasks = group.map(PLink::get).toArray(Task[]::new);

        Arrays.sort(tasks, Comparators.byIntFunction(Task::hashCode)); //keep them in a canonical ordering for equality testing purposes

        float pri = Util.sum(Task::priElseZero, tasks);

        MultiLink<Task,Task> task = new MultiLink<>( tasks, Function.identity(), pri );
        //MultiLink<Task,Term> term = new MultiLink<>( tasks, Task::term, pri );

        for (Task t : tasks) {
            Concept tc = t.concept(nar, false);
            if (tc!=null) {
                tc.tasklinks().putAsync(task);
                //tc.termlinks().putAsync(term);
            }
        }

    }
    @Override
    protected void link(Task tx, Task ty) {
                    float linkPri =
                    //tx.pri() * ty.pri();
                    Util.or(tx.priElseZero(), ty.priElseZero());
            throw new TODO();
//                STMLinkage.link(tx, linkPri, ty, nar);
    }

}
