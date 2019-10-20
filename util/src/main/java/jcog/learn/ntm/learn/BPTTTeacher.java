package jcog.learn.ntm.learn;

import jcog.learn.ntm.NTM;

public class BPTTTeacher implements INTMTeacher
{
    private final NTM machine;
    private final IWeightUpdater weightUpdater;
    private final IWeightUpdater gradientResetter;
    public BPTTTeacher(NTM machine, IWeightUpdater weightUpdater) {
        this.machine = machine;
        this.weightUpdater = weightUpdater;
        gradientResetter = new GradientResetter();
    }

    public NTM getMachine() {
        return machine;
    }


    @Override
    public NTM[] trainInternal(double[][] input, double[][] knownOutput) {

        var machines = new NTM[input.length];
        machine.initializeMemoryState();

        

        machines[0] = new NTM(machine);
        machines[0].process(input[0]);
        for (var i = 1; i < input.length; i++) {
            machines[i] = new NTM(machines[i - 1]);
            machines[i].process(input[i]);
        }

        
        gradientResetter.reset();
        machine.updateWeights(gradientResetter);

        
        for (var i = input.length - 1; i >= 0; i--)        {
            machines[i].backwardErrorPropagation(knownOutput[i]);
        }
        machine.backwardErrorPropagation();

        
        weightUpdater.reset();
        machine.updateWeights(weightUpdater);

        return machines;
    }



}


