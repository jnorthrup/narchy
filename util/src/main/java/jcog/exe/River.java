///*
// * Copyright (c) [2016] [ <ether.camp> ]
// * This file is part of the ethereumJ library.
// *
// * The ethereumJ library is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Lesser General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * The ethereumJ library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// * GNU Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public License
// * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
// */
//package jcog.exe;
//
//import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.concurrent.Executor;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.concurrent.locks.ReentrantLock;
//import java.util.function.Consumer;
//import java.util.function.Function;
//
///**
// * Queues execution tasks into a single pipeline where some tasks can be executed in parallel
// * but preserve 'messages' order so the next task process messages on a single thread in
// * the same order they were added to the previous executor
// *
// * Created by Anton Nashatyrev on 23.02.2016.
// *
// * River is to Stream as ...
// */
//public class River<In, Out>{
////original name: public class ExecutorPipeline <In, Out>{
//
//    private final Executor exec;
//    private final boolean preserveOrder;
//
//    /** transfer function */
//    private final Function<In, Out> each;
//
//    @Nullable private final Consumer<Throwable> exceptionHandler;
//    @Nullable private River<Out, ?> next;
//
//    private final AtomicLong orderCounter = new AtomicLong();
//    private long nextOutTaskNumber;
//    private final LongObjectHashMap<Out> orderMap = new LongObjectHashMap();
//    private final ReentrantLock lock = new ReentrantLock();
//
//    public River(int blockingcapacity, boolean preserveOrder,
//                 Function<In, Out> each,
//                 Executor exec,
//                 @Nullable Consumer<Throwable> exceptionHandler) {
//
//        this(preserveOrder, each, exec, exceptionHandler);
//    }
//
//    public River( boolean preserveOrder,
//                 Function<In, Out> each,
//                 Executor exec,
//                 @Nullable Consumer<Throwable> exceptionHandler) {
//        this.exec = exec;
//        this.preserveOrder = preserveOrder;
//        this.each = each;
//        this.exceptionHandler = exceptionHandler;
//    }
//
//    public River<Out, Void> add(final Consumer<Out> consumer) {
//        return add(false, out -> {
//            consumer.accept(out);
//            return null;
//        });
//    }
//
//    public <NextOut> River<Out, NextOut> add(boolean preserveOrder, Function<Out, NextOut> processor) {
//        River<Out, NextOut> ret = new River(preserveOrder, processor, exec, exceptionHandler);
//        next = ret;
//        return ret;
//    }
//
//    private void pushNext(long order, Out res) {
//        if (next != null) {
//            if (!preserveOrder) {
//                next.push(res);
//            } else {
//                lock.lock();
//                try {
//                    if (order == nextOutTaskNumber) {
//                        next.push(res);
//                        while(true) {
//                            Out out = orderMap.remove(++nextOutTaskNumber);
//                            if (out == null) break;
//                            next.push(out);
//                        }
//                    } else {
//                        orderMap.put(order, res);
//                    }
//                } finally {
//                    lock.unlock();
//                }
//            }
//        }
//    }
//
//    public void push(final In in) {
//        final long order = orderCounter.getAndIncrement();
//        exec.execute(() -> {
//            try {
//                pushNext(order, each.apply(in));
//            } catch (Throwable e) {
//                exceptionHandler.accept(e);
//            }
//        });
//    }
//
//
////    public void shutdown() {
////        try {
////            exec.shutdown();
////        } catch (Exception e) {}
////        if (next != null) {
////            exec.shutdown();
////        }
////    }
////
////    public boolean isShutdown() {
////        return exec.isShutdown();
////    }
//
////    /**
////     * Shutdowns executors and waits until all pipeline
////     * submitted tasks complete
////     * @throws InterruptedException
////     */
////    public void join() throws InterruptedException {
////        exec.shutdown();
////        exec.awaitTermination(10, TimeUnit.MINUTES);
////        if (next != null) next.join();
////    }
//}