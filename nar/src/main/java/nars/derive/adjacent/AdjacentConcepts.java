package nars.derive.adjacent;

import nars.derive.Derivation;
import nars.link.TaskLinks;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

/**
 * implementations resolve adjacent concepts to a concept in a context by a particular strategy
 */
@FunctionalInterface public interface AdjacentConcepts {

	/**
	 * samples an adjacent concept term
	 *
	 * @param to the final to of the tasklink (not necessarily link.to() in cases where it's dynamic).
	 *               <p>
	 *               this term is where the tasklink "reflects" or "bounces" to
	 *               find an otherwise unlinked tangent concept
	 *               <p>
	 *               resolves reverse termlink
	 *               return null to avoid reversal
	 * @param links
	 */
	@Nullable Term adjacent(Term from, Term to, byte punc, TaskLinks links, Derivation d);


}
