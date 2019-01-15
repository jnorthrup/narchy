package nars.attention.impl;

import nars.attention.Attention;
import nars.attention.Forgetting;

public class DefaultAttention extends Attention {

    public DefaultAttention() {
        super(new Forgetting.AsyncForgetting());
    }
}
