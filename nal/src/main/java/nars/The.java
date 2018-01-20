package nars;

import jcog.memoize.CaffeineMemoize;
import jcog.memoize.SoftMemoize;
import nars.derive.match.Ellipsis;
import nars.derive.match.EllipsisMatch;
import nars.index.term.ByteKeyProtoCompound;
import nars.subterm.ArrayTermVector;
import nars.subterm.Neg;
import nars.subterm.Subterms;
import nars.subterm.UnitSubterm;
import nars.term.Term;
import nars.term.anon.AnonID;
import nars.term.anon.AnonVector;
import nars.term.compound.CachedUnitCompound;
import nars.term.compound.CompoundCached;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.Op.NEG;
import static nars.Op.PROD;

/**
 * The the
 * immutable singleton instantiator/interner/etc
 *
 * when used as a marker interface, signifies the instance is Immutable
 */
public interface The {

    /* @NotNull */
    public static Subterms subterms(Collection<? extends Term> s) {
        return The.subterms(s.toArray(new Term[s.size()]));
    }

//    static nars.term.sub.Subterms _subterms(Collection<? extends Term> s) {
//        return _subterms(s.toArray(new Term[s.size()]));
//    }

    public static Subterms subterms(Term... s) {
        //return The.Subterms.the.apply(s);
        //return compound(PROD, s).subterms();
        return PROD.the(s).subterms();
    }

    static Subterms _subterms(Term... s) {
        return RawSubtermBuilder.apply(s);
    }

//    final static Memoize<IntArrayPair<Term>, Term> compoundCached =
//            new HijackMemoize<>(
//                    //CaffeineMemoize.build(
//                    (nc) -> {
//                        return _compound(Op.values()[nc.one], nc.two);
//                    }, 256 * 1024
//                    , 4, false);
    //, false);

    public static Term compound(Op o, Term... u) {

//        boolean cache = cacheable(u);
//
//        if (!cache) {
        return rawCompoundBuilder.apply(o, u);
//        } else {
//            //System.out.println(o + " " + Arrays.toString(u));
//            return compoundCached.apply(new IntArrayPair(o.id, u));
//        }
    }



        Function<Term[], nars.subterm.Subterms> RawSubtermBuilder = (t) -> {
            if (t.length == 0)
                return nars.subterm.Subterms.Empty;

            boolean purelyAnon = true;
            for (Term x : t) {
                if (x instanceof EllipsisMatch)
                    throw new RuntimeException("ellipsis match should not be a subterm of ANYTHING");
                if (purelyAnon) {
                    if (!(x instanceof AnonID)) {
                        if (/*t.length == 1 && */x.op()==NEG && x.unneg() instanceof AnonID) {
                            //allow anon here, but not t.length > 1 there is still some problem probably with commutives
                            //purelyAnon = true
                        } else {
                          purelyAnon = false;
                        }
                    }
                }
            }

            if (!purelyAnon) {
                switch (t.length) {
                    case 0:
                    case 1:
                        //return new TermVector1(t[0]);
                        return new UnitSubterm(t[0]);
                    //case 2:
                    //return new TermVector2(t);
                    default:
                        return new ArrayTermVector(t);
                }
            } else {
                return new AnonVector(t);
            }

        };

//
//        static final Function<SubtermsKey, nars.term.sub.Subterms> rawSubtermBuilderBuilder = (n) -> RawSubtermBuilder.apply(n.commit());
//
//        public static final Supplier<Function<Term[], nars.term.sub.Subterms>> SoftSubtermBuilder = () ->
//                new MemoizeSubtermBuilder(new SoftMemoize<>(rawSubtermBuilderBuilder, 256 * 1024, true));
//
//        public static final Supplier<Function<Term[], nars.term.sub.Subterms>> WeakSubtermBuilder = () ->
//                new MemoizeSubtermBuilder(new SoftMemoize<>(rawSubtermBuilderBuilder, 256 * 1024, false));
//
//        public static final Supplier<Function<Term[], nars.term.sub.Subterms>> CaffeineSubtermBuilder = () ->
//                new MemoizeSubtermBuilder(CaffeineMemoize.build(rawSubtermBuilderBuilder, 256 * 1024, false));
//
//
//        public static final Supplier<Function<Term[], nars.term.sub.Subterms>> HijackSubtermBuilder = () ->
//                new MemoizeSubtermBuilder(new HijackMemoize(rawSubtermBuilderBuilder,
//                            128 * 1024 + 7 /* ~prime-ish maybe */,
//                            5));
//
//        public static Function<Term[], nars.term.sub.Subterms> the =
//                RawSubtermBuilder;
//
//        private static class MemoizeSubtermBuilder implements Function<Term[], nars.term.sub.Subterms> {
//            final Memoize<SubtermsKey, nars.term.sub.Subterms> cache;
//
//            /**
//             * TODO make adjustable
//             */
//            int maxVol = 16;
//            final int minSubterms = 1;
//
//            private MemoizeSubtermBuilder(Memoize<SubtermsKey, nars.term.sub.Subterms> cache) {
//                this.cache = cache;
//            }
//
//            @Override
//            public nars.term.sub.Subterms apply(Term[] terms) {
//                if (terms.length < minSubterms || Util.sumExceeds(Term::volume, maxVol, terms))
//                    return RawSubtermBuilder.apply(terms);
//                else
//                    return cache.apply(SubtermsKey.the(terms));
//            }
//        }
//




        BiFunction<Op, Term[], Term> rawCompoundBuilder = (o, subterms) -> {
            assert (!o.atomic) : o + " is atomic, with subterms: " + (subterms);

            boolean hasEllipsis = false;

            for (int i = 0, subtermsSize = subterms.length; i < subtermsSize; i++) {
                Term x = subterms[i];
                if (!hasEllipsis && x instanceof Ellipsis)
                    hasEllipsis = true;
            }


            int s = subterms.length;
            assert (o.maxSize >= s) :
                    "subterm overflow: " + o + ' ' + (subterms);
            assert (o.minSize <= s || hasEllipsis) :
                    "subterm underflow: " + o + ' ' + (subterms);

            if (s == 1) {
                switch (o) {
                    case NEG:
                        return Neg.the(subterms[0]);
                    case PROD:
                        //use full CachedCompound for PROD
                        //use default below
                        break;
                    default:
                        return new CachedUnitCompound(o, subterms[0]);
                }


            }

            return new CompoundCached(o, The._subterms(subterms));

        };

        Supplier<BiFunction<Op, Term[], Term>> SoftCompoundBuilder = () ->
                new BiFunction<>() {

                    final SoftMemoize<ByteKeyProtoCompound, Term> cache = new SoftMemoize<>((v) -> rawCompoundBuilder.apply(v.op, v.subs /* HACK */), 64 * 1024, true);

                    @Override
                    public Term apply(Op op, Term[] terms) {
                        return cache.apply(new ByteKeyProtoCompound(op, terms).commit());
                    }
                };
        //
        Supplier<BiFunction<Op, Term[], Term>> CaffeineCompoundBuilder = () -> new BiFunction<>() {

            final CaffeineMemoize<ByteKeyProtoCompound, Term> cache = CaffeineMemoize.build((v) -> rawCompoundBuilder.apply(v.op, v.subs /* HACK */),
                    256 * 1024, false);

            @Override
            public Term apply(Op op, Term[] terms) {
                return cache.apply(new ByteKeyProtoCompound(op, terms).commit());
            }
        };
        //
//        public static final Supplier<BiFunction<Op, Term[], Term>> HijackCompoundBuilder = ()->new BiFunction<>() {
//
//            final HijackMemoize<NewCompound, Term> cache
//                    = new HijackMemoize<>((x) -> rawCompoundBuilder.apply(x.op, x.subs),
//                    128 * 1024 + 7 /* ~prime */, 3);
//
//            @Override
//            public Term apply(Op o, Term[] subterms) {
//                return cache.apply(
//                        new NewCompound(o, subterms).commit()
//                );
//            }
//        };
//

        //CaffeineCompoundBuilder.get();
        //HijackCompoundBuilder;



}
