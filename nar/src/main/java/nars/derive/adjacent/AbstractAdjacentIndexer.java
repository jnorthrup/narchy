package nars.derive.adjacent;

import jcog.data.list.FasterList;
import nars.concept.Concept;
import nars.concept.snapshot.Snapshot;
import nars.derive.Derivation;
import nars.link.TaskLinks;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static nars.Op.ATOM;

/**
 * caches results of an exhaustive search (ex: of all or some concepts in memory) and supplies tangents using this
 */
public abstract class AbstractAdjacentIndexer extends AdjacentIndexer {

	final String id;

	protected AbstractAdjacentIndexer() {
		 id = getClass().getSimpleName();
	}

	public int ttl(Derivation d) {
		//return -1; //permanent
		return Math.round(d.dur() * ATOM_TANGENT_REFRESH_DURS);
	}

	@Override
	public @Nullable Term adjacent(Term from, Term to, byte punc, TaskLinks links, Derivation d) {

		if (to.hasAny(ATOM)) {

			List<Term> tangent = Snapshot.get(to, d.nar, id, d.time(), ttl(d), (Concept targetConcept, List<Term> t) -> {
				//TOO SLOW, impl indexes
				FasterList<Term> l = d.nar.concepts().map(c -> {
					Term ct = c.term();
					return (!ct.equals(to) && test(ct, to)) ? ct : null;
				}).filter(Objects::nonNull).collect(Collectors.toCollection(FasterList::new));

				if (l.isEmpty())
					return Collections.EMPTY_LIST;
				else {
					l.trimToSize();
					return l;
				}
			});

			if (tangent != null) {
				int ts = tangent.size();
				if (ts > 0) {
					return tangent.get(ts > 1 ? d.random.nextInt(ts) : 0); //System.out.println(target + "\t" + tangent);
				}
			}

		}
		return null;
	}

	abstract public boolean test(Term concept, Term target);


}
