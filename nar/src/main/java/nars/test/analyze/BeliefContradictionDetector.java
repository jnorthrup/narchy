package nars.test.analyze;

import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.control.NARPart;
import nars.table.TaskTable;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;

import java.util.function.Consumer;

public class BeliefContradictionDetector extends NARPart implements Consumer<Task> {

    public BeliefContradictionDetector(NAR n) {
        super(n);

    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        nar.onTask(this);
    }

    @Override
    public void accept(Task task) {
        if (task.isBeliefOrGoal()) {
            Concept c = nar.concept(task);
            if (c != null) {
                detectContradiction(c, task.punc());
            }
        }
    }

    protected static void detectContradiction(Concept concept, byte punc) {
        if (concept.term().hasAny(Op.Temporal)) {
            return; //TODO
        }
        TaskTable table = concept.table(punc);
        int n = table.taskCount();
        if (n > 1) {
            FloatArrayList h = new FloatArrayList(n);
            table.forEachTask(new Consumer<Task>() {
                @Override
                public void accept(Task t) {
                    h.add(t.freq());
                }
            });
            if (h.count(new FloatPredicate() {
                @Override
                public boolean accept(float x) {
                    return x < 0.5f;
                }
            }) > 0 && h.count(new FloatPredicate() {
                @Override
                public boolean accept(float x) {
                    return x > 0.5f;
                }
            }) > 0) {
                print(table);
            }
        }
    }

    private static void print(TaskTable table) {
        table.forEachTask(new Consumer<Task>() {
            @Override
            public void accept(Task t) {
                System.out.println(t.proof());
            }
        });
    }

}
