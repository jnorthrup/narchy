package nars.term.util.builder;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.cache.Intermed.InternedCompoundTransform;

import java.util.UUID;
import java.util.function.Function;

/** memoizes certain target operations in addition to interning */
public class MemoizingTermBuilder extends InterningTermBuilder {

    //TODO <Term,Term>
    private final Function<InternedCompoundTransform, Term> normalize;
    private final Function<InternedCompoundTransform, Term> root;

    public MemoizingTermBuilder() {
        this(UUID.randomUUID().toString(), deepDefault, volMaxDefault, sizeDefault);
    }

    public MemoizingTermBuilder(String id, boolean deep, int volInternedMax, int cacheSizePerOp) {
        super(id, cacheSizePerOp, volInternedMax, deep);


        root = newOpCache("root", cacheSizePerOp, new Function<InternedCompoundTransform, Term>() {
            @Override
            public Term apply(InternedCompoundTransform j) {
                return _root((Compound) j.term);
            }
        });

        normalize = newOpCache("normalize", cacheSizePerOp, new Function<InternedCompoundTransform, Term>() {
            @Override
            public Term apply(InternedCompoundTransform j) {
                return MemoizingTermBuilder.super.normalize((Compound) j.term, (byte) 0);
            }
        });

//        concept = newOpCache("concept", j -> super.concept((Compound) j.sub0()), cacheSizePerOp);

    }

    @Override
    public Term normalize(Compound x, byte varOffset) {

//        if (!x.hasVars())
//            throw new WTF();

		//new LighterCompound(PROD, x, NORMALIZE)));
		return (int) varOffset == 0 && internable(x) ? normalize.apply(new InternedCompoundTransform(x)) : super.normalize(x, varOffset);

    }



    @Override
    public Term root(Compound x) {
        boolean t = x.hasAny(Op.Temporal);
        if (!t)
            return x;

		return internable(x) ? root.apply(new InternedCompoundTransform(x)) : _root(x);
    }

    private boolean internable(Compound x) {
        return x.volume() < volInternedMax && x.the();
    }


}
