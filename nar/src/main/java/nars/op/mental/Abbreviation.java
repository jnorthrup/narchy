package nars.op.mental;


import jcog.bloom.StableBloomFilter;
import jcog.data.atomic.AtomicFloat;
import jcog.io.lz.QuickLZ;
import jcog.math.MutableIntRange;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.control.How;
import nars.control.channel.CauseChannel;
import nars.derive.model.Derivation;
import nars.io.IO;
import nars.link.TaskLink;
import nars.subterm.Subterms;
import nars.task.NALTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.transform.AbstractTermTransform;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Base64;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

/**
 * compound<->dynamic atom abbreviation.
 *
 * @param S serial target type
 */
public class Abbreviation/*<S extends Term>*/ extends How {


    /**
     * whether to use a (strong, proxying) alias atom concept
     */


    private static final Logger logger = LoggerFactory.getLogger(Abbreviation.class);
//    private static final AtomicInteger currentTermSerial = new AtomicInteger(0);

    //public static final byte ABBREVIATION_PREFIX = '=';
    public static final byte ABBREVIATION_PREFIX = '=';
    public static final byte[] ABBREVIATION_PREFIX_QUOTED = new byte[] { '"', ABBREVIATION_PREFIX };

    public static Atom abbreviateTerm(Compound x) {
        byte[] xx = IO.termToBytes(x);
        xx = QuickLZ.compress(xx, 1);
        //Deflater d = new Deflater(); d.read.deflate(xx);d.

        return Atomic.atom("=" +
                Base64.getEncoder().encodeToString(xx) //HACK use higher radix
                //Base122.encode(xx)
        );

//        //HACK
//        byte[] xxx = new byte[xx.length+1];
//        xxx[0] = ABBREVIATION_PREFIX;
//        System.arraycopy(xx, 0, xxx, 1, xx.length);
//
//        return new Atom(bytes(ATOM.id, xxx));

        //return Atomic.atom(String.valueOf((char)ABBREVIATION_PREFIX) + x); //TODO use compressed byte[] serialization
    }

    public static Term unabbreviateTerm(Term x) {
        if (x instanceof Atom) {
            byte[] b = ((Atom) x).bytes();
            if (((Atom)x).startsWith(ABBREVIATION_PREFIX_QUOTED)) {
                b = Arrays.copyOfRange(b, 4+1, b.length-1); //HACK

                b = QuickLZ.decompress(
                        Base64.getDecoder().decode(b)
                        //Base122.decode(new String(b))
                );

                return IO.bytesToTerm(b);
            }
        }

        return x;
    }

    /**
     * generated abbreviation belief's confidence
     */
    private final Number abbreviationConfidence;
    /**
     * accepted volume range, inclusive
     */
    public final MutableIntRange volume;

    private final StableBloomFilter<Term> stm;
    private final int stmSize;


    private final CauseChannel<Task> in;


    public Abbreviation(String termPrefix, int volMin, int volMax, NAR nar) {
        super();

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


        this.in = nar.newChannel(this);

        this.abbreviationConfidence =
                new AtomicFloat(nar.confDefault(BELIEF));


        volume = new MutableIntRange(volMin, volMax);

        nar.start(this);
    }

    /** whether to attempt unabbreviation of a premise  */
    public static Task unabbreviate(Task task, Derivation d) {

        float v = task.volume();
        if (d.random.nextFloat() >= v / (d.termVolMax/2f))
            return Abbreviation.unabbreviate(task, d.nar);
        else
            return task; //dont unabbreviate
    }


    final static class ABBREVIATE extends AbstractTermTransform.NegObliviousTermTransform {

        ABBREVIATE() {
        }

        @Override
        public Term applyCompound(Compound x, Op newOp, int newDT) {
            Term a = super.applyCompound(x, newOp, newDT);

            if (a instanceof Compound && !(a.hasAny(Op.Variable))) {
                return abbreviateTerm((Compound)a);
//                Term ac = a.concept();
//                if (a.equals(ac)) { //avoid temporals
//                    Concept aa = resolver.apply(ac);
//                    if (aa != null) {
//                        Termed aaa = aa.meta(ABBREVIATION_META);
//                        if (aaa != null)
//                            return aa.term();
//                            //return apply(aaa.term()); //abbreviation
//                    }
//                }
            }
            return a;
        }

    }
    final static class UNABBREVIATE extends AbstractTermTransform.NegObliviousTermTransform {

        final Function<Term /* Atomic */,Concept> resolver;

        UNABBREVIATE(Function<Term,Concept> resolver) {
            this.resolver = resolver;
        }

        @Override
        public Term applyAtomic(Atomic a) {

            if (a instanceof Atom) {
                return unabbreviateTerm(a);
//                Concept aa = resolver.apply(a);
//                if (aa != null) {
//                    Termed aaa = aa.meta(ABBREVIATION_META);
//                    if (aaa != null)
//                        return aaa.term(); //unabbreviation
//                }
            }
            return a;
        }

    }
    public static Term abbreviate(Term x, NAR n) {
        return abbreviate(x);
    }
    public static Task unabbreviate(Task x, NAR n) {
        return Task.clone(x, unabbreviate(x.term(), n));
    }
    public static Term unabbreviate(Term x, NAR n) {
        return unabbreviate(x, n::concept);
    }

    public static Term abbreviate(Term x) {
        return x.transform(new ABBREVIATE());
    }

    public static Term unabbreviate(Term x, Function<Term,Concept> resolver) {
        return x.transform(new UNABBREVIATE(resolver));
    }
//
//    private void abbreviateNext(What w) {
//        pending.pop(null, 1, t -> abbreviateNext(t, w));
//    }

    private void abbreviateNext(Term t, float pri, What w) {

        stm.forget(1f, nar.random());
        stm.add(t);



        Concept abbreviable = nar.concept(t, true);
        if (abbreviable != null && !abbreviable.isDeleted() &&
                !(abbreviable instanceof PermanentConcept) &&
                !(abbreviable instanceof AliasConcept) &&
                abbreviable.term().equals(t)) /* identical to its conceptualize */ {

//            Object a = abbreviable.meta(ABBREVIATION_META);
//            if (a != null) {
//                //already abbreviated
//                //TODO - add a forwarding similarity from old term to new term
//                // Concept c = nar.concept((Term)a);
//                return;
//            }

            Term abbreviation;
            if ((abbreviation = abbreviate(pri, abbreviable, w)) != null) {
//                abbreviable.meta(ABBREVIATION_META, abbreviation);
            }

        }

    }

    private boolean tryEncode(Term x, float pri, What w) {

        if (stm.contains(x))
            return false;

        if (!(x instanceof Compound))
            return false;

        if (x.hasAny(Op.Variable) || !x.equals(x.root()))
            return false;

        int tv = x.volume();
        if (tv < Abbreviation.this.volume.lo())
            return false;

        //recurse
        Subterms ss = x.subterms();
        int ssn = ss.subs();
        for (Term s : ss) {
            if (tryEncode(s, pri, w))
                return true;
        }

        if (tv > Abbreviation.this.volume.hi())
            return false;


        Term y = unabbreviate(x, w.nar);
        if (!y.equals(x)) {
            return tryEncode(y, pri, w);
        }

        Term tc = x.concept();
        if (!tc.equals(x))
            return false;

        x = tc;

        abbreviateNext(x, pri, w);
        return true;

    }



//    private String nextSerialTerm() {
//
//        return termPrefix + Integer.toString(currentTermSerial.incrementAndGet(), 36);
//
//
//    }


    private Term abbreviate(float pri, Concept abbrConcept, What w) {

        Compound abbreviated = (Compound) abbrConcept.term();

        if (!(abbrConcept instanceof AliasConcept) && !(abbrConcept instanceof PermanentConcept)) {


            Term aliasTerm = Abbreviation.abbreviateTerm(abbreviated); //Atomic.the(nextSerialTerm());
            AliasConcept a1 = new AliasConcept(aliasTerm, abbrConcept);
//            a1.meta(ABBREVIATION_META, abbreviated);
            //nar.on(a1);

            nar.memory.set(abbreviated, a1); //redirect reference from the original concept to the alias
            nar.memory.set(a1.term(), a1);



            Term abbreviation = newRelation(abbreviated, aliasTerm);
            if (abbreviation == null)
                return null;

            Task abbreviationTask = Task.tryTask(abbreviation, BELIEF,
                    $.t(1f, abbreviationConfidence.floatValue()),
                    (te, tr) -> {

                        NALTask ta = NALTask.the(te, BELIEF, tr, nar.time(), ETERNAL, ETERNAL, nar.evidence());
                        ta.log("Abbreviate");
                        ta.pri(nar.priDefault(BELIEF));

                        return ta;
                    });


            if (abbreviationTask != null) {

                in.accept(abbreviationTask, w);

                onAbbreviated(abbreviated, aliasTerm);


                return aliasTerm;
            }

        }

        return null;
    }

    protected void onAbbreviated(Term abbreviated, Term alias) {
        logger.info("{} => {}", alias, abbreviated);
    }


    @Nullable
    private Term newRelation(Term abbreviated, Term id) {
        return $.sim(abbreviated, id);
    }


    @Override
    public void next(What w, BooleanSupplier kontinue) {
        do {

            TaskLink a = w.sample();
            if (a == null)
                break;

            Term at = a.from();
            if (at instanceof Compound)
                tryEncode(at, a.priElseZero(), w);

        } while (kontinue.getAsBoolean());
    }

    @Override
    public float value() {
        return in.value();
    }
}
