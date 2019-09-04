package nars.op.time;

import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.functor.AbstractInlineFunctor;

import static nars.term.atom.Bool.Null;
import static nars.time.Tense.XTERNAL;

/** temporal quantifiers */
public class TimeFunc {
	abstract static class TemporalAccessor {
		/** discovers the 'other' events besides 'relativeTo', and returns as a term (a sequence if necessary) */
		abstract public Term otherEvents(Term t, Term relativeTo, boolean inclBefore, boolean includeIt, boolean includeAfter);

		/** measures delta time between events in 't' , or XTERNAL if uncomputable */
		abstract public int dt(Term t, Term x, Term y);
	}

	static final TemporalAccessor ConjAccessor = new TemporalAccessor() {

		@Override
		public Term otherEvents(Term t, Term relativeTo, boolean inclBefore, boolean includeIt, boolean includeAfter) {
			return Null;  //TODO
		}

		@Override
		public int dt(Term t, Term x, Term y) {
			return XTERNAL; //TODO
		}
	};
	static final TemporalAccessor ImplAccessor = new TemporalAccessor() {

		@Override
		public Term otherEvents(Term t, Term relativeTo, boolean inclBefore, boolean includeIt, boolean includeAfter) {
			return Null; //TODO
		}

		@Override
		public int dt(Term t, Term x, Term y) {
			return XTERNAL;  //TODO
		}
	};

	public static class before extends AbstractInlineFunctor {

		public before() {
			super("before");
		}

		@Override
		public Term apply(Evaluation e, Subterms args) {
			if (args.subs()!=3)
				return Null;

			Term temporal = args.sub(0);
			TemporalAccessor a;
			switch (temporal.op()) {
				case CONJ: a = ConjAccessor; break;
				case IMPL: a = ImplAccessor; break;
				default: return Null;
			}

			Term x = args.sub(1), y = args.sub(2);
			//TODO
			return Null;
		}



	};
}
