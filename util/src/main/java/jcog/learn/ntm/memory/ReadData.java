package jcog.learn.ntm.memory;

import jcog.learn.ntm.control.UVector;
import jcog.learn.ntm.control.Unit;

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
        for (var i = 0; i < cellWidth; i++) {
            var temp = 0.0;
            for (var j = 0; j < cellHeight; j++) {
                temp += head.addressingVector.value[j] * mem.data[j][i].value;
            }
            
            
            
            
            read[i] = new Unit(temp);
        }
    }

    public void backwardErrorPropagation() {
        var addressingVectorUnit = head.addressingVector;

        var memData = memory.data;

        var h = this.cellHeight;
        var w = this.cellWidth;
        var read = this.read;

        for (var i = 0; i < h; i++) {
            var gradient = 0.0;

            var dataVector = memData[i];

            for (var j = 0; j < w; j++) {

                var readUnitGradient = read[j].grad;
                var dataUnit = dataVector[j];
                gradient += readUnitGradient * dataUnit.value;
                dataUnit.grad += readUnitGradient * addressingVectorUnit.value[i];
            }
            addressingVectorUnit.grad[i] += gradient;
        }
    }

    /** TODO return UMatrix of ReadData UVector's */
    public static ReadData[] getVector(NTMMemory memory, HeadSetting[] h) {
        var x = memory.headNum();

        var vector = IntStream.range(0, x).mapToObj(i -> new ReadData(h[i], memory)).toArray(ReadData[]::new);
        return vector;
    }

}


