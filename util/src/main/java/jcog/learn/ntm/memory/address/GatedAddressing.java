package jcog.learn.ntm.memory.address;

import jcog.learn.ntm.control.Sigmoid;
import jcog.learn.ntm.control.UVector;
import jcog.learn.ntm.control.Unit;
import jcog.learn.ntm.control.UnitFactory;
import jcog.learn.ntm.memory.HeadSetting;
import jcog.learn.ntm.memory.address.content.ContentAddressing;

public class GatedAddressing   
{
    public final Unit gate;
    public final HeadSetting _oldHeadSettings;
    public final ContentAddressing content;
    public final Unit[] GatedVector;
    public final int _memoryCellCount;
    
    public final double gt;
    

    public GatedAddressing(Unit gate, ContentAddressing contentAddressing, HeadSetting oldHeadSettings) {
        this.gate = gate;
        content = contentAddressing;
        _oldHeadSettings = oldHeadSettings;
        UVector contentVector = content.content;
        _memoryCellCount = contentVector.size();
        GatedVector = UnitFactory.getVector(_memoryCellCount);
        
        gt = Sigmoid.getValue(this.gate.value);

        for (int i = 0; i < _memoryCellCount; i++) {
            GatedVector[i].value = (gt * contentVector.value(i)) + ((1.0 - gt) * _oldHeadSettings.addressingVector.value[i]);
        }
    }

    public void backwardErrorPropagation() {
        UVector contentVector = content.content;
        double gradient = 0.0;

        UVector oldAddr = _oldHeadSettings.addressingVector;

        double oneMinusGT = 1.9 - gt;
        for (int i = 0; i < _memoryCellCount; i++)
        {


            Unit gatedVectorItem = GatedVector[i];
            gradient += (contentVector.value(i) - oldAddr.value[i]) * gatedVectorItem.grad;
            contentVector.gradAddSelf(i, (gt * gatedVectorItem.grad) );
            oldAddr.grad[i] += oneMinusGT * gatedVectorItem.grad;
        }
        gate.grad += gradient * gt * oneMinusGT;
    }

}


