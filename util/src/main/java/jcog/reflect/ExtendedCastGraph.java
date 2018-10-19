/*
 * The MIT License
 *
 * Copyright 2015 Kamnev Georgiy (nt.gocha@gmail.com).
 *
 */

package jcog.reflect;

import jcog.reflect.spi.GetTypeConvertor;

import java.util.ServiceLoader;
import java.util.function.Function;

/** @author Kamnev Georgiy (nt.gocha@gmail.com)
 * @see xyz.cofe.typeconv.spi.GetTypeConvertor
 */
public class ExtendedCastGraph extends BaseCastGraph {

    /**
     * Конструктор по умолчанию
     */
    public ExtendedCastGraph() {
        for (GetTypeConvertor gtc : ServiceLoader.load(GetTypeConvertor.class)) {
            if (gtc == null) continue;
            Function conv = gtc.getConvertor();
            if (conv == null) continue;
            Class srcType = gtc.getSourceType();
            if (srcType == null) continue;
            Class trgType = gtc.getTargetType();
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



    @Override
    public BaseCastGraph clone() {
        return new ExtendedCastGraph(this);
    }
}
