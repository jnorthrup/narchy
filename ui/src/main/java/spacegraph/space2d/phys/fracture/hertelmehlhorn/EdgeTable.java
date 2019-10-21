package spacegraph.space2d.phys.fracture.hertelmehlhorn;

import spacegraph.space2d.phys.fracture.util.HashTabulka;
import spacegraph.space2d.phys.fracture.util.Node;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Hashovacia tabulka hran pre hertel-mehlhornov algoritmus.
 *
 * @author Marek Benovic
 */
class EdgeTable extends HashTabulka<Diagonal> {
    public Diagonal get(int i1, int i2) {
        return Stream.iterate(super.hashtable[Diagonal.hashCode(i1, i2) & super.n], Objects::nonNull, new UnaryOperator<Node<Diagonal>>() {
            @Override
            public Node<Diagonal> apply(Node<Diagonal> chain) {
                return chain.next;
            }
        }).map(new Function<Node<Diagonal>, Diagonal>() {
            @Override
            public Diagonal apply(Node<Diagonal> chain) {
                return chain.value;
            }
        }).filter(new Predicate<Diagonal>() {
            @Override
            public boolean test(Diagonal e) {
                return (e.n11.index == i1 && e.n12.index == i2) || (e.n11.index == i2 && e.n12.index == i1);
            }
        }).findFirst().orElse(null);
    }

    private void remove(int i1, int i2) {
        Diagonal e = get(i1, i2);
        super.remove(e);
    }

    public void remove(Diagonal e) {
        remove(e.n11.index, e.n12.index);
    }
}