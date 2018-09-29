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

import jcog.reflect.spi.GetTypeConvertor;

import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * Расширеное преоброзование типов основанное на использовании
 * JavaSE Service Loader. <br>
 * <p>
 * Для расширения сущесвующих преобразований необходимо реализовать сервис GetTypeConvertor
 * (
 * <a href="https://docs.oracle.com/javase/tutorial/ext/basics/spi.html">oracle tutorial</a>
 * , plugin для maven: eu.somatik.serviceloader-maven-plugin:serviceloader-maven-plugin:1.0.3
 * ).
 *
 * @author Kamnev Georgiy (nt.gocha@gmail.com)
 * @see xyz.cofe.typeconv.spi.GetTypeConvertor
 */
public class ExtendedCastGraph extends BaseCastGraph {
    //<editor-fold defaultstate="collapsed" desc="log Функции">
//    private static final Logger logger = Logger.getLogger(ExtendedCastGraph.class.getName());
//    private static final Level logLevel = logger.getLevel();

    /**
     * Конструктор по умолчанию
     */
    public ExtendedCastGraph() {
        for (GetTypeConvertor gtc : ServiceLoader.load(GetTypeConvertor.class)) {
            if (gtc == null) continue;
            Function conv = gtc.getConvertor();
            Class srcType = gtc.getSourceType();
            Class trgType = gtc.getTargetType();
            if (conv == null) continue;
            if (srcType == null) continue;
            if (trgType == null) continue;
            set(srcType, trgType, conv);
        }
    }

    /**
     * Конструктор копирования
     *
     * @param src образей для копирования
     */
    public ExtendedCastGraph(ExtendedCastGraph src) {
        super(src);
    }

    //</editor-fold>

    /**
     * Создание клона
     *
     * @return клон
     */
    @Override
    public BaseCastGraph clone() {
        return new ExtendedCastGraph(this);
    }
}
