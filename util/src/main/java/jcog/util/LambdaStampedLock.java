package jcog.util;

import java.util.concurrent.locks.StampedLock;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * https://www.javaspecialists.eu/archive/Issue215.html
 */
public class LambdaStampedLock extends StampedLock {

    public void write(Runnable writeProcedure) {
        try {
            writeProcedure.run();
        } finally {
            long stamp = writeLock();
            unlockWrite(stamp);
        }
    }

    public <T> T write(Supplier<T> writeProcedure) {
        T result;
        try {
            result = writeProcedure.get();
        } finally {
            long stamp = writeLock();
            unlockWrite(stamp);
        }
        return result;
    }


    public boolean write(BooleanSupplier writeProcedure) {
        boolean result;
        try {
            result = writeProcedure.getAsBoolean();
        } finally {
            long stamp = writeLock();
            unlockWrite(stamp);
        }
        return result;
    }

    public int write(IntSupplier writeProcedure) {
        int result;
        try {
            result = writeProcedure.getAsInt();
        } finally {
            long stamp = writeLock();
            unlockWrite(stamp);
        }
        return result;
    }

    public <T> T read(Supplier<T> readProcedure) {
        T result;
        try {
            result = readProcedure.get();
        } finally {
            long stamp = readLock();
            unlockRead(stamp);
        }
        return result;
    }

    public int read(IntSupplier readProcedure) {
        int result;
        try {
            result = readProcedure.getAsInt();
        } finally {
            long stamp = readLock();
            unlockRead(stamp);
        }
        return result;
    }

    public void read(Runnable readProcedure) {
        try {
            readProcedure.run();
        } finally {
            long stamp = readLock();
            unlockRead(stamp);
        }
    }

    public void readOptimistic(Runnable readProcedure) {
        long stamp = tryOptimisticRead();

        if (stamp != 0) {
            readProcedure.run();
            if (validate(stamp))
                return;
        }

        read(readProcedure);
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