package jcog.learn.ntm.control;

import jcog.learn.ntm.learn.IWeightUpdater;
import jcog.learn.ntm.memory.ReadData;

import java.util.Arrays;
import java.util.stream.IntStream;

public class HiddenLayer {
	public final IDifferentiableFunction activation;
	public final int inputs;
	public final int heads;
	public final int memoryUnitSizeM;


	public final UVector hiddenLayerThresholds;


	public final UMatrix inputToHiddenLayerWeights;


	public final Unit[][][] readDataToHiddenLayerWeights;


	public final UVector neurons;

	public HiddenLayer(int controllerSize, int inputSize, int headCount, int memoryUnitSizeM) {
		inputs = inputSize;
		heads = headCount;
		this.memoryUnitSizeM = memoryUnitSizeM;
		this.neurons = new UVector(controllerSize);
		activation = new SigmoidActivation();
		readDataToHiddenLayerWeights = UnitFactory.getTensor3(controllerSize, headCount, memoryUnitSizeM);
		inputToHiddenLayerWeights = new UMatrix(controllerSize, inputSize);
		hiddenLayerThresholds = new UVector(controllerSize);
	}

	private HiddenLayer(Unit[][][] readDataToHiddenLayerWeights, UMatrix inputToHiddenLayerWeights, UVector hiddenLayerThresholds, UVector hiddenLayer, int inputSize, int headCount, int memoryUnitSizeM, IDifferentiableFunction activationFunction) {
		this.readDataToHiddenLayerWeights = readDataToHiddenLayerWeights;
		this.inputToHiddenLayerWeights = inputToHiddenLayerWeights;
		this.hiddenLayerThresholds = hiddenLayerThresholds;
		neurons = hiddenLayer;
		inputs = inputSize;
		heads = headCount;
		this.memoryUnitSizeM = memoryUnitSizeM;
		activation = activationFunction;
	}

	@Override
	public HiddenLayer clone() {
//		try {
			return new HiddenLayer(readDataToHiddenLayerWeights, inputToHiddenLayerWeights, hiddenLayerThresholds,
				new UVector(neurons()),
				inputs, heads, memoryUnitSizeM, activation);
//		} catch (RuntimeException __dummyCatchVar0) {
//			throw __dummyCatchVar0;
//		} catch (Exception __dummyCatchVar0) {
//			throw new RuntimeException(__dummyCatchVar0);
//		}

	}

	public final int neurons() {
		return neurons.size();
	}

	public int inputs() {
		return inputs;
	}


	public void forwardPropagation(double[] i, ReadData[] d) {

		double[] nv = neurons.value;

		double[] hlt = hiddenLayerThresholds.value;

		int N = neurons();

		for (int n = 0; n < N; n++) {
			nv[n] = activation.valueOf(
			    readDataContributionToHiddenLayer(n, d) +
                inputContributionToHiddenLayer(n, i) +
                hlt[n]);
		}
	}

	private double readDataContributionToHiddenLayer(int neuronIndex, ReadData[] readData) {
		Unit[][] readWeightsForEachHead = readDataToHiddenLayerWeights[neuronIndex];
		double tempSum = 0;
		for (int headIndex = 0; headIndex < heads; headIndex++) {
			Unit[] headWeights = readWeightsForEachHead[headIndex];
			ReadData read = readData[headIndex];
            Unit[] r = read.read;
            int bound = memoryUnitSizeM;
            double sum = IntStream.range(0, bound).mapToDouble(memoryCellIndex -> headWeights[memoryCellIndex].value * r[memoryCellIndex].value).sum();
            tempSum += sum;
		}
		return tempSum;
	}

	private double inputContributionToHiddenLayer(int neuronIndex, double[] input) {
        return inputToHiddenLayerWeights.row[neuronIndex].sumDot(input);
	}


	public void updateWeights(IWeightUpdater u) {
		u.updateWeight(readDataToHiddenLayerWeights);
		u.updateWeight(inputToHiddenLayerWeights);
		u.updateWeight(hiddenLayerThresholds);
	}

	public void backwardErrorPropagation(double[] input, ReadData[] reads) {
		double[] hiddenLayerGradients = calculateHiddenLayerGradinets();
		updateReadDataGradient(hiddenLayerGradients, reads);
		updateInputToHiddenWeightsGradients(hiddenLayerGradients, input);
		updateHiddenLayerThresholdsGradients(hiddenLayerGradients);
	}

	private double[] calculateHiddenLayerGradinets() {
		int n = neurons();
        double[] g = this.neurons.grad;
		double[] v = this.neurons.value;
		IDifferentiableFunction a = this.activation;
        double[] hiddenLayerGradients = new double[10];
        int count = 0;
        for (int i = 0; i < n; i++) {
            double derivative = a.derivative(g[i], v[i]);
            if (hiddenLayerGradients.length == count)
                hiddenLayerGradients = Arrays.copyOf(hiddenLayerGradients, count * 2);
            hiddenLayerGradients[count++] = derivative;
        }
        hiddenLayerGradients = Arrays.copyOfRange(hiddenLayerGradients, 0, count);
        return hiddenLayerGradients;
	}

	private void updateReadDataGradient(double[] hiddenLayerGradients, ReadData[] reads) {
        int n = neurons(), h = heads, m = memoryUnitSizeM;
        for (int neuronIndex = 0; neuronIndex < n; neuronIndex++) {
			Unit[][] neuronToReadDataWeights = readDataToHiddenLayerWeights[neuronIndex];
			double hiddenLayerGradient = hiddenLayerGradients[neuronIndex];
			for (int headIndex = 0; headIndex < h; headIndex++) {
				ReadData readData = reads[headIndex];
				Unit[] neuronToHeadReadDataWeights = neuronToReadDataWeights[headIndex];
				Unit[] r = readData.read;
				for (int memoryCellIndex = 0; memoryCellIndex < m; memoryCellIndex++) {
					r[memoryCellIndex].grad += hiddenLayerGradient * neuronToHeadReadDataWeights[memoryCellIndex].value;
					neuronToHeadReadDataWeights[memoryCellIndex].grad += hiddenLayerGradient * r[memoryCellIndex].value;
				}
			}
		}
	}

	private void updateInputToHiddenWeightsGradients(double[] hiddenLayerGradients, double[] input) {
		int n = neurons();
		UVector[] inputToHiddenLayerWeights = this.inputToHiddenLayerWeights.row;
		for (int i = 0; i < n; i++) {
			double hiddenGradient = hiddenLayerGradients[i];
			updateInputGradient(hiddenGradient, inputToHiddenLayerWeights[i], input);
		}
	}

	private void updateInputGradient(double hiddenLayerGradient, UVector inputToHiddenNeuronWeights, double[] input) {
		double[] g = inputToHiddenNeuronWeights.grad;
		int n = this.inputs;
		for (int i = 0; i < n; i++)
			g[i] += hiddenLayerGradient * input[i];
	}

	private void updateHiddenLayerThresholdsGradients(double[] hiddenLayerGradients) {
		double[] hgrad = hiddenLayerThresholds.grad;
		int n = neurons();
		for (int i = 0; i < n; i++)
			hgrad[i] += hiddenLayerGradients[i];
	}

}


