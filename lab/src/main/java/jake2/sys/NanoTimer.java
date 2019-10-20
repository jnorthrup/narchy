/*
 * NanoTimer.java
 * Copyright (C) 2005
 * 
 * $Id: NanoTimer.java,v 1.1 2005-07-01 14:11:00 hzi Exp $
 */
package jake2.sys;


public final class NanoTimer extends Timer {

	private final long base;

	NanoTimer() {
		base = System.nanoTime();
	}
	
	@Override
    public long currentTimeMillis() {
        long time = System.nanoTime();
        long delta = time - base;
		if (delta < 0L) {
			delta += Long.MAX_VALUE + 1L;
		}
		return (long)((double) delta * 0.000001);
	}

}
