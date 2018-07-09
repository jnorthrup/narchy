package nars.task.util;

import nars.Task;
import nars.term.Termed;
import nars.util.SoftException;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 10/15/16.
 */
public final class InvalidTaskException extends SoftException {

    private final Termed task;


    public InvalidTaskException(Termed t, String message) {
        super(message);
        this.task = t;
        if (t instanceof Task)
            ((Task) t).delete();
    }

    @NotNull
    @Override
    public String getMessage() {
        String m = super.getMessage();
        return ((m!=null) ? m + ": " : "") + (task!=null ? task.toString() : "");



    }

}
