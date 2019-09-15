package nars.exe.impl;

import jcog.TODO;

/** realtime hard deadline single-thread
 * TODO
 * */
public class UniExecRT extends UniExec {
	@Override protected long timeSliceNS() {
		//TODO compare with realtime clock
		throw new TODO();
	}
}
