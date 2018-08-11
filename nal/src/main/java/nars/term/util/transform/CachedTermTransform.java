package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CachedTermTransform implements TermTransform {

    private final TermTransform proxy;
    private final BiFunction<Term,Function<Term,Term>,Term> cache;
//    private final Logger logger = LoggerFactory.getLogger(CachedTermTransform.class);

    public CachedTermTransform(TermTransform proxy, Map<Term, Term> cache) {
        this(proxy, cache::computeIfAbsent);
    }

    private CachedTermTransform(TermTransform proxy, BiFunction<Term, Function<Term, Term>, Term> cache) {
        this.proxy = proxy;
        this.cache = cache;
    }

    @Override
    public final @Nullable Term transformAtomic(Atomic atomic) {
        return proxy.transformAtomic(atomic);
    }

    @Override
    public Term transformCompound(Compound x) {
        return cache.apply(x, xx -> {
            Term y = proxy.transformCompound((Compound) xx);
            //return (y == null) ? nulled(xx) : y.term();
            return y;
        });
    }


//    protected Term nulled(Term x) {
//        logger.warn("x transformed to null {}", x);
//        return Null;
//    }

}
