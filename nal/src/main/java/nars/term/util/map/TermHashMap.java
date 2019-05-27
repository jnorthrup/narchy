package nars.term.util.map;

import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.data.set.UnenforcedConcatSet;
import nars.term.Term;
import nars.term.anon.Intrin;
import org.eclipse.collections.api.tuple.primitive.ShortObjectPair;
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

    public final ShortObjectHashMap<X> id;
    public final Map<Term, X> other;

    public TermHashMap() {
        this(new ShortObjectHashMap<>(16),
                //new UnifiedMap<>(16)
                new HashMap<>(16, 0.99f)
        );
    }

    public TermHashMap(ShortObjectHashMap<X> idMap, Map<Term, X> otherMap) {
        super();
        id = idMap;
        other = otherMap;
    }

    @Override
    public int size() {
        return id.size() + other.size();
    }

    public void clear() {

        //int sizeBeforeClear = id.size();
        if (!id.isEmpty())
            id.clear();
//        if (sizeBeforeClear > compactThreshold())
//            id.compact();


        other.clear();
        //other = null;
    }


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
        short aid = Intrin.id(key);
        if (aid != 0) {

            return id.updateValueWith(aid, () -> f.apply(key, null), (p,k) -> f.apply(k, p), key);

        } else {


            return other.compute(key, f);

        }
    }

    public X computeIfAbsent(Term key,
                             Function<? super Term, ? extends X> mappingFunction) {


        short aid = Intrin.id(key);
        return aid != 0 ?
                id.getIfAbsentPutWith(aid, mappingFunction::apply, key) :
                other.computeIfAbsent(key, mappingFunction);

    }


    @Override
    public X get(Object key) {
        short aid = Intrin.id((Term) key);
        return aid != 0 ? id.get(aid) : other.get(key);
    }

    @Override
    public X put(Term key, X value) {
        short aid = Intrin.id(key);
        return aid != 0 ?
                id.put(aid, value) :
                other.put(key, value);
    }

    @Override
    public X remove(Object key) {
        short aid = Intrin.id((Term) key);
        return aid != 0 ? id.remove(aid) : other.remove(key);
    }


    @Override
    public void forEach(BiConsumer<? super Term, ? super X> action) {
        id.forEachKeyValue((x, y) -> {
            if (y!=null)
                action.accept(Intrin.term(x), y);
        });
        if (!other.isEmpty()) {
            //other.forEach(action);
            other.forEach((x,y)->{
               if (y!=null)
                   action.accept(x,y);
            });
        }
    }



    static final class AnonMapEntrySet<X> extends AbstractSet<Map.Entry<Term, X>> {
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

    static final class AnonEntry<X> implements Map.Entry<Term, X> {

        private final ShortObjectPair<X> x;

        AnonEntry(ShortObjectPair<X> x) {
            this.x = x;
        }

        @Override
        public Term getKey() {
            return Intrin.term(x.getOne());
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
