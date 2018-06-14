//package nars.op.mental;
//
//import jcog.bag.impl.HijackBag;
//import jcog.bag.impl.hijack.PLinkHijackBag;
//import jcog.pri.PLink;
//import jcog.pri.PriReference;
//import jcog.pri.Priority;
//import nars.*;
//import nars.concept.PermanentConcept;
//import nars.task.NALTask;
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.var.Variable;
//import net.byteseek.automata.factory.MutableStateFactory;
//import net.byteseek.automata.trie.TrieFactory;
//import net.byteseek.io.reader.ByteArrayReader;
//import net.byteseek.matcher.automata.ByteMatcherTransitionFactory;
//import net.byteseek.matcher.automata.SequenceMatcherTrie;
//import net.byteseek.matcher.multisequence.ListMultiSequenceMatcher;
//import net.byteseek.matcher.multisequence.MultiSequenceMatcher;
//import net.byteseek.matcher.multisequence.TrieMultiSequenceMatcher;
//import net.byteseek.matcher.sequence.ByteSequenceMatcher;
//import net.byteseek.matcher.sequence.SequenceMatcher;
//import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//import java.util.Collection;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.function.Function;
//
//import static java.util.stream.Collectors.toList;
//import static nars.Op.BELIEF;
//import static nars.term.Terms.compoundOrNull;
//import static nars.time.Tense.ETERNAL;
//
///**
// * Created by me on 2/11/17.
// */
//public class Compressor extends Abbreviation /* implements RemovalListener<Compound, Compressor.Abbr>*/ {
//
//    static final Logger logger = LoggerFactory.getLogger(Compressor.class);
//
//    final MutableStateFactory<SequenceMatcher> stateFactory = new MutableStateFactory<>();
//    final ByteMatcherTransitionFactory<SequenceMatcher> transitionFactory = new ByteMatcherTransitionFactory<>();
//    final TrieFactory<SequenceMatcher> smFactory = sequences -> new SequenceMatcherTrie(sequences, stateFactory, transitionFactory);
//
//    private MultiSequenceMatcher encoder, decoder;
//
//    //final Cache<Compound, Abbr> code;
//    final HijackBag<Compound,Abbr> code;
//
//    final Map<SequenceMatcher, Abbr> dc = new ConcurrentHashMap(); //HACK
//    final Map<SequenceMatcher, Abbr> ec = new ConcurrentHashMap(); //HACK
//
//    class Abbr extends PLink<Compound> {
//
//        @NotNull
//        public AliasConcept compressed;
//
//        SequenceMatcher encode;
//        SequenceMatcher decode;
//        byte[] encoded;
//        byte[] decoded;
//
//        private final float score;
//        private NALTask relation;
//
//        Abbr(Compound decompressed, float initialPri) {
//            super(decompressed, 0 );
//
//            this.score = score( decompressed );
//            boost( initialPri );
//        }
//
//        public void start() throws RuntimeException {
//
//            Compound decompressed = get();
//            this.compressed = AliasConcept.get(newSerialTerm(), decompressed, nar);
//
//            Compound s = compoundOrNull(
//                    $.sim
//                    //$.equi
//                            (compressed, decompressed)
//            );
//            if (s == null)
//                throw new RuntimeException("unrelateable: " + compressed + " for " + decompressed);
//
//            if (compressed == null)
//                throw new RuntimeException("could not create alias concept: " + compressed + " for " + decompressed);
//
//            this.encoded = IO.termToBytes(this.compressed);
//            this.decoded = IO.termToBytes(decompressed);
//            this.decode = new ByteSequenceMatcher(encoded);
//            this.encode = new ByteSequenceMatcher(decoded);
//
//            nar.on(this.compressed);
//            ec.put(encode, this);
//            dc.put(decode, this);
//
//            relation = new NALTask(
//                    s, BELIEF, $.t(1f, abbreviationConfidence.floatValue()),
//                    nar.time(), ETERNAL, ETERNAL,
//                    new long[]{nar.time.nextStamp()}
//            );
//            relation.meta(Abbreviation.class, new Term[] { decompressed, compressed } );
//            relation.log("Abbreviate");
//            relation.budget(nar);
//            nar.input(relation);
//
//            recompile();
//        }
//
//        public void stop() {
//            NALTask r = this.relation;
//            if (r !=null) {
//                this.relation = null;
//
//                r.delete();
//
//                ec.remove(encode);
//                dc.remove(decode);
//
//                recompile();
//            }
//        }
//
//        public void boost(float pri) {
//            priAdd( score * pri );
//        }
//
//        public boolean ready() {
//            return this.relation!=null;
//        }
//
//    }
//
//
//    /* boost in proportion to the volume of the uncompressed term, with a scaling factor relative to the capacity of the bag (to avoid clipping at 1.0) */
//    protected float score(Compound decompressed) {
//        return 1f/code.capacity() * (decompressed.volume() - 1); /* -1 to compensate for the cost of the abbreviation, ie. an atom */
//    }
//
//
//    public Compressor(@NotNull NAR n, String termPrefix, int volMin, int volMax, float selectionRate, int pendingCapacity, int maxCodes) {
//        super(n, termPrefix, volMin, volMax, selectionRate, pendingCapacity);
//
//        code =
//                //Caffeine.newBuilder().maximumSize(maxCodes).removalListener(this).executor(n.exe).builder();
//                new PLinkHijackBag(maxCodes, 4) {
//                    @Override
//                    public void onRemoved(@NotNull Object value) {
//                        ((Abbr)value).stop();
//                        ((Abbr)value).delete();
//                    }
//
//                    @Override
//                    protected boolean replace(float incoming, Object existing) {
//                        return hijackGreedy(
//                            incoming,
//                            ((Abbr)existing).priSafe(-1)
//                        );
//                    }
//
//                };
//    }
//
//
//    @Override
//    protected float onOut(PriReference<Compound> b) {
//        Compound x = b.get();
////        if (code.getIfPresent(x)!=null)
////            return 0; //already in
//
////        Compound y = decode(x);
////        int xxl = y.volume();
////        if (xxl >= volume.lo() && xxl <= volume.hi()) {
//        //return super.onOut(b);
//        x = compoundOrNull(x.unneg());
//        if (x != null) {
//            abbreviate(x, b);
//            return 1f;
//        } else
//            return 0; //rejected
//    }
//
//    @Override
//    protected boolean abbreviate(@NotNull Compound abbreviated, @NotNull Priority b) {
//
//
//        Op ao = abbreviated.op();
//        if (ao == Op.EQUI || ao == Op.IMPL) //HACK for equivalence relation
//            return false;
//
//        abbreviated = decode(abbreviated);
//        if (abbreviated.volume() > volume.hi())
//            return false; //expanded too much
//
//        /** dont abbreviate PermanentConcept's themselves */
//        if (nar.concept(abbreviated) instanceof PermanentConcept)
//            return false;
//
//        float p = b.priElseZero();
//
//        Abbr abb = code.get(abbreviated);
//        if (abb != null) {
//            abb.boost( p ); //boost it
//            return false;
//        }
//
//        abb = new Abbr(abbreviated  /** store fully decompress */, p);
//
//        code.commit();
//
//        if (code.put(abb) == null) {
//            return false; //failed insert
//        }
//
//        try {
//            abb.start();
//        } catch (RuntimeException e) {
//            logger.error("start: {}", e);
//            code.remove(abb.get());
//        }
//
//        return true;
//    }
//
//
//
//    final AtomicBoolean busy = new AtomicBoolean();
//
//    private void recompile() {
//
//        nar.runLater(() -> {
//            if (busy.compareAndSet(false, true)) {
//
//                decoder = matcher(x -> x.decode);
//                encoder = matcher(x -> x.encode);
//                busy.set(false);
//
//            }
//        });
//
//    }
//
//
//    private MultiSequenceMatcher matcher(Function<Abbr, SequenceMatcher> which) {
//
//        Collection<SequenceMatcher> mm = HijackBag.stream(code.map.get()).filter(x -> x!=null && x.ready()).map(which).collect(toList());
//            //code.asMap().values().stream().map(which).collect(Collectors.toList());
//
//        switch (mm.size()) {
//            case 0:
//                return null;
//
//            case 1:
//                return new ListMultiSequenceMatcher(mm);
//
//            default:
//                return new TrieMultiSequenceMatcher(smFactory, mm);
//
//        }
//    }
//
//    @NotNull
//    public Task encode(Task tt) {
//        return transcode(tt, true);
//    }
//
//    @NotNull
//    public Task decode(Task tt) {
//        return transcode(tt, false);
//    }
//
//    public Task transcode(Task tt, boolean en) {
//
//        Term i = tt.term();
//        Term o = transcode(i, en);
//        if (o != i) {
//
//            try {
//                //HACK wrap in product if atomic
////                if (!(o instanceof Compound))
////                    o = $.p(o);
//
//                //System.out.println("  compress: " + i + " to " + o);
//
//                if (o instanceof Compound) {
//                    @Nullable Task rr = Task.clone(tt, (Compound) o);
//                    if (rr != null)
//                        return rr;
//                }
//            } catch (Throwable t) {
//                //HACK ignore
//            }
//        }
//
//        return tt;
//
//    }
//
//    @NotNull
//    public Term transcode(@NotNull Term x, boolean en) {
//        if (!(x instanceof Compound)) {
//            //if encoding or t is a variable, do nothing
//            if (en || x instanceof Variable)
//                return x;
//        }
//
//        byte[] ii = IO.termToBytes(x);
//        byte[] oo = transcode(ii, en);
//
//        if (ii!=oo) {
//            Term y = IO.termFromBytes(oo, nar.terms);
//            if (y != null)
//                return y;
//        }
//
//        return x;
//    }
//
//    @NotNull
//    public byte[] transcodeBytes(Term t, boolean en) {
//        byte[] ii = IO.termToBytes(t);
//        if (!(t instanceof Compound))
//            return ii;
//        else {
//            return transcode(ii, en);
//        }
//    }
//
//    @NotNull
//    public Term encode(Term t) {
//        return transcode(t, true);
//    }
//
//    @NotNull
//    public Term decode(Term t) {
//        return transcode(t, false);
//    }
//
//    @NotNull
//    public Compound decode(Compound t) {
//        return (Compound) transcode(t, false);
//    }
//
//
//    public byte[] transcode(byte[] b, boolean en) {
//
//        MultiSequenceMatcher coder = en ? this.encoder : this.decoder;
//        if (coder == null)
//            return b;
//
//        //byte[] bOriginal = b;
//
//
//        int ii = 0;
//
//
//        ByteArrayReader wr = null;
//        final IntArrayList termPos = new IntArrayList();
//
//        int numCodes = code.size();
//
//        //POSSIBLE OPTIMIZATIONS
//        //  if the substitution occurred within a non-commutive term, it is probably safe to continue from before the substitution, not from the beginning
//        //  obtain the byte[] width of each segment, and if this is less than a known min compression/decompression target range, skip it since no code would apply there
//
//        do {
//
//            if (wr == null) {
//                ii = 0;
//                termPos.clear();
//                try {
//                    IO.mapSubTerms(b, (o, depth, p) -> termPos.add(p));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (Exception e) {
//                    //logger.error("{}", e);
//                    return b;
//                }
//                wr = new ByteArrayReader(b);
//            }
//
//            int i = termPos.get(ii++);
//
//            try {
//
//                /*Array*/
//                SequenceMatcher m = coder.firstMatch(wr, i);
//                if (m != null) {
//                    //if (!mm.isEmpty()) {
//                    //int mms = mm.size();
//
//                    //for (int i1 = 0, mmSize = mms; i1 < mmSize; i1++) {
//                    //SequenceMatcher m = mm.get(i1);
//                    Abbr c = abbr(m, en);
//                    if (c != null) {
//                        c.priAdd(1f/ numCodes);
//
//                        //substitute
//                        final byte[] to = en ? c.encoded : c.decoded;
//                        final byte[] from = en ? c.decoded : c.encoded;
//
//                        final byte[] prev = b;
//                        int change = to.length - from.length;
//                        if (change < -b.length)
//                            throw new ArrayIndexOutOfBoundsException();
//
//                        final byte[] next = new byte[b.length + change];
//
//                        System.arraycopy(prev, 0, next, 0, i);
//                        System.arraycopy(to, 0, next, i, to.length);
//                        int l = b.length;
//                        if (i < l)
//                            System.arraycopy(prev, i + from.length, next, i + to.length, l - (i + from.length));
//
//                        wr = null;
//                        b = next;
//                        continue; //restart
//                    }
//
//                }
//
//            } catch (IOException e) {
//                //logger.error(" {}", e);
//                return b;
//            }
//
//        } while (ii < termPos.size());
//
//
//        return b;
//
//    }
//
//    private Abbr abbr(SequenceMatcher m, boolean en) {
//        return (en ? this.ec : this.dc).get(m);
//    }
//
//
//    public static void main(String[] args) throws Narsese.NarseseException {
//        NAR n = new NARS().get();
//        Compressor c = new Compressor(n, "_", 4, 8, 0.5f, 8, 32);
//
//        n.onTask(x -> {
//            Task y = c.encode(x);
//            if (!y.equals(x))
//                System.out.println(y);
//        });
//        n.log();
//        n.input("a:b. b(a,c, 1, 1, 2). c:(d,a). d:e.");
//        n.run(800);
//
//
//    }
//}
