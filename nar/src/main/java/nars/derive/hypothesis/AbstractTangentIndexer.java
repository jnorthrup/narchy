package nars.derive.hypothesis;

import jcog.data.list.FasterList;
import nars.Task;
import nars.concept.Concept;
import nars.concept.snapshot.Snapshot;
import nars.derive.Derivation;
import nars.link.TaskLink;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static nars.Op.ATOM;

/**
 * caches results of an exhaustive search (ex: of all or some concepts in memory) and supplies tangents using this
 */
public abstract class AbstractTangentIndexer extends TangentIndexer {

	final String id;

	protected AbstractTangentIndexer() {
		 id = getClass().getSimpleName();
	}

	public int ttl(Derivation d) {
		//return -1; //permanent
		return Math.round(d.dur() * ATOM_TANGENT_REFRESH_DURS);
	}

	@Nullable
	protected Term tangentRandom(Compound target, Derivation d) {

		if (target.hasAny(ATOM)) {

			List<Term> tangent = Snapshot.get(target, d.nar, id, d.time(), ttl(d), (Concept targetConcept, List<Term> t) -> {
				FasterList<Term> l = d.nar.concepts().filter(c -> {
					Term ct = c.term();
					return !ct.equals(target) && test(ct, target);
				}).map(Termed::term).collect(Collectors.toCollection(FasterList::new));
				if (l.isEmpty())
					return Collections.EMPTY_LIST;
				else {
					l.trimToSize();
					return l;
				}
			});

			if (tangent != null && !tangent.isEmpty())
				return tangent.get(d.random.nextInt(tangent.size())); //System.out.println(target + "\t" + tangent);

		}
		return null;
	}

	abstract public boolean test(Term concept, Term target);

	@Override
	protected @Nullable Term decompose(Compound src, Task task, Derivation d) {
		Term t = tangentRandom(src, d);
		return t != null ? t : super.decompose(src, task, d);
	}

}
