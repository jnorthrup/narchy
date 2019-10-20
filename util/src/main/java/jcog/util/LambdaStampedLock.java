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
        var stamp = writeLock();
        try {
            writeProcedure.run();
        } finally {
            unlockWrite(stamp);
        }
    }

    public <T> T write(Supplier<T> writeProcedure) {
        var stamp = writeLock();

        try {
            return writeProcedure.get();
        } finally {
            unlockWrite(stamp);
        }

    }


    public boolean write(BooleanSupplier writeProcedure) {
        var stamp = writeLock();

        try {
            return writeProcedure.getAsBoolean();
        } finally {
            unlockWrite(stamp);
        }

    }

    public int write(IntSupplier writeProcedure) {
        var stamp = writeLock();
        try {
            return writeProcedure.getAsInt();
        } finally {
            unlockWrite(stamp);
        }
    }

    public <T> T read(Supplier<T> readProcedure) {
        var stamp = readLock();

        try {
            return readProcedure.get();
        } finally {
            unlockRead(stamp);
        }

    }

    public int read(IntSupplier readProcedure) {
        var stamp = readLock();
        int result;
        try {
            result = readProcedure.getAsInt();
        } finally {
            unlockRead(stamp);
        }
        return result;
    }

    public void read(Runnable readProcedure) {
        var stamp = readLock();
        try {
            readProcedure.run();
        } finally {
            unlockRead(stamp);
        }
    }

    public void readOptimistic(Runnable readProcedure) {
        var stamp = tryOptimisticRead();

        if (stamp != 0) {
            readProcedure.run();
            if (validate(stamp))
                return;
        }

        read(readProcedure);
    }

    public boolean writeConditional(BooleanSupplier condition, Runnable action) {
        var stamp = readLock();
        try {
            while (condition.getAsBoolean()) {
                var writeStamp = tryConvertToWriteLock(stamp);
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