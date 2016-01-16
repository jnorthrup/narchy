package nars.bag.impl;

import nars.NAR;
import nars.bag.BLink;
import nars.task.Task;
import nars.util.ArraySortedIndex;
import nars.util.data.sorted.SortedIndex;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Accumulates tasks for output, ignoring evidence and choosng max confidence.
 * Rank is determined by confidence x priority which merges additively.
 */
public class DigestBag implements Consumer<Task> {

	@NotNull
    public final SortedIndex<BLink<Task>> list;

    public DigestBag(int capacity) {
        list = new ArraySortedIndex<BLink<Task>>(capacity) {
            @Override public float score(@NotNull BLink<Task> v) {

                //return v.getPriority() * v.get().getConfidenceIfTruthOr(1f);
                return v.getPriority();
            }
        };
    }

    @Override public void accept(@NotNull Task t) {
        list.insert(new BLink(t, t.getBudget()));
    }

    public static class OutputBuffer {

        public DigestBag buffer;

        public OutputBuffer(@NotNull NAR n, int capacity) {
            this.buffer = new DigestBag(capacity);

            n.memory.eventTaskProcess.on(buffer);

            /** answer adapter */
            n.memory.eventAnswer.on(tt -> {
                if (tt.getOne().isInput()) {
                    Task t = tt.getTwo();
                    t.log("Answers " + tt.getOne());
                    buffer.accept(t);
                }
            });

        }
    }
}
