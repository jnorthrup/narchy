package nars.term.util;

import nars.NAL;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.builder.TermBuilder;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjDiff;
import nars.term.util.conj.ConjSeq;
import nars.time.Tense;

import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * statements include: inheritance -->, similarity <->, and implication ==>
 */
public class Statement {


    public static Term statement(TermBuilder B, Op op, int dt, Term subject, Term predicate) {
        if (subject == Null || predicate == Null)
            return Null;

        boolean dtConcurrent = dt != XTERNAL && Conj.concurrent(dt);

        if (dtConcurrent) {
            if (subject.equals(predicate))
                return True;
            else if (subject.equalsNeg(predicate))
                return False;
            else if (subject instanceof Bool || predicate instanceof Bool)
                return Null;

            if (op == INH || op == SIM) {
                  if (subject.unneg().equalsRoot(predicate.unneg()))
                    return Null; //dont support non-temporal statements where the root is equal because they cant be conceptualized
            }
        }


        if (op == IMPL) {

            if (subject == True)
                return predicate;
            if (subject == False)
                return False; //return Null;
            if (!NAL.IMPLICATION_SUBJECT_CAN_CONTAIN_IMPLICATION && subject.hasAny(IMPL)) {
                return Null; //throw new TODO();
            }
            if (!subject.op().eventable)
                return Null;
        }

        boolean negate = false;

        if (op == IMPL) {
            if (predicate.op() == NEG) {
                predicate = predicate.unneg();
                negate = !negate;
            }

            switch (predicate.op()) {
                case BOOL:
                    //reduce to the subject as a general condition for the superclass to utilize
                    if (predicate == True)
                        return subject;
                    else if (predicate == False)
                        return subject.neg();
                    else
                        return Null;

                case IMPL: {
                    Term newSubj, inner = predicate.sub(0);
                    if (dt == DTERNAL || dt == XTERNAL) {
                        newSubj = CONJ.the(B, dt, subject, inner);
                    } else {
                        newSubj = ConjSeq.sequence(subject, 0, inner, subject.eventRange() + dt, B);
                    }
                    return statement(B, IMPL, predicate.dt(), newSubj, predicate.sub(1)).negIf(negate); //recurse
                }

            }

            if (!predicate.op().eventable)
                return Null;


            //TODO simple case when no CONJ or IMPL are present

            if (dt != XTERNAL) {
                int subjDT = subject.dt();
                if (subjDT != XTERNAL && predicate.dt() != XTERNAL) { // && !subject.OR(x->x instanceof Ellipsis) && !predicate.OR(x->x instanceof Ellipsis) ) {

                    if (!(subject instanceof Compound) && !(predicate instanceof Compound)) {
                        //no validity test necessary
                    } else if (!Term.commonStructure(subject, predicate)) {
                        //no validity test necessary
                    } else if (subject instanceof Compound && !(predicate instanceof Compound) && !subject.containsRecursively(predicate)) {
                        //no validity test necessary
                    } else if (predicate instanceof Compound && !(subject instanceof Compound) && !predicate.containsRecursively(subject)) {
                        //no validity test necessary
                    } else {

                        long so, po; //subject and predicate occurrences

                        so = subjDT != DTERNAL ? 0 : (dt != DTERNAL ? 0 : ETERNAL);
                        po = (subjDT != DTERNAL || predicate.dt() != DTERNAL) ?
                                (dt != DTERNAL ? dt : 0)
                                :
                                (dt != DTERNAL ? dt : ETERNAL);

                        int subjRange = subject.eventRange();
                        if (po != ETERNAL) {
                            po += subjRange;
                            if (so == ETERNAL)
                                so = 0;
                        }


//                    //test for validity by creating the hypothetical conjunction analog of the implication
//                    Conj x = new Conj();
//                    if (!x.addAt(so, subject))
//                        throw new WTF();
//                    if (!x.addAt(po, predicate))
//                        return False;
//                    Term cx = x.target();
//                    if (cx instanceof Bool)
//                        return cx;

                        //subtract any common subject components from predicate
                        boolean subjNeg = subject.op() == NEG;
                        ConjBuilder newPredConj = ConjDiff.the(predicate, po, subject.negIf(subjNeg), so, subjNeg);
                        Term newPred = newPredConj.term(B);


                        boolean predChange = !predicate.equals(newPred);

                        if (predChange) {

                            if (newPred instanceof Bool)
                                return newPred; //collapse


                            if (dt != DTERNAL) {
                                long shift = newPredConj.shift();
                                if (shift == ETERNAL) {
                                    //??
                                    dt = DTERNAL;
                                } else {
//

                                    dt = Tense.occToDT(shift - subjRange);

                                    if (newPred.dt() == 0 && predicate.dt() == DTERNAL && predicate.subterms().equals(newPred.subterms())) {
                                        //HACK return to dternal
                                        if (newPred instanceof Compound)
                                            newPred = ((Compound) newPred).dt(DTERNAL, B);

                                    }
                                }

                            }

                            if (!newPred.equals(predicate)) { //HACK check again
//                            try {
                                return statement(B, IMPL, dt, subject, newPred).negIf(negate); //recurse
//                            } catch (StackOverflowError e) {
//                                System.out.println("stack overflow: ==> " + subject + ' ' + dt + ' ' + newPred + '<' + predicate);
//                                throw new WTF("stack overflow: ==> " + subject + ' ' + dt + ' ' + newPred + '<' + predicate);
//                            }
                            }

                        }

                    }
                }
            }
        }


        if ((op != IMPL
                || dt == DTERNAL /* allow parallel IMPL unless there is a sequence that could separate the events from overlap */)
                || (dt == 0 && !Conj.isSeq(subject) && !Conj.isSeq(predicate))
        ) {

            Predicate<Term> delim = (op == IMPL) ?
                    recursiveCommonalityDelimeterStrong : Op.recursiveCommonalityDelimeterWeak;

            if ((containEachOther(subject, predicate, delim)))
                return Null;
        }

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
//            }

//            if (op == INH /*|| op == SIM*/) {
//                if (NAL.term.INH_CLOSED_BOOLEAN_DUALITY_MOBIUS_PARADIGM) {
//                    //EXPERIMENTAL support for negated inheritance subterms
//                    boolean sn = subject.op() == NEG;
//                    boolean pn = predicate.op() == NEG;
//                    if (!sn && !pn) {
//                        //normal
//                    } else if (sn && pn) {
//                        //double-negative
//
//                        //return Null; // (--x --> --y) => (x --> y)??
//
//                        subject = subject.unneg();
//                        predicate = predicate.unneg();
//
//                    } else if (sn) {
//                        negate = true;
//                        subject = subject.unneg();
//                    } else /* pn */ {
//                        negate = true;
//                        predicate = predicate.unneg();
//                    }
//                }
//            }

        if (op == SIM) {
//            if (subject instanceof Bool || predicate instanceof Bool) {
//
//            }
            if (subject.compareTo(predicate) > 0) {
                //swap order
                Term x = predicate;
                predicate = subject;
                subject = x;
            }
        }

        Term t = B.theCompound(op, dt, subject, predicate);

        //if (Param.DEBUG) {
        //test image normalization
        if (op == INH) {
            //TODO accept TermBuilder b as parameter
            Term tt = Image.imageNormalize(t);
            if (tt instanceof Bool) {
                t = tt;
            }
        }
        //}


        return t.negIf(negate);
    }

}