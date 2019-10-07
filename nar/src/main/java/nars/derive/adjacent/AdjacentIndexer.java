package nars.derive.adjacent;

import jcog.data.list.table.Table;
import nars.concept.Concept;
import nars.concept.snapshot.Snapshot;
import nars.concept.snapshot.TaskLinkSnapshot;
import nars.derive.Derivation;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

/**
 * caches ranked reverse atom termlinks in concept meta table
 * stateless
 */
public class AdjacentIndexer implements AdjacentConcepts {

	int ATOM_TANGENT_REFRESH_DURS = 1;

	protected boolean cache(Term target) {
		//return target instanceof Atom;
		return target.volume() <=
			//5
			//3;
			1;
	}

	@Override
	@Nullable
	public Term adjacent(Term from, Term to, byte punc, TaskLinks links, Derivation d) {


//		float probability;
//		Op srcOp = link.from().op();
//		if ((srcOp == INH || srcOp == SIM) && link.from().contains/* non-recursively */(target)) {
//			probability = 0.9f; //HACK
//		} else {
//			float probBase = 0.5f;
//			probability =
//				(float) (probBase / Math.pow(target.volume(), 2));
//		}

		//1f/Math.max(2,link.from().volume());
		//1-1f/Math.max(2,link.from().volume());
		//1-1f/(Math.max(1,link.from().volume()-1));
		//1f / (1 + link.from().volume()/2f);
		//1f / (1 + link.from().volume());
		//1 - 1f / (1 + s.volume());
		//1 - 1f / (1 + t.volume());

//		if (d.random.nextFloat() > probability)
//			return null;

		if (cache(to)) {
			TaskLinkSnapshot match = Snapshot.get(to, d.nar, links.links.id(false, true), d.time(), Math.round(d.dur() * ATOM_TANGENT_REFRESH_DURS), (Concept T, TaskLinkSnapshot s) -> {
				if (s == null)
					s = new TaskLinkSnapshot();
				s.commit(T.term(), links.links, ((Table<?, TaskLink>) links.links).capacity(), true);
				return s;
			});

			if (match == null || match.isEmpty())
				return null;

			return match.sample(x -> !from.equals(x), punc, d.random);
		} else {

			return DirectTangent.the.adjacent(from, to, punc, links, d);
		}
	}


}
