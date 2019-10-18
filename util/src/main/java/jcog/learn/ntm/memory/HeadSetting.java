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


        final int cellCount = getShiftedVector().length;

        addressingVector = new UVector(cellCount);

        

        final double[] addr = addressingVector.value;

        final Unit[] sv = getShiftedVector();
        double sum = 0.0;
        int bound = cellCount;
        for (int i = 0; i < bound; i++) {
            double v = (addr[i] = Math.pow(sv[i].value, gammaIndex));
            sum += v;
        }

        addressingVector.valueMultiplySelf(1.0/sum);
        
    }

    public HeadSetting(Unit gamma, int memoryColumnsN, ContentAddressing contentAddressing) {
        this.gamma = gamma;
        this.shiftedAddressing = null;

        addressingVector = new UVector(memoryColumnsN);

        final double[] addr = addressingVector.value;
        for (int i = 0;i < memoryColumnsN;i++) {
            addr[i] = contentAddressing.content.value(i);
        }
    }

    public void backwardErrorPropagation() {

        final Unit[] sv = getShiftedVector();
        final int cells = sv.length;

        double[] lns = new double[cells];
        double[] temps = new double[cells];


        final double gammaIndex = getGammaIndex();

        final double[] addrValue = addressingVector.value;
        final double[] addrGrad = addressingVector.grad;

        int bound1 = cells;
        for (int i1 = 0; i1 < bound1; i1++) {
            Unit weight = sv[i1];
            double weightValue = weight.value;
            if (weightValue < NTMMemory.EPSILON) {
                continue;
            }


            double gradient = 0.0;

            for (int j = 0; j < cells; j++) {

                final double dataWeightValue = addrValue[j];
                final double dataWeightGradient = addrGrad[j];
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
        double gradient2 = 0.0;
        int bound = cells;
        for (int i = 0; i < bound; i++) {
            if (!(sv[i].value < NTMMemory.EPSILON)) {
                double v = addrGrad[i] * (addrValue[i] * (lns[i] - lnexps));
                gradient2 += v;
            }
        }


        gradient2 /= (1.0 + Math.exp(-gamma.value));
        gamma.grad += gradient2;
    }

    public static HeadSetting[] getVector(NTMMemory memory) {
        final int x = memory.headNum();

        List<HeadSetting> list = new ArrayList<>();
        int bound = x;
        for (int i = 0; i < bound; i++) {
            HeadSetting headSetting = new HeadSetting(
                    new Unit(0.0),
                    memory.memoryHeight,
                    memory.getContentAddressing()[i]);
            list.add(headSetting);
        }
        HeadSetting[] vector = list.toArray(new HeadSetting[0]);

        return vector;
    }


}


