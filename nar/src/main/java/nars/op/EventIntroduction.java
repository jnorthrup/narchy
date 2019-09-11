package nars.op;

import nars.NAR;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.compound.Sequence;
import nars.term.util.conj.Conj;
import org.eclipse.collections.api.block.function.primitive.ObjectIntToObjectFunction;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static nars.Op.CONJ;
import static nars.Op.IMPL;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/** introduction applied to subevents and subconditions */
public abstract class EventIntroduction extends Introduction {
    EventIntroduction(NAR nar, int capacity) {
        super(nar, capacity);
    }

    EventIntroduction(NAR nar) {
        super(nar);
    }

    @Override
    protected boolean filter(Term next) {
        return /*next.isAny(CONJ.bit | IMPL.bit  ) && Tense.dtSpecial(next.dt()) &&*/
                //next.count(x -> x instanceof Compound) > 1;
                next instanceof Compound && next.hasAny(CONJ);
                    //&& next.hasAny(Op.Temporal);
    }

    @Override
    protected final @Nullable Term newTerm(Term xx) {
        Term y = applyAndNormalize(xx, volMax-2);
        return y != xx ? y : null;
    }

    Term applyAndNormalize(Term x) {
        return applyAndNormalize(x, Integer.MAX_VALUE);
    }

    private Term applyAndNormalize(Term x, int volMax) {
        Term y = apply(x, this::apply, volMax);
        return y!=null && !x.equals(y) && y.volume() <= volMax ? y.normalize() : x;
    }

    abstract protected Term apply(Term x, int volMax);

    /** dont separate in conj and impl components if variables because renormalization will cause them to become distinct HACK */
    public static Term apply(Term x, ObjectIntToObjectFunction<Term,Term> each, int volMax) {
        if (volMax <= 0 || x instanceof Sequence)
            return x; //HACK incompatible with sequences for now

        Op xo = x.op();
        switch (xo) {
            case NEG:
                Term xu = x.unneg();
                Term y = each.valueOf(xu, volMax - 1);
                return y != null && y != xu ? y.neg() : x;
            case IMPL:
                return x; //HACK dont support IMPL since they can conflict when &&'d with the factor

//                return x.hasAny(Op.Variable) ? x :
//                    impl(x, volMax, x.sub(0), x.sub(1), each);
            case CONJ:
                if (Conj.isSeq(x)) {
                    if ((x.subStructure() & CONJ.bit) != 0) {
                        //search for any embedded parallel conj
                        Map<Term, Term> replacement = new UnifiedMap(1);
                        if (!x.eventsAND((when, what) -> {
                            if (what instanceof Compound && what.op() == CONJ && !Conj.isSeq(what) && what.dt()!=XTERNAL && !replacement.containsKey(what)) {
                                Term what2 = each.valueOf(what, volMax - (x.volume() - what.volume()) - 1);
                                if (what2 != null)
                                    replacement.put(what, what2);
                            }
                            return true;
                        }, 0, false, true /* xternal disabled */))
                            return x; //fail
                        if (!replacement.isEmpty()) {
                            Term xx = x.replace(replacement);
                            if (!(xx instanceof Bool))
                                return xx;
                        }
                    }

                    return x; //basic sequence, or nothing

                } else {
                    //ok continue below
                }
//
//                if (x.dt() == XTERNAL || x.hasAny(Op.Variable))
//                    return x; //unchanged
//
//                if (!Tense.dtSpecial(x.dt()))
//                    return conjSeq(x, volMax, x.sub(0), x.sub(1), each);
//                else
//                    return each.valueOf(x, volMax - 1);
                break;
        }
        return each.valueOf(x, volMax);
    }

//    public static Term conjSeq(Term x, int volMax, Term subj, Term pred, ObjectIntToObjectFunction<Term, Term> each) {
//        int dt = x.dt();
//        if (dt == DTERNAL) dt = 0; //HACK
//        Term subjFactored = apply(subj, each,volMax - pred.volume() - 1);
//        if (subjFactored == null) subjFactored = subj;
//        Term predFactored = apply(pred, each,volMax - subj.volume() - 1);
//        if (predFactored == null) predFactored = pred;
//        if ((subjFactored!=subj) || (predFactored!=pred))
//            //return ConjSeq.sequence(subjFactored, dt + (subjFactored.eventRange() - subj.eventRange()), predFactored);
//            return CONJ.the(subjFactored, dt, predFactored);
//        else
//            return x; //unchanged
//    }
    public static Term impl(Term x, int volMax, Term subj, Term pred, ObjectIntToObjectFunction<Term, Term> each) {
        int dt = x.dt();
        if (dt == DTERNAL) dt = 0; //HACK

        boolean phase = ThreadLocalRandom.current().nextBoolean();
        Term subjFactored = null, predFactored = null;
        for (int i = 0; i < 2; i++) {
            if ((i == 0 && phase) || (i==1 && !phase)) {
                subjFactored = apply(subj, each, volMax - pred.volume() - 1);
                if (subjFactored == null) subjFactored = subj;
            }

            if ((i == 1 && phase) || (i==0 && !phase)) {
                predFactored = apply(pred, each, volMax - subj.volume() - 1);
                if (predFactored == null) predFactored = pred;
            }
        }

        if ((!subj.equals(subjFactored)) || (!pred.equals(predFactored)))
            return IMPL.the(subjFactored, dt + (subjFactored.eventRange() - subj.eventRange()), predFactored);
        else
            return x; //unchanged
    }


}
