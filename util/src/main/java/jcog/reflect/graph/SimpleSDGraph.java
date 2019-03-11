/*
 * The MIT License
 *
 * Copyright 2014 Kamnev Georgiy (nt.gocha@gmail.com).
 *
 * Данная лицензия разрешает, безвозмездно, лицам, получившим копию данного программного
 * обеспечения и сопутствующей документации (в дальнейшем именуемыми "Программное Обеспечение"),
 * использовать Программное Обеспечение без ограничений, включая неограниченное право на
 * использование, копирование, изменение, объединение, публикацию, распространение, сублицензирование
 * и/или продажу копий Программного Обеспечения, также как и лицам, которым предоставляется
 * данное Программное Обеспечение, при соблюдении следующих условий:
 *
 * Вышеупомянутый копирайт и данные условия должны быть включены во все копии
 * или значимые части данного Программного Обеспечения.
 *
 * ДАННОЕ ПРОГРАММНОЕ ОБЕСПЕЧЕНИЕ ПРЕДОСТАВЛЯЕТСЯ «КАК ЕСТЬ», БЕЗ ЛЮБОГО ВИДА ГАРАНТИЙ,
 * ЯВНО ВЫРАЖЕННЫХ ИЛИ ПОДРАЗУМЕВАЕМЫХ, ВКЛЮЧАЯ, НО НЕ ОГРАНИЧИВАЯСЬ ГАРАНТИЯМИ ТОВАРНОЙ ПРИГОДНОСТИ,
 * СООТВЕТСТВИЯ ПО ЕГО КОНКРЕТНОМУ НАЗНАЧЕНИЮ И НЕНАРУШЕНИЯ ПРАВ. НИ В КАКОМ СЛУЧАЕ АВТОРЫ
 * ИЛИ ПРАВООБЛАДАТЕЛИ НЕ НЕСУТ ОТВЕТСТВЕННОСТИ ПО ИСКАМ О ВОЗМЕЩЕНИИ УЩЕРБА, УБЫТКОВ
 * ИЛИ ДРУГИХ ТРЕБОВАНИЙ ПО ДЕЙСТВУЮЩИМ КОНТРАКТАМ, ДЕЛИКТАМ ИЛИ ИНОМУ, ВОЗНИКШИМ ИЗ, ИМЕЮЩИМ
 * ПРИЧИНОЙ ИЛИ СВЯЗАННЫМ С ПРОГРАММНЫМ ОБЕСПЕЧЕНИЕМ ИЛИ ИСПОЛЬЗОВАНИЕМ ПРОГРАММНОГО ОБЕСПЕЧЕНИЯ
 * ИЛИ ИНЫМИ ДЕЙСТВИЯМИ С ПРОГРАММНЫМ ОБЕСПЕЧЕНИЕМ.
 */
package jcog.reflect.graph;

import jcog.data.graph.MapNodeGraph;

/**
 * Однонаправленный граф. Примитивная реализация с переборными алгоритмами.
 *
 * @param <N> Тип вершины
 * @param <E> Тип ребра
 * @author GoCha
 */
public class SimpleSDGraph<N, E> extends MapNodeGraph<N,E> {

    public SimpleSDGraph() {
        super();
    }

    public final void set(N a, N b, E e) {
        addEdge(a, e, b);
    }

    @Override
    public boolean addEdge(N from, E data, N to) {
        return addEdge(addNode(from), data, addNode(to));
    }

//    /**
//     * Срабатывает при удалении ребра из графа
//     *
//     * @param es Ребро
//     */
//    protected void onEdgeRemoved(FromTo<jcog.data.graph.Node<N,E>,E> es) {
//    }
//
//
//    /**
//     * Срабатывает при добавлении ребра в граф
//     *
//     * @param es Ребро
//     */
//    protected void onEdgeAdded(FromTo<jcog.data.graph.Node<N,E>,E> es) {
//    }

//    /**
//     * Удаление ребра
//     *
//     * @param es Ребро
//     */
//    protected void remove(FromTo<jcog.data.graph.Node<N,E>,E> es) {
//        edgeRemove(es);
//        onEdgeRemoved(es);
//    }

//    /* (non-Javadoc)
//     * @see org.gocha.collection.graph.IGraph#addAt
//     */
//    @Override
//    public void add(N node) {
//        if (node == null) {
//            throw new IllegalArgumentException("node == null");
//        }
//
//        if (!contains(node)) {
//            nodes.add(node);
//            onNodeAdded(node);
//        }
//    }

//    /* (non-Javadoc)
//     * @see org.gocha.collection.graph.IGraph#remove
//     */
//    @Override
//    public void remove(N node) {
//        if (node == null) {
//            throw new IllegalArgumentException("node == null");
//        }
//
//        Collection<Edge<N, E>> list = factory.createEdgePairs();
//
//        for (Edge<N, E> edge : edges) {
//            if (edge.getNodeA().equals(node) || edge.getNodeB().equals(node)) {
//                list.add(edge);
//            }
//        }
//
//        for (Edge<N, E> edge : list) {
//            remove(edge);
//        }
//
//        N oldNode = null;
//        for (N n : nodes) {
//            if (n.equals(node)) {
//                oldNode = n;
//                break;
//            }
//        }
//        if (oldNode != null) {
//            nodes.remove(oldNode);
//            onNodeRemoved(oldNode);
//        }
//    }

//    /* (non-Javadoc)
//     * @see org.gocha.collection.graph.IGraph#clearEdges
//     */
//    @Override
//    public void clearEdges() {
//        Collection<Edge<N, E>> oldEdges = edges;
//        edges = factory.createEdgePairs();
//        for (Edge<N, E> e : oldEdges) onEdgeRemoved(e);
//        oldEdges.clear();
//    }
//
//    /* (non-Javadoc)
//     * @see org.gocha.collection.graph.IGraph#clearAll
//     */
//    @Override
//    public void clearAll() {
//        Collection<Edge<N, E>> oldEdges = edges;
//        edges = factory.createEdgePairs();
//        for (Edge<N, E> e : oldEdges) onEdgeRemoved(e);
//        oldEdges.clear();
//
//        Collection<N> oldNodes = nodes;
//        nodes = factory.createNodes();
//        for (N n : oldNodes) onNodeRemoved(n);
//        oldNodes.clear();
//    }
//
//    /* (non-Javadoc)
//     * @see org.gocha.collection.graph.SingleGraph#get
//     */
//    public E get(N a, N b) {
//        return node(a).edges(false,true).
////        if (a == null) {
////            throw new IllegalArgumentException("a == null");
////        }
////        if (b == null) {
////            throw new IllegalArgumentException("b == null");
////        }
////
////        for (FromTo<Node<N, E>,E> e : edges()) {
////            if (e.from().equals(a) && e.to().equals(b)) {
////                return e.getEdge();
////            }
////        }
////        return null;
//    }


//    /* (non-Javadoc)
//     * @see org.gocha.collection.graph.IGraph#setEdges
//     */
//    @Override
//    public void setEdges(N a, N b, Iterable<E> edges) {
//        if (edges == null) {
//            throw new IllegalArgumentException("edges == null");
//        }
//        if (a == null) {
//            throw new IllegalArgumentException("a == null");
//        }
//        if (b == null) {
//            throw new IllegalArgumentException("b == null");
//        }
//
//        removeEdge(a, b);
//
//        if (edges != null) {
//            for (E e : edges) {
//                set(a, b, e);
//            }
//        }
//    }
//
//    /* (non-Javadoc)
//     * @see org.gocha.collection.graph.IGraph#removeEdge
//     */
//    @Override
//    public void removeEdge(N a, N b) {
//        if (a == null) {
//            throw new IllegalArgumentException("a == null");
//        }
//        if (b == null) {
//            throw new IllegalArgumentException("b == null");
//        }
//
//        Collection<Edge<N, E>> oldEdgeslist = factory.createEdgePairs();
//        for (Edge<N, E> e : edges) {
//            if (e.getNodeA().equals(a) && e.getNodeB().equals(b)) {
//                oldEdgeslist.add(e);
//            }
//        }
//
//        for (Edge<N, E> e : oldEdgeslist) {
//            remove(e);
//        }
//    }
}
