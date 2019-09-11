package nars.truth.dynamic;

import jcog.util.ObjectLongLongPredicate;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.time.Tense;

import static nars.Op.IMPL;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

public enum DynamicImplConjTruth {
	;

	public static final AbstractDynamicTruth ImplSubjDisj = new DynamicStatementTruth.AbstractInhImplSectTruth(true, true) {
		@Override
		protected boolean truthNegComponents() {
			return true;
		}

		@Override
		public boolean temporal() {
			return true;
		}

		@Override
		public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
			Subterms ss = superterm.subterms();
			Term subj = ss.sub(0);
			assert (subj instanceof Neg);
			return decomposeImplConj(superterm, start, end, each, ss.sub(1),
				(Compound) (subj.unneg()), true, true);
		}

		@Override
		public Term reconstruct(Compound superterm, long start, long end, DynTaskify components) {
			return reconstruct(superterm, components, true, false);
		}
	};
	//    public static final AbstractDynamicTruth ImplSubjConj = new DynamicStatementTruth.AbstractInhImplSectTruth(true, false) {
//        @Override
//        protected boolean truthNegComponents() {
//            return false;
//        }
//
//        @Override
//        public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
//            Subterms ss = superterm.subterms();
//            return decomposeImplConj(superterm, start, end, each, ss.sub(1), (Compound) ss.sub(0), true,
//                    false /* reconstruct as-is; union only applies to the truth calculation */);
//        }
//
//        @Override
//        public Term reconstruct(Compound superterm, DynTaskify components, long start, long end) {
//            return reconstruct(superterm, components, true, false
//                    /* reconstruct as-is; union only applies to the truth calculation */);
//        }//    };
	public static final AbstractDynamicTruth ImplPred = new DynamicStatementTruth.AbstractInhImplSectTruth(false, false) {
		@Override
		public boolean evalComponents(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each) {
			Subterms supertermSubs = superterm.subterms();
			Term common = DynamicStatementTruth.stmtCommon(subjOrPred, supertermSubs);
			Compound decomposed = (Compound) DynamicStatementTruth.stmtCommon(!subjOrPred, supertermSubs);
			return decomposeImplConj(superterm, start, end, each, common, decomposed, false, false);
		}

		@Override
		public boolean temporal() {
			return true;
		}
	};

	private static boolean decomposeImplConj(Compound superterm, long start, long end, ObjectLongLongPredicate<Term> each, Term common, Compound decomposed, boolean subjOrPred, boolean negateConjComponents) {

		int superDT = superterm.dt() == DTERNAL ? 0 : superterm.dt();
		int decRange = subjOrPred ? decomposed.eventRange() : 0;
		return DynamicConjTruth.ConjIntersection.evalComponents(decomposed, start, end, (what, s, e) -> {

			int innerDT;
			if (superDT == XTERNAL) {

				innerDT = XTERNAL; //force XTERNAL since 0 or DTERNAL will collapse

			} else {
//                if (s == start && e - s >= decRange) {
//                    innerDT = 0; //eternal/immediate component
//                } else
				long d = s - start;
				innerDT = Tense.occToDT(subjOrPred ? decRange - d : d) + superDT;
			}

			Term i = subjOrPred ?
				IMPL.the(what.negIf(negateConjComponents), innerDT, common)
				:
				IMPL.the(common, innerDT, what).negIf(negateConjComponents);

			return i instanceof Compound && i.unneg().op() == IMPL && each.accept(i, start, end);
		});
	}
}
