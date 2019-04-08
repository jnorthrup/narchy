package nars.time.event;

import org.jetbrains.annotations.Nullable;

/** after a task is perceived and processed */
abstract public class WhenTask extends WhenInternal {

    /** punctuations, or null for all */
    @Nullable
    abstract byte[] punc();

}
