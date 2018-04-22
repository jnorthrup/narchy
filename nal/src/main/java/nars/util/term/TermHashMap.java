package nars.util.term;

import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.data.UnenforcedConcatSet;
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
        return (id != null ? id.size() : 0) + (other != null ? other.size() : 0);
    }

    public void clear() {
        if (id != null) {
            int sizeBeforeClear = id.size();
            id.clear();
            if (sizeBeforeClear > compactThreshold())
                id.compact(); //shrink internal key/value array
        }
        //id = null;

        if (other != null) other.clear();
    }

    public int compactThreshold() {
        return initialHashCapacity()*2;
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

    @Override
    public X compute(Term key, BiFunction<? super Term, ? super X, ? extends X> f) {
        if (key instanceof AnonID) {
            short a = ((AnonID) key).anonID();
            if (id == null) {
                X next = f.apply(key, null);
                if (next!=null) {
                    ensureIDMap().put(a, next);
                }
                return next;
            } else {
                return id.updateValue(a, ()->f.apply(key,null), (p)->
                        f.apply(key, p));
            }
        } else {
            //use the map's native computeIfAbsent..
            if (other == null) {
                X next = f.apply(key, null);
                if (next!=null) {
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

//        X v;
//        if ((v = get(key)) == null) {
//            X newValue;
//            if ((newValue = mappingFunction.apply(key)) != null) {
//                put(key, newValue);
//                return newValue;
//            }
//        }
//        return v;

        if (key instanceof AnonID) {
            short a = ((AnonID) key).anonID();
            if (id == null) {
                X next = mappingFunction.apply(key);
                if (next!=null) {
                    (id = newIDMap()).put(a, next);
                }
                return next;
            } else {
                return id.getIfAbsentPut(a, () ->
                        mappingFunction.apply(key));
            }
        } else {
            //use the map's native computeIfAbsent..
            if (other == null) {
                X next = mappingFunction.apply(key);
                if (next!=null) {
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
        if (o == null) {
            return this.id = newIDMap();
        } else {
            return o;
        }
    }
    private Map<Term, X> ensureOtherMap() {
        Map<Term, X> o = this.other;
        if (o == null) {
            return this.other = newOtherMap();
        } else {
            return o;
        }
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
            return ensureIDMap().put(((AnonID) key).anonID(), value);
        } else {
            return ensureOtherMap().put(key, value);
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

    protected int initialHashCapacity() {
        return 8;
    }

    protected ShortObjectHashMap<X> newIDMap() {
        return new ShortObjectHashMap<>(initialHashCapacity());
    }

    protected Map<Term, X> newOtherMap() {
        return new UnifiedMap<>(initialHashCapacity(), 0.99f);
        //return new HashMap(initialHashCapacity(), 0.99f);
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

//    /**
//     * Extended indexing and aggregate functionality
//     * TODO - computes Subterms-like aggregates on the keySet
//     */
//    static class TermHashMapX<X> extends TermHashMap<X> {
//        /**
//         * an accumulated structure of the keys, lazily updated
//         */
//        public int structure() {
//            throw new TODO();
//        }
//
//        /**
//         * an accumulated structure of the keys, lazily updated
//         */
//        public int volume() {
//            throw new TODO();
//        }
//
//        //other Subterms methods
//    }

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
