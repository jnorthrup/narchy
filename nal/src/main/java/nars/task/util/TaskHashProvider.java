package nars.task.util;

import jcog.bloom.hash.DynBytesHashProvider;
import jcog.bloom.hash.HashProvider;
import jcog.data.byt.DynBytes;
import nars.Task;
import nars.io.IO;

public class TaskHashProvider implements HashProvider<Task> {

    final DynBytes d = new DynBytes(256);

    @Override
    public int hash1(Task t) {
        IO.taskToBytes(t, d.clear());
        return DynBytesHashProvider.the.hash1(d);
    }

    @Override
    public int hash2(Task t) {
        return DynBytesHashProvider.the.hash2(d);
    }

}
