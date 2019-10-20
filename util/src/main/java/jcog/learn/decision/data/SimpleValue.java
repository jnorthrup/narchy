package jcog.learn.decision.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author Ignas
 */
public final class SimpleValue<L> implements Function<String,L> {

    private final Map<String, L> values = Maps.newHashMap();

    @SafeVarargs
    private SimpleValue(String[] header, L... dataValues) {
        super();
        for (var i = 0; i < header.length; i++) {
            this.values.put(header[i], dataValues[i]);
        }
    }

    /**
     * Create data sample without labels which is used on trained tree.
     */
    public static SimpleValue classification(String[] header, Object... values) {
        Preconditions.checkArgument(header.length == values.length);
        return new SimpleValue(header, values);
    }

    /**
     * @param header
     * @param values
     * @return
     */
    public static SimpleValue data(String[] header, Object... values) {
        Preconditions.checkArgument(header.length == values.length);
        return new SimpleValue(header, values);
    }

    @Override
    public @Nullable L apply(String column) {
        return values.get(column);
    }






    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "SimpleDataSample [values=" + values + ']';
    }

}
