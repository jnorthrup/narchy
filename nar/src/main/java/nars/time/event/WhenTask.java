package nars.time.event;

import org.jetbrains.annotations.Nullable;

/** after a task is perceived and processed */
public abstract class WhenTask extends WhenInternal {

    /** punctuations, or null for all */
    abstract @Nullable byte[] punc();

}
