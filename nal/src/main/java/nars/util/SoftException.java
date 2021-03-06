package nars.util;

import com.google.common.base.Joiner;
import nars.NAL;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by me on 10/30/16.
 */
public class SoftException extends RuntimeException {

    private static final int depth = 7;

    private @Nullable StackWalker.StackFrame[] stack;

    public SoftException() {
        super();
    }

    public SoftException(String message) {
        super(message);
    }

    @Override
    public final synchronized Throwable fillInStackTrace() {
        if (NAL.DEBUG) {
            stack = StackWalker.getInstance().walk(
                    new Function<Stream<StackWalker.StackFrame>, StackWalker.StackFrame[]>() {
                        @Override
                        public StackWalker.StackFrame[] apply(Stream<StackWalker.StackFrame> s) {
                            return s.skip(5L).limit((long) depth).toArray(StackWalker.StackFrame[]::new);
                        }
                    }
            );
        }
        return this;
    }

    @Override
    public String toString() {
        String m = getMessage();
        return (m!=null ? m + '\n' : "") + (stack!=null ? Joiner.on("\n").join(stack) : "");
    }

    @Override
    public void printStackTrace() {
        if (stack!=null) {
            for (@Nullable StackWalker.StackFrame stackFrame : stack) {
                System.err.println(stackFrame);
            }
        }
        super.printStackTrace();
    }
}
