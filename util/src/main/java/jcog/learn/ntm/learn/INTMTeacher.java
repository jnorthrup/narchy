package jcog.learn.ntm.learn;

import jcog.learn.ntm.NTM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@FunctionalInterface
public interface INTMTeacher {

    default List<double[]> trainAndGetOutput(double[][] input, double[][] knownOutput) {

        var machines = trainInternal(input, knownOutput);
        return getMachineOutputs(machines);

    }

    default NTM[] train(double[][] input, double[][] knownOutput) {
        return trainInternal(input, knownOutput);
    }

    NTM[] trainInternal(double[][] input, double[][] knownOutput);

    static List<double[]> getMachineOutputs(NTM[] machines) {
        List<double[]> realOutputs = Arrays.stream(machines).map(NTM::getOutput).collect(Collectors.toCollection(() -> new ArrayList<>(machines.length)));
        return realOutputs;
    }
}


