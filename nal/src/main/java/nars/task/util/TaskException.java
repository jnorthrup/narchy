package nars.task.util;

import nars.term.Termlike;
import nars.util.SoftException;

/**
 * reports problems when constructing a Task
 */
public final class TaskException extends SoftException {

    private final Termlike task;


    public TaskException(String message, Termlike t) {
        super(message);
        this.task = t;
//        if (t instanceof Task)
//            ((Task) t).delete();
    }

    
    @Override
    public String getMessage() {
        var m = super.getMessage();
        return ((m!=null) ? m + ": " : "") + (task!=null ? task.toString() : "");



    }

}
