package jcog.learn.markov;

import com.google.common.collect.Streams;
import jcog.data.list.FasterList;
import jcog.pri.WLink;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * A class for generating a Markov phrase out of some sort of
 * arbitrary comparable data.
 *
 * @param <T> the type of data you would like to generate phrases for (e.g., <code>java.lan
 * @author pkilgo
 */
public class MarkovChain<T> {

    /**
     * HashMap to help us resolve data to the node that contains it
     */
    public final Map<List<T>, Chain<T>> nodes;

    /**
     * Stores how long our tuple length is (how many data elements a node has)
     */
    public final int arity;

    public MarkovChain(int n) {
        this(new HashMap(), n);
    }

    public MarkovChain(Map<List<T>, Chain<T>> nodes, int n) {
        this.nodes = nodes;
        this.arity = n;

        if (n <= 0) throw new IllegalArgumentException("Can't have MarkovChain with tuple length <= 0");
    }

    /**
     * Node that marks the beginning of a phrase. All Markov phrases start here.
     */
    public final Chain START = new Chain(List.of());
    /**
     * Node that signals the end of a phrase. This node should have no edges.
     */
    public final Chain END = new Chain(List.of());


    /**
     * Forget everything.
     */
    public void clear() {
        nodes.clear();
        START.clear();
        END.clear();
    }


    public MarkovChain learn(Iterable<T> phrase) {
        return learn(Streams.stream(phrase));
    }

    public MarkovChain learn(Stream<T> phrase) {
        return learn(phrase, 1f);
    }

    /**
     * Interpret an ArrayList of data as a possible phrase.
     *
     * @param phrase to learn
     */
    public MarkovChain learn(Stream<T> phrase, float strength) {

        
        final Chain[] current = {START};

        
        final List<T>[] tuple = new List[]{new FasterList<T>()};

        
        
        phrase.forEach((T t) -> {
            List<T> tu = tuple[0];

            int sz = tu.size();
            if (sz < arity) {
                tu.add(t);
            } else {
                current[0] = current[0].learn(getOrAdd(tu), strength);
                (tuple[0] = new FasterList<>(1)).add(t);
            }
        });

        Chain c = current[0];
        List<T> t = tuple[0];

        
        if (!t.isEmpty()) {
            c = c.learn(getOrAdd(t), strength);
        }

        
        c.learn(END, strength);
        return this;
    }

    /**
     * Interpret an array of data as a valid phrase.
     *
     * @param phrase to interpret
     */
    public MarkovChain learn(T[] phrase) {
        return learn(Stream.of(phrase));
    }

    public MarkovChain learnAll(T[]... phrases) {
        for (T[] p : phrases)
            learn(p);
        return this;
    }

    public MarkovSampler<T> sample() {
        return sample(ThreadLocalRandom.current());
    }

    public MarkovSampler<T> sample(Random rng) {
        return new MarkovSampler(this, rng);
    }

    /**
     * This method is an alias to find a node if it
     * exists or create it if it doesn't.
     *
     * @param x to find a node for
     * @return the newly created node, or resolved node
     */
    private Chain getOrAdd(List<T> x) {
        if (x.size() > arity) {
            throw new IllegalArgumentException(
                    String.format("Invalid tuple length %d. This structure: %d", x.size(), arity)
            );
        }

        return nodes.computeIfAbsent(x, Chain::new);
    }


    /**
     * This is our Markov phrase node. It contains the data
     * that this node represents as well as a list of edges to
     * possible nodes elsewhere in the graph.
     *
     * @author pkilgo
     */
    public static class Chain<T> {


        /**
         * The data this node represents
         */
        public final List<T> data;


        /**
         * A list of edges to other nodes
         */
        protected final Map<Chain<T>, WLink<Chain<T>>> edges = new LinkedHashMap();
        private final int hash;

        /**
         * Blank constructor for data-less nodes (the header or trailer)
         */
        public Chain() {
            this(Collections.emptyList());
        }

        /**
         * Constructor for node which will contain data.
         *
         * @param d the data this node should represent
         */
        public Chain(List<T> d) {
            this.data = d;
            this.hash = data.hashCode();
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            throw new UnsupportedOperationException();
        }

        /**
         * Get the data from the tuple at given position
         *
         * @param i the index of the data
         * @return data at index
         */
        public T get(int i) {
            return data.get(i);
        }
















































        public boolean isTerminal() {
            return data.isEmpty();
        }

        public void clear() {
            edges.clear();
        }

        /**
         * Returns this node's tuple's size.
         *
         * @return size of tuple represented by this node
         */
        public int length() {
            return data.size();
        }

        /**
         * Add more weight to the given node
         * or create an edge to that node if we didn't
         * already have one.
         *
         * @param n node to add more weight to
         * @return the node that was learned
         */
        public Chain<T> learn(final Chain<T> n, float strength) {
            
            WLink<Chain<T>> e = edges.computeIfAbsent(n, nn -> new WLink<>(nn, 0));
            e.priAdd(strength);
            return e.get();
        }

        /**
         * Randomly choose which is the next node to go to, or
         * return null if there are no edges.
         *
         * @return next node, or null if we could not choose a next node
         */
        protected Chain next(Random rng) {
            if (edges.isEmpty()) return null;
            return selectRoulette(rng, edges.values()).get();
        }





























        static <T> WLink<T> selectRoulette(Random RNG, Collection<WLink<T>> edges) {
            int s = edges.size();
            if (s == 0)
                return null;
            if (s == 1)
                return edges.iterator().next();

            
            float totalScore = 0;
            for (WLink e : edges)
                totalScore += e.pri();

            
            float r = RNG.nextFloat() * totalScore;

            
            int current = 0;

            
            for (WLink e : edges) {

                
                float dw = e.pri();

                if (r >= current && r < current + dw) {
                    return e;
                }

                
                current += dw;
            }

            
            return edges.iterator().next();
        }

    }

}
