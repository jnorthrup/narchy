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

import java.util.Collection;
import java.util.List;

/**
 * Однонаправленный граф. Примитивная реализация с переборными алгоритмами.
 *
 * @param <N> Тип вершины
 * @param <E> Тип ребра
 * @author GoCha
 */
public class SimpleSDGraph<N, E> implements SingleDirectedGraph<N, E> {
    /**
     * Фабрика
     */
    protected GraphFactory<N, E> factory;

    /**
     * Список ребр
     */
    protected Collection<Edge<N, E>> edges;

    /**
     * Список вершин
     */
    protected Collection<N> nodes;

    /**
     * Конструктор
     */
    public SimpleSDGraph() {
        this(new DefaultGraphFactory<>());
    }

    /**
     * Конструктор
     *
     * @param factory Фабрика графа
     */
    public SimpleSDGraph(GraphFactory<N, E> factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory == null");
        }
        this.factory = factory;

        edges = factory.createEdgePairs();
        nodes = factory.createNodes();
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#getNodes
     */
    @Override
    public Iterable<N> getNodes() {
        return nodes;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#getEdges
     */
    @Override
    public Iterable<Edge<N, E>> getEdges() {
        return edges;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#getEdges
     */
    @Override
    public List<E> getEdges(N a, N b) {
        if (a == null) {
            throw new IllegalArgumentException("a == null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b == null");
        }

        E e = get(a, b);
        if (e != null) return List.of(e);
        return List.of();
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#edgesOf
     */
    @Override
    public Collection<Edge<N, E>> edgesOf(N node) {
        if (node == null) {
            throw new IllegalArgumentException("node == null");
        }

        Collection<Edge<N, E>> list = factory.createEdgePairs();
        for (Edge<N, E> e : edges) {
            if (e.getNodeA().equals(node) || e.getNodeB().equals(node)) list.add(e);
        }
        return list;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#edgesOfNodeA
     */
    @Override
    public Collection<Edge<N, E>> edgesOfNodeA(N node) {
        if (node == null) {
            throw new IllegalArgumentException("node == null");
        }

        Collection<Edge<N, E>> list = factory.createEdgePairs();
        for (Edge<N, E> e : edges) {
            if (e.getNodeA().equals(node)) list.add(e);
        }
        return list;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#edgesOfNodeB
     */
    @Override
    public Collection<Edge<N, E>> edgesOfNodeB(N node) {
        if (node == null) {
            throw new IllegalArgumentException("node == null");
        }

        Collection<Edge<N, E>> list = factory.createEdgePairs();
        for (Edge<N, E> e : edges) {
            if (e.getNodeB().equals(node)) list.add(e);
        }
        return list;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#hasEdge
     */
    @Override
    public boolean hasEdge(N a, N b) {
        if (a == null) {
            throw new IllegalArgumentException("a == null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b == null");
        }

        for (Edge<N, E> e : edges) {
            if (e.getNodeA().equals(a) && e.getNodeB().equals(b)) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#getEdge
     */
    @Override
    public E getEdge(N a, N b) {
        if (a == null) {
            throw new IllegalArgumentException("a == null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b == null");
        }

        return get(a, b);
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#contains
     */
    @Override
    public boolean contains(N node) {
        if (node == null) {
            throw new IllegalArgumentException("node == null");
        }

        return indexOf(node) >= 0;
    }

    /**
     * Возвращает индекс вершины в списке вершин
     *
     * @param node Вершина
     * @return индекс или -1 если таковой нет в списке
     */
    protected int indexOf(N node) {
        if (node == null) {
            throw new IllegalArgumentException("node == null");
        }

        int idx = -1;
        for (N n : nodes) {
            idx++;
            if (n.equals(node)) return idx;
        }
        return -1;
    }

    /**
     * Срабатывает при удалении вершины из графа
     *
     * @param node Вершина
     */
    protected void onNodeRemoved(N node) {
    }

    /**
     * Срабатывает при удалении ребра из графа
     *
     * @param es Ребро
     */
    protected void onEdgeRemoved(Edge<N, E> es) {
    }

    /**
     * Срабатывает при добавлении вершины в граф
     *
     * @param node Вершина
     */
    protected void onNodeAdded(N node) {
    }

    /**
     * Срабатывает при добавлении ребра в граф
     *
     * @param es Ребро
     */
    protected void onEdgeAdded(Edge<N, E> es) {
    }

    /**
     * Удаление ребра
     *
     * @param es Ребро
     */
    protected void remove(Edge<N, E> es) {
        if (es == null) {
            throw new IllegalArgumentException("es == null");
        }

        edges.remove(es);
        onEdgeRemoved(es);
    }

    /**
     * Добавление ребра
     *
     * @param es Ребро
     */
    protected void add(Edge<N, E> es) {
        if (es == null) {
            throw new IllegalArgumentException("es == null");
        }

        edges.add(es);
        onEdgeAdded(es);
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#addAt
     */
    @Override
    public void add(N node) {
        if (node == null) {
            throw new IllegalArgumentException("node == null");
        }

        if (!contains(node)) {
            nodes.add(node);
            onNodeAdded(node);
        }
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#remove
     */
    @Override
    public void remove(N node) {
        if (node == null) {
            throw new IllegalArgumentException("node == null");
        }

        Collection<Edge<N, E>> list = factory.createEdgePairs();

        for (Edge<N, E> edge : edges) {
            if (edge.getNodeA().equals(node) || edge.getNodeB().equals(node)) {
                list.add(edge);
            }
        }

        for (Edge<N, E> edge : list) {
            remove(edge);
        }

        N oldNode = null;
        for (N n : nodes) {
            if (n.equals(node)) {
                oldNode = n;
                break;
            }
        }
        if (oldNode != null) {
            nodes.remove(oldNode);
            onNodeRemoved(oldNode);
        }
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#clearEdges
     */
    @Override
    public void clearEdges() {
        Collection<Edge<N, E>> oldEdges = edges;
        edges = factory.createEdgePairs();
        for (Edge<N, E> e : oldEdges) onEdgeRemoved(e);
        oldEdges.clear();
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#clearAll
     */
    @Override
    public void clearAll() {
        Collection<Edge<N, E>> oldEdges = edges;
        edges = factory.createEdgePairs();
        for (Edge<N, E> e : oldEdges) onEdgeRemoved(e);
        oldEdges.clear();

        Collection<N> oldNodes = nodes;
        nodes = factory.createNodes();
        for (N n : oldNodes) onNodeRemoved(n);
        oldNodes.clear();
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.SingleGraph#get
     */
    @Override
    public E get(N a, N b) {
        if (a == null) {
            throw new IllegalArgumentException("a == null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b == null");
        }

        for (Edge<N, E> e : edges) {
            if (e.getNodeA().equals(a) && e.getNodeB().equals(b)) {
                return e.getEdge();
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.SingleGraph#setAt
     */
    @Override
    public void set(N a, N b, E e) {
        if (e == null) {
            throw new IllegalArgumentException("e == null");
        }
        if (a == null) {
            throw new IllegalArgumentException("a == null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b == null");
        }

        if (!contains(a)) add(a);
        if (!contains(b)) add(b);

        removeEdge(a, b);

        Edge<N, E> newESet = factory.createEdge(a, b, e);
        add(newESet);
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#setEdge
     */
    @Override
    public void setEdge(N a, N b, E edge) {
        if (a == null) {
            throw new IllegalArgumentException("a == null");
        }
        if (edge == null) {
            throw new IllegalArgumentException("edge == null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b == null");
        }

        set(a, b, edge);
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#setEdges
     */
    @Override
    public void setEdges(N a, N b, Iterable<E> edges) {
        if (edges == null) {
            throw new IllegalArgumentException("edges == null");
        }
        if (a == null) {
            throw new IllegalArgumentException("a == null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b == null");
        }

        removeEdge(a, b);

        if (edges != null) {
            for (E e : edges) {
                set(a, b, e);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#removeEdge
     */
    @Override
    public void removeEdge(N a, N b) {
        if (a == null) {
            throw new IllegalArgumentException("a == null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b == null");
        }

        Collection<Edge<N, E>> oldEdgeslist = factory.createEdgePairs();
        for (Edge<N, E> e : edges) {
            if (e.getNodeA().equals(a) && e.getNodeB().equals(b)) {
                oldEdgeslist.add(e);
            }
        }

        for (Edge<N, E> e : oldEdgeslist) {
            remove(e);
        }
    }
}
