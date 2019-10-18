package jcog.learn.ntm.control;

import jcog.learn.ntm.learn.IWeightUpdater;
import jcog.learn.ntm.memory.address.Head;

import java.util.function.Consumer;
import java.util.stream.IntStream;

public class OutputLayer {
    private final int _outputSize;
    private final int controllerSize;
    private final int memoryWidth;
    private final int _headUnitSize;
    
    private final Unit[][] _hiddenToOutputLayerWeights;
    
    private final Unit[][][] _hiddenToHeadsWeights;
    
    public UVector outputs;
    
    public final Head[] heads;

    public OutputLayer(int outputSize, int controllerSize, int headCount, int memoryUnitSizeM) {
        _outputSize = outputSize;
        this.controllerSize = controllerSize;
        memoryWidth = memoryUnitSizeM;
        _headUnitSize = Head.getUnitSize(memoryUnitSizeM);
        _hiddenToOutputLayerWeights = UnitFactory.getTensor2(outputSize, controllerSize + 1);
        _hiddenToHeadsWeights = UnitFactory.getTensor3(headCount, _headUnitSize, controllerSize + 1);
        heads = new Head[headCount];
        outputs = null;
    }

    private OutputLayer(Unit[][] hiddenToOutputLayerWeights, Unit[][][] hiddenToHeadsWeights, UVector outputs, Head[] heads, int outputSize, int controllerSize, int memoryWidth, int headUnitSize) {
        _hiddenToOutputLayerWeights = hiddenToOutputLayerWeights;
        _hiddenToHeadsWeights = hiddenToHeadsWeights;
        this.heads = heads;
        this.controllerSize = controllerSize;
        _outputSize = outputSize;
        this.outputs = outputs;
        this.memoryWidth = memoryWidth;
        _headUnitSize = headUnitSize;
    }

    public void forwardPropagation(HiddenLayer hiddenLayer) {

        final double[] hiddenLayerNeurons = hiddenLayer.neurons.value;

        final double[] out = outputs.value;

        for (int i = 0; i < _outputSize; i++) {
            
            double sum;
            Unit[] weights = _hiddenToOutputLayerWeights[i];

            double result = 0.0;
            int bound = controllerSize;
            for (int j = 0; j < bound; j++) {
                double v = weights[j].value * hiddenLayerNeurons[j];
                result += v;
            }
            sum = result;

            
            sum += weights[controllerSize].value;
            out[i] = Sigmoid.getValue(sum);
        }

        for (int i = 0; i < heads.length; i++) {

            
            Unit[][] headsWeights = _hiddenToHeadsWeights[i];
            final Head head = heads[i];

            for (int j = 0; j < headsWeights.length; j++) {
                double sum;
                Unit[] headWeights = headsWeights[j];

                double result = 0.0;
                int bound = controllerSize;
                for (int k = 0; k < bound; k++) {
                    double v = headWeights[k].value * hiddenLayerNeurons[k];
                    result += v;
                }
                sum = result;

                
                sum += headWeights[controllerSize].value;
                head.get(j).value += sum;
            }
        }
    }

    @Override
    public OutputLayer clone() {


        Head[] heads = Head.getVector(this.heads.length, i -> memoryWidth);
        return new OutputLayer(_hiddenToOutputLayerWeights, _hiddenToHeadsWeights,
                new UVector(_outputSize), heads, _outputSize, controllerSize, memoryWidth, _headUnitSize);

    }

    public void backwardErrorPropagation(final double[] knownOutput, final HiddenLayer hiddenLayer) {

        final Head[] heads = this.heads;

        outputs.setDelta(knownOutput);

        double[] hiddenGrad = hiddenLayer.neurons.grad;
        double[] outGrad = outputs.grad;

        final int cs = this.controllerSize;

        Unit[][] hiddenToOutputLayerWeights = _hiddenToOutputLayerWeights;

        int os = _outputSize;
        for (int j = 0; j < os; j++) {
            

            final double unitGrad = outGrad[j];

            final Unit[] weights = hiddenToOutputLayerWeights[j];


            for (int i = 0; i < cs; i++) {
                hiddenGrad[i] += weights[i].value * unitGrad;
            }
        }
        int hl = heads.length;
        for (int j = 0; j < hl; j++) {
            
            Head head = heads[j];
            Unit[][] weights = _hiddenToHeadsWeights[j];
            for (int k = 0; k < _headUnitSize; k++) {
                final double unitGrad = head.get(k).grad;
                final Unit[] weightsK = weights[k];
                for (int i = 0; i < cs; i++) {
                    hiddenGrad[i] += weightsK[i].value * unitGrad;
                }
            }
        }

        double[] hiddenValue = hiddenLayer.neurons.value;

        for (int i = 0; i < os; i++) {
            
            Unit[] wyh1I = _hiddenToOutputLayerWeights[i];
            final double yGrad = outGrad[i];
            for (int j = 0; j < cs; j++) {
                wyh1I[j].grad += hiddenValue[j] * yGrad;
            }
            wyh1I[controllerSize].grad += yGrad;
        }

        for (int i = 0; i < hl; i++) {
            
            

            Head head = heads[i];
            final Unit[][] units = _hiddenToHeadsWeights[i];
            for (int j = 0; j < _headUnitSize; j++) {
                double headUnitGrad = head.get(j).grad;
                final Unit[] unitJ = units[j];
                for (int k = 0; k < controllerSize; k++) {
                    unitJ[k].grad += headUnitGrad * hiddenValue[k];
                }
                unitJ[controllerSize].grad += headUnitGrad;
            }
        }
    }

    public void updateWeights(Consumer<Unit> updateAction) {
        Consumer<Unit[][]> tensor2UpdateAction = Unit.tensor2UpdateAction(updateAction);
        Consumer<Unit[][][]> tensor3UpdateAction = Unit.tensor3UpdateAction(updateAction);
        tensor2UpdateAction.accept(_hiddenToOutputLayerWeights);
        tensor3UpdateAction.accept(_hiddenToHeadsWeights);
    }

    public void updateWeights(IWeightUpdater weightUpdater) {
        weightUpdater.updateWeight(_hiddenToOutputLayerWeights);
        weightUpdater.updateWeight(_hiddenToHeadsWeights);
    }

    public double[] getOutput() {
        return outputs.value;





    }

    public final double getOutput(final int i) {
        return outputs.value[i];
    }

    public int size() {
        return _outputSize;
    }
}


