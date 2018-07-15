package nars;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TaskJsonCodec {


    static final String PRI = String.valueOf('$');
    static final String TRUTH = String.valueOf('%');
    static final String WHEN = String.valueOf('`');

    public static class Native {
        public static void taskify(Task x, ObjectNode y) {

            y.put(String.valueOf((char) (x.punc())), x.term().toString());
            y.put(PRI, (short) (x.pri() * Short.MAX_VALUE));
            if (x.isBeliefOrGoal())
                y.put(TRUTH, x.truth().toString());
            if (!x.isEternal()) {
                long start = x.start();
                y.putArray(WHEN).add(start).add(x.end() - start);
            }
        }
    }

//    public static class TaskJson {
//        final String term;
//        final Truth truth;
//        final byte punc;
//        final long start, end;
//
//        public TaskJson(String term, Truth truth, byte punc, long start, long end) {
//            this.term = term;
//            this.truth = truth;
//            this.punc = punc;
//            this.start = start;
//            this.end = end;
//        }
//    }
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
