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

import java.util.*;

/**
 * Базовый путь
 *
 * @param <N> Тип вершины
 * @param <E> Тип ребра
 * @author gocha
 */
public class BasicPath<N, E> extends AbstractPath<N, E> {
    protected final List<Edge<N, E>> list;
    private volatile Map<N, Integer> countMap;

    public BasicPath() {
        list = new ArrayList<>();
    }

    public BasicPath(BasicPath<N, E> sample) {
        super(sample);
        list = new ArrayList<>();
        if (sample != null && sample.list != null) {
            list.addAll(sample.list);
        }
    }

    @Override
    public boolean has(N a) {
        return count(a) > 0;
    }

    @Override
    public List<E> edges(int beginIndex, int endIndex) {
        if (beginIndex < 0) throw new IllegalArgumentException("beginIndex<0");
        if (endIndex < 0) throw new IllegalArgumentException("endIndex<0");

        List<E> l = new ArrayList<>();
        int ncnt = nodeCount();
        if (ncnt == 0) return l; //empty
        if (ncnt == 1) return l; //empty

        for (Edge<N, E> ed : fetch(beginIndex, endIndex)) {
            l.add(ed.getEdge());
        }

        return l;
    }

    @Override
    public List<Edge<N, E>> fetch(int beginIndex, int endIndex) {
        int ncnt = nodeCount();

        if (beginIndex < 0) {
            if ((ncnt + beginIndex) < 0) {
                beginIndex = 0;
            } else {
                beginIndex = ncnt + beginIndex;
            }
            //throw new IllegalArgumentException("beginIndex<0");
        }

        if (endIndex < 0) {
            if ((ncnt + endIndex) < 0) {
                endIndex = ncnt;
            } else {
                endIndex = ncnt + endIndex;
            }
        }

        List<Edge<N, E>> l = new ArrayList<>();

        if (ncnt == 0) return l; //empty
        if (ncnt == 1) return l; //empty

        int dir = endIndex - beginIndex;

        if (dir > 0) {
            for (int i = beginIndex; i < endIndex; i++) {
                if (i < list.size()) {
                    Edge<N, E> e = list.get(i);
                    if (e != null && e.getEdge() != null) l.add(e);
                }
            }
        } else if (dir < 0) {
            for (int i = Math.min(beginIndex, endIndex); i < Math.max(beginIndex, endIndex); i++) {
                if (i < list.size()) {
                    Edge<N, E> e = list.get(i);
                    if (e != null && e.getEdge() != null) {
                        l.add(0,
                                new DefaultGraphFactory.MutableEdge<>(
                                        e.getNodeB(), e.getNodeA(), e.getEdge()
                                )
                        );
                    }
                }
            }
        }
        return l;
    }

    @Override
    public BasicPath<N, E> start(N n) {
        if (n == null) throw new IllegalArgumentException("n == null");

        BasicPath<N, E> newPath = new BasicPath<>();
        newPath.list.add(new DefaultGraphFactory.MutableEdge<>(n, null, null));
        return newPath;
    }

    @Override
    public BasicPath<N, E> join(N n, E e) {
        if (n == null) throw new IllegalArgumentException("n == null");
        int ncnt = nodeCount();
        if (ncnt >= 1) {
            if (e == null) throw new IllegalArgumentException("e == null");
            if (ncnt == 1) {
                BasicPath<N, E> newPath = new BasicPath<>();
                newPath.list.add(
                        new DefaultGraphFactory.MutableEdge<>(node(0), n, e)
                );
                return newPath;
            } else {
                BasicPath<N, E> newPath = new BasicPath<>();
                newPath.list.addAll(this.list);

                N a = node(ncnt - 1);
                newPath.list.add(
                        new DefaultGraphFactory.MutableEdge<>(a, n, e)
                );

                return newPath;
            }
        } else {
            return start(n);
        }
    }

    @Override
    public BasicPath<N, E> clone() {
        return new BasicPath<>(this);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public BasicPath<N, E> clear() {
        return new BasicPath<>();
    }

    @Override
    public int count(N n) {
        if (n == null) return 0;
        int cnt = 0;
        for (int ni = 0; ni < nodeCount(); ni++) {
            N a = node(ni);
            if (n.equals(a)) {
                cnt++;
            }
        }
        return cnt;
    }

    private Set<N> nodeSet() {
        Set<N> nset = new HashSet<>();
        int nc = nodeCount();
        for (int ni = 0; ni < nc; ni++) {
            nset.add(node(ni));
        }
        return nset;
    }

    private Map<N, Integer> getCountMap() {
        if (countMap != null) return countMap;
        synchronized (this) {
            if (countMap != null) return countMap;
            countMap = new LinkedHashMap<>();
            for (N n : nodeSet()) {
                countMap.put(n, count(n));
            }
            return countMap;
        }
    }

    @Override
    public boolean hasCycles() {
        boolean has = false;
        for (Integer c : getCountMap().values()) {
            if (c != null && c > 1) {
                has = true;
                break;
            }
        }
        return has;
    }

    @Override
    public int nodeCount() {
        if (list == null) return 0;

        int lsize = list.size();
        if (lsize < 1) return 0;
        if (lsize == 1) {
            Edge<N, E> e = list.get(0);
            if (e.getEdge() != null && e.getNodeB() != null) return 2;
            return 1;
        }

        return lsize + 1;
    }

    @Override
    public N node(int nodeIndex) {
        int ncnt = nodeCount();
        if (nodeIndex < 0) {
            if ((ncnt + nodeIndex) < 0) return null;
            return node(ncnt + nodeIndex);
        }
        if (nodeIndex >= nodeCount()) return null;
        if (nodeIndex == 0) {
            Edge<N, E> edge = list.get(0);
            switch (direction) {
                case AB:
                    return edge.getNodeA();
                case BA:
                    return edge.getNodeB();
            }
        } else if (nodeIndex == 1) {
            Edge<N, E> edge = list.get(0);
            switch (direction) {
                case AB:
                    return edge.getNodeB();
                case BA:
                    return edge.getNodeA();
            }
        } else {
            Edge<N, E> edge = list.get(nodeIndex - 1);
            switch (direction) {
                case AB:
                    return edge.getNodeB();
                case BA:
                    return edge.getNodeA();
            }
        }
        return null;
    }
}
