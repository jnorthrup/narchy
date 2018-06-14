package jcog.lab;

import org.junit.jupiter.api.Test;

import java.util.List;

class LabTest {

    static class Dummy {
        public float a = 0;
    }

    @Test
    public void test1() {
        Lab<Dummy> lab = new Lab<>(()->new Dummy());
        Lab.Trial<Dummy> t = lab.get((m, trial) -> {
            for (int i = 0; i < 10; i++) {
                m.a = (float) Math.sin(i);
                trial.sense();
            }

            m.a = 0.5f;
            m.a = 0.2f;
        }, List.of(
            Sensor.numeric("a", (Dummy m) -> m.a)
        ) );
        t.run();
        t.data.print();
    }
}