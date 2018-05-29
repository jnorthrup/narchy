package nars.util.term;

import jcog.tree.radix.MyConcurrentRadixTree;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

/**
 * String interner that maps strings to integers and resolves them
 * bidirectionally with a globally shared Atomic concept
 */
public class TermRadixTree extends MyConcurrentRadixTree<Termed> {

    @Override
    public Termed put(Termed value) {
        return put(key(value), value);
    }

    @NotNull
    public static TermBytes key(Termed value) {
        return TermBytes.termByVolume(value.term());
    }

    public Termed get(TermBytes term) {
        return getValueForExactKey(term);
    }

}
