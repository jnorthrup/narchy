package nars.attention.impl;

import nars.attention.Activator;
import nars.attention.Attention;
import nars.attention.Forgetting;
import nars.attention.derive.DefaultPuncWeightedDerivePri;

public class DefaultAttention extends Attention {

    public DefaultAttention() {
        super(new DefaultPuncWeightedDerivePri(), new Activator(),
                new Forgetting.AsyncForgetting());
    }
}
