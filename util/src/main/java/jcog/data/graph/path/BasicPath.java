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
package jcog.data.graph.path;

import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.Node;
import jcog.data.graph.edge.ImmutableDirectedEdge;
import jcog.data.list.FasterList;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Базовый путь
 *
 * @param <N> Тип вершины
 * @param <E> Тип ребра
 * @author gocha
 */
public class BasicPath<N, E> extends AbstractPath<N, E> {
    protected final List<FromTo<Node<N, E>, E>> list;
    private ObjectIntHashMap<N> countMap;
    final N start;

    public BasicPath(MapNodeGraph<N, E> graph, N start) {
        super(graph);
        this.list = new FasterList<>(0);
        this.start = start;
    }

    public BasicPath<N, E> clone() {
        BasicPath p = new BasicPath(graph, start);
        p.list.addAll(list);
        return p;
    }

    @Override
    public boolean has(N a) {
        return count(a) > 0;
    }

    @Override
    public List<E> edges(int beginIndex, int endIndex) {
        if (beginIndex < 0) throw new IllegalArgumentException("beginIndex<0");
        if (endIndex < 0) throw new IllegalArgumentException("endIndex<0");

        int ncnt = nodeCount();
        if (ncnt == 0 || ncnt == 1) return Collections.EMPTY_LIST; //empty


        List<E> l = new ArrayList<>();
        for (FromTo<Node<N, E>, E> nodeEFromTo : fetch(beginIndex, endIndex)) {
            E id = nodeEFromTo.id();
            l.add(id);
        }

        return l;
    }

    @Override
    public List<FromTo<Node<N, E>, E>> fetch(int from, int to) {
        int ncnt = nodeCount();

        if (from < 0) from = Math.max((ncnt + from), 0);

        if (to < 0) to = (ncnt + to) < 0 ? ncnt : ncnt + to;

        to = Math.min(list.size(), to);

        if (ncnt == 0 || ncnt == 1) return Collections.EMPTY_LIST; //empty

        int dir = to - from;

        List<FromTo<Node<N, E>, E>> l = new FasterList<>(to - from);

        if (dir > 0) {
            for (int i = from; i < to; i++) {
                FromTo<Node<N, E>, E> e = list.get(i);
                if (e != null && e.id() != null)
                    l.add(e);
            }
        } else if (dir < 0) {
            for (int i = Math.min(from, to); i < Math.max(from, to); i++) {
                FromTo<Node<N, E>, E> e = list.get(i);
                if (e != null && e.id() != null) {
                    l.add(0, new ImmutableDirectedEdge<>(e.to(), e.id(), e.from()));
                }
            }
        }
        return l;
    }

    @Override
    public BasicPath<N, E> spawn(N n) {
        return new BasicPath<>(graph, n);
    }

    protected ImmutableDirectedEdge edge(N f, E e, N t) {
        ImmutableDirectedEdge<N, E> z = new ImmutableDirectedEdge(graph.addNode(f), e, graph.addNode(t));
        graph.addEdge(z);
        return z;
    }

    @Override
    public BasicPath<N, E> append(E e, N n) {
        if (n == null) throw new IllegalArgumentException("n == null");
        int ncnt = nodeCount();
        BasicPath<N, E> newPath = new BasicPath<N, E>(graph, start);
        N last;
        if (ncnt == 0) {
            last = start;
        } else {
            last = node(ncnt - 1);
            newPath.list.addAll(this.list);
        }
        newPath.list.add(edge(last, e, n));
        return newPath;
    }


    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public BasicPath<N, E> clear() {
        return new BasicPath<>(graph, start);
    }

    @Override
    public int count(N n) {
        if (n == null) return 0;
        int s = nodeCount();
        long count = 0L;
        for (int ni = 0; ni < s; ni++) {
            if (n.equals(node(ni))) {
                count++;
            }
        }
        int cnt = (int) count;

        return cnt;
    }

    private Set<N> nodeSet() {
        int nc = nodeCount();
        Set<N> nset = new UnifiedSet<>(nc);
        for (int ni = 0; ni < nc; ni++) {
            nset.add(node(ni));
        }
        return nset;
    }

    private ObjectIntHashMap<N> getCountMap() {
//        if (countMap != null) return countMap;
        //synchronized (this) {
            if (countMap != null) return countMap;

        Set<N> ns = nodeSet();
            countMap = new ObjectIntHashMap<>(ns.size());
            for (N n : ns)
                countMap.put(n, count(n));

            return countMap;
        //}
    }

    @Override
    public boolean hasCycles() {
        return getCountMap().values().anySatisfy(x -> x > 1);
//        MutableIntIterator vv = getCountMap().values().intIterator();
//        while (vv.hasNext()) {
//            int c = vv.next();
//            if (c > 1)
//                return true;
//        }
//        return false;
    }

    @Override
    public int nodeCount() {
        if (list == null) return 0;
        int lsize = list.size();
        if (lsize < 1)
            return 0;
        else if (lsize == 1) {
            FromTo<Node<N, E>, E> e = list.get(0);
            return ((e.id() != null) && (e.to().id() != null)) ? 2 : 1;
        } else
            return lsize + 1; //???
    }

    @Override
    public N node(int nodeIndex) {
        int ncnt = nodeCount();
        if (nodeIndex < 0) {
            return (ncnt + nodeIndex) < 0 ? null : node(ncnt + nodeIndex);
        }
//        if (nodeIndex >= nodeCount())
//            return null;
        switch (nodeIndex) {
            case 0:
                return Direction.AB.next(list.get(0));
            case 1:
                return Direction.BA.next(list.get(0));
            default:
                return Direction.BA.next(list.get(nodeIndex - 1)); //??

//            switch (direction) {
//                case AB:
//                    return edge.to().id();
//                case BA:
//                default:
//                    return edge.from().id();
//            }
        }
//        return null;
    }


}
