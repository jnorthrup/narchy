package nars.web;

import com.fasterxml.jackson.databind.node.ArrayNode;
import nars.Task;

public class TaskJsonCodec {


//    static final String PRI = String.valueOf('$');
//    static final String TRUTH = String.valueOf('%');
//    static final String WHEN = String.valueOf('`');

    public static class Native {
        public static void taskify(Task x, ArrayNode y) {

            y.add(String.valueOf((char)x.punc()));
            /* 0 */ y.add((int) (x.pri() * (float) Short.MAX_VALUE));
            /* 1 */ y.add(x.term().toString());
            if (x.isBeliefOrGoal())
                /* 2 */ y.add(x.truth().toString());
            if (!x.isEternal()) {
                long start = x.start();
                y.addArray().add(start).add(x.end() - start);
            }
        }
    }

    //
//
//    public static class Client {
//
//        @JSBody(params = {"data","each"},
//                script = "msgpack.decode(new Uint8Array(data), window.msgPackOptions ).forEach(x=>{" +
//                        "each( javaMethods.get(\"\").invoke() "+
//                        "});"
//        )
//        public native static void decodeArray(JSObject data, Consumer<TaskJson> each);
//
//    }
}
