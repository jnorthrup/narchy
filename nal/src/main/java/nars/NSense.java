package nars;

import jcog.Util;
import jcog.math.FloatFirstOrderDifference;
import jcog.math.FloatPolarNormalized;
import jcog.math.FloatSupplier;
import nars.concept.ScalarConcepts;
import nars.concept.SensorConcept;
import nars.control.CauseChannel;
import nars.control.DurService;
import nars.task.ITask;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.function.*;

import static nars.$.$;
import static nars.$.*;

/**
 * agent sensor builder
 */
public interface NSense {

    @NotNull Atomic LOW = Atomic.the("low");
    @NotNull Atomic MID = Atomic.the("mid");
    @NotNull Atomic HIH = Atomic.the("hih");

    @NotNull Map<SensorConcept,CauseChannel<ITask>> sensors();

    NAR nar();


    @NotNull
    default SensorConcept sense(@NotNull Term term, @NotNull BooleanSupplier value)  {
        return sense(term, () -> value.getAsBoolean() ? 1f : 0f);
    }

    @NotNull
    default SensorConcept sense(@NotNull Term term, FloatSupplier value)  {
        return sense(term, value, (x) -> $.t(x, nar().confDefault(Op.BELIEF)));
    }

    @NotNull
    default SensorConcept sense(@NotNull Term term, FloatSupplier value, FloatToObjectFunction<Truth> truthFunc) {
        SensorConcept s = new SensorConcept(term, nar(), value, truthFunc);

        addSensor(s);
        return s;
    }

    default void addSensor(SensorConcept c) {
        CauseChannel<ITask> existing = sensors().put(c, nar().newCauseChannel(c));
        assert(existing == null);
        nar().on(c);
    }

    /**
     * learning rate
     */
    default float alpha() {
        return nar().confDefault(Op.BELIEF);
    }

    /**
     * interpret an int as a selector between enumerated values
     */
    default <E extends Enum> void senseSwitch(String term, @NotNull Supplier<E> value) throws Narsese.NarseseException {
        E[] values = ((Class<? extends E>) value.get().getClass()).getEnumConstants();
        for (E e : values) {
            Term t = switchTerm(term, e.toString());
            sense(t, () -> value.get() == e);
        }
    }

    @NotNull
    static Term switchTerm(String a, String b) throws Narsese.NarseseException {
        return switchTerm($(a), $(b));
    }

    @NotNull
    static Term switchTerm(Term a, Term b) {
        //return "(" + e + " --> " + term + ")";
        return p(a, b);//"(" + term + " , " + e + ")";
    }

    default void senseSwitch(Term term, @NotNull IntSupplier value, int min, int max) {
        senseSwitch(term, value, Util.intSequence(min, max));
    }

    /**
     * interpret an int as a selector between (enumerated) integer values
     */
    default void senseSwitch(Term term, @NotNull IntSupplier value, @NotNull int[] values) {
        for (int e : values) {
            Term t = switchTerm(term, the(e));
            sense(t, () -> value.getAsInt() == e);
        }
    }

    /**
     * interpret an int as a selector between (enumerated) object values
     */
    default <O> void senseSwitch(String term, @NotNull Supplier<O> value, @NotNull O... values) throws Narsese.NarseseException {
        for (O e : values) {
            Term t = switchTerm(term, '"' + e.toString() + '"');
            sense(t, () -> value.get().equals(e));
        }
    }


    default void senseFields(String id, @NotNull Object o) {
        Field[] ff = o.getClass().getDeclaredFields();
        for (Field f : ff) {
            if (Modifier.isPublic(f.getModifiers())) {
                sense(id, o, f.getName());
            }
        }
    }

//    public NObj read(String... expr) {
//        for (String e : expr)
//            read(e);
//        return this;
//    }

    default void sense(String id, Object o, @NotNull String exp) {

//        try {
//            //Object x = Ognl.parseExpression(exp);
//            Object initialValue = Ognl.getValue(exp, o);
//
//
//            String classString = initialValue.getClass().toString().substring(6);
//            switch (classString) {
//                case "java.lang.Double":
//                case "java.lang.Float":
//                case "java.lang.Long":
//                case "java.lang.Integer":
//                case "java.lang.Short":
//                case "java.lang.Byte":
//                case "java.lang.Boolean":
//                    senseNumber(id, o, exp);
//                    break;
//
//                //TODO String
//
//                default:
//                    throw new RuntimeException("not handled: " + classString);
//            }
//                /*if (y != null) {
//                    System.out.println("read: \t" + o + " " + exp + " " + x + " " + x.getClass() + " " + initialValue + " " + initialValue.getClass());
//                }*/
//        } catch (Exception e1) {
//            e1.printStackTrace();
//        }
    }

    @NotNull
    default List<SensorConcept> senseNumber(int from, int to, IntFunction<String> id, IntFunction<FloatSupplier> v) throws Narsese.NarseseException {
        List<SensorConcept> l = newArrayList(to-from);
        for (int i = from; i < to; i++) {
            l.add( senseNumber( id.apply(i), v.apply(i)) );
        }
        return l;
    }


    default SensorConcept senseNumberDifference(Term id, FloatSupplier v) {
        return senseNumber(id, new FloatPolarNormalized(
                new FloatFirstOrderDifference(nar()::time, v)) );
    }

    default SensorConcept senseNumber(Term id, FloatSupplier v) {
        SensorConcept c = new SensorConcept(id, nar(), v,
                (x) -> t(Util.unitize(x), nar().confDefault(Op.BELIEF))
        );
        addSensor(c);
        return c;
    }

    @NotNull
    default ScalarConcepts senseNumber(FloatSupplier v, ScalarConcepts.ScalarEncoder model, Term... states) {

        assert(states.length > 1);

        ScalarConcepts fs = new ScalarConcepts(
               v, nar(),
                model,
                states
        );

        onFrame(fs);
        return fs;
    }

    DurService onFrame(Consumer r);

    @NotNull
    default ScalarConcepts senseNumber(Term id, FloatSupplier v, int precision, ScalarConcepts.ScalarEncoder model)  {
        return senseNumber(v, model, Util.map(0, precision,
                (int x) ->
                        //($.inh(id, $.the(x)))
                        ($.inh($.the(x), id))
                        //($.p(id, $.the(x)))
                ,
                Term[]::new));
    }

    @NotNull
    default ScalarConcepts senseNumberBi(Term id, FloatSupplier v)  {
        return senseNumber(v, ScalarConcepts.Needle, inh(LOW, id), inh(HIH, id));
    }
    @NotNull
    default ScalarConcepts senseNumberTri(Term id, FloatSupplier v)  {
        return senseNumber(v, ScalarConcepts.Needle,  inh(LOW, id), inh(MID, id), inh(HIH, id));
    }

    default SensorConcept senseNumber(String id, FloatSupplier v) throws Narsese.NarseseException {
        return senseNumber($(id), v);
    }

//    /**
//     * generic lowest common denominator numeric input
//     */
//    @NotNull
//    default Object senseNumber(String id, Object o, @NotNull String _expr) {
//        return null;
//
//        //TODO use Nashorn to evaluate expressions
//
////        Object expr;
////        try {
////            expr = Ognl.parseExpression(_expr);
////        } catch (OgnlException e) {
////            throw new RuntimeException(e);
////        }
////        FuzzyScalarConcepts fs = new FuzzyScalarConcepts(
////
////                new FloatNormalized(() -> {
////                    try {
////                        Object v = Ognl.getValue(expr, o, Object.class);
////                        if (v instanceof Boolean) {
////                            return (Boolean) v ? 1f : 0f;
////                        } else if (v instanceof Number) {
////                            return ((Number) v).floatValue();
////                        } else {
////                            return Float.NaN; //unknown
////                        }
////                    } catch (OgnlException e) {
////                        e.printStackTrace();
////                        return Float.NaN;
////                    }
////                }), nar(), id + ":(" + term(expr) + ')'
////        );//.resolution(0.05f);
////        sensors().addAll(fs.sensors);
////        return fs;
//    }
//
//    @NotNull
//    private static String term(Object expr) {
//
//        if (expr instanceof ASTConst) {
//
//            String ae = expr.toString();
//            return ae
//                    .substring(1, ae.length() - 1); //it's raw field name, wont need quoted
//
//        } else if ((expr instanceof ASTStaticMethod) || (expr instanceof ASTMethod)) {
//            String ae = expr.toString();
//            String key = //"\"" +
//                    ae.substring(0, ae.indexOf('('));
//            //+ "\"";
//            key = key.replace("@", "X");
//            //HACK remove the '@' from the key so it doesnt need quoted:
//
//            return key + '(' +
//                    term((SimpleNode) expr)
//                    + ')';
//        } else if (expr instanceof SimpleNode) {
//            return term((SimpleNode) expr);
//        } else {
//            //safest for unknown type but semantics are lost
//            return "\"" + expr + '"';
//        }
//    }
//
//
//    private static String term(@NotNull SimpleNode a) {
//        int c = a.jjtGetNumChildren();
//
//        StringBuilder sb = new StringBuilder(16);//.append('(');
//        for (int i = 0; i < c; i++) {
//            sb.append(term(a.jjtGetChild(i)));
//            if (i != c - 1)
//                sb.append(',');
//        }
//        return sb./*.append(')').*/toString();
//    }
//

}
