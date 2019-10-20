package jcog.util;

import java.util.concurrent.locks.StampedLock;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * https://www.javaspecialists.eu/archive/Issue215.html
 */
@SuppressWarnings("TooBroadScope")
public class LambdaStampedLock extends StampedLock {

    public void write(final Runnable writeProcedure) {
        final long stamp = writeLock();
        try {
            writeProcedure.run();
        } finally {
            unlockWrite(stamp);
        }
    }

    public <T> T write(final Supplier<T> writeProcedure) {
        final long stamp = writeLock();

        try {
            return writeProcedure.get();
        } finally {
            unlockWrite(stamp);
        }

    }


    public boolean write(final BooleanSupplier writeProcedure) {
        final long stamp = writeLock();

        try {
            return writeProcedure.getAsBoolean();
        } finally {
            unlockWrite(stamp);
        }

    }

    public int write(final IntSupplier writeProcedure) {
        final long stamp = writeLock();
        try {
            return writeProcedure.getAsInt();
        } finally {
            unlockWrite(stamp);
        }
    }

    public <T> T read(final Supplier<T> readProcedure) {
        final long stamp = readLock();

        try {
            return readProcedure.get();
        } finally {
            unlockRead(stamp);
        }

    }

    public int read(final IntSupplier readProcedure) {
        final long stamp = readLock();
        int result;
        try {
            result = readProcedure.getAsInt();
        } finally {
            unlockRead(stamp);
        }
        return result;
    }

    public void read(final Runnable readProcedure) {
        final long stamp = readLock();
        try {
            readProcedure.run();
        } finally {
            unlockRead(stamp);
        }
    }

    public void readOptimistic(final Runnable readProcedure) {
        final long stamp = tryOptimisticRead();

        if (stamp != 0) {
            readProcedure.run();
            if (validate(stamp))
                return;
        }

        read(readProcedure);
    }

    public boolean writeConditional(final BooleanSupplier condition, final Runnable action) {
        long stamp = readLock();
        try {
            while (condition.getAsBoolean()) {
                final long writeStamp = tryConvertToWriteLock(stamp);
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