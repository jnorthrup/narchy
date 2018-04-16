package nars.util.term.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static nars.Op.Null;

public class CachedTermTransform implements TermTransform {

    final TermTransform proxy;
    final BiFunction<Term,Function<Term,Term>,Term> cache;
    private final Logger logger = LoggerFactory.getLogger(CachedTermTransform.class);

    public CachedTermTransform(TermTransform proxy, Map<Term, Term> cache) {
        this(proxy, cache::computeIfAbsent);
    }

    public CachedTermTransform(TermTransform proxy, BiFunction<Term,Function<Term,Term>,Term>  cache) {
        this.proxy = proxy;
        this.cache = cache;
    }

    @Override
    public final @Nullable Termed transformAtomic(Term atomic) {
        return proxy.transformAtomic(atomic);
    }

    @Override
    public Term transformCompound(Compound x) {
        return cache.apply(x, xx -> {
            Term y = proxy.transformCompound((Compound) xx);
            return (y == null) ? nulled(xx) : y.term();
        });
    }


    protected Term nulled(Term x) {
        logger.warn("x transformed to null {}", x);
        return Null;
    }

}
