package jcog.learn.deep;

import jcog.Util;
import jcog.util.ArrayUtil;

import java.util.Random;

import static jcog.learn.deep.utils.sigmoid;

public class DBN {

    public int N;
    public int n_ins;
    public int[] hidden_layer_sizes;
    public int n_outs;
    public int n_layers;
    public HiddenLayerDiscrete[] sigmoid_layers;
    public RBM[] rbm_layers;
    public LogisticRegressionDiscrete log_layer;
    public Random rng;


    public DBN(int N, int n_ins, int[] hidden_layer_sizes, int n_outs, int n_layers, Random rng) {
        int input_size;

        this.N = N;
        this.n_ins = n_ins;
        this.hidden_layer_sizes = hidden_layer_sizes;
        this.n_outs = n_outs;
        this.n_layers = n_layers;

        this.sigmoid_layers = new HiddenLayerDiscrete[n_layers];
        this.rbm_layers = new RBM[n_layers];

        if(rng == null)	this.rng = new Random(1234);
        else this.rng = rng;

        
        for(int i=0; i<this.n_layers; i++) {
            if(i == 0) {
                input_size = this.n_ins;
            } else {
                input_size = this.hidden_layer_sizes[i-1];
            }

            
            this.sigmoid_layers[i] = new HiddenLayerDiscrete(input_size, this.hidden_layer_sizes[i], null, null, rng);

            
            this.rbm_layers[i] = new RBM(input_size, this.hidden_layer_sizes[i], this.sigmoid_layers[i].W, this.sigmoid_layers[i].b, null, rng) {
                @Override
                public double activate(double a) {
                    return Util.tanhFast((float)a);
                }
            };
        }

        
        this.log_layer = new LogisticRegressionDiscrete(this.hidden_layer_sizes[this.n_layers-1], this.n_outs);
    }

    public void pretrain(double[][] train_X, double lr, int k, int epochs) {
        double[] layer_input = ArrayUtil.EMPTY_DOUBLE_ARRAY;
        int prev_layer_input_size;
        double[] prev_layer_input;

        for(int i=0; i<n_layers; i++) {  
            for(int epoch=0; epoch<epochs; epoch++) {  
                for(int n=0; n<N; n++) {  
                    
                    for(int l=0; l<=i; l++) {

                        if(l == 0) {
                            layer_input = new double[n_ins];
                            System.arraycopy(train_X[n], 0, layer_input, 0, n_ins);
                        } else {
                            if(l == 1) prev_layer_input_size = n_ins;
                            else prev_layer_input_size = hidden_layer_sizes[l-2];

                            prev_layer_input = new double[prev_layer_input_size];
                            System.arraycopy(layer_input, 0, prev_layer_input, 0, prev_layer_input_size);

                            layer_input = new double[hidden_layer_sizes[l-1]];

                            sigmoid_layers[l-1].sample_h_given_v(prev_layer_input, layer_input);
                        }
                    }

                    rbm_layers[i].contrastive_divergence(layer_input, lr/N, k);
                }
            }
        }
    }

    public void finetune(double[][] train_X, double[][] train_Y, double lr, int epochs) {
        double[] layer_input = ArrayUtil.EMPTY_DOUBLE_ARRAY;
        
        double[] prev_layer_input = ArrayUtil.EMPTY_DOUBLE_ARRAY;

        for(int epoch=0; epoch<epochs; epoch++) {
            for(int n=0; n<N; n++) {

                
                for(int i=0; i<n_layers; i++) {
                    if(i == 0) {
                        prev_layer_input = new double[n_ins];
                        System.arraycopy(train_X[n], 0, prev_layer_input, 0, n_ins);
                    } else {
                        prev_layer_input = new double[hidden_layer_sizes[i-1]];
                        System.arraycopy(layer_input, 0, prev_layer_input, 0, hidden_layer_sizes[i - 1]);
                    }

                    layer_input = new double[hidden_layer_sizes[i]];
                    sigmoid_layers[i].sample_h_given_v(prev_layer_input, layer_input);
                }

                log_layer.train(layer_input, train_Y[n], lr);
            }
            
        }
    }

    public void predict(double[] x, double[] y) {
        double[] layer_input = ArrayUtil.EMPTY_DOUBLE_ARRAY;
        
        double[] prev_layer_input = new double[n_ins];
        System.arraycopy(x, 0, prev_layer_input, 0, n_ins);

        double linear_output;


        
        for(int i=0; i<n_layers; i++) {
            layer_input = new double[sigmoid_layers[i].n_out];

            for(int k=0; k<sigmoid_layers[i].n_out; k++) {
                linear_output = 0.0;

                for(int j=0; j<sigmoid_layers[i].n_in; j++) {
                    linear_output += sigmoid_layers[i].W[k][j] * prev_layer_input[j];
                }
                linear_output += sigmoid_layers[i].b[k];
                layer_input[k] = sigmoid(linear_output);
            }

            if(i < n_layers-1) {
                prev_layer_input = new double[sigmoid_layers[i].n_out];
                System.arraycopy(layer_input, 0, prev_layer_input, 0, sigmoid_layers[i].n_out);
            }
        }

        for(int i=0; i<log_layer.n_out; i++) {
            y[i] = 0;
            for(int j=0; j<log_layer.n_in; j++) {
                y[i] += log_layer.W[i][j] * layer_input[j];
            }
            y[i] += log_layer.b[i];
        }

        log_layer.softmax(y);
    }

    private static void test_dbn() {
        Random rng = new Random(123);

        double pretrain_lr = 0.1;
        int pretraining_epochs = 1000;
        int k = 1;
        double finetune_lr = 0.1;
        int finetune_epochs = 500;

        int train_N = 6;
        int test_N = 4;
        int n_ins = 6;
        int n_outs = 2;
        int[] hidden_layer_sizes = {3, 3};
        int n_layers = hidden_layer_sizes.length;

        
        double[][] train_X = {
                {1, 1, 1, 0, 0, 0},
                {1, 0, 1, 0, 0, 0},
                {1, 1, 1, 0, 0, 0},
                {0, 0, 1, 1, 1, 0},
                {0, 0, 1, 1, 0, 0},
                {0, 0, 1, 1, 1, 0}
        };

        double[][] train_Y = {
                {1, 0},
                {1, 0},
                {1, 0},
                {0, 1},
                {0, 1},
                {0, 1},
        };


        
        DBN dbn = new DBN(train_N, n_ins, hidden_layer_sizes, n_outs, n_layers, rng);

        
        dbn.pretrain(train_X, pretrain_lr, k, pretraining_epochs);

        
        dbn.finetune(train_X, train_Y, finetune_lr, finetune_epochs);


        
        double[][] test_X = {
                {1, 1, 0, 0, 0, 0},
                {1, 1, 1, 1, 0, 0},
                {0, 0, 0, 1, 1, 0},
                {0, 0, 1, 1, 1, 0},
        };

        double[][] test_Y = new double[test_N][n_outs];

        
        for(int i=0; i<test_N; i++) {
            dbn.predict(test_X[i], test_Y[i]);
            for(int j=0; j<n_outs; j++) {
                System.out.print(test_Y[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        test_dbn();
    }
}
