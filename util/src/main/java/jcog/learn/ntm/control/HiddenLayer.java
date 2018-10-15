package jcog.learn.ntm.control;

import jcog.learn.ntm.learn.IWeightUpdater;
import jcog.learn.ntm.memory.ReadData;

public class HiddenLayer   
{
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
        readDataToHiddenLayerWeights = UnitFactory.getTensor3(controllerSize,headCount,memoryUnitSizeM);
        inputToHiddenLayerWeights = new UMatrix(controllerSize,inputSize);
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
        try
        {
            return new HiddenLayer(readDataToHiddenLayerWeights, inputToHiddenLayerWeights, hiddenLayerThresholds,
                    new UVector(neurons()),
                    inputs, heads, memoryUnitSizeM, activation);
        }
        catch (RuntimeException __dummyCatchVar0)
        {
            throw __dummyCatchVar0;
        }
        catch (Exception __dummyCatchVar0)
        {
            throw new RuntimeException(__dummyCatchVar0);
        }
    
    }

    public final int neurons() {
        return neurons.size();
    }
    public int inputs() {
        return inputs;
    }


    
    public void forwardPropagation(double[] input, ReadData[] readData) {

        final double[] nv = neurons.value;

        final double[] hlt = hiddenLayerThresholds.value;

        final int N = neurons();

        for (int neuronIndex = 0; neuronIndex < N; neuronIndex++) {
            
            double sum = 0.0;
            sum += getReadDataContributionToHiddenLayer(neuronIndex, readData);
            sum += getInputContributionToHiddenLayer(neuronIndex, input);

            
            sum += hlt[neuronIndex];

            
            nv[neuronIndex] = activation.value(sum);
        }
    }

    private double getReadDataContributionToHiddenLayer(int neuronIndex, ReadData[] readData) {
        Unit[][] readWeightsForEachHead = readDataToHiddenLayerWeights[neuronIndex];
        double tempSum = 0;
        for (int headIndex = 0;headIndex < heads;headIndex++)
        {
            Unit[] headWeights = readWeightsForEachHead[headIndex];
            ReadData read = readData[headIndex];
            for (int memoryCellIndex = 0;memoryCellIndex < memoryUnitSizeM;memoryCellIndex++)
            {
                tempSum += headWeights[memoryCellIndex].value * read.read[memoryCellIndex].value;
            }
        }
        return tempSum;
    }

    private double getInputContributionToHiddenLayer(int neuronIndex, double[] input) {
        UVector inputWeights = inputToHiddenLayerWeights.row(neuronIndex);
        return inputWeights.sumDot(input);
    }











    public void updateWeights(IWeightUpdater weightUpdater) {
        weightUpdater.updateWeight(readDataToHiddenLayerWeights);
        weightUpdater.updateWeight(inputToHiddenLayerWeights);
        weightUpdater.updateWeight(hiddenLayerThresholds);
    }

    public void backwardErrorPropagation(double[] input, ReadData[] reads) {
        double[] hiddenLayerGradients = calculateHiddenLayerGradinets();
        updateReadDataGradient(hiddenLayerGradients, reads);
        updateInputToHiddenWeightsGradients(hiddenLayerGradients, input);
        updateHiddenLayerThresholdsGradients(hiddenLayerGradients);
    }

    private double[] calculateHiddenLayerGradinets() {
        double[] hiddenLayerGradients = new double[neurons()];
        for (int i = 0;i < neurons();i++) {
            
            hiddenLayerGradients[i] = activation.derivative(neurons.grad(i),  neurons.value(i));

            
            
        }
        return hiddenLayerGradients;
    }

    private void updateReadDataGradient(double[] hiddenLayerGradients, ReadData[] reads) {
        for (int neuronIndex = 0;neuronIndex < neurons(); neuronIndex++) {
            Unit[][] neuronToReadDataWeights = readDataToHiddenLayerWeights[neuronIndex];
            double hiddenLayerGradient = hiddenLayerGradients[neuronIndex];
            for (int headIndex = 0;headIndex < heads;headIndex++) {
                ReadData readData = reads[headIndex];
                Unit[] neuronToHeadReadDataWeights = neuronToReadDataWeights[headIndex];
                for (int memoryCellIndex = 0;memoryCellIndex < memoryUnitSizeM;memoryCellIndex++) {
                    readData.read[memoryCellIndex].grad += hiddenLayerGradient * neuronToHeadReadDataWeights[memoryCellIndex].value;
                    neuronToHeadReadDataWeights[memoryCellIndex].grad += hiddenLayerGradient * readData.read[memoryCellIndex].value;
                }
            }
        }
    }

    private void updateInputToHiddenWeightsGradients(double[] hiddenLayerGradients, double[] input) {
        for (int neuronIndex = 0;neuronIndex < neurons(); neuronIndex++) {
            double hiddenGradient = hiddenLayerGradients[neuronIndex];
            UVector inputToHiddenNeuronWeights = inputToHiddenLayerWeights.row(neuronIndex);
            updateInputGradient(hiddenGradient, inputToHiddenNeuronWeights, input);
        }
    }

    private void updateInputGradient(double hiddenLayerGradient, UVector inputToHiddenNeuronWeights, double[] input) {
        double[] g = inputToHiddenNeuronWeights.grad;
        for (int inputIndex = 0;inputIndex < inputs;inputIndex++) {
            g[inputIndex] += hiddenLayerGradient * input[inputIndex];
        }
    }

    private void updateHiddenLayerThresholdsGradients(final double[] hiddenLayerGradients) {
        final double[] hgrad = hiddenLayerThresholds.grad;
        for (int neuronIndex = 0;neuronIndex < neurons(); neuronIndex++) {
            hgrad[neuronIndex] += hiddenLayerGradients[neuronIndex];
        }
    }

}


