package jcog.learn.ntm.memory;

import jcog.learn.ntm.control.UVector;
import jcog.learn.ntm.control.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/** TODO extend UVector for 'read' */
public class ReadData  {
    public final HeadSetting head;
    public final Unit[] read;
    private final NTMMemory memory;
    private final int cellWidth;
    private final int cellHeight;

    public ReadData(HeadSetting head, NTMMemory mem) {
        this.head = head;
        memory = mem;
        cellWidth = memory.memoryWidth;
        cellHeight = memory.memoryHeight;

        read = new Unit[cellWidth];
        for (int i = 0;i < cellWidth;i++) {
            double temp = 0.0;
            for (int j = 0;j < cellHeight;j++) {
                temp += head.addressingVector.value[j] * mem.data[j][i].value;
            }
            
            
            
            
            read[i] = new Unit(temp);
        }
    }

    public void backwardErrorPropagation() {
        UVector addressingVectorUnit = head.addressingVector;

        Unit[][] memData = memory.data;

        int h = this.cellHeight;
        int w = this.cellWidth;
        Unit[] read = this.read;

        for (int i = 0; i < h; i++) {
            double gradient = 0.0;

            Unit[] dataVector = memData[i];

            for (int j = 0; j < w; j++) {

                double readUnitGradient = read[j].grad;
                Unit dataUnit = dataVector[j];
                gradient += readUnitGradient * dataUnit.value;
                dataUnit.grad += readUnitGradient * addressingVectorUnit.value[i];
            }
            addressingVectorUnit.grad[i] += gradient;
        }
    }

    /** TODO return UMatrix of ReadData UVector's */
    public static ReadData[] getVector(NTMMemory memory, HeadSetting[] h) {
        int x = memory.headNum();

        List<ReadData> list = new ArrayList<>();
        for (int i = 0; i < x; i++) {
            ReadData readData = new ReadData(h[i], memory);
            list.add(readData);
        }
        ReadData[] vector = list.toArray(new ReadData[0]);
        return vector;
    }

}


