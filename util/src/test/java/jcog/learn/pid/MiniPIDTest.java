package jcog.learn.pid;

import org.junit.jupiter.api.Test;

import static jcog.Texts.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniPIDTest {

    @Test
    void test1() {

        MiniPID miniPID = new MiniPID(0.25, 0.01, 0.4);
        miniPID.outLimit(10);
        
        
        
        miniPID.setSetpointRange(40);

        miniPID.setpoint(0);
        double target = 100;
        miniPID.setpoint(target);

        double actual = 0;
        for (int i = 0; i < 200; i++) {

            if (i == 60)
                target = 50;

            double output = miniPID.out(actual, target);
            actual += output;
            System.out.println(INSTANCE.n4(output) + ' ' + INSTANCE.n4(actual));
        }
        assertTrue(Math.abs(target - actual) < 0.05f);
    }
}