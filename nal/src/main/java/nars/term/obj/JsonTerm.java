package nars.term.obj;

import com.fasterxml.jackson.databind.JsonNode;
import jcog.Util;
import nars.$;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static nars.Op.SETe;

/**
 * Created by me on 4/2/17.
 */
public enum JsonTerm { ;


    public static Term the(JsonNode j) {

        if (j.isArray()) {
            int s = j.size();
            List<Term> list = new ArrayList<>();
            for (int i = 0; i < s; i++) {
                Term the = the(j.get(i));
                list.add(the);
            }
            Term[] subterms = list.toArray(new Term[0]);
            return $.INSTANCE.p(subterms);

        } else if (j.isValueNode()) {
            if (j.isTextual()) {
                return $.INSTANCE.quote(j.textValue());
            } else if (j.isNumber()) {
                return $.INSTANCE.the(j.numberValue());
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (j.isObject()) {
            Term[] s = new Term[j.size()];
            int[] i = {0};
            j.fields().forEachRemaining(new Consumer<Map.Entry<String, JsonNode>>() {
                @Override
                public void accept(Map.Entry<String, JsonNode> f) {
                    Atomic k = $.INSTANCE.quote(f.getKey());
                    Term v = the(f.getValue());
                    s[i[0]++] =
                            $.INSTANCE.inh(v, k);


                }
            });
            return SETe.the(s);
        } else {
            throw new UnsupportedOperationException("TODO");
        }
    }


    public static Term the(String json) {

        JsonNode x = Util.jsonNode(json);
        return the(x);

    }
}
