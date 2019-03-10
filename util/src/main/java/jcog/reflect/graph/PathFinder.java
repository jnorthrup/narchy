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

import jcog.sort.FloatRank;
import jcog.sort.RankedTopN;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Поиск путей в графе. <p>
 * Производит обход графа формируя возможные пути.
 * Обход производится по крайчащим путям.
 * Конечная вершина поиска определяется пользователем данного класса. <p>
 * За один вызов next() выдает один возможный путь из указанной точки.
 *
 * @param <N> Тип вершины
 * @param <E> Тип ребра
 * @author gocha
 */
public class PathFinder<N, E> extends RankedTopN<Path<N,E>> {

    private static final Logger logger = Logger.getLogger(PathFinder.class.getName());
    /**
     * Граф в котором производится поиск
     */
    protected SingleDirectedGraph<N, E> graph;
    /**
     * Направление движения
     */
    protected Path.Direction direction;



    /**
     * Конструктор
     *
     * @param graph     Одно направленный граф
     * @param start     Начальная вершина пути
     * @param direction Направление движения
     * @param getWeight Получение веса ребра, вес должен быть положительным или равен нулю
     */
    public PathFinder(
            int capacity,
            SingleDirectedGraph<N, E> graph,
            N start,
            Path.Direction direction,
            Function<Edge<N, E>, Double> getWeight
    ) {
        super(new Path[capacity], FloatRank.the(rank(getWeight)));
        if (getWeight == null) throw new IllegalArgumentException("getWeight==null");
        if (start == null) throw new IllegalArgumentException("start==null");
        if (direction == null) throw new IllegalArgumentException("direction==null");
        if (graph == null) throw new IllegalArgumentException("graph==null");

        this.graph = graph;
        this.direction = direction;
        //this.validator = createValidtor();

        Iterable<Edge<N, E>> next = getNextEdges(start);
        for (Edge<N, E> e : next) {
            Path<N, E> bp = createPath(/*direction*/);

            //bp = bp.addAt(e);
            bp = bp.start(start);
            bp = bp.join(GraphTools.secondNode(e, direction), e.getEdge());
            add(bp);
        }
    }

    /**
     * Конструктор
     *
     * @param graph      Одно направленный граф
     * @param start      Начальная вершина пути
     * @param direction  Направление движения
     * @param comparator Сравнение длины путей
     */
    public PathFinder(
            int capacity,
            SingleDirectedGraph<N, E> graph,
            N start,
            Path.Direction direction,
            FloatRank<Path<N, E>> comparator
    ) {
        super(new Path[capacity], comparator);
        if (graph == null) {
            throw new IllegalArgumentException("graph==null");
        }
        if (start == null) {
            throw new IllegalArgumentException("start==null");
        }
        if (direction == null) {
            throw new IllegalArgumentException("direction==null");
        }
        if (comparator == null) {
            throw new IllegalArgumentException("comparator==null");
        }

        this.graph = graph;
        this.direction = direction;
        //this.validator = createValidtor();

        Iterable<Edge<N, E>> next = getNextEdges(start);
        for (Edge<N, E> e : next) {
            Path<N, E> bp = createPath(/*direction*/);
            bp = bp.start(start);
            bp = bp.join(GraphTools.secondNode(e, direction), e.getEdge());
            add(bp);
        }
    }

    private static void logFiner(String message, Object... args) {
        logger.log(Level.FINER, message, args);
    }

    //</editor-fold>

//    /**
//     * Полчение веса пути
//     *
//     * @param path      Путь
//     * @param getWeight Получение веса ребра
//     * @return Вес пути
//     */
//    protected double getIntWeightOf(Path<N, E> path, Function<Edge<N, E>, Double> getWeight) {
//        double w = 0;
//        for (Edge<N, E> e : path.fetch(0, path.nodeCount())) {
//            double we = getWeight.apply(e);
//            w += we;
//        }
//        return w;
//    }

    /**
     * Создание Comparator для пути
     *
     * @param getWeight Получение веса ребра
     * @return Comparator
     */
    protected static <N,E> FloatFunction<Path<N, E>> rank(Function<Edge<N, E>, Double> getWeight) {
        return p -> (float)p.sum(getWeight::apply);
    }


    /**
     * Создаает путь
     *
     * @return Путь
     */
    protected Path<N, E> createPath(/*Path.Direction d*/) {
        //p = p.direction(d);
        return new BasicPath<>();
    }

    /**
     * Добавляет ребро в конец пути
     *
     * @param path Путь
     * @param e    Ребро
     * @return Новый путь
     */
    protected Path<N, E> append(Path<N, E> path, Edge<N, E> e) {
        N n = GraphTools.secondNode(e, direction);
        return path.join(n, e.getEdge());
    }
    //protected Predicate<Edge<N,E>> validator = null;

    /*
     * Создает предикат проверки циклов в пути.
     * @return true - Проверяемое ребро AB <b>не</b> существует в пути; <p>
     * false - Проверяемое ребро AB <b>существует</b> в пути;
    protected Predicate<Edge<N,E>> createValidtor(){
        Predicate<Edge<N,E>> p = new Predicate<Edge<N,E>>() {
            @Override
            public boolean validate(Edge<N,E> value) {
                if( paths==null )return false;
                if( value==null )return false;
                for( Path<N,E> p : paths ){
                    //if( p.has(value.getNodeA(), value.getNodeB()) )return false;
                    
                    N na = GraphTools.secondNode(value, direction);
                    if( na==null )return false;
                    
                    if( p.count(na)>0 )return false;
                }
                return true;
            }
        };
        return p;
    }
     */

    /**
     * Извлекает исходящие ребра/дуги из вершины n в соот. движению.
     *
     * @param n Вершина
     * @return Ребра/дуги направления движения.
     */
    protected Iterable<Edge<N, E>> getNextEdges(N n) {
        
        /*if( validator!=null ){
            itr = Iterators.predicate(itr, validator);
        }*/
        return direction.equals(Path.Direction.AB)
                ? graph.edgesOfNodeA(n)
                : graph.edgesOfNodeB(n);
    }

    /* (non-Javadoc)
     * @see java.util.Iterator
     */
    public boolean hasNext() {
        return !isEmpty();
    }

    /* (non-Javadoc)
     * @see java.util.Iterator
     */
    public Path<N, E> next() {
        if (isEmpty()) return null;

        Path<N, E> p = pop();

        if (p.nodeCount() > 0) {
            N last = p.node(-1);
            if (!p.hasCycles()) {
                Iterable<Edge<N, E>> next = getNextEdges(last);

                for (Edge<N, E> e : next)
                    add(append(p, e));

            } else {
                logFiner("cycle detected");
            }
        }

        return p;
    }



}
