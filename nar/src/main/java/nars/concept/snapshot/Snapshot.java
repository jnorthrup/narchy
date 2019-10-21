package nars.concept.snapshot;

import nars.NAR;
import nars.concept.Concept;
import nars.term.Term;
import nars.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/** a container for time-stamped expiring cache of data that can be stored in Concept meta maps */
public class Snapshot<X> {

	/** TODO use AtomicFieldUpdater */
	protected final AtomicBoolean busy = new AtomicBoolean(false);

	/** occurrence time after which the data should be invalidated or refreshed */
	protected volatile long expires = Long.MIN_VALUE;

	/** of type:
	 *     X, SoftReference<X>, WeakReference<X> */
	protected volatile Object value;


	/** concept()'s the given term in the given NAR */
    public static @Nullable <X> X get(Term src, NAR nar, String id, long now, int ttl, BiFunction<Concept, X, X> updater) {
        Concept c = nar.conceptualize(src);
		return c != null ? get(c, id, now, ttl, updater) : null;
	}

	public static @Nullable <X> X get(Concept src, String id, long now, int ttl, BiFunction<Concept, X, X> updater) {
		return src.<Snapshot<X>>meta(id, Snapshot::new)
			.get(now, ttl, new UnaryOperator<X>() {
                @Override
                public X apply(X existing) {
                    return updater.apply(src, existing);
                }
            });
	}


	public @Nullable X get() {
        Object v = value;
		return v instanceof Reference ? ((Reference<X>) v).get() : (X) v;
	}

	/** here the value may be returned as-is or wrapped in a soft or weak ref */
	protected Object wrap(X x) {
		return x;
	}

	/** ttl = cycles of cached value before next expiration ( >= 0 )
	 * 			or -1 to never expire */
	public X get(long now, int ttl,  UnaryOperator<X> updater) {
        long e = expires;
        X current = get();
		if ((now >= e || current == null) && busy.compareAndSet(false, true)) {
			try {
                X nextX = updater.apply(current);
				this.expires = ttl >= 0 ? now + (long) ttl : Tense.TIMELESS /* forever */;
				this.value = wrap(nextX);
				return nextX;
			} finally {
				busy.set(false);
			}
		} else {
			//return the current value
			return current;
		}
	}
}
