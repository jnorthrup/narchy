package nars.time.event;

import org.jetbrains.annotations.Nullable;

abstract public class AtTask extends InternalEvent {

    /** punctuations, or null for all */
    @Nullable
    abstract byte[] punc();

}
