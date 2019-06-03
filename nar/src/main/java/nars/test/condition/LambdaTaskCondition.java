package nars.test.condition;

import nars.Task;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class LambdaTaskCondition extends TaskCondition {
    private final Predicate<Task> tc;

    public LambdaTaskCondition(Predicate<Task> tc) {
        this.tc = tc;
    }

    @Override public boolean matches(@Nullable Task task) {
        return tc.test(task);
    }
}
