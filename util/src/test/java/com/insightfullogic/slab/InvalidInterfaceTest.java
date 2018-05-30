package com.insightfullogic.slab;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class InvalidInterfaceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidInterfaceTest.class);


    public static Collection<Class<? extends Cursor>> data() {
        return List.of(
                NoGettersOrSetters.class, InvalidReturnGetter.class, ParameterGetter.class,
                InvalidReturnSetter.class, NoParameterSetter.class, IndexField.class);
    }


    @ParameterizedTest @MethodSource("data")
    public void interfaceIsInvalid(Class<? extends Cursor> representingKlass) {
        assertThrows(InvalidInterfaceException.class, () -> {
            Allocator.of(representingKlass);
            LOGGER.error(representingKlass.getName() + " failed");

        });
    }

    

    public interface NoGettersOrSetters extends Cursor {
        void neitherGetterNorSetter();
    }

    public interface InvalidReturnGetter extends Cursor {
        Object getFoo();
    }

    public interface ParameterGetter extends Cursor {
        int getFoo(long bar);
    }

    public interface InvalidReturnSetter extends Cursor {
        Object setFoo();
    }

    public interface NoParameterSetter extends Cursor {
        void setFoo();
    }

    public interface IndexField extends Cursor {
        @Override
        int getIndex();

        void setIndex(int index);
    }

}
