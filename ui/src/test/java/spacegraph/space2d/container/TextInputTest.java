package spacegraph.space2d.container;

import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextInputTest {

    /**
     * https:
     * https:
     */
    final static JShell js = JShell
        .create();

    @Test
    public void JShellCompletion() {

        js.types().forEach(System.out::println);
        js.imports().forEach(System.out::println);
        js.types().forEach(System.out::println);
        js.methods().forEach(System.out::println);




        List<SourceCodeAnalysis.Suggestion> sugg = js.sourceCodeAnalysis().completionSuggestions("Thre", 3, new int[1]);
        Set<String> ss = sugg.stream().map(SourceCodeAnalysis.Suggestion::continuation).collect(toSet());
        assertTrue(ss.contains("Thread"));
        assertTrue(ss.contains("ThreadDeath"));
        

    }

    @Test
    public void JShellEvalSnippet() {
        for (SnippetEvent e : js.eval("Thread.currentThread()")) {
            StringBuilder sb = new StringBuilder();
            if (e.causeSnippet() == null) {
                
                switch (e.status()) {
                    case VALID:
                        sb.append("Successful ");
                        break;
                    case RECOVERABLE_DEFINED:
                        sb.append("With unresolved references ");
                        break;
                    case RECOVERABLE_NOT_DEFINED:
                        sb.append("Possibly reparable, failed  ");
                        break;
                    case REJECTED:
                        sb.append("Failed ");
                        break;
                    default:
                        sb.append(e.status()).append(' ');
                        break;
                }
                sb.append((e.previousStatus() == Snippet.Status.NONEXISTENT) ?
                        "addition" : "modification")
                        .append(" of ")
                        .append(e.snippet().source());

                System.out.println(sb);
                if (e.value() != null) {
                    System.out.println("value: " + e.value() + '\n');
                }
            }
        }
    }

    static class JShellTest {
        public static void main(String[] arg) throws InterruptedException {

            do {
                System.out.print("Enter some Java code: ");

                String input = "Thread.currentThread();";

                
                if (input == null) {
                    break;
                }

                java.util.List<SnippetEvent> events = js.eval(input);
                for (SnippetEvent e : events) {
                    StringBuilder sb = new StringBuilder();
                    if (e.causeSnippet() == null) {
                        
                        switch (e.status()) {
                            case VALID:
                                sb.append("Successful ");
                                break;
                            case RECOVERABLE_DEFINED:
                                sb.append("With unresolved references ");
                                break;
                            case RECOVERABLE_NOT_DEFINED:
                                sb.append("Possibly reparable, failed  ");
                                break;
                            case REJECTED:
                                sb.append("Failed ");
                                break;
                        }
                        if (e.previousStatus() == Snippet.Status.NONEXISTENT) {
                            sb.append("addition");
                        } else {
                            sb.append("modification");
                        }
                        sb.append(" of ");
                        sb.append(e.snippet().source());
                        System.out.println(sb);
                        if (e.value() != null) {
                            System.out.printf("Value is: %s\n", e.value());
                        }
                        System.out.flush();
                    }
                }
                try {
                    System.in.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (true);
            System.out.println("\nGoodbye");

            Thread.currentThread().join();
        }


    }

}
