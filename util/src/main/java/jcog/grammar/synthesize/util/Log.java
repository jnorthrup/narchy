













package jcog.grammar.synthesize.util;

@Deprecated public enum Log {
	;

	private static boolean verbose = false;

    public static void init(boolean verbose) {

        Log.verbose = verbose;

    }

    public static void info(String s) {



        if (verbose) {
            System.out.println(s);
        }








    }

    public static void err(Exception e) {
        if (verbose) {
            e.printStackTrace();
        } else {
            System.err.println(e.getMessage());
        }








    }
}