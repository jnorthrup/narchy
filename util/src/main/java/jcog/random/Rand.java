package jcog.random;

/** simplified interface for Random Number Generators that dont necessarily extend java.util.Random and its overhead */
public interface Rand {

    float nextFloat();

    long nextLong();

    int nextInt(int i);

    void setSeed(long s);
}
