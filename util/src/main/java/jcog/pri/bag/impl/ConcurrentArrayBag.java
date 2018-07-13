package jcog.pri.bag.impl;

import jcog.Util;
import jcog.pri.bag.Bag;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import jcog.util.QueueLock;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

@Deprecated abstract public class ConcurrentArrayBag<K,X extends Priority> extends ArrayBag<K,X> {

    private final QueueLock<X> toPut;


    protected ConcurrentArrayBag(PriMerge mergeFunction, int cap) {
        this(mergeFunction, new HashMap<>(cap), cap);
    }

    protected ConcurrentArrayBag(PriMerge mergeFunction, @NotNull Map<K, X> map, int cap) {
        super(mergeFunction, map);
        setCapacity(cap);

        BlockingQueue q = Util.blockingQueue(cap * 2);
        this.toPut = new QueueLock<X>(q, super::putAsync, (batchSize) -> {





        });
    }

    @Override
    public Bag<K, X> commit(Consumer<X> update) {
        super.commit(update);
        
          
        
        return this;
    }






    @Override
    public void putAsync(X b) {
        toPut.accept(b);
    }



}

