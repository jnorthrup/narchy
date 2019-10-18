package jcog.learn.ntm.memory;

import jcog.learn.ntm.control.UVector;
import jcog.learn.ntm.control.Unit;
import jcog.learn.ntm.memory.address.ShiftedAddressing;
import jcog.learn.ntm.memory.address.content.ContentAddressing;

import java.util.ArrayList;
import java.util.List;
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

        double gammaIndex = getGammaIndex();


        int cellCount = getShiftedVector().length;

        addressingVector = new UVector(cellCount);

        

        double[] addr = addressingVector.value;

        Unit[] sv = getShiftedVector();
        double sum;
        int bound = cellCount;
        sum = IntStream.range(0, bound).mapToDouble(i -> (addr[i] = Math.pow(sv[i].value, gammaIndex))).sum();

        addressingVector.valueMultiplySelf(1.0/sum);
        
    }

    public HeadSetting(Unit gamma, int memoryColumnsN, ContentAddressing contentAddressing) {
        this.gamma = gamma;
        this.shiftedAddressing = null;

        addressingVector = new UVector(memoryColumnsN);

        double[] addr = addressingVector.value;
        for (int i = 0;i < memoryColumnsN;i++) {
            addr[i] = contentAddressing.content.value(i);
        }
    }

    public void backwardErrorPropagation() {

        Unit[] sv = getShiftedVector();
        int cells = sv.length;

        double[] lns = new double[cells];
        double[] temps = new double[cells];


        double gammaIndex = getGammaIndex();

        double[] addrValue = addressingVector.value;
        double[] addrGrad = addressingVector.grad;

        int bound1 = cells;
        for (int i1 = 0; i1 < bound1; i1++) {
            Unit weight = sv[i1];
            double weightValue = weight.value;
            if (weightValue < NTMMemory.EPSILON) {
                continue;
            }


            double gradient = 0.0;

            for (int j = 0; j < cells; j++) {

                double dataWeightValue = addrValue[j];
                double dataWeightGradient = addrGrad[j];
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

        double s = 0.0;
        double lnexp = 0.0;
        for (int i = 0;i < cells;i++) {
            lnexp += lns[i] * temps[i];
            s += temps[i];
        }
        double lnexps = lnexp / s;
        double gradient2;
        int bound = cells;
        gradient2 = IntStream.range(0, bound).filter(i -> !(sv[i].value < NTMMemory.EPSILON)).mapToDouble(i -> addrGrad[i] * (addrValue[i] * (lns[i] - lnexps))).sum();


        gradient2 /= (1.0 + Math.exp(-gamma.value));
        gamma.grad += gradient2;
    }

    public static HeadSetting[] getVector(NTMMemory memory) {
        int x = memory.headNum();

        int bound = x;
        HeadSetting[] vector = IntStream.range(0, bound).mapToObj(i -> new HeadSetting(
                new Unit(0.0),
                memory.memoryHeight,
                memory.getContentAddressing()[i])).toArray(HeadSetting[]::new);

        return vector;
    }


}


