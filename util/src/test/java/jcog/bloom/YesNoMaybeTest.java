package jcog.bloom;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YesNoMaybeTest {

    @Test
    void test1() {

        int uniques = 32;

        Predicate<String> a = (s) -> ((s.hashCode() & 1) == 0);
        YesNoMaybe<String> x = new YesNoMaybe<>(
                a,
                String::getBytes, uniques * 2, 0.05f);

        int batches = 100;
        for (int i = 0; i < batches * uniques; i++) {
            String s = "abc" + (int)(Math.random()*uniques);

            boolean should = a.test(s);
            boolean guess = x.test(s);

            assertEquals(should, guess);
        }

        assertTrue(x.hitRate(false) > 0.9f);
    }
}