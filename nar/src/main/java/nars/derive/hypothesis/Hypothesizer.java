package nars.derive.hypothesis;

import jcog.data.list.FasterList;
import jcog.data.list.table.Table;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.snapshot.Snapshot;
import nars.concept.snapshot.TaskLinkSnapshot;
import nars.derive.Derivation;
import nars.derive.premise.Premise;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.time.When;
import nars.unify.UnifyAny;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static nars.Op.*;

/** generator of hypotheses */
public interface Hypothesizer {

	void premises(Predicate<Premise> p, When<NAR> when, TaskLinks links, Derivation d);

	/**
	 * samples the tasklink bag for a relevant reversal
	 * memoryless, determined entirely by tasklink bag, O(n)
	 */
	class DirectTangent extends AbstractHypothesizer {

		public static final DirectTangent the = new DirectTangent();

		private DirectTangent() {

		}

		@Override
		protected Term reverse(Term target, TaskLink link, Task task, TaskLinks links, Derivation d) {

			//< 1 .. 1.0 isnt good
			float probBase =
				0.5f;
			//0.33f;
			float probDirect =
				//(float) (probBase / Util.sqr(Math.pow(2, target.volume()-1)));
				(float) (probBase / Math.pow(target.volume(), 2));
			//(float) (probBase / Math.pow(2, target.volume()-1));
			//probBase * (target.volume() <= 2 ? 1 : 0);
			//probBase * 1f / Util.sqr(Util.sqr(target.volume()));
			//probBase * 1f / term.volume();
			//probBase * 1f / Util.sqr(term.volume());
			//probBase *  1f / (term.volume() * Math.max(1,(link.from().volume() - term.volume())));

			if (d.random.nextFloat() >= probDirect)
				return null; //term itself

			return sampleReverseMatch(target, link, links, d);

		}

		@Nullable
		public Term sampleReverseMatch(Term target, TaskLink link, TaskLinks links, Derivation d) {
			final Term[] T = {target};
			links.sampleUnique(d.random, (ll) -> {
				if (ll != link) {
					Term t = ll.reverseMatch(target);
					if (t != null) {
						T[0] = t;
						return false; //done
					}
				}
				return true;
			});
			return T[0];
		}
	}

	/**
	 * caches ranked reverse atom termlinks in concept meta table
	 * stateless
	 */
	class TangentSnapshotter extends AbstractHypothesizer {

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
				(float) (0.5f / Math.pow(target.volume(), 3));
			//1f/Math.max(2,link.from().volume());
			//1-1f/Math.max(2,link.from().volume());
			//1-1f/(Math.max(1,link.from().volume()-1));
			//1f / (1 + link.from().volume()/2f);
			//1f / (1 + link.from().volume());
			//1 - 1f / (1 + s.volume());
			//1 - 1f / (1 + t.volume());

			if (!(d.random.nextFloat() <= probability))
				return null;

			if (cache(target)) {
				TaskLinkSnapshot match = Snapshot.get(target, d.nar, links.links.id(false, true), d.time(), Math.round(d.dur() * ATOM_TANGENT_REFRESH_DURS), (Concept T, TaskLinkSnapshot s) -> {
					if (s == null)
						s = new TaskLinkSnapshot();
					s.commit(T.term(), links.links, ((Table<?, TaskLink>) links.links).capacity(), true);
					return s;
				});

				return match != null && !match.links.isEmpty() ?
					match.sample(((Predicate<TaskLink>) link::equals).negate(),
						task.punc(), d.random) :
					null;
			}

			return DirectTangent.the.sampleReverseMatch(target, link, links, d);
		}


	}

	/** caches results of an exhaustive search (ex: of all or some concepts in memory) and supplies tangents using this */
	abstract class AbstractIndexSnapshotter extends TangentSnapshotter {

		static final String id = AbstractIndexSnapshotter.class.getSimpleName();

		public int ttl(Derivation d) {
			return -1; //permanent
		}

		@Nullable
		protected Term tangentRandom(Term target, Derivation d) {

			if (target instanceof Compound && target.hasAny(ATOM)) {

				List<Term> tangent = Snapshot.get(target, d.nar, id, d.time(), ttl(d), (Concept targetConcept, List<Term> t) -> {
                    FasterList<Term> l = d.nar.concepts().filter(c -> {
                        Term ct = c.term();
                        return !ct.equals(target) && test(ct, target);
                    }).map(Termed::term).collect(Collectors.toCollection(FasterList::new));
                    if (l.isEmpty())
                        return List.of();
                    else {
                        l.trimToSize();
                        return l;
                    }
                });

				if (tangent!=null && !tangent.isEmpty())
					return tangent.get(d.random.nextInt(tangent.size())); //System.out.println(target + "\t" + tangent);

			}
			return null;
		}

		abstract public boolean test(Term concept, Term target);

		@Override
		protected @Nullable Term forward(Term target, TaskLink link, Task task, Derivation d) {
			Term t = tangentRandom(target, d);
			return t != null ? t : super.forward(target, link, task, d);
		}

	}

	class ExhaustiveIndexSnapshotter extends AbstractIndexSnapshotter {
		@Override
		public boolean test(Term t, Term target) {
			return t instanceof Compound &&
				((Compound) t).unifiesRecursively(target, z -> z.hasAny(ATOM));
		}
	}
	class FirstOrderIndexSnapshotter extends AbstractIndexSnapshotter {
		@Override
		public boolean test(Term concept, Term target) {
			if (concept instanceof Compound) {
				Op op = concept.op();
				if (op == target.op()) {
					if (new UnifyAny().unifies(concept, target))
						return true;
				} /*else if (op ==CONJ) {


					//..
				} else if (op == IMPL) {
					//..
				} else*/ {
					//try subterms
					UnifyAny u = new UnifyAny();
					return concept.subterms().OR(z -> u.unifies(z.unneg(), target));
				}

			}
			return false;
		}
	}

}
