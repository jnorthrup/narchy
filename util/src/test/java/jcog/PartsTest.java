package jcog;

import jcog.service.Part;
import jcog.service.Parts;
import org.junit.jupiter.api.Test;

class PartsTest {

    @Test
    void testParts1() {

        Parts<?, String> s = new Parts("");
        StringBuilder sb = new StringBuilder();

        s.add("x", new DummyPart(sb));
        s.add("y", new DummyPart(sb));

        s.print(System.out);


        s.stop();

        s.print(System.out);

        
    }

    private static class DummyPart extends Part {
        private final StringBuilder sb;

        DummyPart(StringBuilder sb) {
            this.sb = sb;
        }

        @Override
        public void off() {

        }

        @Override
        protected void start(Object x) {
            sb.append(this).append(" start\n");
        }

        @Override
        protected void stop(Object x){
            sb.append(this).append(" stop\n");
        }
    }
}