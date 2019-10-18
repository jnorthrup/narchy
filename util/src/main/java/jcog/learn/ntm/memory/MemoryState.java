package jcog.learn.ntm.memory;

import jcog.learn.ntm.control.UVector;
import jcog.learn.ntm.control.Unit;
import jcog.learn.ntm.memory.address.GatedAddressing;
import jcog.learn.ntm.memory.address.Head;
import jcog.learn.ntm.memory.address.ShiftedAddressing;
import jcog.learn.ntm.memory.address.content.BetaSimilarity;
import jcog.learn.ntm.memory.address.content.ContentAddressing;
import jcog.learn.ntm.memory.address.content.CosineSimilarityFunction;
import jcog.learn.ntm.memory.address.content.SimilarityMeasure;

public class MemoryState {
    public final NTMMemory memory;
    public final HeadSetting[] heading;
    public final ReadData[] read;

    public MemoryState(NTMMemory memory) {
        this.memory = memory;
        heading = HeadSetting.getVector(memory);
        read = ReadData.getVector(memory, heading);
    }

    public MemoryState(NTMMemory memory, HeadSetting[] headSettings, ReadData[] readDatas) {
        this.memory = memory;
        heading = headSettings;
        read = readDatas;
    }


    public void backwardErrorPropagation() {
        for (ReadData readData : read)
            readData.backwardErrorPropagation();

        memory.backwardErrorPropagation();
        for (HeadSetting headSetting : memory.heading) {
            headSetting.backwardErrorPropagation();
            headSetting.shiftedAddressing.backwardErrorPropagation();
            headSetting.shiftedAddressing.gatedAddressing.backwardErrorPropagation();
            headSetting.shiftedAddressing.gatedAddressing.content.backwardErrorPropagation();
            for (BetaSimilarity similarity : headSetting.shiftedAddressing.gatedAddressing.content.BetaSimilarities) {
                similarity.backwardErrorPropagation();
                similarity.measure.backwardErrorPropagation();
            }
        }
    }

    public void backwardErrorPropagation2() {

        ContentAddressing[] ca = memory.getContentAddressing();


        for (int i = 0; i < read.length; i++) {

            ReadData readI = read[i];
            ContentAddressing cai = ca[i];

            readI.backwardErrorPropagation();

            UVector caiContent = cai.content;

            int s = readI.head.addressingVector.size();

            for (int j = 0; j < s; j++) {
                caiContent.gradAddSelf(j, readI.head.addressingVector.grad[j]);
            }

            cai.backwardErrorPropagation();
        }
    }

    public MemoryState process(Head[] heads) {
        int headCount = heads.length;
        int memoryColumnsN = memory.memoryHeight;
        ReadData[] newReadDatas = new ReadData[headCount];
        HeadSetting[] newHeadSettings = new HeadSetting[headCount];

        Unit[][] memoryData = memory.data;

        for (int i = 0; i < headCount; i++) {
            Head head = heads[i];
            BetaSimilarity[] similarities = new BetaSimilarity[memory.memoryHeight];
            for (int j = 0; j < memoryColumnsN; j++) {

                similarities[j] = new BetaSimilarity(head.getBeta(),
                        new SimilarityMeasure(new CosineSimilarityFunction(), head.getKeyVector(), memoryData[j]));
            }
            ContentAddressing ca = new ContentAddressing(similarities);
            GatedAddressing ga = new GatedAddressing(head.getGate(), ca, heading[i]);
            ShiftedAddressing sa = new ShiftedAddressing(head.getShift(), ga);
            newHeadSettings[i] = new HeadSetting(head.getGamma(), sa);
            newReadDatas[i] = new ReadData(newHeadSettings[i], memory);
        }
        return new MemoryState(
                new NTMMemory(newHeadSettings, heads, memory),
                newHeadSettings, newReadDatas);
    }

}


