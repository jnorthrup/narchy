package nars.attention.impl;

import nars.attention.Activator;
import nars.attention.Attention;
import nars.attention.Forgetting;

public class DefaultAttention extends Attention {

    public DefaultAttention() {
        super(new Activator(),
                new Forgetting.AsyncForgetting());
    }
}
