// Software: Multi-Layer Perceptron Library in Java
// Author: Hy Truong Son
// Major: BSc. Computer Science
// Class: 2013 - 2016
// Institution: Eotvos Lorand University
// Email: sonpascal93@gmail.com
// Website: http://people.inf.elte.hu/hytruongson/
// Final update: October 4th, 2015
// Copyright 2015 (c) Hy Truong Son. All rights reserved. Only use for academic purposes.

package jcog.learn;

import jcog.Util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/** from: https://github.com/HyTruongSon/MLP-Java
 * TODO collect and compare with other MLP impl
 * */
public class MLP2 {
	
	// +-----------+
	// | Constants |
	// +-----------+
	
	private float Epochs       = 1000;     // Default number of iterations
	private float LearningRate = 1e-3f;     // Default learning rate (good only for stochastic learning process)
	private float Momentum     = 0.9f;      // Default momentum for stochastic learning process
	private float Lambda       = 0.0f;      // Default regularization parameter for batch learning process, no regularization 
	private float Epsilon      = 1e-3f;     // Default epsilon for stopping stochastic learning process
	
	// +----------------------+
	// | Variables Definition |
	// +----------------------+
	
	
	static class MLPLayer {
		int size;
		float[] out;
		float[] theta;
		float[][] delta;
		float[][] weight;
	}

	MLPLayer[] layer;

	private float[] Expected;
	
	private final Random rng = new Random();
	
	private BufferedInputStream FileInput;
	
	private int randomInt(int number){
		return rng.nextInt(number);
	}
	
	// +-------------------+
	// | Memory allocation |
	// +-------------------+
	
	private void initMemory(int nLayers){
		layer = new MLPLayer[nLayers];
		for (int i = 0; i < nLayers; i++)
			layer[i] = new MLPLayer();
	}
	
	// +----------------------------------+
	// | Randomize weights for each layer |
	// +----------------------------------+
	
	private void initCoefficient(){
		int nLayers = layer.length;
		Expected = new float [layer[nLayers - 1].size];
		
		for (int i = 0; i < nLayers; i++){
			if (i != 0)
				layer[i].theta = new float [layer[i].size];
		
			layer[i].out = new float [layer[i].size];
			
			if (i != nLayers - 1){
				layer[i].weight = new float [layer[i].size][layer[i + 1].size];
				layer[i].delta = new float [layer[i].size][layer[i + 1].size];
			}
		}
		
		for (int i = 0; i < nLayers - 1; i++)
			for (int j = 0; j < layer[i].size; j++)
				for (int v = 0; v < layer[i + 1].size; v++){
					layer[i].weight[j][v] = rng.nextFloat(); // (float)(randomInt(10)) / (10.0f * layer[i + 1].size);
					int t = randomInt(2);
					if (t == 1) layer[i].weight[j][v] = - layer[i].weight[j][v];
				}
	}
	
	// +---------------------------------------------------------------------------------+
    // | Constructor for the case 2 layers (no hidden layer): Input Layer - Output Layer |
	// +---------------------------------------------------------------------------------+
	
	public MLP2(int InputLayer, int OutputLayer){
		initMemory(2);
		
		layer[0].size = InputLayer;
		layer[1].size = OutputLayer;
		
		initCoefficient();
	}
	
	// +-------------------------------------------------------------------------------------------------------------+
	// | Constructor for the case 3 layers (1 hidden layer, most popular): Input Layer - Hidden Layer - Output Layer |
	// +-------------------------------------------------------------------------------------------------------------+
	
	public MLP2(int InputLayer, int HiddenLayer, int OutputLayer){
		initMemory(3);
		
		layer[0].size = InputLayer;
		layer[1].size = HiddenLayer;
		layer[2].size = OutputLayer;
		
		initCoefficient();
	}
	
	// +-------------------------------------------------------------------------------------------------------------------+
	// | Constructor for the case 4 layers (2 hidden layers): Input Layer - Hidden Layer 1 - Hidden Layer 2 - Output Layer |
	// +-------------------------------------------------------------------------------------------------------------------+
	
	public MLP2(int InputLayer, int HiddenLayer1, int HiddenLayer2, int OutputLayer){
		initMemory(4);
		
		layer[0].size = InputLayer;
		layer[1].size = HiddenLayer1;
		layer[2].size = HiddenLayer2;
		layer[3].size = OutputLayer;
		
		initCoefficient();
	}
	
	// +-----------------------------------------------------+
	// | Constructor for the case 5 layers (3 hidden layers) |
	// +-----------------------------------------------------+
	
	public MLP2(int InputLayer, int HiddenLayer1, int HiddenLayer2, int HiddenLayer3, int OutputLayer){
		initMemory(5);
		
		layer[0].size = InputLayer;
		layer[1].size = HiddenLayer1;
		layer[2].size = HiddenLayer2;
		layer[3].size = HiddenLayer3;
		layer[4].size = OutputLayer;
		
		initCoefficient();
	}
	
	// +-----------------------------------------------------+
	// | Constructor for the case 6 layers (4 hidden layers) |
	// +-----------------------------------------------------+
	
	public MLP2(int InputLayer, int HiddenLayer1, int HiddenLayer2, int HiddenLayer3, int HiddenLayer4, int OutputLayer){
		initMemory(6);
		
		layer[0].size = InputLayer;
		layer[1].size = HiddenLayer1;
		layer[2].size = HiddenLayer2;
		layer[3].size = HiddenLayer3;
		layer[4].size = HiddenLayer4;
		layer[5].size = OutputLayer;
		
		initCoefficient();
	}
	
//	// +--------------------------------+
//	// | Read a float from a text file |
//	// +--------------------------------+
//	
//	private float readfloat() throws IOException {
//		String str = "";
//		
//		while (true){
//			int aByte = FileInput.read();
//			char aChar = (char)(aByte);
//			if (aChar != ' '){
//				str += aChar;
//				break;
//			}
//		}
//		
//		while (true){
//			int aByte = FileInput.read();
//			if (aByte == -1) break;
//			char aChar = (char)(aByte);
//			if (aChar == ' ') break;
//			str += aChar;
//		}
//		
//		return float.parsefloat(str);
//	}
//	
//	// +-------------------+
//	// | Parameter Setters |
//	// +-------------------+
//	
//	public void setWeights(String FileName) throws IOException {
//		FileInput = new BufferedInputStream (new FileInputStream(FileName));
//		
//		for (int i = 0; i < nLayers - 1; i++)
//			for (int j = 0; j < layer[i].size; j++)
//				for (int v = 0; v < layer[i + 1].size; v++)
//					layer[i].weight[j][v] = readfloat();
//					
//		FileInput.close();
//	}
	
	public void setMomentum(float value){
		Momentum = value;
	}
	
	public void setEpochs(int value){
		Epochs = value;
	}
	
	public void setLearningRate(float value){
		LearningRate = value;
	}
	
	public void setRegularizationParameter(float value){
	    Lambda = value;
	}
	
	public void setEpsilon(float value){
		Epsilon = value;
	}
	
	public void setLayer(int layerIndex, float[][] weight){
	    for (int i = 0; i < layer[layerIndex].size; i++)
		    for (int j = 0; j < layer[layerIndex + 1].size; j++)
		        layer[layerIndex].weight[i][j] = weight[i][j];
	}
	
	// +-------------------+
	// | Parameter Getters |
	// +-------------------+
	
	public float getMomentum(){
		return Momentum;
	}
	
	public float getEpochs(){
		return Epochs;
	}
	
	public float getLearningRate(){
		return LearningRate;
	}
	
	public float getRegularizationParameter(){
	    return Lambda;
	}
	
	public float getEpsilon(){
		return Epsilon;
	}
	
//	public void getLayer(int layerIndex, float weight[][]){
//	    for (int i = 0; i < layer[layerIndex].size; i++)
//		    for (int j = 0; j < layer[layerIndex + 1].size; j++)
//		        weight[i][j] = layer[layerIndex].weight[i][j];
//	}
	
//	// +--------------------------------------------+
//	// | Write weights of all layers to a text file |
//	// +--------------------------------------------+
//	
//	public void writeWeights(String FileName) throws IOException {		
//		FileWriter FileOutput = new FileWriter(FileName);
//        PrintWriter Writer = new PrintWriter(FileOutput);
//		
//		for (int i = 0; i < nLayers - 1; i++)
//			for (int j = 0; j < layer[i].size; j++)
//				for (int v = 0; v < layer[i + 1].size; v++)
//					Writer.print(layer[i].weight[j][v] + " ");
//			
//		Writer.close();
//	}
	
	// +-------------------------------+
	// | Sigmoid - Activation Function |
	// +-------------------------------+
	
//	private float Sigmoid(float x){
//		return 1.0f / (1.0f + Math.exp(-x));
//	}

    // +----------------------+
    // | Feed-Forward Process |
    // +----------------------+

	private void Perceptron(){
		int nLayers = layer.length;

		for (int i = 1; i < nLayers; i++)
			for (int j = 0; j < layer[i].size; j++){
				float net = 0.0f;
				for (int v = 0; v < layer[i - 1].size; v++)
					net += layer[i - 1].out[v] * layer[i - 1].weight[v][j];
				layer[i].out[j] = Util.sigmoid(net);
			}	
	}

    // +---------------+
    // | Norm L2 error |
    // +---------------+

	private float SquareError(){
		float res = 0.0f;
		int nLayers = layer.length;
		for (int i = 0; i < layer[nLayers - 1].size; i++){
			float diff = Expected[i] - layer[nLayers - 1].out[i];
			res += 0.5 * diff * diff;
		}
		
		return res;
	}
	
	// +-----------------------+
	// | Gradients Computation |
	// +-----------------------+

    private void initGradients(){
		int nLayers = layer.length;
        for (int i = 0; i < layer[nLayers - 1].size; i++){
			float out = layer[nLayers - 1].out[i];
			layer[nLayers - 1].theta[i] = out * (1 - out) * (Expected[i] - out);
		}
	
		for (int i = 1; i < nLayers - 1; i++)
			for (int j = 0; j < layer[i].size; j++){
				float sum = 0.0f;
				for (int v = 0; v < layer[i + 1].size; v++)
					sum += layer[i + 1].theta[v] * layer[i].weight[j][v];
				float out = layer[i].out[j];
				layer[i].theta[j] = out * (1 - out) * sum;
			}
    }

    // +-----------------------------+
    // | Stochastic Back-Propagation |
    // +-----------------------------+

	private void StochasticBackPropagation(){		
		initGradients();

		int nLayers = layer.length;
		for (int i = 0; i < nLayers - 1; i++)
			for (int j = 0; j < layer[i].size; j++)
				for (int v = 0; v < layer[i + 1].size; v++){
					float delta = layer[i].delta[j][v];
					float out = layer[i].out[j];
					float theta = layer[i + 1].theta[v];
					
					layer[i].delta[j][v] = LearningRate * theta * out + Momentum * delta;
					layer[i].weight[j][v] += layer[i].delta[j][v];
				}
	}
	
	// +---------------------+
	// | Stochastic Learning |
	// +---------------------+

	public float StochasticLearning(float[] Input, float[] ExpectedOutput){
		for (int i = 0; i < layer[0].size; i++)
			layer[0].out[i] = Input[i];

		int nLayers = layer.length;
		for (int i = 0; i < layer[nLayers - 1].size; i++)
			Expected[i] = ExpectedOutput[i];
		
		for (int i = 0; i < nLayers - 1; i++) {
			for (int j = 0; j < layer[i].size; j++) {
				Arrays.fill(layer[i].delta[j], 0f);
			}
		}

		for (int iter = 0; iter < Epochs; iter++){
			Perceptron();
			StochasticBackPropagation();
			
			float error = SquareError();
			if (error < Epsilon)
			    return error;
		}
		
		return SquareError();
	}
	
	// +----------------------------------------------+
	// | Stochastic learning with a new learning rate |
	// +----------------------------------------------+
	
	public float StochasticLearning(float[] Input, float[] ExpectedOutput, float LearningRate){
	    this.LearningRate = LearningRate;
	    
	    return StochasticLearning(Input, ExpectedOutput);
	}
	
	// +------------------------+
	// | Batch Back-Propagation |
	// +------------------------+
	
	private void BatchBackPropagation(int nSamples){
	    initGradients();

		int nLayers = layer.length;
		for (int i = 0; i < nLayers - 1; i++)
			for (int j = 0; j < layer[i].size; j++)
				for (int v = 0; v < layer[i + 1].size; v++){
					float out = layer[i].out[j];
					float theta = layer[i + 1].theta[v];
					
					layer[i].delta[j][v] += (LearningRate * theta * out) / nSamples;
				}
	}
	
	// +----------------+
	// | Batch Learning |
	// +----------------+
	
	public float BatchLearning(int nSamples, float[][] Input, float[][] ExpectedOutput, String WeightsFileName) {
		int nLayers = layer.length;
		for (int iter = 0; iter < Epochs; iter++){
	        System.out.print("Iteration " + iter + ": ");
	        
	        for (int i = 0; i < nLayers - 1; i++) {
				for (int j = 0; j < layer[i].size; j++) {
					Arrays.fill(layer[i].delta[j], 0f);
//					for (int v = 0; v < layer[i + 1].size; v++)
//						layer[i].delta[j][v] = 0.0f;
				}
			}
	        
	        float ReconstructionError = 0.0f;
	        
	        for (int sample = 0; sample < nSamples; sample++){
	            for (int i = 0; i < layer[0].size; i++)
			        layer[0].out[i] = Input[sample][i];

		        for (int i = 0; i < layer[nLayers - 1].size; i++)
			        Expected[i] = ExpectedOutput[sample][i];
			        
			    Perceptron();
			    ReconstructionError += SquareError();
			    
			    BatchBackPropagation(nSamples);
	        }
	        
	        for (int i = 0; i < nLayers - 1; i++)
			    for (int j = 0; j < layer[i].size; j++)
				    for (int v = 0; v < layer[i + 1].size; v++){
					    float RegularizationTerm = LearningRate * Lambda * layer[i].weight[j][v] / nSamples;
					    
					    layer[i].weight[j][v] += layer[i].delta[j][v] - RegularizationTerm;
					}
		    
//		    System.out.println("Saving weights to file");
//		    writeWeights(WeightsFileName);
		    System.out.println("Reconstruction Error = " + (ReconstructionError));
	    }
	    
	    float ReconstructionError = computeReconstructionError(nSamples, Input, ExpectedOutput);
	    System.out.println("Reconstruction Error = " + (ReconstructionError));
	    
	    return ReconstructionError;
	}
	
	// +----------------------------------------------------------------------------------------------------------+ 
	// | Batch learning with new setups for learning rate, regularization parameter and file name to save weights |
	// +----------------------------------------------------------------------------------------------------------+
	
	public float BatchLearning(int nSamples, float[][] Input, float[][] ExpectedOutput, float LearningRate, float Lambda, String WeightsFileName) throws IOException {
	    this.LearningRate = LearningRate;
	    this.Lambda = Lambda;
	    
	    return BatchLearning(nSamples, Input, ExpectedOutput, WeightsFileName);
	}
	
	// +----------------------------------+
	// | Reconstruction Error Computation |
	// +----------------------------------+
	
	public float computeReconstructionError(int nSamples, float[][] Input, float[][] ExpectedOutput){
	    float ReconstructionError = 0.0f;
		int nLayers = layer.length;
	    for (int sample = 0; sample < nSamples; sample++){
	        for (int i = 0; i < layer[0].size; i++)
			    layer[0].out[i] = Input[sample][i];
			        
			Perceptron();

			for (int i = 0; i < layer[nLayers - 1].size; i++)
			    Expected[i] = ExpectedOutput[sample][i];
			    
			ReconstructionError += SquareError();
	    }
	    
	    return ReconstructionError;
	}
	
	// +--------------------------------+
	// | Prediction for a single sample |
	// +--------------------------------+
	
	public void Predict(float[] Input, float[] PredictedOutput){
		for (int i = 0; i < layer[0].size; i++)
			layer[0].out[i] = Input[i];
		
		Perceptron();

		int nLayers = layer.length;

		for (int i = 0; i < layer[nLayers - 1].size; i++)
			PredictedOutput[i] = layer[nLayers - 1].out[i];
	}
	
	// +-------------------------------------------------------------+
	// | Prediction for a single sample and return the norm L2 error |
	// +-------------------------------------------------------------+
	
	public float Predict(float[] Input, float[] ExpectedOutput, float[] PredictedOutput){
		int nLayers = layer.length;

		for (int i = 0; i < layer[0].size; i++)
			layer[0].out[i] = Input[i];
		
		Perceptron();

		for (int i = 0; i < layer[nLayers - 1].size; i++)
			PredictedOutput[i] = layer[nLayers - 1].out[i];

	    for (int i = 0; i < layer[nLayers - 1].size; i++)
		    Expected[i] = ExpectedOutput[i];
		    
		return SquareError();
	}
	
    // +-----------------------------------+
	// | Prediction for the whole database |
	// +-----------------------------------+
	
	public void Predict(int nSamples, float[][] Input, float[][] PredictedOutput){
		int nLayers = layer.length;

		for (int sample = 0; sample < nSamples; sample++){
	        for (int i = 0; i < layer[0].size; i++)
			    layer[0].out[i] = Input[sample][i];
		
		    Perceptron();

		    for (int i = 0; i < layer[nLayers - 1].size; i++)
			    PredictedOutput[sample][i] = layer[nLayers - 1].out[i];
	    }
	}
	
	// +----------------------------------------------------------------+
	// | Prediction for the whole database and return the norm L2 error |
	// +----------------------------------------------------------------+
	
	public float Predict(int nSamples, float[][] Input, float[][] ExpectedOutput, float[][] PredictedOutput){
	    float ReconstructionError = 0.0f;
	    int nLayers = layer.length;
	    for (int sample = 0; sample < nSamples; sample++){
	        for (int i = 0; i < layer[0].size; i++)
			    layer[0].out[i] = Input[sample][i];
		
		    Perceptron();

		    for (int i = 0; i < layer[nLayers - 1].size; i++)
			    PredictedOutput[sample][i] = layer[nLayers - 1].out[i];

			for (int i = 0; i < layer[nLayers - 1].size; i++)
		        Expected[i] = ExpectedOutput[sample][i];
		        
		    ReconstructionError += SquareError();
	    }
	    
	    return ReconstructionError;
	}

}
