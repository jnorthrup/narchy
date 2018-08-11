package nars.term.util;

import nars.term.Compound;
import nars.term.Term;

import java.util.function.BiConsumer;

/**
 * Extends SubtermVisitor with awareness of the superterm being traversed
 */
@FunctionalInterface
public interface SubtermVisitorX extends BiConsumer<Term /* subterm */ , Compound /* superterm */>
{

}
