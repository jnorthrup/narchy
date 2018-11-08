package jcog.lab;

import jcog.Util;
import org.junit.jupiter.api.Test;

class OptiliveTest {

    @Test
    void test1() {
        Lab<LabTest.Model> a = new Lab<>(LabTest.Model::new).varAuto();

        Optilive<LabTest.Model,?> o = a.optilive(LabTest.Model::score);

        o.start();
        Util.sleepMS(1000);
        o.pause();
        o.resume();
        Util.sleepMS(1000);
        o.stop();
    }

}