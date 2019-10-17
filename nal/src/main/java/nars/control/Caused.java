package nars.control;

import nars.term.Term;
import org.jetbrains.annotations.Nullable;

/** explains why it exists */
@FunctionalInterface
public interface Caused {

	@Nullable Term why();

}
