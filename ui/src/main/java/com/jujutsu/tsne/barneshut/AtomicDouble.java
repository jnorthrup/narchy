package com.jujutsu.tsne.barneshut;

import java.util.concurrent.atomic.AtomicReference;

class AtomicDouble {
    private final AtomicReference<Double> value = new AtomicReference<>(0.0);
    double addAndGet(double delta) {
        return value.updateAndGet((x)-> x+ delta);
//        while (true) {
//            double currentValue = value.get();
//            double newValue = currentValue + delta;
//            if (value.compareAndSet(currentValue, newValue))
//                return currentValue;
//        }
    }
    
    double get() {
    	return value.get();
    }
}