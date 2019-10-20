package jcog.learn.ntm;

import jcog.learn.ntm.control.FeedForwardController;
import jcog.learn.ntm.learn.IWeightUpdater;
import jcog.learn.ntm.memory.MemoryState;
import jcog.learn.ntm.memory.NTMMemory;
import jcog.learn.ntm.memory.address.Head;

public class NTM implements AbstractNTM
{
    public final FeedForwardController control;
    public final NTMMemory memory;

    private MemoryState prev;
    private MemoryState now;

    /** current input */
    private double[] prevInput;

    public NTM(NTM oldMachine) {
        control = oldMachine.control.clone();
        memory = oldMachine.memory;
        now = oldMachine.getNow();
        prev = oldMachine.getPrev();
        prevInput = null;
    }

    public NTM(int inputSize, int outputSize, int controllerSize, int headCount, int memoryHeight, int memoryWidth, IWeightUpdater initializer) {
        memory = new NTMMemory(memoryHeight,memoryWidth,headCount);
        control = new FeedForwardController(controllerSize,inputSize,outputSize,headCount,memoryWidth);
        now = prev = null;
        prevInput = null;
        updateWeights(initializer);
    }


    public final void updateWeights(IWeightUpdater weightUpdater) {
        memory.updateWeights(weightUpdater);
        control.updateWeights(weightUpdater);
    }

    @Override
    public void process(double[] input) {
        this.prevInput = input;

        prev = now;
        control.process(input, prev.read);
        now = prev.process(getHeads());
    }

    @Override
    public double[] getOutput() {
        return control.getOutput();
    }


    public double getOutput(int i) {
        return control.getOutput(i);
    }

    public MemoryState getNow() {
        return now;
    }

    public MemoryState getPrev() {
        return prev;
    }

    public Head[] getHeads() {
        return control.output.heads;
    }

    public void initializeMemoryState() {
        now = new MemoryState(memory);
        
    }

    public void backwardErrorPropagation(double[] knownOutput) {
        now.backwardErrorPropagation();
        control.backwardErrorPropagation(knownOutput, prevInput, prev.read);
    }

    public void backwardErrorPropagation() {
        now.backwardErrorPropagation2();
    }


    public double getInput(int i) {
        if (prevInput!=null)
            return prevInput[i];
        return (double) 0;
    }

    public int inputSize() {
        return control.inputSize();
    }
    public int outputSize() {
        return control.outputSize();
    }
}


