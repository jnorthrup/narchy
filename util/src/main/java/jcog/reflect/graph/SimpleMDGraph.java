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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Направленый мультграф граф
 *
 * @param <N> Тип вершины графа
 * @param <E> Тип ребра между вершинами
 * @author GoCha
 */
public class SimpleMDGraph<N, E> implements MultipleDirectedGraph<N, E> {
    /**
     * Объект для синхронизации
     */
    protected final Object sync;
    /**
     * Фабрика графа
     */
    protected GraphFactory<N, E> factory;
    /**
     * Ребра
     */
    protected Collection<Edge<N, E>> edges;
    /**
     * Вершины
     */
    private Collection<N> nodes;

    /**
     * Конструктор
     */
    public SimpleMDGraph() {
        this(new DefaultGraphFactory<>());
    }

    /**
     * Конструктор
     *
     * @param factory Фабрика графа
     */
    public SimpleMDGraph(GraphFactory<N, E> factory) {
        this.sync = this;

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
     * @see org.gocha.collection.graph.IGraph#contains
     */
    @Override
    public boolean contains(N node) {
        if (node == null) {
            throw new IllegalArgumentException("node == null");
        }

        synchronized (sync) {
            return indexOf(node) >= 0;
        }
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

        synchronized (sync) {
            int idx = -1;
            for (N n : nodes) {
                idx++;
                if (n.equals(node)) return idx;
            }
            return -1;
        }
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

        synchronized (sync) {
            for (E e : get(a, b)) {
                return e;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#getEdges
     */
    @Override
    public Iterable<E> getEdges(N a, N b) {
        if (a == null) {
            throw new IllegalArgumentException("a == null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b == null");
        }

        synchronized (sync) {
            Iterable<E> en = get(a, b);
            if (en == null) return new ArrayList<>();
            return en;
        }
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

        synchronized (sync) {
            Collection<E> en = get(a, b);
            if (en == null) return false;
            return !en.isEmpty();
        }
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
            if (e.getNodeA().equals(node) || e.getNodeB().equals(node))
                list.add(e);
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
            if (e.getNodeA().equals(node))
                list.add(e);
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
            if (e.getNodeB().equals(node))
                list.add(e);
        }
        return list;
    }

    /**
     * Срабатывает при удалении ребра из графа
     *
     * @param es Ребро
     */
    protected void onEdgeRemoved(Edge<N, E> es) {
    }

    /**
     * Срабатывает при добавлении ребра в граф
     *
     * @param es Ребро
     */
    protected void onEdgeAdded(Edge<N, E> es) {
    }

    /**
     * Срабатывает при добавлении вершины в граф
     *
     * @param node Вершина
     */
    protected void onNodeAdded(N node) {
    }

    /**
     * Срабатывает при удалении вершины из графа
     *
     * @param node Вершина
     */
    protected void onNodeRemoved(N node) {
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.MultiGraph#get
     */
    @Override
    public Collection<E> get(N a, N b) {
        if (a == null) {
            throw new IllegalArgumentException("a == null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b == null");
        }

        Collection<E> list = factory.createEdges();
        for (Edge<N, E> e : edges) {
            if (e.getNodeA().equals(a) && e.getNodeB().equals(b)) {
                list.add(e.getEdge());
            }
        }
        return list;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.MultiGraph#set
     */
    @Override
    public void set(N a, N b, Iterable<E> e) {
        if (a == null) {
            throw new IllegalArgumentException("a == null");
        }
        if (e == null) {
            throw new IllegalArgumentException("e == null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b == null");
        }

        if (!contains(a)) add(a);
        if (!contains(b)) add(b);

        Collection<Edge<N, E>> oldEdgeslist = factory.createEdgePairs();
        for (Edge<N, E> ed : edges) {
            if (ed.getNodeA().equals(a) && ed.getNodeB().equals(b)) {
                oldEdgeslist.add(ed);
            }
        }

        for (Edge<N, E> ed : oldEdgeslist) {
            remove(ed);
        }

        for (E ed : e) {
            if (ed != null) {
                Edge<N, E> newESet = factory.createEdge(a, b, ed);
                add(newESet);
            }
        }
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
     * @see org.gocha.collection.graph.IGraph#add
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

//        int i = indexOf(node);
//        if (i >= 0)
//        {
//            Node oldNode = nodeList.get(i);
//            nodeList.remove(i);
//            onNodeRemoved(oldNode);
//        }

        N oldNode = null;
        for (N n : nodes) {
            if (n.equals(node)) {
                oldNode = n;
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
        for (Edge<N, E> e : edges) remove(e);
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#clearAll
     */
    @Override
    public void clearAll() {
        for (Edge<N, E> e : edges) remove(e);
        for (N n : nodes) remove(n);
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

        List<E> l = new ArrayList<>();
        l.add(edge);
        set(a, b, l);
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.IGraph#setEdges
     */
    @Override
    public void setEdges(N a, N b, Iterable<E> edges) {
        if (a == null) {
            throw new IllegalArgumentException("a == null");
        }
        if (edges == null) {
            throw new IllegalArgumentException("edges == null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b == null");
        }

        set(a, b, edges);
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
        for (Edge<N, E> ed : edges) {
            if (ed.getNodeA().equals(a) && ed.getNodeB().equals(b)) {
                oldEdgeslist.add(ed);
            }
        }

        for (Edge<N, E> ed : oldEdgeslist) {
            remove(ed);
        }
    }
}
