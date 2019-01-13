package alice.tuprolog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static alice.tuprolog.Theory.resource;

public class TestEinsteinRiddle {
    
    @Disabled
    @Test
    public void einsteinsRiddle() throws IOException, URISyntaxException, InvalidTheoryException {

        final boolean[] finished = {false};

        
        new Prolog()
            .input(resource("einsteinsRiddle.pl"))
            .solve("einstein(_,X), write(X).", o -> {
                System.out.println(o);
                if (finished[0])
                    return;

                Assertions.assertEquals("yes.\nX / german", o.toString());
                finished[0] = true;
            });
    }
    
}
