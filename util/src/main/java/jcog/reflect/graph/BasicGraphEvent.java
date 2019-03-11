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

import jcog.data.graph.FromTo;
import jcog.data.graph.MapNodeGraph;

/**
 * Описывает событие графа
 *
 * @param <N> Тип вершины
 * @param <E> Тип ребра
 * @author GoCha
 */
public class BasicGraphEvent<N, E> implements GraphEvent<N, E> {
    private final FromTo<jcog.data.graph.Node<N,E>,E> _Edge;
    private final N _Node;
    private final MapNodeGraph<N, E> _Graph;
    private final GraphAction _Action;

    /**
     * Конструктор
     *
     * @param graph  Граф
     * @param action Действие
     * @param node   Вершина
     * @param edge   Ребро
     */
    public BasicGraphEvent(MapNodeGraph<N, E> graph, GraphAction action, N node, FromTo<jcog.data.graph.Node<N,E>,E> edge) {
        if (action == null) {
            throw new IllegalArgumentException("action == null");
        }
        if (graph == null) {
            throw new IllegalArgumentException("graph == null");
        }

        _Node = node;
        _Graph = graph;
        _Edge = edge;
        _Action = action;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.GraphEvent#getEdge
     */
    @Override
    public FromTo<jcog.data.graph.Node<N,E>,E> getEdge() {
        return _Edge;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.GraphEvent#getNode
     */
    @Override
    public N getNode() {
        return _Node;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.GraphEvent#getGraph
     */
    @Override
    public MapNodeGraph<N, E> getGraph() {
        return _Graph;
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.GraphEvent#getAction
     */
    @Override
    public GraphAction getAction() {
        return _Action;
    }
}
