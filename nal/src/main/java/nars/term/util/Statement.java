package nars.term.util;

import jcog.TODO;
import nars.Op;
import nars.Param;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.builder.HeapTermBuilder;
import nars.time.Tense;
import nars.unify.ellipsis.Ellipsis;

import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * statements include: inheritance -->, similarity <->, and implication ==>
 */
public class Statement {

    public static Term statement(Op op, int dt, Term subject, Term predicate) {
        if (subject == Null || predicate == Null)
            return Null;

        boolean dtConcurrent = dt != XTERNAL && Conj.concurrent(dt);
        if (dtConcurrent) {
            if (subject.equals(predicate))
                return True;
            if (op == INH || op == SIM) {

                if ((subject == False && predicate == True) || (predicate == False && subject == True))
                    return False;

                if (subject instanceof Bool || predicate instanceof Bool)
                    return Null;

                if (subject.unneg().equalsRoot(predicate.unneg()))
                    return Null; //dont support non-temporal statements where the root is equal because they cant be conceptualized
            }
        }


        if (op == IMPL) {

            if (subject == True)
                return predicate;
            if (subject == False)
                return Null;
            if (subject.hasAny(IMPL))
                return Null;

            //test this after all of the recursions because they may have logically eliminated an IMPL that was in the input
            //TODO valid cases where subj has impl?

            switch (predicate.op()) {
                case BOOL:
                    //reduce to the subject as a general condition for the superclass to utilize
                    if (predicate == True)
                        return subject;
                    if (predicate == False)
                        return subject.neg();
                    return Null;
                case NEG:
                    return statement(IMPL, dt, subject, predicate.unneg()).neg();//recurse
                case IMPL: {
                    Term newSubj, inner = predicate.sub(0);
                    if (dt == DTERNAL || dt == XTERNAL) {
                        newSubj = HeapTermBuilder.the.conj(dt, subject, inner);
                    } else {
                        newSubj = Conj.sequence(subject, 0, inner, subject.eventRange() + dt);
                    }
                    return statement(IMPL, predicate.dt(), newSubj, predicate.sub(1)); //recurse
                }
            }

            if (!subject.op().eventable || !predicate.op().eventable)
                return Null;

            //implicit dternal to parallel promotion in temporal implication
            if (dt!=XTERNAL && dt!=DTERNAL) {
                if (subject.op()==CONJ && subject.dt()==DTERNAL && !Conj.isSeq(subject))
                    subject = subject.dt(0);
                if (predicate.op()==CONJ && predicate.dt()==DTERNAL && !Conj.isSeq(predicate))
                    predicate = predicate.dt(0);
            }

            int subjDT = subject.dt();

            if (Term.commonStructure(subject, predicate)) {

                //TODO simple case when no CONJ or IMPL are present

                if (dt != XTERNAL && subjDT != XTERNAL && predicate.dt() != XTERNAL && !subject.OR(x->x instanceof Ellipsis) && !predicate.OR(x->x instanceof Ellipsis) ) {



                    boolean subjNeg = subject.op() == NEG;

                    long po;
                    if (dt == DTERNAL) {
                        po = predicate.dt() == DTERNAL ? ETERNAL : 0;
                    } else {
                        po = subject.eventRange() + dt;
                    }


                    ConjDiff newPredConj = ConjDiff.the(subjDT != DTERNAL ? 0 : (dt != DTERNAL ? 0 : ETERNAL), subject.negIf(subjNeg), po, predicate, subjNeg);
                    Term newPred = newPredConj.term();

                    boolean predChange = !predicate.equals(newPred);
                    if (predChange) {

                        if (newPred instanceof Bool) {
                            return newPred;
                        }


                        if (dt != DTERNAL) {
                            int shift = Tense.occToDT(newPredConj.shift());
//                            int shift = predicate.subTimeFirst(newPred.eventFirst());
//
//                            if (shift == DTERNAL && predicate.subterms().equals(newPred.subterms()) && predicate.dt() == DTERNAL && newPred.dt() == 0) {
//                                shift = 0; //HACK handle dternal -> to zero implicit conversion
//                            }
//                            if (shift == DTERNAL && Tense.dtSpecial(predicate.dt()) && Tense.dtSpecial(newPred.dt()) && newPred.AND(predicate::contains)) {
//                                shift = 0; //sub-condition of parallel
//                            }

                            //int shift;
                            //if (newPred.op() != CONJ) {
                            //    shift = predicate.subTimeFirst(newPred);
                            //} else {
//                                int[] s = new int[]{DTERNAL};
//                                Term finalPredicate = predicate;
//                                newPred.eventsWhile((when, what) -> {
//                                    int wshift = finalPredicate.subTimeFirst(what);
//                                    if (wshift != DTERNAL) {
//                                        s[0] = Tense.occToDT(wshift - when);
//                                        return false;
//                                    }
//                                    return true; //keep going
//                                }, 0, true, true, false, 0);
                                //shift = s[0];
                            //}
                            if (shift == DTERNAL || shift == XTERNAL) {
                                if (Param.DEBUG)
                                    throw new TODO();
                                else
                                    return Null; //??
                            }

                            dt += shift;

                        }

                        predicate = newPred;
                        if (predicate.op()==NEG)
                            return statement(IMPL, dt, subject, predicate.unneg()).neg();//recurse
                    }
                }

            }





        } else if (op == SIM) {
            if (subject instanceof Bool || predicate instanceof Bool) {

            }
            if (subject.compareTo(predicate) > 0) {
                //swap order
                Term x = predicate;
                predicate = subject;
                subject = x;
            }
        }

        if ((op != IMPL || dtConcurrent) /*&& !subject.hasAny(Op.VAR_PATTERN) && !predicate.hasAny(Op.VAR_PATTERN)*/) {

            Predicate<Term> delim = (op == IMPL) ?
                    recursiveCommonalityDelimeterStrong : Op.recursiveCommonalityDelimeterWeak;

            if ((containEachOther(subject, predicate, delim)))
                return Null;

//            boolean sa = subject instanceof AliasConcept.AliasAtom;
//            if (sa) {
//                Term sd = ((AliasConcept.AliasAtom) subject).target;
//                if (sd.equals(predicate) || containEachOther(sd, predicate, delim))
//                    return Null;
//            }
//            boolean pa = predicate instanceof AliasConcept.AliasAtom;
//            if (pa) {
//                Term pd = ((AliasConcept.AliasAtom) predicate).target;
//                if (pd.equals(subject) || containEachOther(pd, subject, delim))
//                    return Null;
//            }
//            if (sa && pa) {
//                if (containEachOther(((AliasConcept.AliasAtom) subject).target, ((AliasConcept.AliasAtom) predicate).target, delim))
//                    return Null;
//            }
        }

        //return builder.compound(op, dt, subject, predicate);
        Term t = HeapTermBuilder.the.theCompound(op, dt, subject, predicate);

        //if (Param.DEBUG) {
        //test image normalization
        if (op==INH) {
            Term tt = Image.imageNormalize(t);
            if (tt instanceof Bool) {
                return tt;
            }
        }
        //}

        return t;
    }

    private static class ConjDiff extends Conj {
        private final Conj se;
        private final boolean invert;
        private final long[] seEvents;

        public static ConjDiff the(long subjAt, Term subj, long offset, Term subtractWith, boolean invert) {
            Conj subtractFrom = new Conj(subjAt, subj);

            if (subtractFrom.eventCount(ETERNAL)>0 && subtractFrom.event.size() > 1) {
                //has both eternal and temporal components; mask the eternal components so they are not eliminated from the result
                subtractFrom.removeAll(ETERNAL);
            }

            return new ConjDiff(subtractFrom, offset, subtractWith, invert);
        }

        ConjDiff(Conj subtractFrom, long offset, Term subtractWith, boolean invert) {
            super(subtractFrom.termToId, subtractFrom.idToTerm);
            this.se = subtractFrom;
            this.seEvents = se.event.keySet().toArray();
            this.invert = invert;
            add(offset, subtractWith);
            //distribute();
        }

//        @Override
//        protected boolean addConjEventFactored() {
//            //disable adding in factored form
//            return false;
//        }

        @Override
        protected int addFilter(long at, Term x, byte id) {
            if (at == ETERNAL) {
                boolean hasAbsorb = false;
                for (long see : seEvents) {
                    int f = test(see, id);
                    if (f == -1) return -1;
                    if (f == +1) hasAbsorb = true; //but keep checking for contradictions first
                }
                if (hasAbsorb)
                    return +1; //ignore this term (dont repeat in the predicate)
            } else {
                int f = test(at, id);
                if (f == -1) return -1;
                int f2 = (at == ETERNAL) ? f : test(ETERNAL, id);
                if (f2 == -1) return -1;
                if (f == +1 || f2 == +1) return +1; //ignore this term (dont repeat in the predicate)
            }
            return 0;
        }

        private int test(long at, byte id) {
            return se.conflictOrSame(at, (byte) (id * (invert ? -1 : +1)));
        }
    }
}
