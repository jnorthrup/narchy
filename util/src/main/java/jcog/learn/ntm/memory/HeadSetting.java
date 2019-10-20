package jcog.learn.ntm.memory;

import jcog.learn.ntm.control.UVector;
import jcog.learn.ntm.control.Unit;
import jcog.learn.ntm.memory.address.ShiftedAddressing;
import jcog.learn.ntm.memory.address.content.ContentAddressing;

import java.util.stream.IntStream;

public class HeadSetting   
{
    public final UVector addressingVector;
    public final ShiftedAddressing shiftedAddressing;
    public final Unit gamma;


    public final Unit[] getShiftedVector() {
        return shiftedAddressing.shifted;
    }

    public final double getGammaIndex() {
        return Math.log(Math.exp(gamma.value) + 1.0) + 1.0;
    }

    public HeadSetting(Unit gamma, ShiftedAddressing shiftedAddressing) {
        this.gamma = gamma;

        this.shiftedAddressing = shiftedAddressing;

        var gammaIndex = getGammaIndex();


        var cellCount = getShiftedVector().length;

        addressingVector = new UVector(cellCount);


        var addr = addressingVector.value;

        var sv = getShiftedVector();
        var bound = cellCount;
        var sum = IntStream.range(0, bound).mapToDouble(i -> (addr[i] = Math.pow(sv[i].value, gammaIndex))).sum();

        addressingVector.valueMultiplySelf(1.0/sum);
        
    }

    public HeadSetting(Unit gamma, int memoryColumnsN, ContentAddressing contentAddressing) {
        this.gamma = gamma;
        this.shiftedAddressing = null;

        addressingVector = new UVector(memoryColumnsN);

        var addr = addressingVector.value;
        for (var i = 0; i < memoryColumnsN; i++) {
            addr[i] = contentAddressing.content.value(i);
        }
    }

    public void backwardErrorPropagation() {

        var sv = getShiftedVector();
        var cells = sv.length;

        var lns = new double[cells];
        var temps = new double[cells];


        var gammaIndex = getGammaIndex();

        var addrValue = addressingVector.value;
        var addrGrad = addressingVector.grad;

        var bound1 = cells;
        for (var i1 = 0; i1 < bound1; i1++) {
            var weight = sv[i1];
            var weightValue = weight.value;
            if (weightValue < NTMMemory.EPSILON) {
                continue;
            }


            var gradient = 0.0;

            for (var j = 0; j < cells; j++) {

                var dataWeightValue = addrValue[j];
                var dataWeightGradient = addrGrad[j];
                if (i1 == j) {
                    gradient += dataWeightGradient * (1.0 - dataWeightValue);
                } else {
                    gradient -= dataWeightGradient * dataWeightValue;
                }
            }
            gradient = ((gradient * gammaIndex) / weightValue) * addrValue[i1];
            weight.grad += gradient;

            lns[i1] = Math.log(weightValue);
            temps[i1] = Math.pow(weightValue, gammaIndex);
        }

        var s = 0.0;
        var lnexp = 0.0;
        for (var i = 0; i < cells; i++) {
            lnexp += lns[i] * temps[i];
            s += temps[i];
        }
        var lnexps = lnexp / s;
        var bound = cells;
        var gradient2 = IntStream.range(0, bound).filter(i -> !(sv[i].value < NTMMemory.EPSILON)).mapToDouble(i -> addrGrad[i] * (addrValue[i] * (lns[i] - lnexps))).sum();


        gradient2 /= (1.0 + Math.exp(-gamma.value));
        gamma.grad += gradient2;
    }

    public static HeadSetting[] getVector(NTMMemory memory) {
        var x = memory.headNum();

        var bound = x;
        var vector = IntStream.range(0, bound).mapToObj(i -> new HeadSetting(
                new Unit(0.0),
                memory.memoryHeight,
                memory.getContentAddressing()[i])).toArray(HeadSetting[]::new);

        return vector;
    }


}


