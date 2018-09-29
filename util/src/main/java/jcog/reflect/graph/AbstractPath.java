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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Путь в графе
 *
 * @param <N> Тип вершины
 * @param <E> Тип ребра
 * @author gocha
 */
public abstract class AbstractPath<N, E> implements Path<N, E> {
    /**
     * Описывает направление движения
     */
    protected Path.Direction direction = Path.Direction.AB;

    public AbstractPath() {
    }

    public AbstractPath(AbstractPath<N, E> sample) {
        if (sample != null) {
            this.direction = sample.direction;
        }
    }

    /* (non-Javadoc)
     * @see Path
     */
    @Override
    abstract public Path<N, E> clone();

    @Override
    public E edge(int beginIndex, int endIndex) {
        if (Math.abs(beginIndex - endIndex) > 1)
            throw new IllegalArgumentException("distance beginIndex .. endIndex > 1");

        List<E> ed = edges(
                Math.min(beginIndex, endIndex),
                Math.max(beginIndex, endIndex)
        );

        if (ed == null || ed.isEmpty()) return null;
        return ed.get(0);
    }

    @Override
    public Path<N, E> segment(int beginIdx, int endExc) {
        if (beginIdx == endExc) {
            return clone().clear();
        }

        int ncnt = nodeCount();
        if (ncnt == 0) {
            return clone().clear();
        }

        int andiff = Math.abs(beginIdx - endExc);
        if (andiff == 0) {
            return clone().clear();
        }

        if (andiff == 1) {
            int minidx = Math.min(beginIdx, endExc);
            if (minidx < 0 || minidx >= ncnt) return clone().clear();
            return clone().clear().start(node(minidx));
        }

        List<Edge<N, E>> edges = fetch(beginIdx, endExc);

        Path<N, E> path = clone().clear();

        if (edges != null) {
            int esize = edges.size();

            if (esize == 1) {
                path = path.
                        start(edges.get(0).getNodeA()).
                        join(edges.get(0).getNodeB(), edges.get(0).getEdge())
                ;
            } else if (esize > 1) {
                path = path.
                        start(edges.get(0).getNodeA()).
                        join(edges.get(0).getNodeB(), edges.get(0).getEdge())
                ;
                for (int ei = 1; ei < esize; ei++) {
                    path = path.join(edges.get(ei).getNodeB(), edges.get(ei).getEdge());
                }
            }
        }

        return path;
    }

    @Override
    public List<Path<N, E>> cycles() {
        List<Path<N, E>> list = new ArrayList<>();

        Map<N, List<Integer>> nposmap = new LinkedHashMap<>();
        for (int ni = 0; ni < nodeCount(); ni++) {
            N na = node(ni);
            List<Integer> npos = nposmap.computeIfAbsent(na, k -> new ArrayList<>());
            npos.add(ni);
        }

        for (Map.Entry<N, List<Integer>> en : nposmap.entrySet()) {
            List<Integer> npos = en.getValue();
            if (npos.size() > 1) {
                Integer from = npos.get(0);
                Integer to = npos.get(npos.size() - 1);
                if (from < to) {
                    list.add(segment(from, to + 1));
                }
            }
        }

        return list;
    }
}
