package nars.exe.impl;

import jcog.TODO;

import java.util.function.BooleanSupplier;

/** realtime hard deadline single-thread
 * TODO
 * */
public class UniExecRT extends UniExec {
	@Override protected BooleanSupplier runUntil() {
		//TODO compare with realtime clock
		throw new TODO();
	}
}
