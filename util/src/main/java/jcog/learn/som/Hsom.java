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
        gamma = SomSize / 2f;
        this.numInputs = numInputs;
        this.SomSize = SomSize;
        for (var i1 = 0; i1 < SomSize; i1++) {
            for (var i2 = 0; i2 < SomSize; i2++) {
                coords1[i1][i2] = (float) ((float) i1 * 1.0); 
                coords2[i1][i2] = (float) ((float) i2 * 1.0);
            }
        }
        for (var x = 0; x < SomSize; x++) {
            for (var y = 0; y < SomSize; y++) {
                for (var z = 0; z < numInputs; z++) {
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
        var leak = Leak;

        for (j = 0; j < numInputs; j++) {
            if (!Leaky) {
                this.inputs[j] = input[j] * InMul;
            } else {
                this.inputs[j] += (-leak * this.inputs[j]) + (1f- leak) * input[j];
            }
        }
        var minv = Float.POSITIVE_INFINITY;
        for (var i1 = 0; i1 < SomSize; i1++) {
            for (var i2 = 0; i2 < SomSize; i2++) {
                var summe = 0.0f;
                var ll = links[i1][i2];
                for (j = 0; j < numInputs; j++) {
                    var ij = inputs[j];
                    var lljminij = ll[j] - ij;
                    
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
        var x = winnerx;
        var y = winnery;
        for (var i = 0; i < numInputs; i++) {
            outarr[i] = links[x][y][i] * outmul;
        }
    }

    float hsit(int i1, int i2) {
        var cc = this.coords1;
        var winnerx = this.winnerx;
        var winnery = this.winnery;
        var diff1 = (cc[i1][i2] - cc[winnerx][winnery])
                * (cc[i1][i2] - cc[winnerx][winnery]);
        var dd = this.coords2;
        var diff2 = (dd[i1][i2] - dd[winnerx][winnery])
                * (dd[i1][i2] - dd[winnerx][winnery]);
        var gammaSq = 2 * gamma * gamma;
        return (1.0f / ((float) Math.sqrt(Math.PI * gammaSq)))
                * ((float) Math.exp((diff1 + diff2) / (-gammaSq)));
    }

    /**
     * inputs and trains it
     */
    public void learn(float[] input) {
        input(input);

        var eta = this.eta;
        if (eta != 0.0f) {
            var l = this.links;
            var ii = this.inputs;

            for (var i1 = 0; i1 < SomSize; i1++) {
                for (var i2 = 0; i2 < SomSize; i2++) {
                    var h = hsit(i1, i2);
                    var ll = l[i1][i2];
                    for (var j = 0; j < numInputs; j++) {
                        var lx = l[i1][i2][j];
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
