package jcog.signal;

import jcog.Texts;
import jcog.learn.Autoencoder;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 2/18/16.
 */
class AutoencoderTest {

    @Test
    void test_dA() {
        Random rng = new Random(123);
        float noise_level = 0.01f, corruption_level = 0.01f;
        int training_epochs = 40;
        int train_N = 10;
        int test_N = 2;
        int n_visible = 20;
        int n_hidden = 7;
        float learning_rate = 0.1f; //0.1f / train_N;
        boolean sigmoid = true, normalize = false;
        float[][] train_X = {
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0}};
        Autoencoder da = new Autoencoder(n_visible, n_hidden, rng);

        DescriptiveStatistics meanErrorPerInput = new DescriptiveStatistics(train_N * 2);
        for (int epoch = 0; epoch < training_epochs; epoch++) {
            for (int i = 0; i < train_N; i++) {
                float err = da.put(train_X[i], learning_rate, noise_level, corruption_level, sigmoid, normalize, true);
                System.out.println(err);
                meanErrorPerInput.addValue(err/train_X[i].length);
            }
        }
        System.out.println("mean error per input: " + meanErrorPerInput);
        assertTrue(meanErrorPerInput.getMean() < 0.15f);

        
        float[][] test_X = {
                {1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0}};

        for (int i = 0; i < test_N; i++) {
            System.out.println(Texts.n4(test_X[i]));
            float[] reconstructed_X = da.reconstruct(test_X[i]);
            System.out.println(Texts.n4(reconstructed_X));
            float[] encoded_X = new float[n_hidden];
            da.encode(test_X[i], encoded_X, 0, 0, sigmoid,normalize);
            System.out.println('\t' + Texts.n4(encoded_X));
            System.out.println();
        }
    }


}