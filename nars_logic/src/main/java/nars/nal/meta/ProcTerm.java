package nars.nal.meta;

import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Created by me on 12/30/15.
 */
public interface ProcTerm<C> extends Term, Consumer<C> {

    default void appendJavaProcedure(@NotNull StringBuilder s) {
        s.append("/* TODO: " + this + " */\n");
    }

}
