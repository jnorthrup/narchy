package nars.op.mental;


import jcog.TODO;
import jcog.bloom.StableBloomFilter;
import jcog.data.atomic.AtomicFloat;
import jcog.math.MutableIntRange;
import jcog.pri.PLink;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PriArrayBag;
import jcog.pri.op.PriMerge;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.control.channel.CauseChannel;
import nars.exe.Causable;
import nars.link.Activate;
import nars.subterm.Subterms;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

/**
 * compound<->dynamic atom abbreviation.
 *
 * @param S serial target type
 */
public class Abbreviation/*<S extends Term>*/ extends Causable {


    /**
     * whether to use a (strong, proxying) alias atom concept
     */


    private static final Logger logger = LoggerFactory.getLogger(Abbreviation.class);
    private static final AtomicInteger currentTermSerial = new AtomicInteger(0);
    /**
     * generated abbreviation belief's confidence
     */
    public final Number abbreviationConfidence;
    /**
     * accepted volume range, inclusive
     */
    public final MutableIntRange volume;

    private final StableBloomFilter<Term> stm;
    private final int stmSize;

    /**
     * pending codecs
     */
    private final Bag<Term, PLink<Term>> pending;
    private final String termPrefix;
    private final CauseChannel<ITask> input;


    public Abbreviation(String termPrefix, int volMin, int volMax, NAR nar) {
        super();

        pending = new PriArrayBag(PriMerge.plus, 32);

        stmSize = 512;

        stm = Terms.newTermBloomFilter(nar.random(), stmSize);


//        TaskLeak bag = new TaskLeak(/*capacity, */nar) {
//
//
//            @Override
//            public float value() {
//                return 1f;
//            }
//
//            @Override
//            protected boolean filter(Task next) {
//                /*|| vol > volume.hi()*/
//                return next.volume() >= volume.lo();
//            }
//
//            @Override
//            protected float leak(Task t) {
//
//                leak(t.target(), t.priElseZero());
//
//                //TODO control rate in/out
//                abbreviateNext();
//
//                return 1;
//            }
//
//        };


        this.input = nar.newChannel(this);

        this.termPrefix = termPrefix;
        this.abbreviationConfidence =
                new AtomicFloat(nar.confDefault(BELIEF));


        volume = new MutableIntRange(volMin, volMax);

        nar.on(this);
    }

    private void abbreviateNext() {
        pending.pop(null, 1, this::abbreviateNext);
    }

    private void abbreviateNext(PLink<Term> p) {
        abbreviateNext(p.get(), p.priElseZero());
    }

    private void abbreviateNext(Term t, float pri) {

        stm.forget(1f, nar.random());
        stm.add(t);



        Concept abbreviable = nar.concept(t, true);
        if (abbreviable != null &&
                !(abbreviable instanceof PermanentConcept) &&
                !(abbreviable instanceof AliasConcept) &&
                abbreviable.term().equals(t)) /* identical to its conceptualize */ {

            if (!abbreviable.isDeleted() && abbreviable.meta(Abbreviation.class.getName()) != null) {
                return; //already abbreviated
            }
            Term abbreviation;
            if ((abbreviation = abbreviate(pri, abbreviable, super.nar)) != null) {
                abbreviable.meta(Abbreviation.class.getName(), abbreviation);
            }

        }

    }

    private void tryEncode(Term t, float pri) {
        if (!(t instanceof Compound))
            return;

        int tv = t.volume();
        if (tv < Abbreviation.this.volume.lo())
            return;

        //recurse
        Subterms ss = t.subterms();
        int ssn = ss.subs();
        for (Term s : ss)
            tryEncode(s, pri / ssn);

        if (tv > Abbreviation.this.volume.hi())
            return;

        if (stm.contains(t))
            return;

        Term tc = t.concept();
        if (!tc.equals(t))
            return;

        t = tc;

        int cap = pending.capacity();
        pending.putAsync(new PLink<>(t, pri * (float) Math.log(tv) / (cap * cap)));


    }


    protected String nextSerialTerm() {

        return termPrefix + Integer.toString(currentTermSerial.incrementAndGet(), 36);


    }


    protected Term abbreviate(float pri, Concept abbrConcept, NAR nar) {

        Term abbreviated = abbrConcept.term();

        if (abbrConcept != null && !(abbrConcept instanceof AliasConcept) && !(abbrConcept instanceof PermanentConcept)) {


            Term aliasTerm = Atomic.the(nextSerialTerm());
            AliasConcept a1 = new AliasConcept(aliasTerm, abbrConcept);
            a1.meta(Abbreviation.class.getName(), a1);
            //nar.on(a1);

            nar.concepts.set(abbreviated, a1); //redirect reference from the original concept to the alias
            nar.concepts.set(a1.term(), a1);



            Term abbreviation = newRelation(abbreviated, aliasTerm);
            if (abbreviation == null)
                return null;

            Task abbreviationTask = Task.tryTask(abbreviation, BELIEF,
                    $.t(1f, abbreviationConfidence.floatValue()),
                    (te, tr) -> {

                        NALTask ta = new NALTask(
                                te, BELIEF, tr,
                                nar.time(), ETERNAL, ETERNAL,
                                nar.evidence()
                        );


                        ta.log("Abbreviate");
                        ta.pri(pri);

                        return ta;
                    });


            if (abbreviationTask != null) {

                input.input(abbreviationTask);

                onAbbreviated(abbreviationTask.term());


                return aliasTerm;
            }

        }

        return null;
    }

    protected void onAbbreviated(Term term) {
        logger.info("{}", term);
    }


    @Nullable
    protected Term newRelation(Term abbreviated, Term id) {
        return $.sim(abbreviated, id);
    }


    @Override
    protected void next(NAR n, BooleanSupplier kontinue) {
        do {

            Activate a = null;
            if (a == null)
                throw new TODO();
//            Activate a = n.concepts.sampleConcept(n.random());
//            if (a == null)
//                break;

            Term at = a.term();
            if (at instanceof Compound) {
                tryEncode(at, a.priElseZero());
            }

            abbreviateNext();


        } while (kontinue.getAsBoolean());
    }

    @Override
    public float value() {
        return input.value();
    }
}
