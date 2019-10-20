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
                grad(p[BA], x - 1, y, z)),
                lerp(u, grad(p[AB], x, y - 1, z),
                        grad(p[BB], x - 1, y - 1, z))),
                lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1),
                        grad(p[BA + 1], x - 1, y, z - 1)),
                        lerp(u, grad(p[AB + 1], x, y - 1, z - 1), grad(p[BB + 1], x - 1, y - 1, z - 1))));
    }

    static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    static double grad(int hash, double x, double y, double z) {
        var h = hash & 15;
        double u = h < 8 ? x : y,
                v = h < 4 ? y : h == 12 || h == 14 ? x : z;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    int[] p = new int[512];

    public double perlinNoise(double x, double y) {
        double n = 0;

        for (var i = 0; i < 8; i++) {
            var stepSize = 64.0 / ((1 << i));
            n += noise(x / stepSize, y / stepSize, 128) * 1.0 / (1 << i);
        }

        return n;
    }

    public void shuffle(long seed) {
        var random = new Random(seed);
        var permutation = new int[10];
        var count = 0;
        for (var i1 = 0; i1 < 256; i1++) {
            if (permutation.length == count) permutation = Arrays.copyOf(permutation, count * 2);
            permutation[count++] = i1;
        }
        permutation = Arrays.copyOfRange(permutation, 0, count);

        for (var i = 0; i < 256; i++) {
            var j = random.nextInt(256 - i) + i;
            var tmp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = tmp;
            p[i + 256] = p[i] = permutation[i];
        }
    }
}