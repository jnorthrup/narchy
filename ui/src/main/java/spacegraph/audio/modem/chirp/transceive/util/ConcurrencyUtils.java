//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package spacegraph.audio.modem.chirp.transceive.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.*;

final class ConcurrencyUtils {
    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool(new ConcurrencyUtils.CustomThreadFactory(new ConcurrencyUtils.CustomExceptionHandler()));
    private static int THREADS_BEGIN_N_1D_FFT_2THREADS = 8192;
    private static int THREADS_BEGIN_N_1D_FFT_4THREADS = 65536;
    private static int THREADS_BEGIN_N_2D = 65536;
    private static int THREADS_BEGIN_N_3D = 65536;
    private static int NTHREADS = prevPow2(getNumberOfProcessors());

    private ConcurrencyUtils() {
    }

    private static int getNumberOfProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static int getNumberOfThreads() {
        return NTHREADS;
    }

    public static void setNumberOfThreads(int var0) {
        NTHREADS = prevPow2(var0);
    }

    public static int getThreadsBeginN_1D_FFT_2Threads() {
        return THREADS_BEGIN_N_1D_FFT_2THREADS;
    }

    public static void setThreadsBeginN_1D_FFT_2Threads(int var0) {
        THREADS_BEGIN_N_1D_FFT_2THREADS = Math.max(var0, 512);

    }

    public static int getThreadsBeginN_1D_FFT_4Threads() {
        return THREADS_BEGIN_N_1D_FFT_4THREADS;
    }

    public static void setThreadsBeginN_1D_FFT_4Threads(int var0) {
        THREADS_BEGIN_N_1D_FFT_4THREADS = Math.max(var0, 512);

    }

    public static int getThreadsBeginN_2D() {
        return THREADS_BEGIN_N_2D;
    }

    public static void setThreadsBeginN_2D(int var0) {
        THREADS_BEGIN_N_2D = var0;
    }

    public static int getThreadsBeginN_3D() {
        return THREADS_BEGIN_N_3D;
    }

    public static void setThreadsBeginN_3D(int var0) {
        THREADS_BEGIN_N_3D = var0;
    }

    public static void resetThreadsBeginN_FFT() {
        THREADS_BEGIN_N_1D_FFT_2THREADS = 8192;
        THREADS_BEGIN_N_1D_FFT_4THREADS = 65536;
    }

    public static void resetThreadsBeginN() {
        THREADS_BEGIN_N_2D = 65536;
        THREADS_BEGIN_N_3D = 65536;
    }

    public static int nextPow2(int var0) {
        if (var0 < 1) {
            throw new IllegalArgumentException("x must be greater or equal 1");
        } else if ((var0 & var0 - 1) == 0) {
            return var0;
        } else {
            var0 |= var0 >>> 1;
            var0 |= var0 >>> 2;
            var0 |= var0 >>> 4;
            var0 |= var0 >>> 8;
            var0 |= var0 >>> 16;
            return var0 + 1;
        }
    }

    private static int prevPow2(int var0) {
        if (var0 < 1) {
            throw new IllegalArgumentException("x must be greater or equal 1");
        } else {
            return (int) Math.pow(2.0D, Math.floor(Math.log(var0) / Math.log(2.0D)));
        }
    }

    public static boolean isPowerOf2(int var0) {
        if (var0 <= 0) {
            return false;
        } else {
            return (var0 & var0 - 1) == 0;
        }
    }

    public static void sleep(long var0) {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }

    }

    public static Future<?> submit(Runnable var0) {
        return THREAD_POOL.submit(var0);
    }

    public static void waitForCompletion(Future<?>[] var0) {
        int var1 = var0.length;

        try {
            for (Future<?> future : var0) {
                future.get();
            }
        } catch (ExecutionException | InterruptedException var3) {
            var3.printStackTrace();
        }

    }

    private static class CustomThreadFactory implements ThreadFactory {
        private static final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        private final UncaughtExceptionHandler handler;

        CustomThreadFactory(UncaughtExceptionHandler var1) {
            this.handler = var1;
        }

        public Thread newThread(Runnable var1) {
            Thread var2 = defaultFactory.newThread(var1);
            var2.setUncaughtExceptionHandler(this.handler);
            return var2;
        }
    }

    private static final class CustomExceptionHandler implements UncaughtExceptionHandler {
        private CustomExceptionHandler() {
        }

        public void uncaughtException(Thread var1, Throwable var2) {
            var2.printStackTrace();
        }
    }
}
