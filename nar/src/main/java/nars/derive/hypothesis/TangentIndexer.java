package nars.derive.hypothesis;

import jcog.data.list.table.Table;
import nars.Task;
import nars.concept.Concept;
import nars.concept.snapshot.Snapshot;
import nars.concept.snapshot.TaskLinkSnapshot;
import nars.derive.Derivation;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.term.Term;
import nars.term.atom.Atom;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * caches ranked reverse atom termlinks in concept meta table
 * stateless
 */
public class TangentIndexer extends AbstractHypothesizer {

	int ATOM_TANGENT_REFRESH_DURS = 1;

	protected boolean cache(Term target) {
		return target instanceof Atom;
		//return target.volume() <= 3;
	}

	@Override
	@Nullable
	protected Term reverse(Term target, TaskLink link, Task task, TaskLinks links, Derivation d) {

		float probability =
			//0.5f;
			(float) (0.5f / Math.pow(target.volume(), 4));
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

			if (match != null && !match.links.isEmpty())
				return match.sample(((Predicate<TaskLink>) link::equals).negate(),
					task.punc(), d.random);
		}

		return DirectTangent.the.sampleReverseMatch(target, link, links, d);
	}


}
