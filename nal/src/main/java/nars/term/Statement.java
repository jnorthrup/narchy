package nars.term;

import nars.Op;
import nars.term.atom.Bool;
import nars.term.util.Conj;
import nars.term.util.TermBuilder;

import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;

/** statements include: inheritance -->, similarity <->, and implication ==> */
public class Statement {

    public static Term statement(Op op, int dt, Term subject, Term predicate, TermBuilder builder) {
        if (subject == Null || predicate == Null)
            return Null;

        boolean dtConcurrent = Conj.concurrent(dt) && dt!=XTERNAL;
        if (dtConcurrent) {
            if (subject.equals(predicate))
                return True;
            if ((op == INH || op == SIM) && subject.equalsRoot(predicate))
                return Null; //dont support non-temporal statements where the root is equal because they cant be conceptualized
        }




        if (op == IMPL) {


            if (subject == True)
                return predicate;
            if (subject == False)
                return Null;
            //test this after all of the recursions because they may have logically eliminated an IMPL that was in the input
            //TODO valid cases where subj has impl?
            if (subject.hasAny(IMPL))
                return Null;

            switch (predicate.op()) {
                case BOOL:
                    return Null;
                case NEG:
                    return statement(IMPL, dt, subject, predicate.unneg(), builder).neg();//recurse
                case IMPL: {
                    Term newSubj, inner = predicate.sub(0);
                    if (dt==DTERNAL || dt == XTERNAL) {
                        newSubj = CONJ.the(subject, dt, inner);
                    } else {
                        newSubj = Conj.conjMerge(subject, 0, inner, subject.dtRange() + dt);
                    }
                    return statement(IMPL, predicate.dt(), newSubj, predicate.sub(1), builder); //recurse
                }
            }




            if (dt != XTERNAL && subject.dt() != XTERNAL && predicate.dt() != XTERNAL) {

//                if (dt==DTERNAL && subject.dt()!=DTERNAL) {
//                    //parallelize the impl if the subject is a sequence
//                    dt = 0;
//                }
                //TODO simple case when no CONJ or IMPL are present

                if (Term.commonStructure(subject, predicate)) {
//                ArrayHashSet<LongObjectPair<Term>> se = new ArrayHashSet<>(4);
//                subject.eventsWhile((w, t) -> {
//                    se.add(PrimitiveTuples.pair(w, t));
//                    return true;
//                }, 0, true, true, false, 0);
                    Conj se = new Conj();

                    boolean subjNeg = subject.op()==NEG;

                    se.add(subject.dt() != DTERNAL ? 0 : (dt != DTERNAL ? 0 : ETERNAL), subject.negIf(subjNeg));

                    final boolean[] subjChange = {false};

                    long po;
                    if (dt == DTERNAL) {
                        if (predicate.dt()==DTERNAL) {
                            po = ETERNAL;
                        } else
                            po = 0;
                    } else {
                        po = subject.dtRange() + dt;
                    }

                    //TODO extract this to a ConjConflict class
                    ConjEliminator pe = new ConjEliminator(se,
                            po,
                            predicate, subjNeg);
                    Term newPred = pe.term();

                    boolean predChange = !predicate.equals(newPred);
                    if (predChange) {

                        if (newPred instanceof Bool) {
                            return newPred;
                        }

                        if (dt != DTERNAL) {
                            Term f = Conj.firstEventTerm(newPred);
                            int shift = predicate.subTimeFirst(f);
                            if (shift == DTERNAL)
                                return Null; //??
                            dt += shift;
                        }

                        predicate = newPred;
                    }
                    if (subjChange[0]) {
                        Term newSubj = se.term().negIf(subjNeg);
                        if (newSubj instanceof Bool) {
                            if (newSubj == True)
                                return predicate;
                        }
                        if (dt != DTERNAL) {
                            //TODO instead of dtRange, it should be calculated according to the time of the last event that matches the last event of the new subject
                            //otherwise it is inaccurate for repeating terms like (x &&+1 x) where it will by default stretch the wrong direction
                            int dr = newSubj.dtRange() - subject.dtRange();
                            dt += dr;
                        }
                        subject = newSubj;
                    }



                    if (subjChange[0] || predChange) {
                        return statement(IMPL, dt, subject, predicate, builder); //recurse
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

        return builder.compound(op, dt, subject, predicate);
    }

    private static class ConjEliminator extends Conj {
        private final Conj se;
        private final Term result;
        private final boolean invert;
        private final long[] seEvents;


        public ConjEliminator(Conj subtractFrom, long offset, Term subtractWith, boolean invert) {
            super(subtractFrom.termToId, subtractFrom.idToTerm);
            this.se = subtractFrom;
            this.seEvents = se.event.keySet().toArray();
            this.invert = invert;
            add(offset, subtractWith);
            this.result = term();
        }

        @Override
        protected int addFilter(long at, Term x, byte id) {
            if (at == ETERNAL) {
               for (long see : seEvents) {
                   int f = test(see, id);
                   if (f == -1) return -1;
                   if (f == +1) return +1; //ignore this term (dont repeat in the predicate)
               }
            } else {
                int f = test(at, id);
                int f2 = (at == ETERNAL || f == -1) ? f : test(ETERNAL, id);
                if (f == -1 || f2 == -1) return -1;
                if (f == +1 || f2 == +1) return +1; //ignore this term (dont repeat in the predicate)
            }
            return 0;
        }

        private int test(long at, byte id) {
            return se.conflictOrSame(at, (byte) (id * (invert ? -1 : +1)));
        }
    }
}
