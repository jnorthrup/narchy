package jcog.optimize;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImputerTest {

    static class SimplePOJO {
        public int a;
        public boolean b;
    }

    @Test
    public void test1() {
        Imputer i = new Imputer();

        SimplePOJO x = new SimplePOJO();
        x.a = 1;
        x.b = false;

        Tweaks<SimplePOJO> ti = i.learn(x, "default");
        assertEquals(2, ti.tweaks.size());


        SimplePOJO y = new SimplePOJO();
        List<Tweak> yy = i.apply(y, "default");
        assertEquals(2, yy.size());
        assertEquals(1, x.a);
        assertEquals(true, x.b);
    }

}