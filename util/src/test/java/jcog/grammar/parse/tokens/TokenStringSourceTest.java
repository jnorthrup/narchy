package jcog.grammar.parse.tokens;

import org.junit.jupiter.api.Test;

public class TokenStringSourceTest {
    /**
     * Shows the example in the class comment.
     *
     * @param args
     *            ignored
     */
    @Test
    public void testTokenize() {

        String s = "I came; I saw; I left in peace;";

        TokenStringSource tss = new TokenStringSource(new Tokenizer(s), ";");

        while (tss.hasMoreTokenStrings()) {
            System.out.println(tss.nextTokenString());
        }
    }

}