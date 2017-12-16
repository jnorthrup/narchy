package nars;

import jcog.Util;
import jcog.list.FasterList;
import jcog.memoize.CaffeineMemoize;
import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
import jcog.memoize.SoftMemoize;
import nars.derive.match.Ellipsis;
import nars.derive.match.EllipsisMatch;
import nars.index.term.NewCompound;
import nars.term.SubtermsKey;
import nars.term.Term;
import nars.term.anon.AnonID;
import nars.term.anon.AnonVector;
import nars.term.atom.Bool;
import nars.term.compound.CachedCompound;
import nars.term.compound.CachedUnitCompound;
import nars.term.sub.ArrayTermVector;
import nars.term.sub.UnitSubterm;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.Op.*;

/**
 * The the
 * immutable singleton instantiator/interner/etc
 */
public enum The {
    ;

    /* @NotNull */
    public static nars.term.sub.Subterms subterms(Term... s) {
        return The.Subterms.the.apply(s);
    }

    /* @NotNull */
    public static nars.term.sub.Subterms subterms(Collection<? extends Term> s) {
        return The.Subterms.the.apply(s.toArray(new Term[s.size()]));
    }

    @Deprecated /* @NotNull */ public static Term compound(Op o, Term... subterms) {

        return Compound.the.apply(o, new FasterList<>(subterms));
    }

    /* @NotNull */
    protected static Term compound(Op o, List<Term> subterms) {
        if (o == NEG)
            return Compound.rawCompoundBuilder.apply(o, subterms); //dont intern neg's
        return Compound.the.apply(o, subterms);
    }


    public static final class Subterms {

        public static final Function<Term[], nars.term.sub.Subterms> RawSubtermBuilder = (t) -> {

            boolean purelyAnon = true;
            for (Term x : t) {
                if (x instanceof EllipsisMatch)
                    throw new RuntimeException("ellipsis match should not be a subterm of ANYTHING");
                if (purelyAnon && !(x instanceof AnonID))
                    purelyAnon = false;
            }

            if (!purelyAnon) {
                switch (t.length) {
                    case 0:
                        return nars.term.sub.Subterms.Empty;
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


        static final Function<SubtermsKey, nars.term.sub.Subterms> rawSubtermBuilderBuilder = (n) -> RawSubtermBuilder.apply(n.commit());

        public static final Supplier<Function<Term[], nars.term.sub.Subterms>> SoftSubtermBuilder = () ->
                new MemoizeSubtermBuilder(new SoftMemoize<>(rawSubtermBuilderBuilder, 256 * 1024, true));

        public static final Supplier<Function<Term[], nars.term.sub.Subterms>> WeakSubtermBuilder = () ->
                new MemoizeSubtermBuilder(new SoftMemoize<>(rawSubtermBuilderBuilder, 256 * 1024, false));

        public static final Supplier<Function<Term[], nars.term.sub.Subterms>> CaffeineSubtermBuilder = () ->
                new MemoizeSubtermBuilder(CaffeineMemoize.build(rawSubtermBuilderBuilder, 256 * 1024, false));


        public static final Supplier<Function<Term[], nars.term.sub.Subterms>> HijackSubtermBuilder = () ->
                new MemoizeSubtermBuilder(new HijackMemoize(rawSubtermBuilderBuilder,
                            128 * 1024 + 7 /* ~prime-ish maybe */,
                            4));
//                new Function<>() {
//
//                    final HijackMemoize<NewCompound, nars.term.sub.Subterms> cache
//                            = new HijackMemoize<>((x) -> RawSubtermBuilder.apply(x.subs),
//                            128 * 1024 + 7 /* ~prime */, 4);
//
//                    @Override
//                    public nars.term.sub.Subterms apply(Term[] o) {
//                        return cache.apply(
//                                new NewCompound(PROD, o).commit()
//                        );
//                    }
//                };

        public static Function<Term[], nars.term.sub.Subterms> the =
                //CaffeineSubtermBuilder.get();
                RawSubtermBuilder;

        private static class MemoizeSubtermBuilder implements Function<Term[], nars.term.sub.Subterms> {
            final Memoize<SubtermsKey, nars.term.sub.Subterms> cache;

            /**
             * TODO make adjustable
             */
            int maxVol = 10;
            final int minSubterms = 2;

            private MemoizeSubtermBuilder(Memoize<SubtermsKey, nars.term.sub.Subterms> cache) {
                this.cache = cache;
            }

            @Override
            public nars.term.sub.Subterms apply(Term[] terms) {
                if (terms.length < minSubterms || Util.sumExceeds(Term::volume, maxVol, terms))
                    return RawSubtermBuilder.apply(terms);
                else
                    return cache.apply(SubtermsKey.the(terms));
            }
        }

    }

    public static class Compound {

        public static final BiFunction<Op, List<Term>, Term> rawCompoundBuilder = (o, subterms) -> {
            assert (!o.atomic) : o + " is atomic, with subterms: " + (subterms);

            boolean hasEllipsis = false;
            boolean prohibitBool = !o.allowsBool;

            for (int i = 0, subtermsSize = subterms.size(); i < subtermsSize; i++) {
                Term x = subterms.get(i);
                if (prohibitBool && x instanceof Bool)
                    return Null;
                if (!hasEllipsis && x instanceof Ellipsis)
                    hasEllipsis = true;
            }


            int s = subterms.size();
            assert (o.maxSize >= s) :
                    "subterm overflow: " + o + ' ' + (subterms);
            assert (o.minSize <= s || hasEllipsis) :
                    "subterm underflow: " + o + ' ' + (subterms);

            switch (s) {
                case 1:
                    return new CachedUnitCompound(o, subterms.get(0));

                default:
                    return new CachedCompound(o, subterms(subterms));
            }

        };

        public static final Supplier<BiFunction<Op, List<Term>, Term>> SoftCompoundBuilder = () ->
                new BiFunction<>() {

                    final SoftMemoize<NewCompound, Term> cache = new SoftMemoize<>((v) -> rawCompoundBuilder.apply(v.op, new FasterList(v.subs) /* HACK */), 64 * 1024, true);

                    @Override
                    public Term apply(Op op, List<Term> terms) {
                        return cache.apply(new NewCompound(op, terms).commit());
                    }
                };
        //
        public static final Supplier<BiFunction<Op, List<Term>, Term>> CaffeineCompoundBuilder = () -> new BiFunction<>() {

            final CaffeineMemoize<NewCompound, Term> cache = CaffeineMemoize.build((v) -> rawCompoundBuilder.apply(v.op, new FasterList(v.subs) /* HACK */),
                    256 * 1024, false);

            @Override
            public Term apply(Op op, List<Term> terms) {
                return cache.apply(new NewCompound(op, terms).commit());
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
        public static BiFunction<Op, List<Term>, Term> the =
                rawCompoundBuilder;
        //CaffeineCompoundBuilder.get();
        //HijackCompoundBuilder;

    }

}
