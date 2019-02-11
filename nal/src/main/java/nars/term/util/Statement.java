package nars.term.util;

import jcog.WTF;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
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
                        newSubj = CONJ.the(dt, subject, inner);
                    } else {
                        if (dt == XTERNAL || dt == DTERNAL) {
                            newSubj = CONJ.the(dt, subject, inner);
                        } else {
                            newSubj = ConjSeq.sequence(subject, 0, inner, subject.eventRange() + dt);
                        }
                    }
                    return statement(IMPL, predicate.dt(), newSubj, predicate.sub(1)); //recurse
                }
            }

            if (!subject.op().eventable || !predicate.op().eventable)
                return Null;

            int subjDT = subject.dt();

            //TODO simple case when no CONJ or IMPL are present

            if (dt != XTERNAL && subjDT != XTERNAL && predicate.dt() != XTERNAL) { // && !subject.OR(x->x instanceof Ellipsis) && !predicate.OR(x->x instanceof Ellipsis) ) {

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
                    Term newPred = newPredConj.term();


                    boolean predChange = !predicate.equals(newPred);

                    if (predChange) {

                        if (newPred instanceof Bool) {
                            return newPred;
                        }


                        if (dt != DTERNAL) {
                            int shift = Tense.occToDT(newPredConj.shift());
//                            if (shift == DTERNAL || shift == XTERNAL) {
//                                if (Param.DEBUG)
//                                    throw new TODO();
//                                else
//                                    return Null; //??
//                            }

                            dt = shift - subjRange;

                            if (newPred.dt() == 0 && predicate.dt() == DTERNAL && predicate.subterms().equals(newPred.subterms())) {
                                //HACK return to dternal
                                newPred = newPred.dt(DTERNAL);
                                if (newPred instanceof Bool) {
                                    return newPred;
                                }
                            }
                        }

                        if (!newPred.equals(predicate)) { //HACK check again
                            try {
                                return statement(IMPL, dt, subject, newPred); //recurse
                            } catch (StackOverflowError e) {
                                System.out.println("stack overflow: ==> " + subject + ' ' + dt + ' ' + newPred + '<' + predicate);
                                throw new WTF("stack overflow: ==> " + subject + ' ' + dt + ' ' + newPred + '<' + predicate);
                            }
                        }

//                        predicate = newPred;
//                        if (predicate.op() == NEG)
//                            return statement(IMPL, dt, subject, predicate.unneg()).neg();//recurse

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

        if ((op != IMPL || dt == DTERNAL /* allow parallel IMPL because sequences may separate the events from overlap */) /*&& !subject.hasAny(Op.VAR_PATTERN) && !predicate.hasAny(Op.VAR_PATTERN)*/) {

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
        Term t = Op.terms.theCompound(op, dt, subject, predicate);

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

}
