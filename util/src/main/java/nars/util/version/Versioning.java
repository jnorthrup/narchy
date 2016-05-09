package nars.util.version;

import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/** versioning context that holds versioned instances */
abstract public class Versioning extends FasterList<Versioned> {

    public Versioning(int capacity) {
        super(0, new Versioned[capacity]);
    }


    private int now;

    /** serial id's assigned to each Versioned */
    private int nextID = 1;


    @NotNull
    @Override
    public String toString() {
        return now + ":" + super.toString();
    }

    public final int now() {
        return now;
    }


    /** start a new version with a commit, returns current version  */
    public final int newChange(Versioned v) {
        int c = commit();
        if (!addIfCapacity(v))
            throw new OutOfMemoryError("Versioned stack fault");
        return c;
    }

    /** track change on current commit, returns current version */
    public final int continueChange(Versioned v) {
        if (!addIfCapacity(v))
            throw new RuntimeException();
        return now;
    }

    public final int commit() {
        return ++now;
    }


    /** reverts to previous state */
    public final void revert() {
        revert(now -1);
    }

    /** reverts/undo to previous state */
    public final void revert(int when) {
        int was = now;
        if (was != when)
            doRevert(when);
    }

    private final void doRevert(int when) {
        //if (was < when)
            //throw new RuntimeException("reverting to future time");
        now = when;

        int s = size()-1;
        if (s == -1) return; //empty

        Versioned[] ii = this.items;
        while (ii[s].revertNext(when)) {
            if (--s < 0)
                break;
        }

        popTo(s);
    }

    /** assigns a new serial ID to a versioned item for its use as a hashcode */
    public final int track() {
        return nextID++;
    }


    abstract public <X> FasterList<X> newValueStack();
    abstract public int[] newIntStack();
    abstract public void onDeleted(@NotNull Versioned v);


    public void delete() {
        forEach((Consumer<? super Versioned>)Versioned::delete);
        clear();
    }

}
