package nars.task.proxy;

import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

public class SpecialPuncTermAndTruthTask extends SpecialTermTask {

    private final Truth truth;
    private final byte punc;

    public SpecialPuncTermAndTruthTask(Term term, byte punc, Truth truth, Task task) {
        super(term, task);
        this.truth = truth;
        this.punc = punc;
    }

    @Override
    public byte punc() {
        return punc;
    }

    @Override
    public @Nullable Truth truth() {
        return truth;
    }

}
