package jcog.learn.pid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MiniPIDTest {

    @Test
    public void test1() {

        MiniPID miniPID = new MiniPID(0.25, 0.01, 0.4);
        miniPID.setOutputLimits(10);
        
        
        
        miniPID.setSetpointRange(40);

        double target = 100;

        double actual = 0;
        double output = 0;

        miniPID.setSetpoint(0);
        miniPID.setSetpoint(target);

        

        
        for (int i = 0; i < 200; i++) {

            

            if (i == 60)
                target = 50;

            
            

            output = miniPID.out(actual, target);
            actual = actual + output;

            
            
            

            
        }
        assertTrue(Math.abs(target - actual) < 0.05f);
    }
}