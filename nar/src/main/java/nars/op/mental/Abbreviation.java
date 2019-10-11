package nars.op.mental;


import nars.Op;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.action.TaskTransformAction;
import nars.io.IO;
import nars.task.proxy.SpecialTermTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.AtomBytes;
import nars.term.atom.Atomic;
import nars.term.util.Image;
import nars.term.util.transform.RecursiveTermTransform;
import nars.unify.constraint.TermMatcher;
import org.jetbrains.annotations.Nullable;

/**
 * compound<->dynamic atom abbreviation.
 *
 * @param S serial target type
 */
public enum Abbreviation { ;

    //private static final Logger logger = LoggerFactory.getLogger(Abbreviation.class);
//    private static final AtomicInteger currentTermSerial = new AtomicInteger(0);

    //public static final byte ABBREVIATION_PREFIX = '=';
//    public static final byte ABBREVIATION_PREFIX = '=';
//    public static final byte[] ABBREVIATION_PREFIX_QUOTED = new byte[] { '"', ABBREVIATION_PREFIX };



    @Nullable public static Atomic abbreviateTerm(Compound x) {

        if (x.hasVars())
            return null; //refuse, can cause problems

        return AtomBytes.atomBytes((Compound)Image.imageNormalize(x));

//        xx = QuickLZ.compress(xx, 1);
        //Deflater d = new Deflater(); d.read.deflate(xx);d.

//        return $.quote(Character.toString(ABBREVIATION_PREFIX) +
//                Base64.getEncoder().encodeToString(xx) //HACK use higher radix
//                //Base122.encode(xx)
//        );

//        //HACK
//        byte[] xxx = new byte[xx.length+1];
//        xxx[0] = ABBREVIATION_PREFIX;
//        System.arraycopy(xx, 0, xxx, 1, xx.length);
//
//        return new Atom(bytes(ATOM.id, xxx));

        //return Atomic.atom(String.valueOf((char)ABBREVIATION_PREFIX) + x); //TODO use compressed byte[] serialization
    }

    @Nullable public static Term unabbreviateTerm(Term x) {
        if (x instanceof AtomBytes) {
            return IO.bytesToTerm( ((AtomBytes)x).bytes(), 3 /* op and 2 length bytes */ );
        }
        return x;
//        if (x instanceof Atom) {
//            Atom a = (Atom) x;
//            byte[] aa = a.bytes();
//            if (a.startsWith(ABBREVIATION_PREFIX_QUOTED)) {
//
//                byte[] b = Arrays.copyOfRange(aa, 4+1, aa.length-1); //HACK
//
//                byte[] c = //Base122.decode(b);
//                           Base64.getDecoder().decode(b);
//
//                byte[] d = QuickLZ.decompress(c);
//
//                Term y = IO.bytesToTerm(d);
//                return y instanceof Bool ?
//                    null :
//                    y;
//            }
//        }
//        return null;
    }

    public abstract static class AbstractAbbreviate extends TaskTransformAction {
        final int volMin;

        public AbstractAbbreviate(int volMin, int volMax) {
            this.volMin = volMin;

            single();

            match(TheTask, new TermMatcher.VolMin(volMin));

            if (volMax < Integer.MAX_VALUE)
                match(TheTask, new TermMatcher.VolMax(volMax));
        }
        @Override
        protected @Nullable Task transform(Task x, Derivation d) {
            Term xx = x.term();
            Term yy = abbreviate((Compound)xx);
            return yy!=null && yy.opID()==xx.opID() ? SpecialTermTask.the(x, yy, true) : null;
        }

        protected abstract Term abbreviate(Compound x);

    }

    public static class AbbreviateRoot extends AbstractAbbreviate {

        public AbbreviateRoot(/*String prefix,*/ int volMin, int volMax) {
            super(volMin, volMax);
        }

        @Override
        @Nullable protected Term abbreviate(Compound x) {
            return abbreviateTerm(x);
        }
    }
    public static class AbbreviateRecursive extends AbstractAbbreviate  {

        final RecursiveTermTransform.NegObliviousTermTransform transform =new RecursiveTermTransform.NegObliviousTermTransform() {
            @Override
            public Term applyPosCompound(Compound x) {
                int v = x.volume();
                if (v >= volMin && v <= subVolMax) {
                    Term y = abbreviateTerm(x); //terminal
                    if (y !=null)
                        return y;
                }

                return super.applyPosCompound(x);
            }
        };

        private final int subVolMax;

        public AbbreviateRecursive(/*String prefix,*/ int volMin, int volMax) {
            super(volMin, Integer.MAX_VALUE);
            this.subVolMax = volMax;
        }

        @Override
        protected Term abbreviate(Compound x) {
            Term y = transform.applyCompound(x);
            return !y.equals(x) ? y : null;
        }

    }

    /** unabbreviates abbreviated root terms (not recursively contained) */
    public static class UnabbreviateRoot extends TaskTransformAction {

        public UnabbreviateRoot(/*String prefix,*/) {
            single();
            match(TheTask, new TermMatcher.Is(Op.ATOM));
            //TODO match prefix and/or other features inside the ATOM
        }

        @Override
        protected @Nullable Task transform(Task x, Derivation d) {
            Term xx = x.term();
            Term yy = unabbreviateTerm(xx);
            if (yy!=null) {
                //TODO volMax test
                return SpecialTermTask.the(x, yy, true);
            } else
                return null;
        }
    }
    /** unabbreviates abbreviated root terms (not recursively contained) */
    public static class UnabbreviateRecursive extends TaskTransformAction {

        final static RecursiveTermTransform.NegObliviousTermTransform transform =new RecursiveTermTransform.NegObliviousTermTransform() {
            @Override
            public Term applyAtomic(Atomic a) {
                Term b = unabbreviateTerm(a);
                return b==null ? a : b;
            }
        };

        public UnabbreviateRecursive(/*String prefix,*/) {
            single();
            match(TheTask, new TermMatcher.Has(Op.ATOM));
            //TODO match prefix and/or other features inside the ATOM
            //TODO more specific conditions
        }

        @Override
        protected @Nullable Task transform(Task x, Derivation d) {
            Term xx = x.term();
            Term yy = transform.apply(xx).normalize();
            if (yy.op().taskable && !yy.equals(xx) && yy.volume() <= d.termVolMax) {
                return SpecialTermTask.the(x, yy, true);
            } else
                return null;
        }
    }
//
//    /**
//     * whether to use a (strong, proxying) alias atom concept
//     */
//
//
//
//    /**
//     * generated abbreviation belief's confidence
//     */
//    private final Number abbreviationConfidence;
//    /**
//     * accepted volume range, inclusive
//     */
//    public final MutableIntRange volume;
//
//    private final StableBloomFilter<Term> stm;
//    private final int stmSize;
//
//
//    private final CauseChannel<Task> in;
//
//
//    public Abbreviation(String termPrefix, int volMin, int volMax, NAR nar) {
//        super();
//
//        stmSize = 512;
//
//        stm = Terms.newTermBloomFilter(nar.random(), stmSize);
//
//
////        TaskLeak bag = new TaskLeak(/*capacity, */nar) {
////
////
////            @Override
////            public float value() {
////                return 1f;
////            }
////
////            @Override
////            protected boolean filter(Task next) {
////                /*|| vol > volume.hi()*/
////                return next.volume() >= volume.lo();
////            }
////
////            @Override
////            protected float leak(Task t) {
////
////                leak(t.target(), t.priElseZero());
////
////                //TODO control rate in/out
////                abbreviateNext();
////
////                return 1;
////            }
////
////        };
//
//
//        this.in = nar.newChannel(this);
//
//        this.abbreviationConfidence =
//                new AtomicFloat(nar.confDefault(BELIEF));
//
//
//        volume = new MutableIntRange(volMin, volMax);
//
//        nar.add(this);
//    }
//
//    /** whether to attempt unabbreviation of a premise  */
//    public static Task unabbreviate(Task task, Derivation d) {
//
//        float v = task.volume();
//        if (d.random.nextFloat() >= v / (d.termVolMax/2f))
//            return Abbreviation.unabbreviate(task, d.nar);
//        else
//            return task; //dont unabbreviate
//    }
//
//
//
//
//    final static class ABBREVIATE extends RecursiveTermTransform.NegObliviousTermTransform {
//
//        ABBREVIATE() {
//        }
//
//        @Override
//        public Term applyPosCompound(Compound x) {
//            Term a = super.applyPosCompound(x);
//            return a instanceof Compound && !a.hasAny(Op.Variable) ? abbreviateTerm((Compound) a) : a;
//        }
//
//
//    }
//    final static class UNABBREVIATE extends RecursiveTermTransform.NegObliviousTermTransform {
//
//        final Function<Term /* Atomic */,Concept> resolver;
//
//        UNABBREVIATE(Function<Term,Concept> resolver) {
//            this.resolver = resolver;
//        }
//
//        @Override
//        public Term applyAtomic(Atomic a) {
//
//            if (a instanceof Atom) {
//                return unabbreviateTerm(a);
////                Concept aa = resolver.apply(a);
////                if (aa != null) {
////                    Termed aaa = aa.meta(ABBREVIATION_META);
////                    if (aaa != null)
////                        return aaa.term(); //unabbreviation
////                }
//            }
//            return a;
//        }
//
//    }
//    public static Term abbreviate(Term x, NAR n) {
//        return abbreviate(x);
//    }
//    public static Task unabbreviate(Task x, NAR n) {
//        return Task.clone(x, unabbreviate(x.term(), n));
//    }
//    public static Term unabbreviate(Term x, NAR n) {
//        return unabbreviate(x, n::concept);
//    }
//
//    public static Term abbreviate(Term x) {
//        return x.transform(new ABBREVIATE());
//    }
//
//    public static Term unabbreviate(Term x, Function<Term,Concept> resolver) {
//        return x.transform(new UNABBREVIATE(resolver));
//    }
////
////    private void abbreviateNext(What w) {
////        pending.pop(null, 1, t -> abbreviateNext(t, w));
////    }
//
//    private void abbreviateNext(Term t, float pri, What w) {
//
//        NAR nar = w.nar;
//
//        stm.forget(1f, nar.random());
//        stm.add(t);
//
//
//
//        Concept abbreviable = nar.concept(t, true);
//        if (abbreviable != null && !abbreviable.isDeleted() &&
//                !(abbreviable instanceof PermanentConcept) &&
//                !(abbreviable instanceof AliasConcept) &&
//                abbreviable.term().equals(t)) /* identical to its conceptualize */ {
//
////            Object a = abbreviable.meta(ABBREVIATION_META);
////            if (a != null) {
////                //already abbreviated
////                //TODO - add a forwarding similarity from old term to new term
////                // Concept c = nar.concept((Term)a);
////                return;
////            }
//
//            Term abbreviation;
//            if ((abbreviation = abbreviate(pri, abbreviable, w)) != null) {
////                abbreviable.meta(ABBREVIATION_META, abbreviation);
//            }
//
//        }
//
//    }
//
//    private boolean tryEncode(Term x, float pri, What w) {
//
//        if (stm.contains(x))
//            return false;
//
//        if (!(x instanceof Compound))
//            return false;
//
//        if (x.hasAny(Op.Variable) || !x.equals(x.root()))
//            return false;
//
//        int tv = x.volume();
//        if (tv < Abbreviation.this.volume.lo())
//            return false;
//
//        //recurse
//        Subterms ss = x.subterms();
//        for (Term s : ss) {
//            if (tryEncode(s, pri, w))
//                return true;
//        }
//
//        if (tv > Abbreviation.this.volume.hi())
//            return false;
//
//
//        Term y = unabbreviate(x, w.nar);
//        if (!y.equals(x)) {
//            return tryEncode(y, pri, w);
//        }
//
//        Term tc = x.concept();
//        if (!tc.equals(x))
//            return false;
//
//        x = tc;
//
//        abbreviateNext(x, pri, w);
//        return true;
//
//    }
//
//
//
////    private String nextSerialTerm() {
////
////        return termPrefix + Integer.toString(currentTermSerial.incrementAndGet(), 36);
////
////
////    }
//
//
//    private Term abbreviate(float pri, Concept abbrConcept, What w) {
//
//        NAR nar = w.nar;
//
//        Compound abbreviated = (Compound) abbrConcept.term();
//
//        if (!(abbrConcept instanceof AliasConcept) && !(abbrConcept instanceof PermanentConcept)) {
//
//
//            Term aliasTerm = Abbreviation.abbreviateTerm(abbreviated); //Atomic.the(nextSerialTerm());
//            AliasConcept a1 = new AliasConcept(aliasTerm, abbrConcept);
////            a1.meta(ABBREVIATION_META, abbreviated);
//            //nar.on(a1);
//
//            nar.memory.set(abbreviated, a1); //redirect reference from the original concept to the alias
//            nar.memory.set(a1.term(), a1);
//
//
//
//            Term abbreviation = newRelation(abbreviated, aliasTerm);
//            if (abbreviation == null)
//                return null;
//
//            Task abbreviationTask = Task.tryTask(abbreviation, BELIEF,
//                    $.t(1f, abbreviationConfidence.floatValue()),
//                    (te, tr) -> {
//
//                        AbstractTask ta = NALTask.the(te, BELIEF, tr, nar.time(), ETERNAL, ETERNAL, nar.evidence());
//                        ta.pri(nar.priDefault(BELIEF));
//
//                        return ta;
//                    });
//
//
//            if (abbreviationTask != null) {
//
//                in.accept(abbreviationTask, w);
//
//                onAbbreviated(abbreviated, aliasTerm);
//
//
//                return aliasTerm;
//            }
//
//        }
//
//        return null;
//    }
//
//    protected void onAbbreviated(Term abbreviated, Term alias) {
//        logger.info("{} => {}", alias, abbreviated);
//    }
//
//
//    @Nullable
//    private Term newRelation(Term abbreviated, Term id) {
//        return $.sim(abbreviated, id);
//    }
//
//
////    @Override
////    public void next(What w, BooleanSupplier kontinue) {
////        do {
////
////            TaskLink a = w.sample();
////            if (a == null)
////                break;
////
////            Term at = a.from();
////            if (at instanceof Compound)
////                tryEncode(at, a.priElseZero(), w);
////
////        } while (kontinue.getAsBoolean());
////    }
////
////    @Override
////    public float value() {
////        return in.pri();
////    }


}
