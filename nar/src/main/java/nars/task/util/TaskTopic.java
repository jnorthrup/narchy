package nars.task.util;

import jcog.event.ByteTopic;
import nars.Op;
import nars.Task;

public class TaskTopic extends ByteTopic<Task> {

    public TaskTopic() {
        super(Op.Punctuation);
    }

    public final void emit(Task t) {
        emit(t, t.punc());
    }
}
