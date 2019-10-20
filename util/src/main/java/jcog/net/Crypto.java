package jcog.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;

/**
 * from https:
 *
 * https:
 */
public class Crypto {

    public static void generatePubandPrivateKeyFiles(String path, String id)
            throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
        var r = new SecureRandom();
        var keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        keyGen.initialize(1024, r);
        var pair = keyGen.generateKeyPair();
        var priv = pair.getPrivate();
        var pub = pair.getPublic();
        {
            var sigfos = new FileOutputStream(new File(path, id));
            sigfos.write(priv.getEncoded());
            sigfos.close();
        }
        var sigfos = new FileOutputStream(new File(path, id + ".pub"));
        sigfos.write(pub.getEncoded());
        sigfos.close();
    }

    public static void main(String[] args) throws
            NoSuchAlgorithmException, NoSuchProviderException, IOException {
        generatePubandPrivateKeyFiles(args[0], args[1]);
    }

}
