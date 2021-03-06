/*
 * The MIT License
 *
 * Copyright 2015 Kamnev Georgiy (nt.gocha@gmail.com).
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

package jcog.reflect;


import com.google.common.collect.Lists;
import jcog.data.graph.Node;
import jcog.data.graph.path.FromTo;
import jcog.data.graph.path.Path;
import jcog.data.graph.search.PathFinder;
import jcog.data.list.FasterList;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * Граф конвертирования типов. <br>
 * В качестве вершин графа - Java тип <br>
 * <br>
 * В качестве ребра графа - функция преобразования типа. <br>
 * Ребро может быть взешенно (интерфейс GetWeight). <br>
 * <br>
 * При преобразованиях подбирается кратчайший путь пробразования (GetWeight). <br>
 * <br>
 * В процессе преобразования учитывается возможность
 * автоматического приведения типа (конструкция assignable from)
 * для начальной вершины в пути преобразования.
 *
 * @author Kamnev Georgiy (nt.gocha@gmail.com)
 */
public class CastGraph extends jcog.data.graph.MapNodeGraph<Class, Function> {

    private static final Logger logger = Logger.getLogger(CastGraph.class.getName());

    private static final int PATH_CAPACITY = 64;

//    private static final int findPathMinimum = 1;

    protected final ClassSet classes = new ClassSet();

    protected Function<FromTo<jcog.data.graph.Node<Class, Function>, Function>, Double> edgeWeightFunction = null;

    /**
     * Конструктор по умолчанию
     */
    public CastGraph() {
        super();
    }

    private static Level logLevel() {
        return logger.getLevel();
    }

    private static boolean isLogFine() {
        Level level = logLevel();
        return level != null && level.intValue() <= Level.FINE.intValue();
    }

    //</editor-fold>

    private static void logException(Throwable ex) {
        logger.log(Level.SEVERE, null, ex);
    }

    /**
     * Создание конвертора ребро графа -&gt; вес
     *
     * @return конвертор ребр графа в их веса
     */
    public static Function<FromTo<jcog.data.graph.Node<Class, Function>, Function>, Double> createEdgeWeight() {
        return
                new Function<FromTo<Node<Class, Function>, Function>, Double>() {
                    @Override
                    public Double apply(FromTo<Node<Class, Function>, Function> from) {
                        Object edge = from.id();
                        if (edge instanceof PrioritizedDouble)
                            return ((PrioritizedDouble) edge).weight();
                        return (double) 1;
                    }
                };
    }

//    /**
//     * Поиск возможных конверторов для типа
//     * @param type Тип
//     * @param strongCompare true - жесткое сравнение типов; false - использование конструкции instanceof в сравнении
//     * @param childToParent true - последовательность в порядке от дочерних классов, к родительскому классу <br>
//     * false - обратная последовательность: в порядке от родительского класса к дочерним
//     * @param incParent true - включать в поиск родитеслькие классы
//     * @param incChildren true - включать в поиск дочерние классы
//     * @return Возможные альтернативы преобразований
//     */
//    public Map<Class,UnaryOperator<Object>> getConvertorsFrom(
//        Class type,
//        boolean strongCompare,
//        boolean childToParent,
//        boolean incParent,
//        boolean incChildren
//    ){
//        Map<Class,UnaryOperator<Object>> convs
//            = new TreeMap<Class, Function>(
//                new ClassSet.ClassHeirarchyComparer( childToParent )
//            );
//
//        Iterable<Class> fromClasses = strongCompare ?
//            Iterators.single(type) :
//            classes.getAssignableFrom(type, incParent, incChildren);
//
//        for( Class cnode : fromClasses ){
//            for( Edge<Class,UnaryOperator<Object>> e : this.edgesOfNodeA(cnode) ){
//                UnaryOperator<Object> conv = e.getEdge();
//                Class target = e.getNodeB();
//                convs.put(target, conv);
//            }
//        }
//
//        return convs;
//    }

    @Override
    protected void onAdd(Node<Class, Function> r) {
        super.onAdd(r);
        Class type = r.id();
        if (type != null)
            classes.add(type);
    }

    @Override
    protected void onRemoved(Node<Class, Function> r) {
        Class type = r.id();
        if (type != null)
            classes.remove(type);
        super.onRemoved(r);
    }

    /**
     * Получение начального узла преобразований
     *
     * @param type          Искомый тип
     * @param strongCompare true - жесткое сравнение типов; false - использование конструкции instanceof в сравнении
     * @param childToParent true - последовательность в порядке от дочерних классов, к родительскому классу <br>
     *                      false - обратная последовательность: в порядке от родительского класса к дочерним
     * @param incParent     true - включать в поиск родитеслькие классы
     * @param incChildren   true - включать в поиск дочерние классы
     * @return Перечень классов удовлетворяющих критерию поиска
     */
    public List<Class> roots(Class type, boolean strongCompare, boolean childToParent, boolean incParent, boolean incChildren
    ) {
        Collection<Class> fromClasses = strongCompare ?
                List.of(type) :
                classes.getAssignableFrom(type, incParent, incChildren);
        if (fromClasses.size() <= 1) {
            return fromClasses instanceof List ? ((List)fromClasses) : List.copyOf(fromClasses);
        } else {
            List<Class> list = Lists.newArrayList(fromClasses);
            list.sort(new ClassSet.ClassHierarchyComparer(childToParent));
            return list;
        }
    }

    /**
     * Получение конвертора ребра графа в его вес
     *
     * @return конвертор ребр графа в веса
     */
    public Function<FromTo<jcog.data.graph.Node<Class, Function>, Function>, Double> getEdgeWeight() {
        if (edgeWeightFunction != null) return edgeWeightFunction;
        edgeWeightFunction = createEdgeWeight();
        return edgeWeightFunction;
    }

    /**
     * Поиск пути цепочки преобразований.
     * Возвращает наименьший по длине путь преобразования типа
     *
     * @param from класс начала пути преобразования
     * @param to   конечный класс пути преобразования
     * @return путь или null
     */
    public Path<Class, Function> pathFirst(Class from, Class to) {
        if (from == null) throw new IllegalArgumentException("from==null");
        if (to == null) throw new IllegalArgumentException("to==null");
        Path[] y = new Path[1];
        findPath(from, to, (new Predicate<Path<Class, Function>>() {
            @Override
            public boolean test(Path<Class, Function> x) {
                y[0] = x;
                return false; /* just the first one */
            }
        }));
        return y[0];
    }

    /**
     * Поиск пути цепочки преобразований.
     * Возвращает наименьший по длине путь преобразования типа
     *
     * @param from   класс начала пути преобразования
     * @param to     конечный класс пути преобразования
     * @param filter Фильтр или null
     * @return путь или null
     */
    public boolean findPath(
            Class from,
            Class to,
            java.util.function.Predicate<Path<Class, Function>> filter
    ) {
//        if (from == null) throw new IllegalArgumentException("from==null");
//        if (to == null) throw new IllegalArgumentException("to==null");

        //        pfinder = new PathFinder<>(
//            this, from, Path.Direction.AB, (Edge<Class, Function> from1) -> {
//                Object edge = from1.getEdge();
//                if( edge instanceof GetWeight )
//                    return ((GetWeight)edge).getWeight();
//                return (double)1;
//            });

        PathFinder<Class, Function> pfinder = new PathFinder(
                PATH_CAPACITY,
                this,
                from,
                Path.Direction.AB,
                getEdgeWeight()
        );

        while (pfinder.hasNext()) {
            Path<Class, Function> path = pfinder.next();
            if (path == null) break;
            Class lastnode = path.node(-1);
            //assert(!lastnode.equals(to));
            if (lastnode != null && lastnode == to) {
                if (!filter.test(path))
                    return false;
            }
        }
        return true; //continue
    }

    public List<Path<Class, Function>> paths(Class fromType, Class targetType) {
        if (fromType == null) throw new IllegalArgumentException("fromType==null");
        if (targetType == null) throw new IllegalArgumentException("targetType==null");

        List<Class> starts = roots(fromType, false, true, true, false);
        if (starts == null || starts.isEmpty()) {
            throw new ClassCastException("can't cast " + fromType + " to " + targetType + ", can't find start class");
        }

        List<Path<Class, Function>> p = new FasterList<>();

        for (Class startCls : starts) {



            findPath(
                    startCls,
                    targetType,
                    // java 8
                /*(pathFound) -> {
                    variants.addAt(pathFound);
                    if( variants.size()<findPathMinimum && findPathMinimum>=0 ){
                        return false;
                    }
                    //if( findPathMinimum<2 )return true;
                    return true;
                } */
                    new Predicate<Path<Class, Function>>() {
                        @Override
                        public boolean test(Path<Class, Function> pathFound) {
                            p.add(pathFound);
                            //return variants.size() >= findPathMinimum || findPathMinimum < 0;//if( findPathMinimum<2 )return true;
                            return true; //continue
                        }
                    }
            );

            /*if( path!=null ){
                lvariants.addAt(path);
            }*/

        }


        return p;
    }

    /**
     * Преборазования значения
     *
     * @param <TARGET>   Тип данных который хотим получить
     * @param value      Исходное значение
     * @param targetType Целевой тип
     * @return Преобразованное значение
     * @throws ClassCastException если невозможно преобразование
     */
    public <TARGET> TARGET cast(Object value, Class<TARGET> targetType) {
        if (value == null) throw new IllegalArgumentException("value==null");
        if (targetType == null) throw new IllegalArgumentException("targetType==null");
        Class c = value.getClass();
        if (c == targetType) return (TARGET) value;
        return (TARGET) cast(value, targetType, null, null);
    }

    /**
     * Преборазования значения
     *
     * @param value               Исходное значение
     * @param targetType          Целевой тип
     * @param castedConvertor     Convertor который удачно отработал
     * @param failedCastConvertor Convertor который не удачно отработал
     * @return Преобразованное значение
     * @throws ClassCastException если невозможно преобразование
     */
    public Object cast(
            Object value,
            Class targetType,
            Consumer<Function> castedConvertor,
            @Nullable Consumer<Pair<Function, Throwable>> failedCastConvertor
    ) {
        if (value == null) throw new IllegalArgumentException("value==null");
        if (targetType == null) throw new IllegalArgumentException("targetType==null");

        Class cv = value.getClass();

        List<Path<Class, Function>> lvariants = paths(cv, targetType);
//        lvariants.removeIf(p -> p.nodeCount() > 2);
//
//        Collection<Path<Class, Function>> removeSet = new LinkedHashSet<>();
//        for (Path<Class, Function> p : lvariants) {
//            if (p.nodeCount() < 2) {
//                removeSet.addAt(p);
//            }
//        }
//        lvariants.removeAll(removeSet);

        if (lvariants.isEmpty()) {
            throw new ClassCastException("can't cast " + cv + " to " + targetType
                    + ", no available casters"
            );
        }

        Collection<Throwable> castErrors = new FasterList<>();
        Collection<Converter> scasters = new FasterList<>();

        for (Path<Class, Function> path : lvariants) {
            //int psize = path.size();
            int ncount = path.nodeCount();
            //if( psize==1 ){
            if (ncount == 1) {
                Function conv = path.edge(0, 1);
                try {
                    Object res = conv.apply(value);
                    if (castedConvertor != null) castedConvertor.accept(conv);
                    return res;
                } catch (Throwable ex) {
                    fail(failedCastConvertor, castErrors, conv, ex);
                }
            } else {
                Converter c = Converter.the(path);
                scasters.add(c);
            }
        }

        for (Converter c : scasters) {
            try {
                Object res = c.apply(value);
                if (castedConvertor != null)
                    castedConvertor.accept(c);
                return res;
            } catch (Throwable ex) {
                fail(failedCastConvertor, castErrors, c, ex);
            }
        }

        int ci = -1;
        StringBuilder castErrMess = new StringBuilder();
        for (Throwable err : castErrors) {
            ci++;
            if (ci > 0) castErrMess.append('\n');
            castErrMess.append(err.getMessage());
        }

        throw new ClassCastException("can't cast " + cv + " to " + targetType
                + ", cast failed:\n" + castErrMess
        );
    }

    private static void fail(@Nullable Consumer<Pair<Function, Throwable>> failedCastConvertor, Collection<Throwable> castErrors, Function conv, Throwable ex) {

        if (isLogFine())
            logException(ex);

        castErrors.add(ex);
        if (failedCastConvertor != null)
            failedCastConvertor.accept(pair(conv, ex));
    }

    public <X,Y> List<Function<X, Y>> applicable(Class<? extends X> cfrom, Class<? extends Y> cto) {

        List<Class> roots = roots(cfrom, false, true, true, false);
        if (roots.isEmpty())
            return Collections.EMPTY_LIST;

        List<Function<X, Y>> convertors = new FasterList(roots.size());
        for(Class cf : roots ){
            List<Path<Class, Function>> paths = paths(cf, cto);
            if( paths != null ) {
                for (Path<Class, Function> c : paths) {
                    convertors.add(Converter.the(c));
                }
            }
        }

        return convertors;
    }
}
