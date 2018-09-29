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

import com.google.common.collect.Iterables;

import java.util.*;

/**
 * Однонаправленный граф, с использованием словарей (java.util.Map)
 *
 * @author gocha
 */
public class MappedSDGraph<N, E> implements SingleDirectedGraph<N, E> {
    /**
     * Словарь/Данные графа
     */
    protected Map<N, Map<N, E>> data = null;

    /**
     * Создает карту Вершина / Дуга
     *
     * @return Карта Вершина / Дуга
     */
    protected Map<N, E> createN2EMap() {
        return new TreeMap<>();
    }

    /**
     * Создает карту Вершина / (Вершина / Дуга)
     *
     * @return Карта Вершина / (Вершина / Дуга)
     */
    protected Map<N, Map<N, E>> createN2NEMap() {
        return new TreeMap<>();
    }

    /**
     * Создает ребро
     *
     * @param a Вершниа А
     * @param b Вершина Б
     * @param e Дуга
     * @return Дуга А - Б
     */
    protected Edge<N, E> createEdge(N a, N b, E e) {
        DefaultGraphFactory.MutableEdge<N, E> p = new DefaultGraphFactory.MutableEdge<>();
        p.setEdge(e);
        p.setNodeA(a);
        p.setNodeB(b);
        return p;
    }

    /**
     * Возвращает карту/данные Вершина / (Вершина / Дуга)
     *
     * @return Карта / Данные - Вершина / (Вершина / Дуга)
     */
    protected Map<N, Map<N, E>> getData() {
        if (data != null) return data;
        data = createN2NEMap();
        return data;
    }

    /**
     * Созадает коллекцию дуг
     *
     * @return Коллеция дуг
     */
    protected Collection<Edge<N, E>> createEdgePairs() {
        return new ArrayList<>();
    }

    /**
     * Проверяет на эквивалентность вершины
     *
     * @param a Вершина А
     * @param b Вершина Б
     * @return Эквивалентны вершины А и Б
     */
    protected boolean equals(N a, N b) {
        return a.equals(b);
    }

    /**
     * Создает коллекцию дуг
     *
     * @return Коллеция дуг
     */
    protected Collection<E> createEdges() {
        return new HashSet<>();
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public boolean contains(N node) {
        return getData().containsKey(node);
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public void add(N node) {
        if (node == null) {
            throw new IllegalArgumentException("node==null");
        }
        if (!contains(node)) {
            getData().put(node, createN2EMap());
        }
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public void remove(N node) {
        if (node == null) return;
        if (getData().containsKey(node)) {
            for (Map.Entry<N, Map<N, E>> e : getData().entrySet()) {
                e.getValue().remove(node);
            }
            getData().remove(node);
        }
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public Iterable<N> getNodes() {
        return getData().keySet();
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public Iterable<Edge<N, E>> getEdges() {
        Collection<Edge<N, E>> result = createEdgePairs();
        for (Map.Entry<N, Map<N, E>> e : getData().entrySet()) {
            for (Map.Entry<N, E> e2 : e.getValue().entrySet()) {
                result.add(createEdge(e.getKey(), e2.getKey(), e2.getValue()));
            }
        }
        return result;
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public Iterable<Edge<N, E>> edgesOf(N node) {
        Collection<Edge<N, E>> result = createEdgePairs();

        for (Map.Entry<N, Map<N, E>> _e : getData().entrySet()) {
            Map<N, E> ma = _e.getValue();
            E e = ma.get(node);
            if (e != null) {
                result.add(createEdge(_e.getKey(), node, e));
            }
        }

        Map<N, E> ma = getData().get(node);
        for (Map.Entry<N, E> _e : ma.entrySet()) {
            result.add(createEdge(node, _e.getKey(), _e.getValue()));
        }

        return result;
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public Iterable<Edge<N, E>> edgesOfNodeA(N nodeA) {
        if (nodeA == null) return Collections::emptyIterator;

        Map<N, E> ma = getData().get(nodeA);
        if (ma == null) return Collections::emptyIterator;

        Collection<Edge<N, E>> result = createEdgePairs();

//        for( Map.Entry<N,Map<N,E>> e : getData().entrySet() ){
        for (Map.Entry<N, E> e2 : ma.entrySet()) {
            result.add(createEdge(nodeA, e2.getKey(), e2.getValue()));
        }
//        }
        return result;
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public Iterable<Edge<N, E>> edgesOfNodeB(N nodeB) {
        if (nodeB == null) return Collections::emptyIterator;

        Collection<Edge<N, E>> result = createEdgePairs();
        for (Map.Entry<N, Map<N, E>> _e : getData().entrySet()) {
            Map<N, E> mb = _e.getValue();

            E e = mb.get(nodeB);
            if (e != null) {
                Edge<N, E> ep = createEdge(_e.getKey(), nodeB, e);
                result.add(ep);
            }
        }
        return result;
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public void clearEdges() {
        for (Map.Entry<N, Map<N, E>> e : getData().entrySet()) {
            e.getValue().clear();
        }
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public void clearAll() {
        for (Map.Entry<N, Map<N, E>> e : getData().entrySet()) {
            e.getValue().clear();
        }
        getData().clear();
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public boolean hasEdge(N a, N b) {
        if (a == null) return false;
        if (b == null) return false;
        Map<N, E> ma = getData().get(a);
        return ma != null && ma.containsKey(b);
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public void removeEdge(N a, N b) {
        if (a == null) return;
        if (b == null) return;

        Map<N, E> ma = getData().get(a);
        if (ma == null) return;

        ma.remove(b);
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public E getEdge(N a, N b) {
        if (a == null) return null;
        if (b == null) return null;

        Map<N, E> ma = getData().get(a);
        if (ma == null) return null;

        return ma.get(b);
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public void setEdge(N a, N b, E edge) {
        if (a == null)
            throw new IllegalArgumentException("a==null");
        if (b == null)
            throw new IllegalArgumentException("b==null");

        if (!contains(a)) add(a);
        if (!contains(b)) add(b);

        Map<N, E> ma = getData().get(a);
        if (edge == null) {
            if (ma != null) ma.remove(b);
        } else {
            if (ma != null) ma.put(b, edge);
        }
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public Iterable<E> getEdges(N a, N b) {
        if (a == null || b == null) return Collections::emptyIterator;

        Map<N, E> ma = getData().get(a);
        if (ma == null) return Collections::emptyIterator;

        E e = ma.get(b);
        if (e == null) return Collections::emptyIterator;
        Collection<E> c = createEdges();
        c.add(e);
        return c;
    }

    /* (non-Javadoc) @see Graph */
    @Override
    public void setEdges(N a, N b, Iterable<E> edges) {
        if (a == null) {
            throw new IllegalArgumentException("a==null");
        }
        if (b == null) {
            throw new IllegalArgumentException("b==null");
        }
        if (edges == null) {
            setEdge(a, b, null);
        } else {
            if (!contains(a)) add(a);
            if (!contains(b)) add(b);

            Iterable<E> _e = Iterables.filter(edges, Objects::nonNull);

            long c = Iterables.size(_e);
            if (c == 0) {
                Map<N, E> ma = getData().get(a);
                ma.remove(b);
            } else {
                Map<N, E> ma = getData().get(a);
                int i = -1;
                for (E e : _e) {
                    i++;
                    ma.put(b, e);
                    if (i >= 0) break;
                }
            }
        }
    }

    /* (non-Javadoc) @see SingleGraph */
    @Override
    public E get(N a, N b) {
        return getEdge(a, b);
    }

    /* (non-Javadoc) @see SingleGraph */
    @Override
    public void set(N a, N b, E e) {
        setEdge(a, b, e);
    }
}
