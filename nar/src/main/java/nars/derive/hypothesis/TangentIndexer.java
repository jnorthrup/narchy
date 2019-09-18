package nars.derive.hypothesis;

import jcog.TODO;
import jcog.data.list.table.Table;
import nars.NAL;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.snapshot.Snapshot;
import nars.concept.snapshot.TaskLinkSnapshot;
import nars.derive.Derivation;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import static nars.Op.INH;
import static nars.Op.SIM;

/**
 * caches ranked reverse atom termlinks in concept meta table
 * stateless
 */
public class TangentIndexer extends AbstractHypothesizer {

	int ATOM_TANGENT_REFRESH_DURS = 1;

	protected boolean cache(Term target) {
		//return target instanceof Atom;
		return target.volume() <= 3;
	}

	@Override
	@Nullable
	protected Term reverse(Term target, TaskLink link, Task task, TaskLinks links, Derivation d) {


		float probability;
		Op srcOp = link.from().op();
		if ((srcOp == INH || srcOp == SIM) && link.from().contains/* non-recursively */(target)) {
			probability = 0.9f; //HACK
		} else {
			float probBase = 0.5f;
			probability =
				(float) (probBase / Math.pow(target.volume(), 2));
		}

		//1f/Math.max(2,link.from().volume());
		//1-1f/Math.max(2,link.from().volume());
		//1-1f/(Math.max(1,link.from().volume()-1));
		//1f / (1 + link.from().volume()/2f);
		//1f / (1 + link.from().volume());
		//1 - 1f / (1 + s.volume());
		//1 - 1f / (1 + t.volume());

		if (d.random.nextFloat() > probability)
			return null;

		if (cache(target)) {
			TaskLinkSnapshot match = Snapshot.get(target, d.nar, links.links.id(false, true), d.time(), Math.round(d.dur() * ATOM_TANGENT_REFRESH_DURS), (Concept T, TaskLinkSnapshot s) -> {
				if (s == null)
					s = new TaskLinkSnapshot();
				s.commit(T.term(), links.links, ((Table<?, TaskLink>) links.links).capacity(), true);
				return s;
			});

			if (match == null || match.isEmpty())
				return null;

			Term source = link.from();
			Term result = match.sample(x -> !source.equals(x.from()), task.punc(), d.random);
			if (result!=null && (result.equals(link.from()))) {
				if (NAL.DEBUG)
					throw new TODO();
				result = null; //HACK throw new WTF();
			}
			return result;
		} else {

			Term result = DirectTangent.the.sampleReverseMatch(target, link, links, d);
			if (result == null)
				return null;
			else {
//			if (result != null && (result.equals(link.from()) || result.equals(target)))
//				result = null; //HACK throw new WTF();
				return result;
			}
		}
	}


}