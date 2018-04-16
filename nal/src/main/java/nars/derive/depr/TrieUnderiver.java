//package nars.derive;
//
//import com.google.common.collect.MultimapBuilder;
//import com.google.common.collect.SetMultimap;
//import nars.Task;
//import nars.premise.Premise;
//import nars.term.Term;
//import nars.util.graph.Underiver;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.jgrapht.Graph;
//import org.jgrapht.alg.util.Pair;
//import org.jgrapht.graph.SimpleDirectedGraph;
//
//import java.util.function.Consumer;
//
//
//public class TrieUnderiver implements Underiver {
//
//    @NotNull
//    private final TrieDeriver deriver;
//
//    static class CauseEdge extends Pair<Term,Term> {
//
//        public CauseEdge(Term from, Term to) {
//            super(from, to);
//        }
//
//        @NotNull
//        @Override
//        public String toString() {
//            //return '(' + this.first.toString() + ',' + this.second.toString() + ')';
//            return '(' + this.first.getClass().toString() + ',' + this.second.getClass().toString() + ')';
//        }
//    }
//
//    @NotNull
//    final SimpleDirectedGraph<Term,CauseEdge> g;
//
//    final SetMultimap<Term /*Pattern*/, Term /*Incmoing Condition*/> conclusion =
//            MultimapBuilder.linkedHashKeys().linkedHashSetValues().builder();
//
//    public TrieUnderiver(@NotNull TrieDeriver t) {
//        this.deriver = t;
//        g = new SimpleDirectedGraph<>(CauseEdge::new);
//
//        //traverse the derivation trie to a graph (DAG)
//        t.recurse((a, b) -> {
//            g.addVertex(b);
//
//
//            if (a!=null) {
//                g.addVertex(a);
//                g.addEdge(a, b);
//            }
//        });
//
//        g.edgeSet().forEach(System.out::println);
//        System.out.println(g.vertexSet().size() + " vertices, " + g.edgeSet().size() + " edges");
//
//        conclusion.asMap().forEach((k,v) -> {
//            System.out.println(k + ":\t" + v);
//        });
//
//    }
//
//    @Nullable
//    public Graph<Term,Term> causesOf(Term derivedPattern) {
//        //entire incoming subgraph to a target
//        return null;
//    }
//
//    //TODO jgraphT: AllDirectedPaths
//    //List<GraphPath<V,E>>	getAllPaths(Set<V> sourceVertices, Set<V> targetVertices, boolean simplePathsOnly, Integer maxPathLength)
//
//    @Override
//    public void underive(Task conclusion, Consumer<Premise> eachPossiblePremise) {
//
//    }
//
//    @NotNull
//    @Override
//    public Deriver deriver() {
//        return deriver;
//    }
//
//    public static void main(String[] args) {
//        new TrieUnderiver((TrieDeriver)Deriver.get("nal.nal"));
//    }
//}
