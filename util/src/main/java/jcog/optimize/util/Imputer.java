package jcog.optimize.util;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import jcog.list.FasterList;
import jcog.optimize.Tweak;
import jcog.optimize.Tweaks;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** general-purpose POJO imputer context */
public class Imputer {

    /** row = tweak, col = tag */
    final Table<String,String,Object> rules = Tables.newCustomTable(new HashMap(), LinkedHashMap::new);

    public <X> Tweaks<X> learn(X example, String... tags) {

        //Class clz = example.getClass();
        Tweaks<X> t = new Tweaks<>(example).discover();
        for (Tweak<X,?> u : t.tweaks) {
            Object v = u.get(example);
            //System.out.println(clz + " * " + Joiner.on(',').join(tags) + " ==> " + u + " = " + v);
            for (String tag : tags) {
                rules.put(u.id, tag, v);
            }
        }
        return t;
    }

    /** returns a 'report' of the tweaks applied */
    public <X> Imputing<X> apply(X x, String... tags) {
        return new Imputing<>(x, tags);
    }

    /** results from an inpute invocation (imputation) */
    public class Imputing<X> {
        X subject;

        public final Map<Tweak<X,?>,Object> log = new LinkedHashMap();
        public final List<String> issues = new FasterList();

        Imputing(X subject, String... tags) {
            assert(tags.length > 0);
            this.subject = subject;
            new Tweaks<>(subject).discover().tweaks.forEach((Tweak x) -> {
                Map<String, Object> rr = rules.row(x.id);
                if (rr.isEmpty()) {
                    issues.add("unknown: " + x.id);
                } else {
                    //descend in tags order
                    for (String y : tags) {
                        Object rc = rr.get(y);
                        if (rc!=null) { //TODO support null values
                            log.put(x, rc);
                            x.set(subject, floatize(rc));
                        }
                    }
                }
            });
        }

        /** HACK adapter for TweakFloat's */
        private Object floatize(Object rc) {
            if (rc instanceof Boolean) {
                return ((Boolean)rc)?1f:0f;
            }
            if (rc instanceof Integer) {
                return ((Integer)rc).floatValue();
            }
            return rc;
        }


    }
}
