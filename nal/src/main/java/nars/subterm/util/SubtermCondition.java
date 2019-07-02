package nars.subterm.util;

import nars.term.Term;
import nars.term.util.conj.Conj;

import java.util.function.BiPredicate;

/**
 * tests various potential relations between a containing target and a subterm
 */
public enum SubtermCondition implements BiPredicate<Term, Term> {


    Subterm() {
        @Override
        public boolean test(Term container, Term x) {
            return container.contains(x);
        }

        public float cost() {
            return 0.4f;
        }
    },

    SubtermNeg() {
        @Override
        public boolean test(Term container, Term x) {
            return container.containsNeg(x);
        }

        public float cost() {
            return 0.41f;
        }
    },
//
//    /** intersect contents */
//    Subsect() {
//
//        @Override
//        public boolean test(Term container, Term x) {
//            //return x.op()==CONJ ? x.AND(container::contains) : container.contains(x);
//            return Conj.eventOf(container, x);
//        }
//
//        public float cost() {
//            return 0.7f;
//        }
//    },
//    /** intersect contents */
//    SubsectNeg() {
//
//        @Override
//        public boolean test(Term container, Term x) {
//            //return x.op()==CONJ ? x.AND(container::containsNeg) : container.containsNeg(x);
//            return Conj.eventOf(container, x.neg());
//        }
//
//        public float cost() {
//            return 0.71f;
//        }
//    },

    Recursive() {
        @Override
        public boolean test(Term container, Term x) {
            return container.containsRecursively(x);
        }

        public float cost() {
            return 0.8f;
        }
    },

    Event() {
        @Override
        public final boolean test(Term container, Term x) {
            return Conj.eventOf(container, x);
            //return Conj._eventOf(container, x, Tense.ETERNAL);
        }

        public float cost() {
            return 0.9f;
        }
    },
//    EventFirst() {
//        @Override
//        public final boolean test(Term container, Term x) {
//            return Conj.isEventFirstOrLast(container, x,  true);
//        }
//
//        @Override
//        public boolean testContainer(Term container) {
//            return container.op()==CONJ;
//        }
//
//        public float cost() {
//            return 1.5f;
//        }
//    },
//    EventLast() {
//        @Override
//        public final boolean test(Term container, Term x) {
//            return Conj.isEventFirstOrLast(container, x,false);
//        }
//
//        @Override
//        public boolean testContainer(Term container) {
//            return container.op()==CONJ;
//        }
//
//        public float cost() {
//            return 1.75f;
//        } //more intensive comparison than first
//    }

//    /**
//     * conj containment of another event, or at least one event of another conj
//     */
//    EventsAny() {
//        @Override
//        public boolean testContainer(Term container) {
//            return container.op()==CONJ;
//        }
//        @Override
//        public boolean test(Term container, Term x) {
//            if (container.op() != CONJ)
//                return false;
//
//            if (Conj.containsEvent(container, x))
//                return true;
//
//            if (x.op()==CONJ) {
//                return !x.eventsWhile((when,xx) ->
//                    xx==x || !Conj.containsOrEqualsEvent(container, xx)
//                , 0, true, true, true, 0);
//            }
//
//            return false;
////            if (container.op() != CONJ || container.volume() <= xx.volume() || !Term.commonStructure(container, xx))
////                return false;
////
////            boolean simpleEvent = xx.op() != CONJ;
////            if (simpleEvent) {
////                if (Tense.dtSpecial(container.dt())) { //simple case
////                    return container.contains(xx);
////                } else {
////                    return !container.eventsWhile((when, what) -> !what.equals(xx),
////                            0, true, true, true, 0);
////                }
////            } else {
////                Set<Term> xxe = xx.eventSet();
////                container.eventsWhile((when, what) ->
////                                !xxe.remove(what) || !xxe.isEmpty(),
////                        0, true, true, true, 0);
////                return xxe.isEmpty();
////            }
//        }
//
//        public float cost() {
//            return 2f;
//        }
//    }
    ;

    abstract public float cost();


}
