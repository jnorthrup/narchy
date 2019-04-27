package nars.util;

import com.google.common.base.Joiner;
import nars.NAL;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Created by me on 10/30/16.
 */
public class SoftException extends RuntimeException {

    final static int depth = 7;

    @Nullable
    private StackWalker.StackFrame[] stack;

    public SoftException() {
        super();
    }

    public SoftException(String message) {
        super(message);
    }

    @Override
    public final synchronized Throwable fillInStackTrace() {
        if (!NAL.DEBUG)
            return this; 
        else {
            
            stack = StackWalker.getInstance().walk(
                s -> s.skip(5).limit(depth).toArray(StackWalker.StackFrame[]::new)
            );
            return this;
        }
    }

    @Override
    public String toString() {
        String m = getMessage();
        return (m!=null ? m + '\n' : "") + (stack!=null ? Joiner.on("\n").join(stack) : "");
    }

    @Override
    public void printStackTrace() {
        Stream.of(stack).forEach(System.err::println);
    }
}
