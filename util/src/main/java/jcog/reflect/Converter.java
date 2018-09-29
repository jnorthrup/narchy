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


import jcog.data.list.FasterList;
import jcog.reflect.graph.Edge;
import jcog.reflect.graph.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Последовательность caster-ов
 *
 * @author Kamnev Georgiy (nt.gocha@gmail.com)
 */
public class Converter<X,Y> extends MutableWeightedCaster<X,Y> implements GetWeight {
    
    protected final double defaultItemWeight = 1;
    
    private final WeightChangeListener listener = event -> {
        Double oldw = Converter.this.weight;
        Converter.this.weight = null;
        fireEvent(oldw, null);
    };
    
    protected List<Function> functions;

    public Converter(Path<Class, Function> path) {
        this.functions = new FasterList<>();

        for (Edge<Class, Function> ed : path.fetch(0, path.nodeCount())) {
            if (ed != null) {
                this.functions.add(ed.getEdge());
            }
        }

//        path.stream().filter((ed) -> ( ed!=null )).forEach((ed) -> {
//            this.convertors.add(ed.getEdge());
//        });

        attachListener();
    }

    public Converter(Iterable<Function> convertors) {
        this.functions = new ArrayList<>();
        for (Function conv : convertors) {
            if (conv != null) {
                this.functions.add(conv);
            }
        }

        attachListener();
    }

    //</editor-fold>

    private void attachListener() {
//        this.convertors.stream()
//            .filter( (c) -> ( c instanceof WeightChangeSender ) )
//            .forEach( (c) -> {
//                ((WeightChangeSender)c).addWeightChangeListener(listener,true);
//            });

        for (Function c : this.functions) {
            if (c instanceof WeightChangeSender) {
                ((WeightChangeSender) c).addWeightChangeListener(listener, true);
            }
        }
    }

    public Function[] getConvertors() {
        return functions.toArray(new Function[]{});
    }

    @Override
    public Y apply(X from) {
        Object v = from;
        for (Function conv : functions) {
            v = conv.apply(v);
        }
        return (Y) v;
    }

    @Override
    public Double getWeight() {
        if (weight != null) return weight;

        double w = 0;
        for (Function conv : functions) {
            if (conv instanceof GetWeight) {
                Double wc = ((GetWeight) conv).getWeight();
                if (wc != null)
                    w += wc;
                else
                    w += defaultItemWeight;
            } else {
                w += defaultItemWeight;
            }
        }

        weight = w;
        fireEvent(null, w);
        return weight;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Object w = getWeight();
        sb.append("Sequence");
        sb.append(" w=").append(w);
        sb.append(" {");
        int i = -1;
        for (Object conv : getConvertors()) {
            i++;
            Object wc = defaultItemWeight;
            if (conv instanceof GetWeight) {
                wc = ((GetWeight) conv).getWeight();
            }
            if (i > 0) sb.append(", ");
            sb.append(conv).append(" w=").append(wc);
        }
        sb.append('}');
        return sb.toString();
    }
}
