package nars.derive.adjacent;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.attention.TaskLinkWhat;
import nars.concept.Concept;
import nars.concept.snapshot.Snapshot;
import nars.derive.Derivation;
import nars.link.TaskLinkBag;
import nars.link.TaskLinks;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static nars.Op.ATOM;

/**
 * caches results of an exhaustive search (ex: of all or some concepts in memory) and supplies tangents using this
 */
public abstract class AbstractAdjacentIndexer extends AdjacentIndexer {

	static final AtomicInteger serial = new AtomicInteger();
	final String id = getClass().getSimpleName() + serial.getAndIncrement();

	private static final FasterList<Term> EmptyFasterList = new FasterList(0);


	public int ttl(Derivation d) {
		//return -1; //permanent
		return Math.round(d.dur * (float) ATOM_TANGENT_REFRESH_DURS);
	}

	@Override
	public @Nullable Term adjacent(Term from, Term to, byte punc, TaskLinks links, Derivation d) {

		if (to.hasAny(ATOM)) {

            NAR nar = d.nar;


            List<Term> tangent = Snapshot.get(to, nar, id, d.time, ttl(d), (Concept targetConcept, List<Term> t) -> {
				//TOO SLOW, impl indexes

                TaskLinkBag bag = ((TaskLinkWhat) (d.x)).links.links;
				int[] ttl = {Math.max(4, bag.size() / 8)}; //TODO parameter

                FasterList<Term> l = new FasterList<Term>(ttl[0]);
				bag.sampleUnique(d.random, (c -> {
                    Term ct = c.term();
					if (!ct.equals(to) && test(ct, to))
						l.add(ct);
					return --ttl[0] > 0;
				}));

				if (l.isEmpty())
					return EmptyFasterList;
				else {
					l.trimToSize();
					return l;
				}
			});

			if (tangent != null)
				return ((FasterList<Term>)tangent).get(d.random); //System.out.println(target + "\t" + tangent);

		}
		return null;
	}

	public abstract boolean test(Term concept, Term target);


}
