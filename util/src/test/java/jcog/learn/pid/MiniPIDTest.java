package jcog.learn.pid;

import org.junit.jupiter.api.Test;

import static jcog.Texts.n4;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniPIDTest {

    @Test
    void test1() {

        MiniPID miniPID = new MiniPID(0.25, 0.01, 0.4);
        miniPID.outLimit(10);
        
        
        
        miniPID.setSetpointRange(40);

        double target = 100;

        double actual = 0;
        double output;

        miniPID.setpoint(0);
        miniPID.setpoint(target);

        for (int i = 0; i < 200; i++) {

            if (i == 60)
                target = 50;

            output = miniPID.out(actual, target);
            actual += output;
            System.out.println(n4(output) + ' ' + n4(actual));
        }
        assertTrue(Math.abs(target - actual) < 0.05f);
    }
}