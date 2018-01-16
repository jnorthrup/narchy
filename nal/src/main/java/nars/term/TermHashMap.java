package nars.term;

import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.data.UnenforcedConcatSet;
import nars.term.anon.AnonID;
import org.eclipse.collections.api.tuple.primitive.ShortObjectPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.ShortObjectHashMap;

import java.util.*;
import java.util.function.BiConsumer;
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
        return (id != null ? id.size() : 0) + (other != null ? other.size() : 0);
    }

    public void clear() {
        if (id != null) id.clear();
        //id = null;

        if (other != null) other.clear();
    }

    @Override
    public Set<Entry<Term, X>> entrySet() {
        boolean hasID = id != null && !id.isEmpty();
        boolean hasOther = other != null && !other.isEmpty();
        if (!hasID) {
            if (!hasOther)
                return Collections.emptySet();
            else
                return other.entrySet();
        } else {
            Set<Entry<Term, X>> idEntries = new AnonMapEntrySet<>(id);
            if (!hasOther) {
                return idEntries;
            } else {
                return UnenforcedConcatSet.concat(idEntries, other.entrySet());
            }
        }
    }

    public X computeIfAbsent(Term key,
                              Function<? super Term, ? extends X> mappingFunction) {
//        if (key == null)
//            throw new NullPointerException();
        X v;
        if ((v = get(key)) == null) {
            X newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                put(key, newValue);
                return newValue;
            }
        }

        return v;
    }

    @Override
    public X get(Object key) {
        if (key instanceof AnonID) {
            if (id != null)
                return id.get(((AnonID) key).anonID());
        } else {
            if (other != null)
                return other.get(key);
        }
        return null;
    }

    @Override
    public X put(Term key, X value) {
        if (key instanceof AnonID) {
            if (id == null) id = newIDMap();
            return id.put(((AnonID) key).anonID(), value);
        } else {
            if (other == null) other = newOtherMap();
            return other.put(key, value);
        }
    }

    @Override
    public X remove(Object key) {
        if (key instanceof AnonID) {
            if (id != null) {
                return id.remove(((AnonID) key).anonID());
            }
        } else {
            if (other != null) {
                return other.remove(key);
            }
        }
        return null;
    }

    protected ShortObjectHashMap<X> newIDMap() {
        return new ShortObjectHashMap(8);
    }

    protected Map<Term, X> newOtherMap() {

        //return new UnifiedMap();
        return new HashMap(8);
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

//    @Override
//    public X computeIfAbsent(Term key, Function<? super Term, ? extends X> mappingFunction) {
//        return null;
//    }

    /**
     * Extended indexing and aggregate functionality
     * TODO - computes Subterms-like aggregates on the keySet
     */
    static class TermHashMapX<X> extends TermHashMap<X> {
        /**
         * an accumulated structure of the keys, lazily updated
         */
        public int structure() {
            throw new TODO();
        }

        /**
         * an accumulated structure of the keys, lazily updated
         */
        public int volume() {
            throw new TODO();
        }

        //other Subterms methods
    }

    static class AnonMapEntrySet<X> extends AbstractSet<Map.Entry<Term, X>> {
        private final ShortObjectHashMap<X> id;

        public AnonMapEntrySet(ShortObjectHashMap<X> id) {
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

        public AnonEntry(ShortObjectPair<X> x) {
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
