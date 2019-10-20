package jcog.learn.som;

import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Hsom Self Organizing Map by patham9
 */
public class Hsom {

    final @NotNull float[][][] links;
    final @NotNull float[] inputs;
    final @NotNull float[][] coords1;
    final @NotNull float[][] coords2;
    
    final int numInputs;
    final int SomSize;
    static final boolean Leaky = true;
    float gamma;
    float eta = 0.1f;
    float outmul = 1.0f;
    public int winnerx;
    public int winnery;
    float Leak = 0.05f;
    float InMul = 1.0f;

    public Hsom(int numInputs, int SomSize, @NotNull Random rng) {
        links = new float[SomSize][SomSize][numInputs];
        
        inputs = new float[numInputs];
        coords1 = new float[SomSize][SomSize];
        coords2 = new float[SomSize][SomSize];
        gamma = (float) SomSize / 2f;
        this.numInputs = numInputs;
        this.SomSize = SomSize;
        for (int i1 = 0; i1 < SomSize; i1++) {
            for (int i2 = 0; i2 < SomSize; i2++) {
                coords1[i1][i2] = (float) ((double) (float) i1 * 1.0);
                coords2[i1][i2] = (float) ((double) (float) i2 * 1.0);
            }
        }
        for (int x = 0; x < SomSize; x++) {
            for (int y = 0; y < SomSize; y++) {
                for (int z = 0; z < numInputs; z++) {
                    links[x][y][z] = (rng.nextFloat()/**
                     * 2.0-1.0
                     */
                     * 2f - 1f);
                }
            }
        }
    }

    void input(float[] input) {
        int j;
        float leak = Leak;

        for (j = 0; j < numInputs; j++) {
            if (!Leaky) {
                this.inputs[j] = input[j] * InMul;
            } else {
                this.inputs[j] += (-leak * this.inputs[j]) + (1f- leak) * input[j];
            }
        }
        float minv = Float.POSITIVE_INFINITY;
        for (int i1 = 0; i1 < SomSize; i1++) {
            for (int i2 = 0; i2 < SomSize; i2++) {
                float summe = 0.0f;
                @NotNull float[] ll = links[i1][i2];
                for (j = 0; j < numInputs; j++) {
                    @NotNull float ij = inputs[j];
                    float lljminij = ll[j] - ij;
                    
                    summe += lljminij * lljminij;
                }
                if (summe <= minv) 
                {
                    minv = summe;
                    winnerx = i1;
                    winnery = i2;
                }
            }
        }
    }

    public int winner() {
        return winnerx + (winnery * SomSize);
    }
    void get(float[] outarr) {
        int x = winnerx;
        int y = winnery;
        for (int i = 0; i < numInputs; i++) {
            outarr[i] = links[x][y][i] * outmul;
        }
    }

    float hsit(int i1, int i2) {
        @NotNull float[][] cc = this.coords1;
        int winnerx = this.winnerx;
        int winnery = this.winnery;
        float diff1 = (cc[i1][i2] - cc[winnerx][winnery])
                * (cc[i1][i2] - cc[winnerx][winnery]);
        @NotNull float[][] dd = this.coords2;
        float diff2 = (dd[i1][i2] - dd[winnerx][winnery])
                * (dd[i1][i2] - dd[winnerx][winnery]);
        float gammaSq = 2.0F * gamma * gamma;
        return (1.0f / ((float) Math.sqrt(Math.PI * (double) gammaSq)))
                * ((float) Math.exp((double) ((diff1 + diff2) / (-gammaSq))));
    }

    /**
     * inputs and trains it
     */
    public void learn(float[] input) {
        input(input);

        float eta = this.eta;
        if (eta != 0.0f) {
            @NotNull float[][][] l = this.links;
            @NotNull float[] ii = this.inputs;

            for (int i1 = 0; i1 < SomSize; i1++) {
                for (int i2 = 0; i2 < SomSize; i2++) {
                    float h = hsit(i1, i2);
                    @NotNull float[] ll = l[i1][i2];
                    for (int j = 0; j < numInputs; j++) {
                        @NotNull float lx = l[i1][i2][j];
                        ll[j] = lx + (eta * h * (ii[j] - lx));
                    }
                }
            }
        }
    }

    void set(float AdaptionStrenght, float AdaptioRadius) {
        eta = AdaptionStrenght;
        gamma = AdaptioRadius;
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
