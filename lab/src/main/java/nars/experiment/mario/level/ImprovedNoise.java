package nars.experiment.mario.level;

import java.util.Arrays;
import java.util.Random;

public final class ImprovedNoise {
    public ImprovedNoise(long seed) {
        shuffle(seed);
    }

    public double noise(double x, double y, double z) {
        int X = (int) Math.floor(x) & 255,
                Y = (int) Math.floor(y) & 255,
                Z = (int) Math.floor(z) & 255;
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);
        double u = fade(x),
                v = fade(y),
                w = fade(z);
        int A = p[X] + Y, AA = p[A] + Z, AB = p[A + 1] + Z,
                B = p[X + 1] + Y, BA = p[B] + Z, BB = p[B + 1] + Z;

        return lerp(w, lerp(v, lerp(u, grad(p[AA], x, y, z),
                grad(p[BA], x - 1.0, y, z)),
                lerp(u, grad(p[AB], x, y - 1.0, z),
                        grad(p[BB], x - 1.0, y - 1.0, z))),
                lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1.0),
                        grad(p[BA + 1], x - 1.0, y, z - 1.0)),
                        lerp(u, grad(p[AB + 1], x, y - 1.0, z - 1.0), grad(p[BB + 1], x - 1.0, y - 1.0, z - 1.0))));
    }

    static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y,
                v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    int[] p = new int[512];

    public double perlinNoise(double x, double y) {
        double n = (double) 0;

        for (int i = 0; i < 8; i++) {
            double stepSize = 64.0 / (double) ((1 << i));
            n += noise(x / stepSize, y / stepSize, 128.0) * 1.0 / (double) (1 << i);
        }

        return n;
    }

    public void shuffle(long seed) {
        Random random = new Random(seed);
        int[] permutation = new int[10];
        int count = 0;
        for (int i1 = 0; i1 < 256; i1++) {
            if (permutation.length == count) permutation = Arrays.copyOf(permutation, count * 2);
            permutation[count++] = i1;
        }
        permutation = Arrays.copyOfRange(permutation, 0, count);

        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256 - i) + i;
            int tmp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = tmp;
            p[i + 256] = p[i] = permutation[i];
        }
    }
}