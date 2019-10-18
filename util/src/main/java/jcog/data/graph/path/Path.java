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

import jcog.data.graph.Node;

import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Описывает путь в графе
 *
 * @param <N> Тип вершины графа
 * @param <E> Тип ребра между вершинами
 * @author gocha
 */
public interface Path<N, E> {
    static <N, E> N firstNode(FromTo<Node<N, E>, E> edge, final Direction d) {
        if (d == null) throw new IllegalArgumentException("d == null");
        if (edge == null) throw new IllegalArgumentException("edge == null");

        switch (d) {
            case AB:
                return edge.from().id();
            case BA:
                return edge.to().id();
        }

        return null;
    }

    static <N, E> N secondNode(FromTo<Node<N, E>, E> edge, final Direction d) {
        if (d == null) throw new IllegalArgumentException("d == null");
        if (edge == null) throw new IllegalArgumentException("edge == null");

        switch (d) {
            case AB:
                return edge.to().id();
            case BA:
                return edge.from().id();
        }

        return null;
    }

    /**
     * Создание клона
     *
     * @return клон
     */
    Path<N, E> clone();

    /**
     * Проверяет содержит ли путь вершину
     *
     * @param a вершина A
     * @return true - вершина содержится в пути
     */
    boolean has(N a);

    /**
     * Кол-во определенной вершины в пути
     *
     * @param n вершина
     * @return кол-во
     */
    int count(N n);

    /**
     * Возвращает кол-во вершин в пути
     *
     * @return кол-во вершин
     */
    int nodeCount();

    /**
     * Возвращает вершину
     *
     * @param nodeIndex индекс вершины
     * @return вершина
     */
    N node(int nodeIndex);

    /**
     * Получение ребер между указанными вершинами
     *
     * @param beginIndex начальная вершина
     * @param endExc     конечная (исключительно) вершина
     * @return список ребер
     */
    List<E> edges(int beginIndex, int endExc);

    /**
     * Получение ребра между указаными вершинами. Растояние между вершинами, должно быть 1 ребро.
     *
     * @param beginIndex начальная вершина
     * @param endExc     конечная (исключительно) вершина
     * @return ребро
     */
    E edge(int beginIndex, int endExc);

    /**
     * Получение ребер между указанными вершинами
     *
     * @param beginIndex начальная вершина
     * @param endExc     конечная (исключительно) вершина
     * @return список ребер
     */
    List<FromTo<Node<N,E>,E>> fetch(int beginIndex, int endExc);

    /**
     * Возвращает признак что путь пустой - не содержит вершин и ребер
     *
     * @return true - путь пустой
     */
    boolean isEmpty();

    /**
     * Создает новый путь с начальной вершиной
     *
     * @param n Начальная вершина
     * @return новый путь
     */
    Path<N, E> spawn(N n);

    /**
     * Создает новый путь с добавленным ребром в конце
     *
     * @param e Ребро/дуга
     * @param n Вершина
     * @return новый путь
     */
    Path<N, E> append(E e, N n);

    /**
     * Создает новый пустой путь
     *
     * @return путь
     */
    Path<N, E> clear();

    /**
     * Проверят путь на наличие циклов
     *
     * @return true - в пути присуствуют циклы
     */
    boolean hasCycles();

    /**
     * Возвращает циклы в пути
     *
     * @return список циклов
     */
    List<Path<N, E>> cycles();

    /**
     * Возвращает под путь
     *
     * @param beginIdx начальная вершина
     * @param endExc   конечная (исключительно) вершина
     * @return Под путь
     */
    Path<N, E> subPath(int beginIdx, int endExc);


    default double sum(ToDoubleFunction<FromTo<Node<N, E>, E>> v) {
        double s = 0.0;
        for (FromTo<Node<N, E>, E> nodeEFromTo : fetch(0, nodeCount())) {
            double v1 = v.applyAsDouble(nodeEFromTo);
            s += v1;
        }
        return s;
    }

    /**
     * Описывает напарвления движения
     */
    enum Direction {
        /**
         * Из вершины A в вершину B
         */
        AB {
            @Override
            public <N, E> N next(FromTo<Node<N, E>, E> edge) {
                return edge.from().id();
            }
        },
        /**
         * Из вершины B в вершину A
         */
        BA {
            @Override
            public <N, E> N next(FromTo<Node<N, E>, E> edge) {
                return edge.to().id();
            }
        };


        public abstract <N, E> N next(FromTo<Node<N, E>, E> edge);

    }
}
