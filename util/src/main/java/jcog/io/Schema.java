package jcog.io;

import jcog.io.arff.ARFF;
import jcog.list.FasterList;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Schema {

    protected final List<String> attribute_names;
    protected final Map<String, ARFF.AttributeType> attrTypes;
    protected final Map<String, String[]> nominalCats;

    public Schema(@Nullable Schema copyMetadataFrom) {
        if (copyMetadataFrom!=null) {
            this.attribute_names = copyMetadataFrom.attribute_names;
            this.attrTypes = copyMetadataFrom.attrTypes;
            this.nominalCats = copyMetadataFrom.nominalCats;
        } else {
            this.attribute_names = new FasterList<>();
            this.attrTypes = new HashMap<>();
            this.nominalCats = new HashMap<>();
        }
    }
}
