package nars.control;

import nars.term.Term;
import org.jetbrains.annotations.Nullable;

/** explains why it exists */
public interface Caused {

	@Nullable Term why();

}
