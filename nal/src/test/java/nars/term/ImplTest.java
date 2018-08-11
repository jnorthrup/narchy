package nars.term;

import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.False;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** tests specific to implication compounds TODO */
public class ImplTest {
    @Test
    public void testInvalidImpls() {
        for (String s : new String[]{
                "(--y =|> y)",
                "(--(x &| y) =|> y)",
                "(--(--x &| y) =|> y)"
        })
            assertEquals(False, $$(s));
    }
}
