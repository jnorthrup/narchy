package jcog.net;

import jcog.Util;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UDPeerTest {

    @Test
    public void testDiscoverableByLANMulticast() throws IOException {

        UDPeer x = new UDPeer();
        UDPeer y = new UDPeer();
        x.runFPS(4);
        y.runFPS(4);

        Util.sleep(3000);

        
        assertTrue(x.them.contains(y.me));
        assertTrue(y.them.contains(x.me));
    }
}
