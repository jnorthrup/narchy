package jcog.constraint.continuous;

import jcog.constraint.continuous.exceptions.DuplicateConstraintException;
import jcog.constraint.continuous.exceptions.UnsatisfiableConstraintException;
import org.junit.jupiter.api.Test;

import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by alex on 31/01/16.
 */
class ExpressionVariableTest {
    private static double EPSILON = 1.0e-8;

    @Test
    void lessThanEqualTo() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        DoubleVar x = new DoubleVar("x");
        ContinuousConstraintSolver solver = new ContinuousConstraintSolver();
        solver.add(C.lessThanOrEqualTo(new Expression(100), x));
        solver.update();
        assertTrue(100 <= x.getAsDouble());
        solver.add(C.equals(x, 110));
        solver.update();
        assertEquals(x.getAsDouble(), 110, EPSILON);
    }

    @Test
    void lessThanEqualToUnsatisfiable() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        assertThrows(UnsatisfiableConstraintException.class, () -> {
            DoubleVar x = new DoubleVar("x");
            ContinuousConstraintSolver solver = new ContinuousConstraintSolver();
            solver.add(C.lessThanOrEqualTo(new Expression(100), x));
            solver.update();
            assertTrue(x.getAsDouble() <= 100);
            solver.add(C.equals(x, 10));
            solver.update();
        });
    }

    @Test
    void greaterThanEqualTo() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        DoubleVar x = new DoubleVar("x");
        ContinuousConstraintSolver solver = new ContinuousConstraintSolver();
        solver.add(C.greaterThanOrEqualTo(new Expression(100), x));
        solver.update();
        assertTrue(100 >= x.getAsDouble());
        solver.add(C.equals(x, 90));
        solver.update();
        assertEquals(x.getAsDouble(), 90, EPSILON);
    }

    @Test
    void greaterThanEqualToUnsatisfiable() throws DuplicateConstraintException, UnsatisfiableConstraintException {
        assertThrows(UnsatisfiableConstraintException.class, () -> {
            DoubleVar x = new DoubleVar("x");
            ContinuousConstraintSolver solver = new ContinuousConstraintSolver();
            solver.add(C.greaterThanOrEqualTo(new Expression(100), x));
            solver.update();
            assertTrue(100 >= x.getAsDouble());
            solver.add(C.equals(x, 110));
            solver.update();
        });

    }
}
