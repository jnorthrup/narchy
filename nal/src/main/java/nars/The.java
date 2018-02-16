package nars;

/**
 * The the
 * immutable singleton instantiator/interner/etc
 *
 * when used as a marker interface, signifies the instance is Immutable
 */
public interface The {

    //    static nars.term.sub.Subterms _subterms(Collection<? extends Term> s) {
//        return _subterms(s.toArray(new Term[s.size()]));
//    }

    //    final static Memoize<IntArrayPair<Term>, Term> compoundCached =
//            new HijackMemoize<>(
//                    //CaffeineMemoize.build(
//                    (nc) -> {
//                        return _compound(Op.values()[nc.one], nc.two);
//                    }, 256 * 1024
//                    , 4, false);
    //, false);


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





//        Supplier<BiFunction<Op, Term[], Term>> SoftCompoundBuilder = () ->
//                new BiFunction<>() {
//
//                    final SoftMemoize<ByteKeyProtoCompound, Term> cache = new SoftMemoize<>((v) -> rawCompoundBuilder.apply(v.op, v.subs /* HACK */), 64 * 1024, true);
//
//                    @Override
//                    public Term apply(Op op, Term[] terms) {
//                        return cache.apply(new ByteKeyProtoCompound(op, terms).commit());
//                    }
//                };
//        //
//        Supplier<BiFunction<Op, Term[], Term>> CaffeineCompoundBuilder = () -> new BiFunction<>() {
//
//            final CaffeineMemoize<ByteKeyProtoCompound, Term> cache = CaffeineMemoize.build((v) -> rawCompoundBuilder.apply(v.op, v.subs /* HACK */),
//                    256 * 1024, false);
//
//            @Override
//            public Term apply(Op op, Term[] terms) {
//                return cache.apply(new ByteKeyProtoCompound(op, terms).commit());
//            }
//        };
//        //
////        public static final Supplier<BiFunction<Op, Term[], Term>> HijackCompoundBuilder = ()->new BiFunction<>() {
////
////            final HijackMemoize<NewCompound, Term> cache
////                    = new HijackMemoize<>((x) -> rawCompoundBuilder.apply(x.op, x.subs),
////                    128 * 1024 + 7 /* ~prime */, 3);
////
////            @Override
////            public Term apply(Op o, Term[] subterms) {
////                return cache.apply(
////                        new NewCompound(o, subterms).commit()
////                );
////            }
////        };
////

        //CaffeineCompoundBuilder.get();
        //HijackCompoundBuilder;



}
