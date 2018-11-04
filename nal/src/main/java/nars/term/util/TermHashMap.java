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
        id = newIDMap();
        other = newOtherMap();
    }

    @Override
    public int size() {
        return id.size() + other.size();
    }

    public void clear() {

        //int sizeBeforeClear = id.size();
        id.clear();
//        if (sizeBeforeClear > compactThreshold())
//            id.compact();


        other.clear();
        //other = null;
    }

//    private int compactThreshold() {
//        return initialHashCapacity() * 2;
//    }

    @Override
    public Set<Entry<Term, X>> entrySet() {
        boolean hasID = !id.isEmpty();
        boolean hasOther = !other.isEmpty();
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
        if (aid != 0) {

            return id.updateValue(aid, () -> f.apply(key, null), p -> f.apply(key, p));

        } else {


            return other.compute(key, f);

        }
    }

    public X computeIfAbsent(Term key,
                             Function<? super Term, ? extends X> mappingFunction) {


        short aid = AnonID.id(key);
        if (aid != 0) {


            return id.getIfAbsentPut(aid, () ->
                    mappingFunction.apply(key));

        } else {


            return other.computeIfAbsent(key, mappingFunction);

        }

    }


    @Override
    public X get(Object key) {
        short aid = AnonID.id((Term) key);
        return aid != 0 ? id.get(aid) : other.get(key);
    }

    @Override
    public X put(Term key, X value) {
        short aid = AnonID.id(key);
        return aid != 0 ?
                id.put(aid, value) :
                other.put(key, value);
    }

    @Override
    public X remove(Object key) {
        short aid = AnonID.id((Term) key);
        return aid != 0 ? id.remove(aid) : other.remove(key);
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
        id.forEachKeyValue((x, y) -> action.accept(AnonID.idToTerm(x), y));
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
