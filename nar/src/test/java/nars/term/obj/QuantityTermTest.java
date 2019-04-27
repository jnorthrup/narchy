package nars.term.obj;

import org.junit.jupiter.api.Test;

import javax.measure.Quantity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuantityTermTest {

    @Test
    void test1() {
        


        QuantityTerm q = QuantityTerm.the("5 km");
        assertEquals("(km,5)", q.toString());
        QuantityTerm r = QuantityTerm.the("1 s");
        Quantity<?> qDivR = q.quant.divide(r.quant);
        assertEquals("5 km/s", qDivR.toString());

    }
}