package jcog.learn.lstm;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import static org.apache.commons.math3.util.MathArrays.scaleInPlace;

public class SimpleLSTM  {

	public double[] out;
	public double[] in;

    private final int full_input_dimension;
	private final int output_dimension;
	private final int cell_blocks;
	private final Neuron F;
	private final Neuron G;
	
	private final double [] context;
	
	private final double [][] weightsF;
	private final double [][] weightsG;
	public final double [][] weightsOut;
	
	
	private final double [][] dSdF;
	private final double [][] dSdG;


    private double[] sumF;
	private double[] actF;
	private double[] sumG;
	private double[] actG;
	private double[] actH;
	public double[] full_hidden;
	public double[] deltaOut;
	private double[] deltaH;

	public SimpleLSTM(Random r, int input_dimension, int output_dimension, int cell_blocks)
	{
		this.output_dimension = output_dimension;
		this.cell_blocks = cell_blocks;
		
		context = new double[cell_blocks];
		
		full_input_dimension = input_dimension + cell_blocks + 1;

        NeuronType neuron_type_F = NeuronType.Sigmoid;
        F = Neuron.build(neuron_type_F);
        NeuronType neuron_type_G = NeuronType.Sigmoid;
        G = Neuron.build(neuron_type_G);
		
		weightsF = new double[cell_blocks][full_input_dimension];
		weightsG = new double[cell_blocks][full_input_dimension];
		
		dSdF = new double[cell_blocks][full_input_dimension];
		dSdG = new double[cell_blocks][full_input_dimension];

        double init_weight_range = 1.0;
        for (int i = 0; i < full_input_dimension; i++) {
			for (int j = 0; j < cell_blocks; j++) {
				weightsF[j][i] = (r.nextDouble() * 2.0d - 1d) * init_weight_range;
				weightsG[j][i] = (r.nextDouble() * 2.0d - 1d) * init_weight_range;
			}
		}
		
		weightsOut = new double[output_dimension][cell_blocks + 1];
		
		for (int j = 0; j < cell_blocks + 1; j++) {
			for (int k = 0; k < output_dimension; k++)
				weightsOut[k][j] = (r.nextDouble() * 2.0d - 1d) * init_weight_range;
		}
	}



	public void clear()	{

		Arrays.fill(context, 0.0);

		
		for (int c = 0; c < cell_blocks; c++) {
			Arrays.fill(this.dSdG[c], 0.0);
			Arrays.fill(this.dSdF[c], 0.0);
		}

	}

	/** 0 = total forget, 1 = no forget. proportional version of the RESET operation  */
	public void forget(float forgetRate) {

		float scalingFactor = 1f - forgetRate;

		if (scalingFactor >= 1)
			return; 

		if (scalingFactor <= 0) {
			clear();
			return;
		}

		scaleInPlace(scalingFactor, context);
		for (int c = 0; c < cell_blocks; c++)
			scaleInPlace(scalingFactor, this.dSdG[c]);
		for (int c = 0; c < cell_blocks; c++)
			scaleInPlace(scalingFactor, this.dSdF[c]);


	}

	public double[] predict(double[] input)
	{
		return learn(input, null, -1);
	}


	public double[] learn(double[] input, @Nullable double[] target_output, float learningRate) {

		int cell_blocks = this.cell_blocks, full_input_dimension = this.full_input_dimension;

		


		if ((this.in == null) || (this.in.length != full_input_dimension)) {
			this.in = new double[full_input_dimension];
		}
		double[] full_input = this.in;

		int loc = 0;
		for (int i = 0; i < input.length; ) {
			full_input[loc++] = input[i++];
		}
		for (int c = 0; c < context.length; ) {
			full_input[loc++] = context[c++];
		}
		full_input[loc++] = 1.0; 


		
		if ((sumF == null) || (sumF.length!=cell_blocks)) {
			sumF = new double[cell_blocks];
			actF = new double[cell_blocks];
			sumG = new double[cell_blocks];
			actG = new double[cell_blocks];
			actH = new double[cell_blocks];
			full_hidden = new double[cell_blocks + 1];
			out = new double[output_dimension];
		}
		else {
			
			
			
			
			
		}
		double[] full_hidden = this.full_hidden;

		
		for (int j = 0; j < cell_blocks; j++) {
			double[] wj = weightsF[j];
			double[] wg = weightsG[j];
			double sf = 0, sg = 0;
			for (int i = 0; i < full_input_dimension; i++)			{
				double fi = full_input[i];
				sf += wj[i] * fi;
				sg += wg[i] * fi;
			}
			sumF[j] = sf;
			sumG[j] = sg;
		}
		
		for (int j = 0; j < cell_blocks; j++) {
			double actfj = actF[j] = F.activate(sumF[j]);
			double actgj = actG[j] = G.activate(sumG[j]);


			
			full_hidden[j] = actH[j] = actfj * context[j] + (1.0 - actfj) * actgj;
		}
		
		full_hidden[cell_blocks] = 1.0; 
		
		
		for (int k = 0; k < output_dimension; k++)
		{
            double[] wk = weightsOut[k];
            int bound = cell_blocks + 1;
            double s = IntStream.range(0, bound).mapToDouble(j -> wk[j] * full_hidden[j]).sum();

            out[k] = s;
		}

		
		
		
		
		
		
		
		for (int j = 0; j < cell_blocks; j++) {
			
			double f = actF[j];
			double df = F.derivate(sumF[j]);
			double g = actG[j];
			double dg = G.derivate(sumG[j]);
			double h_ = context[j]; 

			double[] dsg = dSdG[j];
			double[] dsf = dSdF[j];

			for (int i = 0; i < full_input_dimension; i++) {
				
				double prevdSdF = dsf[i];
				double prevdSdG = dsg[i];
				double in = full_input[i];
				
				dsg[i] = ((1.0 - f)*dg*in) + (f*prevdSdG);
				dsf[i] = ((h_- g)*df*in) + (f*prevdSdF);
			}
		}
		
		if (target_output != null) {
			
			

			if ((deltaOut == null) || (deltaOut.length!=output_dimension)) {
				deltaOut = new double[output_dimension];
				deltaH = new double[cell_blocks];
			}
			else {
				
				Arrays.fill(deltaH, 0);
			}

            double SCALE_OUTPUT_DELTA = 1.0;
            double outputDeltaScale = SCALE_OUTPUT_DELTA;

			for (int k = 0; k < output_dimension; k++) {

				double dok  = deltaOut[k] = (target_output[k] - out[k]) * outputDeltaScale;

				double[] wk = weightsOut[k];

				double[] dh = this.deltaH;
				double[] ah = this.actH;
				for (int j = cell_blocks - 1; j >= 0; j--) {
					dh[j] += dok * wk[j];
					wk[j] += dok * ah[j] * learningRate;
				}

				
				wk[cell_blocks] += dok /* * 1.0 */ * learningRate;
			}
			
			
			for (int j = 0; j < cell_blocks; j++) {
				double dhj = deltaH[j];
				updateWeights(learningRate * dhj, full_input_dimension, dSdF[j], weightsF[j]);
				updateWeights(learningRate * dhj, full_input_dimension, dSdG[j], weightsG[j]);
			}
		}
		
		
		
		
		System.arraycopy(actH, 0, context, 0, cell_blocks);
		
		
		return out;
	}

	public static void updateWeights(double learningRate,
									 int length,
									 double[] in,
									 double[] out) {
		for (int i = length - 1; i >= 0; i--) {
			out[i] += in[i] * learningRate;
		}
	}




}


