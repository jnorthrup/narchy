package nars.term.util;

import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.data.set.UnenforcedConcatSet;
import nars.term.Term;
import nars.term.anon.AnonID;
import org.eclipse.collections.api.tuple.primitive.ShortObjectPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.ShortObjectHashMap;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * functionally equivalent to: Map<Term,X>
 * but with special support for AnonID'd terms
 */
public class TermHashMap<X> extends AbstractMap<Term, X> {

    protected ShortObjectHashMap<X> id = null;
    protected Map<Term, X> other = null;

    public TermHashMap() {
        super();
    }

    @Override
    public int size() {
        return (id != null ?
                id.size() : 0) + (other != null ? other.size() : 0);
    }

    public void clear() {
        if (id != null) {
            int sizeBeforeClear = id.size();
            id.clear();
            if (sizeBeforeClear > compactThreshold())
                id.compact();
        }


        if (other != null) other.clear();
        //other = null;
    }

    private int compactThreshold() {
        return initialHashCapacity() * 2;
    }

    @Override
    public Set<Entry<Term, X>> entrySet() {
        boolean hasID = id != null && !id.isEmpty();
        boolean hasOther = other != null && !other.isEmpty();
        if (!hasID) {
            return !hasOther ? Collections.emptySet() : other.entrySet();
        } else {
            Set<Entry<Term, X>> idEntries = new AnonMapEntrySet<>(id);
            return !hasOther ? idEntries : UnenforcedConcatSet.concat(idEntries, other.entrySet());
        }
    }

    @Override
    public X compute(Term key, BiFunction<? super Term, ? super X, ? extends X> f) {
        short aid = AnonID.id(key);
        if (aid!=0) {
            if (id == null) {
                X next = f.apply(key, null);
                if (next != null) {
                    ensureIDMap().put(aid, next);
                }
                return next;
            } else {
                return id.updateValue(aid, () -> f.apply(key, null), (p) ->
                        f.apply(key, p));
            }
        } else {

            if (other == null) {
                X next = f.apply(key, null);
                if (next != null) {
                    ensureOtherMap().put(key, next);
                }
                return next;
            } else {
                return other.compute(key, f);
            }
        }
    }

    public X computeIfAbsent(Term key,
                             Function<? super Term, ? extends X> mappingFunction) {


        short aid = AnonID.id(key);
        if (aid!=0) {

            if (id == null) {
                X next = mappingFunction.apply(key);
                if (next != null) {
                    (id = newIDMap()).put(aid, next);
                }
                return next;
            } else {
                return id.getIfAbsentPut(aid, () ->
                        mappingFunction.apply(key));
            }
        } else {

            if (other == null) {
                X next = mappingFunction.apply(key);
                if (next != null) {
                    (other = newOtherMap()).put(key, next);
                }
                return next;
            } else {
                return other.computeIfAbsent(key, mappingFunction);
            }
        }

    }

    private ShortObjectHashMap<X> ensureIDMap() {
        ShortObjectHashMap<X> o = this.id;
        return o == null ? (this.id = newIDMap()) : o;
    }

    private Map<Term, X> ensureOtherMap() {
        Map<Term, X> o = this.other;
        return o == null ? (this.other = newOtherMap()) : o;
    }

    @Override
    public X get(Object key) {
        short aid = AnonID.id((Term)key);
        if (aid!=0) {
            if (id != null)
                return id.get(aid);
        } else {
            if (other != null)
                return other.get(key);
        }
        return null;
    }

    @Override
    public X put(Term key, X value) {
        short aid = AnonID.id(key);
        return aid!=0 ?
                ensureIDMap().put(aid, value) :
                ensureOtherMap().put(key, value);
    }

    @Override
    public X remove(Object key) {
        short aid = AnonID.id((Term)key);
        if (aid!=0) {
            if (id != null) {
                return id.remove(aid);
            }
        } else {
            if (other != null) {
                return other.remove(key);
            }
        }
        return null;
    }

    private int initialHashCapacity() {
        return 8;
    }

    private ShortObjectHashMap<X> newIDMap() {
        return new ShortObjectHashMap<>(initialHashCapacity());
    }

    private Map<Term, X> newOtherMap() {
        return new UnifiedMap<>(initialHashCapacity(), 0.99f);

    }

    @Override
    public void forEach(BiConsumer<? super Term, ? super X> action) {
        if (id != null)
            id.forEachKeyValue((x, y) -> action.accept(AnonID.idToTerm(x), y));
        if (other != null)
            other.forEach(action);
    }

    /**
     * more intense than a clear but equally effective
     */
    public void delete() {
        id = null;
        other = null;
    }


    static class AnonMapEntrySet<X> extends AbstractSet<Map.Entry<Term, X>> {
        private final ShortObjectHashMap<X> id;

        AnonMapEntrySet(ShortObjectHashMap<X> id) {
            this.id = id;
        }

        @Override
        public Iterator<Entry<Term, X>> iterator() {
            return Iterators.transform(id.keyValuesView().iterator(), AnonEntry::new);
        }

        @Override
        public int size() {
            return id.size();
        }

    }

    static class AnonEntry<X> implements Map.Entry<Term, X> {

        private final ShortObjectPair<X> x;

        AnonEntry(ShortObjectPair<X> x) {
            this.x = x;
        }

        @Override
        public Term getKey() {
            return AnonID.idToTerm(x.getOne());
        }

        @Override
        public X getValue() {
            return x.getTwo();
        }

        @Override
        public X setValue(X value) {
            throw new TODO();
        }
    }

}
