/*
 * SizeChangeListener.java
 * Copyright (C)  2008
 * 
 * $Id: SizeChangeListener.java,v 1.1 2008-03-02 20:21:13 kbrussel Exp $
 */

package jake2;

/** Provides a callback when the user changes the video mode of the
    game. */

@FunctionalInterface
public interface SizeChangeListener {
    void sizeChanged(int width, int height);
}
