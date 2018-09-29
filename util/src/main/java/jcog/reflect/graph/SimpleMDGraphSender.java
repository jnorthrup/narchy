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
import java.util.List;

/**
 * Мультиграф с уведомлением о измениях
 *
 * @param <N> Тип вершины графа
 * @param <E> Тип ребра
 * @author GoCha
 */
public class SimpleMDGraphSender<N, E> extends SimpleMDGraph<N, E> implements GraphSender<N, E> {
    private final List<GraphListener> listeners = new ArrayList<>();

    /**
     * Конструктор
     */
    public SimpleMDGraphSender() {
        super();
    }

    /**
     * Конструктор
     *
     * @param factory Фабрика графа
     */
    public SimpleMDGraphSender(GraphFactory<N, E> factory) {
        super(factory);
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.GraphSender#addGraphListener
     */
    @Override
    public void addGraphListener(GraphListener<N, E> listener) {
        if (listener == null) return;
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.GraphSender#removeGraphListener
     */
    @Override
    public void removeGraphListener(GraphListener<N, E> listener) {
        listeners.remove(listener);
    }

    /**
     * Сообщает подписчикам сообщение
     *
     * @param event Сообщение
     */
    protected void fireGraphEvent(GraphEvent<N, E> event) {
        for (GraphListener listener : listeners) {
            listener.graphEvent(event);
        }
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.MultiDirectGraph#onEdgeAdded
     */
    @Override
    protected void onEdgeAdded(Edge<N, E> es) {
        fireGraphEvent(new BasicGraphEvent<>(this, GraphAction.EDGE_ADDED, null, es));
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.MultiDirectGraph#onEdgeAdded
     */
    @Override
    protected void onNodeAdded(N node) {
        fireGraphEvent(new BasicGraphEvent<>(this, GraphAction.NODE_ADDED, node, null));
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.MultiDirectGraph#onEdgeRemoved
     */
    @Override
    protected void onEdgeRemoved(Edge<N, E> es) {
        fireGraphEvent(new BasicGraphEvent<>(this, GraphAction.EDGE_REMOVED, null, es));
    }

    /* (non-Javadoc)
     * @see org.gocha.collection.graph.MultiDirectGraph#onEdgeRemoved
     */
    @Override
    protected void onNodeRemoved(N node) {
        fireGraphEvent(new BasicGraphEvent<>(this, GraphAction.NODE_REMOVED, node, null));
    }
}