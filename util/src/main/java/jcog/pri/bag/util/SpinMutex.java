package jcog.pri.bag.util;

public interface SpinMutex {

    default int start(int context, int key) {
        long hash = (context << 32) | key;
        if (hash == 0) hash = 1; 
        return start(hash);
    }

    int start(long hash);

    void end(int slot);
}
