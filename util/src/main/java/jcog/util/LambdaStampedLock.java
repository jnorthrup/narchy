package jcog.util;

import java.util.concurrent.locks.StampedLock;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * https:
 */
public class LambdaStampedLock extends StampedLock {

    public void write(Runnable writeProcedure) {
        long stamp = writeLock();
        try {
            writeProcedure.run();
        } finally {
            unlockWrite(stamp);
        }
    }

    public <T> T write(Supplier<T> writeProcedure) {
        long stamp = writeLock();
        T result;
        try {
            result = writeProcedure.get();
        } finally {
            unlockWrite(stamp);
        }
        return result;
    }


    public boolean write(BooleanSupplier writeProcedure) {
        long stamp = writeLock();
        boolean result;
        try {
            result = writeProcedure.getAsBoolean();
        } finally {
            unlockWrite(stamp);
        }
        return result;
    }

    public int write(IntSupplier writeProcedure) {
        long stamp = writeLock();
        int result;
        try {
            result = writeProcedure.getAsInt();
        } finally {
            unlockWrite(stamp);
        }
        return result;
    }

    public <T> T read(Supplier<T> readProcedure) {
        long stamp = readLock();
        T result;
        try {
            result = readProcedure.get();
        } finally {
            unlockRead(stamp);
        }
        return result;
    }

    public int read(IntSupplier readProcedure) {
        long stamp = readLock();
        int result;
        try {
            result = readProcedure.getAsInt();
        } finally {
            unlockRead(stamp);
        }
        return result;
    }

    public void read(Runnable readProcedure) {
        long stamp = readLock();
        try {
            readProcedure.run();
        } finally {
            unlockRead(stamp);
        }
    }

    public <T> T readOptimistic(Supplier<T> readProcedure) {
        long stamp = tryOptimisticRead();
        T result = readProcedure.get();
        if (!validate(stamp)) {
            result = read(readProcedure);
        }
        return result;
    }
    public void readOptimistic(Runnable readProcedure) {
        long stamp = tryOptimisticRead();
        readProcedure.run();
        if (!validate(stamp)) {
            read(readProcedure);
        }
    }

    public int readOptimistic(IntSupplier readProcedure) {
        long stamp = tryOptimisticRead();
        int result = readProcedure.getAsInt();
        if (!validate(stamp)) {
            result = read(readProcedure);
        }
        return result;
    }

    public boolean writeConditional(BooleanSupplier condition, Runnable action) {
        long stamp = readLock();
        try {
            while (condition.getAsBoolean()) {
                long writeStamp = tryConvertToWriteLock(stamp);
                if (writeStamp != 0) {
                    action.run();
                    stamp = writeStamp;
                    return true;
                } else {
                    unlockRead(stamp);
                    stamp = writeLock();
                }
            }
            return false;
        } finally {
            unlock(stamp);
        }
    }
}