package nars.table.eternal;

import nars.Task;
import nars.table.dynamic.DynamicTaskTable;
import nars.task.util.Answer;

public class ShuffledTaskTable extends DynamicTaskTable  {
	public final Task[] tasks;

	public ShuffledTaskTable(Task... tasks) {
		super(tasks[0].term().concept(), tasks[0].isBelief());
		this.tasks = tasks;
		//TODO verify all tasks have the same concept and same punctuation (either belief or goal)
	}

	@Override
	public void match(Answer a) {
		//try one at a time
		a.test(tasks[a.random().nextInt(tasks.length)]);
	}
}
