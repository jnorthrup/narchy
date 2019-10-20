package jcog.learn.ntm.memory;

import jcog.learn.ntm.control.Sigmoid;
import jcog.learn.ntm.control.Unit;
import jcog.learn.ntm.control.UnitFactory;
import jcog.learn.ntm.learn.IWeightUpdater;
import jcog.learn.ntm.memory.address.Head;
import jcog.learn.ntm.memory.address.content.BetaSimilarity;
import jcog.learn.ntm.memory.address.content.ContentAddressing;

import java.lang.ref.WeakReference;
import java.util.stream.IntStream;

public class NTMMemory {

    public final Unit[][] data;
    public final HeadSetting[] heading;
    private final Head[] heads;
    public final WeakReference<NTMMemory> parent;
    private final BetaSimilarity[][] oldSimilar;
    private final double[][] erase;
    public final double[][] add;
    final int memoryHeight;
    final int memoryWidth;

    static final double EPSILON = 0.0001;

    public NTMMemory(int memoryHeight, int memoryWidth, int heads) {
        this(null, memoryHeight, memoryWidth, new Head[heads],
                UnitFactory.getTensor2(memoryHeight, memoryWidth),
                null);


    }

    public final NTMMemory parent() {
        return parent.get();
    }

    public Head getHead(int index) {
        return heads[index];
    }

    private NTMMemory(HeadSetting[] heading, int memoryHeight, int memoryWidth, Head[] heads, Unit[][] data, NTMMemory parent) {
        this.memoryHeight = memoryHeight;
        this.memoryWidth = memoryWidth;
        this.data = data;
        this.heading = heading;
        this.parent = new WeakReference(parent);

        this.heads = heads;

        oldSimilar = BetaSimilarity.getTensor2(heads.length, memoryHeight);
        erase = getTensor2(heads.length, memoryWidth);
        add = getTensor2(heads.length, memoryWidth);
    }

    /** number of heads, even if unallocated */
    int headNum() {
        return erase.length;
    }

    NTMMemory(HeadSetting[] heading, Head[] heads, NTMMemory memory) {
        this(heading, memory.memoryHeight, memory.memoryWidth, memory.heads,
                UnitFactory.getTensor2(memory.memoryHeight, memory.memoryWidth), memory);

        var erasures = getTensor2(memory.memoryHeight, memory.memoryWidth);

        var h = headNum();

        for (var i = 0; i < h; i++) {
            var d = this.heads[i];
            if (d == null)
                this.heads[i] = d = new Head(memory.getWidth());

            var eraseVector = d.getEraseVector();
            var addVector = d.getAddVector();
            var erases = erase[i];
            var adds = add[i];
            for (var j = 0; j < memoryWidth; j++) {
                erases[j] = Sigmoid.getValue(eraseVector[j].value);
                adds[j] = Sigmoid.getValue(addVector[j].value);
            }
        }

        var p = parent();
        for (var i = 0; i < memoryHeight; i++) {

            var oldRow = p.data[i];
            var erasure = erasures[i];
            var row = data[i];
            for (var j = 0; j < memoryWidth; j++) {
                var oldCell = oldRow[j];
                var erase = 1.0;
                var add = 0.0;
                for (var k = 0; k < h; k++) {
                    var addressingValue = this.heading[k].addressingVector.value[i];
                    erase *= (1.0 - (addressingValue * this.erase[k][j]));
                    add += addressingValue * this.add[k][j];
                }
                erasure[j] = erase;
                row[j].value += (erase * oldCell.value) + add;
            }
        }
    }

    private int getWidth() {
        return memoryWidth;
    }

    public void backwardErrorPropagation() {
        for (var i = 0; i < headNum(); i++) {
            var headSetting = heading[i];
            var erase = this.erase[i];
            var add = this.add[i];
            var head = heads[i];
            headSettingGradientUpdate(i, erase, add, headSetting);
            eraseAndAddGradientUpdate(i, erase, add, headSetting, head);
        }
        memoryGradientUpdate();
    }

    private void memoryGradientUpdate() {
        var h = headNum();

        var p = parent();


        var heading = this.heading;
        var erase = this.erase;

        var height = this.memoryHeight;
        var width = this.memoryWidth;

        for (var i = 0; i < height; i++) {

            var oldDataVector = p.data[i];
            var newDataVector = data[i];


            for (var j = 0; j < width; j++) {
                var gradient = 1.0;

                for (var q = 0; q < h; q++) {
                    gradient *= 1.0 - (heading[q].addressingVector.value[i] * erase[q][j]);
                }
                oldDataVector[j].grad += gradient * newDataVector[j].grad;
            }
        }
    }

    private void eraseAndAddGradientUpdate(int headIndex, double[] erase, double[] add, HeadSetting headSetting, Head head) {
        var addVector = head.getAddVector();

        var h = headNum();

        var p = parent();

        for (var j = 0; j < memoryWidth; j++) {
            var gradientErase = 0.0;
            var gradientAdd = 0.0;

            for (var k = 0; k < memoryHeight; k++) {
                var row = data[k];
                var itemGradient = row[j].grad;
                var addressingVectorItemValue = headSetting.addressingVector.value[k];

                var gradientErase2 = p.data[k][j].value;
                for (var q = 0; q < h; q++) {
                    if (q != headIndex) {
                        gradientErase2 *= 1.0 - (heading[q].addressingVector.value[k] * this.erase[q][j]);
                    }

                }

                var gradientAddressing = itemGradient * addressingVectorItemValue;

                gradientErase += gradientAddressing * (-gradientErase2);
                gradientAdd += gradientAddressing;
            }


            var e = erase[j];
            head.getEraseVector()[j].grad += gradientErase * e * (1.0 - e);
            var a = add[j];
            addVector[j].grad += gradientAdd * a * (1.0 - a);
        }
    }

    private void headSettingGradientUpdate(int headIndex, double[] erase, double[] add, HeadSetting headSetting) {
        var h = headNum();

        var p = parent();

        for (var j = 0; j < memoryHeight; j++) {

            var row = data[j];
            var oldRow = p.data[j];
            var gradient = 0.0;
            for (var k = 0; k < memoryWidth; k++) {
                var data = row[k];
                var oldDataValue = oldRow[k].value;
                for (var q = 0; q < h; q++) {
                    if (q == headIndex)
                        continue;


                    var setting = heading[q];
                    oldDataValue *= (1.0 - (setting.addressingVector.value[j] * this.erase[q][k]));
                }
                gradient += ((oldDataValue * (-erase[k])) + add[k]) * data.grad;
            }
            headSetting.addressingVector.grad[j] += gradient;
        }
    }

    ContentAddressing[] getContentAddressing() {
        return ContentAddressing.getVector(headNum(), i -> oldSimilar[i]);
    }

    public void updateWeights(IWeightUpdater weightUpdater) {
        for (var betaSimilarities : oldSimilar) {
            for (var betaSimilarity : betaSimilarities) {
                weightUpdater.updateWeight(betaSimilarity);
            }
        }
        weightUpdater.updateWeight(data);
    }

    private static double[][] getTensor2(int x, int y) {
        var tensor = IntStream.range(0, x).mapToObj(i -> new double[y]).toArray(double[][]::new);


        return tensor;
    }
}
