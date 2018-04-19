package spacegraph.space2d;

import com.google.common.collect.Iterables;
import com.google.common.graph.Graph;
import com.google.common.graph.SuccessorsFunction;
import jcog.data.graph.MapNodeGraph;
import spacegraph.space2d.container.grid.MutableMapContainer;
import spacegraph.space2d.widget.button.PushButton;

import java.util.List;
import java.util.function.Function;

/** 2D directed/undirected graph widget */
public class Graph2D<X> extends MutableMapContainer<X,X> {

    public Graph2D() {
        
    }

    @Override
    protected void doLayout(int dtMS) {

        super.doLayout(dtMS);
    }

    /** adapts guava Graph as input */
    public Graph2D<X> commit(Graph<X> g) {
        return commit(g.nodes(), g::successors);
    }

    public Graph2D<X> commit(SuccessorsFunction<X> g, X start) {
        return commit(g, List.of(start));
    }

    public Graph2D<X> commit(SuccessorsFunction<X> s, Iterable<X> start) {
        return commit(new MapNodeGraph<>(s, start));
    }

    public Graph2D<X> commit(MapNodeGraph<X,Object> g) {
        return commit(
                Iterables.transform(g.nodes(), x-> x.id),
                x-> Iterables.transform(
                        g.node(x).edges(false,true),
                        //zz -> zz.id //edge label
                        zz -> zz.to.id //edge target
                ));
    }

    public Graph2D<X> commit(Iterable<X> nodes, Function<X,Iterable<X>> edges) {
        return update(nodes, edges, false);
    }

    public Graph2D<X> update(Iterable<X> nodes, Function<X,Iterable<X>> edges, boolean addOrReplace ) {

        nodes.forEach((x)->{
            //g.nodes().forEach(x -> {
            //HACK todo use proxyterms in a cache
            //c.termlinks().clear();

            //TODO computeIfAbsent and re-use existing model
            put(x, x, (xx,xxx)->render(xx));

//            edges.apply(x).forEach((X edge) ->
//                    //g.successors(x).forEach((Term y) ->
//                    src.edges.add(new EDraw<>(
//                            src,
//                            //cache.getOrAdd(edge, DefaultSpaceWidget::new)
//                            cache.computeIfAbsent(edge, Graph2D.DefaultSpaceWidget::new)
//                            , 0.5f))
//            );

        });

        return this;
    }

    private Surface render(X vertex) {
        return new PushButton(vertex.toString());
    }


}
