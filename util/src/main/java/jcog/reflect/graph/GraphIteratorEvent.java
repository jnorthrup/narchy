///*
// * The MIT License
// *
// * Copyright 2018 user.
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in
// * all copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// * THE SOFTWARE.
// */
//
//package jcog.reflect.graph;
//
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import jcog.reflect.graph.Path;
//
///**
// * Событие итерации по графу
// * @param <N> Тип вершины графа
// * @param <E> Тип ребра графа
// * @author Kamnev Georgiy (nt.gocha@gmail.com)
// */
//public class GraphIteratorEvent<N,E> {
//    //<editor-fold defaultstate="collapsed" desc="log Функции">
//    private static final Logger logger = Logger.getLogger(GraphIteratorEvent.class.getName());
//    private static final Level logLevel = logger.getLevel();
//
//    private static final boolean isLogSevere =
//        logLevel==null
//        ? true
//        : logLevel.intValue() <= Level.SEVERE.intValue();
//
//    private static final boolean isLogWarning =
//        logLevel==null
//        ? true
//        : logLevel.intValue() <= Level.WARNING.intValue();
//
//    private static final boolean isLogInfo =
//        logLevel==null
//        ? true
//        : logLevel.intValue() <= Level.INFO.intValue();
//
//    private static final boolean isLogFine =
//        logLevel==null
//        ? true
//        : logLevel.intValue() <= Level.FINE.intValue();
//
//    private static final boolean isLogFiner =
//        logLevel==null
//        ? true
//        : logLevel.intValue() <= Level.FINER.intValue();
//
//    private static final boolean isLogFinest =
//        logLevel==null
//        ? true
//        : logLevel.intValue() <= Level.FINEST.intValue();
//
//    private static void logFine(String message,Object ... args){
//        logger.log(Level.FINE, message, args);
//    }
//
//    private static void logFiner(String message,Object ... args){
//        logger.log(Level.FINER, message, args);
//    }
//
//    private static void logFinest(String message,Object ... args){
//        logger.log(Level.FINEST, message, args);
//    }
//
//    private static void logInfo(String message,Object ... args){
//        logger.log(Level.INFO, message, args);
//    }
//
//    private static void logWarning(String message,Object ... args){
//        logger.log(Level.WARNING, message, args);
//    }
//
//    private static void logSevere(String message,Object ... args){
//        logger.log(Level.SEVERE, message, args);
//    }
//
//    private static void logException(Throwable ex){
//        logger.log(Level.SEVERE, null, ex);
//    }
//
//    private static void logEntering(String method,Object ... params){
//        logger.entering(GraphIteratorEvent.class.getName(), method, params);
//    }
//
//    private static void logExiting(String method){
//        logger.exiting(GraphIteratorEvent.class.getName(), method);
//    }
//
//    private static void logExiting(String method, Object result){
//        logger.exiting(GraphIteratorEvent.class.getName(), method, result);
//    }
//    //</editor-fold>
//
//    /**
//     * Итератор по графу
//     */
//    protected GraphIterator<N,E> iterator;
//
//    /**
//     * Конструктор
//     * @param iterator Итератор по графу
//     */
//    public GraphIteratorEvent(GraphIterator<N, E> iterator) {
//        this.iterator = iterator;
//    }
//
//    /**
//     * Итератор по графу
//     * @return итератор
//     */
//    public GraphIterator<N, E> getIterator() {
//        return iterator;
//    }
//
//    //<editor-fold defaultstate="collapsed" desc="class PathFetched">
//    /**
//     * Событие получения очередного пути в графе
//     * @param <N> Тип вершины
//     * @param <E> Тип ребра
//     */
//    public static class PathFetched<N,E> extends GraphIteratorEvent<N,E> {
//        protected Path<N,E> path;
//
//        public PathFetched(GraphIterator<N, E> iterator, Path<N, E> path) {
//            super(iterator);
//            this.path = path;
//        }
//
//        /**
//         * Возвращает очередной путь
//         * @return путь
//         */
//        public Path<N, E> getPath() {
//            return path;
//        }
//
//        protected boolean terminal;
//
//        /**
//         * Указывает является ли путь терминальным - т.е. из данного пути нет исходящих вершин
//         * @return true - путь терминальный
//         */
//        public boolean isTerminal() {
//            return terminal;
//        }
//
//        /**
//         * Указывает является ли путь терминальным - т.е. из данного пути нет исходящих вершин
//         * @param terminal true - путь терминальный
//         */
//        public void setTerminal(boolean terminal) {
//            this.terminal = terminal;
//        }
//    }
//    //</editor-fold>
//
//    //<editor-fold defaultstate="collapsed" desc="class FetchFinish">
//    /**
//     * Событие получения завершения обхода
//     * @param <N> Тип вершины
//     * @param <E> Тип ребра
//     */
//    public static class FetchFinish<N,E> extends GraphIteratorEvent<N,E> {
//        public FetchFinish(GraphIterator<N, E> iterator) {
//            super(iterator);
//        }
//    }
//    //</editor-fold>
//}
