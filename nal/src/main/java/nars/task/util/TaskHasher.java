package nars.task.util;

import jcog.bloom.hash.DynBytesHasher;
import jcog.data.byt.DynBytes;
import nars.Task;
import nars.io.IO;

public class TaskHasher extends DynBytesHasher<Task> {

	public TaskHasher() {
		super(1024);
	}

	@Override
	protected void write(Task t, DynBytes d) {
		IO.taskToBytes(t, d);
	}

}
