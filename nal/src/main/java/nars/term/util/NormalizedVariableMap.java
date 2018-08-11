package nars.term.util;

import com.google.common.collect.Iterators;
import jcog.TODO;
import nars.term.Term;
import nars.term.Variable;
import nars.term.anon.AnonID;
import org.eclipse.collections.api.tuple.primitive.ShortObjectPair;
import org.eclipse.collections.impl.map.mutable.primitive.ShortObjectHashMap;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

class NormalizedVariableMap<X> extends AbstractMap<Variable, X> {

    private final ShortObjectHashMap<X> id = new ShortObjectHashMap<>();

    public NormalizedVariableMap() {
        super();
    }

    @Override
    public int size() {
        return id.size();
    }

    public void clear() {
        id.clear();
    }



    @Override
    public Set<Entry<Variable, X>> entrySet() {
        boolean hasID = !id.isEmpty();

        if (!hasID) {
            return Collections.emptySet();

        } else {
            return new AnonMapEntrySet<>(id);
        }
    }

    @Override
    public X compute(Variable key, BiFunction<? super Variable, ? super X, ? extends X> f) {
        short a = ((AnonID) key).anonID();

        return id.updateValue(a, () -> f.apply(key, null), (p) ->
                f.apply(key, p));
    }

    public X computeIfAbsent(Term key,
                             Function<? super Term, ? extends X> mappingFunction) {
        short a = ((AnonID) key).anonID();

        return id.getIfAbsentPut(a, () ->
                mappingFunction.apply(key));
    }



    @Override
    public X get(Object key) {
        return id.get(((AnonID) key).anonID());
    }

    @Override
    public X put(Variable key, X value) {
        return id.put(((AnonID) key).anonID(), value);
    }

    @Override
    public X remove(Object key) {
        return id.remove(((AnonID) key).anonID());
    }

    @Override
    public void forEach(BiConsumer<? super Variable, ? super X> action) {
        id.forEachKeyValue((x, y) -> action.accept((Variable) AnonID.idToTerm(x), y));

    }





























    static class AnonMapEntrySet<X> extends AbstractSet<Entry<Variable, X>> {
        private final ShortObjectHashMap<X> id;

        AnonMapEntrySet(ShortObjectHashMap<X> id) {
            this.id = id;
        }

        @Override
        public Iterator<Entry<Variable, X>> iterator() {
            return Iterators.transform(id.keyValuesView().iterator(), AnonEntry::new);
        }

        @Override
        public int size() {
            return id.size();
        }

    }

    static class AnonEntry<X> implements Map.Entry<Variable, X> {

        private final ShortObjectPair<X> x;

        AnonEntry(ShortObjectPair<X> x) {
            this.x = x;
        }

        @Override
        public Variable getKey() {
            return (Variable) AnonID.idToTerm(x.getOne());
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

