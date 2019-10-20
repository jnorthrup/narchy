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

        var hiddenLayerNeurons = hiddenLayer.neurons.value;

        var out = outputs.value;

        for (var i = 0; i < _outputSize; i++) {

            var weights = _hiddenToOutputLayerWeights[i];

            var bound = controllerSize;
            var result = IntStream.range(0, bound).mapToDouble(j -> weights[j].value * hiddenLayerNeurons[j]).sum();
            var sum = result;


            sum += weights[controllerSize].value;
            out[i] = Sigmoid.getValue(sum);
        }

        for (var i = 0; i < heads.length; i++) {


            var headsWeights = _hiddenToHeadsWeights[i];
            var head = heads[i];

            for (var j = 0; j < headsWeights.length; j++) {
                var headWeights = headsWeights[j];

                var bound = controllerSize;
                var result = IntStream.range(0, bound).mapToDouble(k -> headWeights[k].value * hiddenLayerNeurons[k]).sum();
                var sum = result;


                sum += headWeights[controllerSize].value;
                head.get(j).value += sum;
            }
        }
    }

    @Override
    public OutputLayer clone() {


        var heads = Head.getVector(this.heads.length, i -> memoryWidth);
        return new OutputLayer(_hiddenToOutputLayerWeights, _hiddenToHeadsWeights,
                new UVector(_outputSize), heads, _outputSize, controllerSize, memoryWidth, _headUnitSize);

    }

    public void backwardErrorPropagation(double[] knownOutput, HiddenLayer hiddenLayer) {

        var heads = this.heads;

        outputs.setDelta(knownOutput);

        var hiddenGrad = hiddenLayer.neurons.grad;
        var outGrad = outputs.grad;

        var cs = this.controllerSize;

        var hiddenToOutputLayerWeights = _hiddenToOutputLayerWeights;

        var os = _outputSize;
        for (var j = 0; j < os; j++) {


            var unitGrad = outGrad[j];

            var weights = hiddenToOutputLayerWeights[j];


            for (var i = 0; i < cs; i++) {
                hiddenGrad[i] += weights[i].value * unitGrad;
            }
        }
        var hl = heads.length;
        for (var j = 0; j < hl; j++) {

            var head = heads[j];
            var weights = _hiddenToHeadsWeights[j];
            for (var k = 0; k < _headUnitSize; k++) {
                var unitGrad = head.get(k).grad;
                var weightsK = weights[k];
                for (var i = 0; i < cs; i++) {
                    hiddenGrad[i] += weightsK[i].value * unitGrad;
                }
            }
        }

        var hiddenValue = hiddenLayer.neurons.value;

        for (var i = 0; i < os; i++) {

            var wyh1I = _hiddenToOutputLayerWeights[i];
            var yGrad = outGrad[i];
            for (var j = 0; j < cs; j++) {
                wyh1I[j].grad += hiddenValue[j] * yGrad;
            }
            wyh1I[controllerSize].grad += yGrad;
        }

        for (var i = 0; i < hl; i++) {


            var head = heads[i];
            var units = _hiddenToHeadsWeights[i];
            for (var j = 0; j < _headUnitSize; j++) {
                var headUnitGrad = head.get(j).grad;
                var unitJ = units[j];
                for (var k = 0; k < controllerSize; k++) {
                    unitJ[k].grad += headUnitGrad * hiddenValue[k];
                }
                unitJ[controllerSize].grad += headUnitGrad;
            }
        }
    }

    public void updateWeights(Consumer<Unit> updateAction) {
        var tensor2UpdateAction = Unit.tensor2UpdateAction(updateAction);
        var tensor3UpdateAction = Unit.tensor3UpdateAction(updateAction);
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

    public final double getOutput(int i) {
        return outputs.value[i];
    }

    public int size() {
        return _outputSize;
    }
}


