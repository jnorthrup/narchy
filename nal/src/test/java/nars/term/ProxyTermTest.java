package nars.term;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;

class ProxyTermTest {

    @Disabled
    @Test
    void testEveryTermMethodProxied() {

        Function<Method, String> methodSummarizer = x ->
                x.getName() + '(' + Arrays.toString(x.getParameterTypes()) + ')';


        for (Class proxy : new Class[] { ProxyTerm.class }) {
            int unoverriden = 0;
            for (Method m : proxy.getMethods()) {
                Class<?> c = m.getDeclaringClass();
                if (c == proxy || c == Object.class)
                    continue;

                System.out.println(proxy + " does not override: " + c + ' ' + m);

                unoverriden++;
            }
            
        }

    }
}