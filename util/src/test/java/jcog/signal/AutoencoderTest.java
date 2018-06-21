package jcog.signal;

import jcog.learn.Autoencoder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by me on 2/18/16.
 */
class AutoencoderTest {

    @Test
    void test_dA() {
        Random rng = new Random(123);
        float corruption_level = 0.3f;
        int training_epochs = 100;
        int train_N = 10;
        int test_N = 2;
        int n_visible = 20;
        int n_hidden = 5;
        float learning_rate = 0.1f / train_N;
        float[][] train_X = {{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1}, {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1}, {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1}, {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0}};
        Autoencoder da = new Autoencoder(n_visible, n_hidden, rng);
        
        for (int epoch = 0; epoch < training_epochs; epoch++) {
            for (int i = 0; i < train_N; i++) {
                da.put(train_X[i], learning_rate, 0, corruption_level, false);
            }
        }

        
        float[][] test_X = {{1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0}};
        float[][] reconstructed_X = new float[test_N][n_visible];
        
        for (int i = 0; i < test_N; i++) {
            float[] encoded_X = new float[n_hidden];
            da.encode(test_X[i], encoded_X, false, false);
            System.out.println(Arrays.toString(test_X[i]));
            System.out.println(Arrays.toString(encoded_X));
            da.reconstruct(test_X[i]);
            for (int j = 0; j < n_visible; j++) {
                System.out.printf("%.5f ", reconstructed_X[i][j]);
            }
            System.out.println();
            System.out.println();
        }
    }


}