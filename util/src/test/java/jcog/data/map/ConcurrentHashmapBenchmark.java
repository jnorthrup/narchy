package jcog.data.map;

import jcog.Texts;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashmapBenchmark {


    static void bench(Map<Long, String> map) {
        long start = System.nanoTime();

        for (long i = 0; i < Iterations; i++) {
            for (int j = 0; j < N; j++) {
                map.put(i, "value");
            }

            for (long h = 0; h < ReadIterations; h++) {
                for (int j = 0; j < N; j++) {
                    map.get(i);
                }
            }

            for (long j = 0; j < N; j++) {
                map.remove(i);
            }
        }

        long end = System.nanoTime();

        System.out.println(map.getClass() + " " + Texts.timeStr(end - start));
    }

    final static int Iterations = 1;
    final static int ReadIterations = 1000;
    final static int N = 1_000_000;

    public static void benchConcurrentOpenHashMap(int concurrency) {
        bench(new ConcurrentOpenHashMap<>(N,  concurrency));
    }
    public static void benchConcurrentHashMap(float loadFactor, int concurrency) {
        bench(new ConcurrentHashMap<>(N, loadFactor, concurrency));
    }
    public static void benchCustomConcurrentHashMap() {
        bench(new CustomConcurrentHashMap<>(N));
    }

    static void benchHashMap(float loadFactor) {
        bench(new HashMap<>(N, loadFactor));
    }
    static void benchUnifiedMap(float loadFactor) {
        bench(new UnifiedMap<>(N, loadFactor));
    }

    public static void main(String[] args) {

        benchHashMap(0.66f);
        benchHashMap(0.95f);
        benchUnifiedMap(0.66f);
        benchUnifiedMap(0.95f);

        benchConcurrentHashMap(0.66f, 1);
        benchConcurrentHashMap(0.95f, 1);
//        benchConcurrentHashMap(0.66f, 8);
//        benchConcurrentHashMap(0.66f, 16);
        benchConcurrentOpenHashMap(1);
//        benchConcurrentOpenHashMap(8);
//        benchConcurrentOpenHashMap(16);
        benchCustomConcurrentHashMap();

    }
}
