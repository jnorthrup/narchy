package nars;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Javascript NAR Runner
 *
 * @author me
 */
public class NARjs {
    static final ScriptEngineManager factory = new ScriptEngineManager();
    private static final ThreadLocal<NARjs> the = ThreadLocal.withInitial(NARjs::new);
    final ScriptEngine js = factory.getEngineByName("JavaScript");

    private NARjs() {
        try {
            js.eval("load('nashorn:mozilla_compat.js')");

            js.eval("importPackage('java.lang')");
            js.eval("importPackage('java.other')");
            js.eval("importPackage('java.io')");

            js.eval("importPackage('nars')");
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }


    }

    public static NARjs the() {
        return the.get();
    }

    public static void printHelp() {
        System.out.println("Help coming soon.");
    }

    public static void main(String[] args) throws java.io.IOException {
        var j = NARjs.the();

        System.out.println(NAR.VERSION
                + " Javascript Console - :h for help, :q to exit");
        var br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("> ");

        String s;
        while ((s = br.readLine()) != null) {

            try {
                if (":q".equals(s))
                    break;
                if (s.startsWith(":h")) {
                    printHelp();
                    continue;
                }

                var ret = j.eval(s);

                if (ret != null) {
                    System.out.println(ret);
                }
            } catch (ScriptException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                System.out.println(e.getClass().getName() + " in parsing: "
                        + e.getMessage());
            } finally {

                System.out.print("> ");

            }
        }

        br.close();
        System.exit(0);
    }

    public Object eval(String s) throws ScriptException {
        return js.eval(s);
    }
}
