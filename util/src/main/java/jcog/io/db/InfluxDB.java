package jcog.io.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.net.UrlEscapers;
import jcog.Util;
import jcog.net.UDP;
import jcog.util.ArrayUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * InfluxDB Client (put/get) + Server emulator (put)
 *   --client: methods to transmit to InfluxDB
 *   --TODO: server: InfluxDB emulation when receiving InfluxDB packets
 *
 * https:
 *
 * curl -G 'http:
 *
 */
public class InfluxDB {

    final UDP udp = null; 

    private final String host;
    private final int port;
    private final String db;

    public InfluxDB(String db) {
        this(db, "localhost", 8086);
    }

    public InfluxDB(String db, String host, int port) {
        this.host = host;
        this.port = port;
        this.db = db;
    }

    /*
    https:
    To write, just send newline separated line protocol over UDP. For better performance send batches of points rather than multiple single points.
    $ echo "cpu value=1"> /dev/udp/localhost/8089
         */
    public void send() {
        String s = "cpu,host=server01,region=uswest load=" + (Math.random() * 100) + ' ' + System.currentTimeMillis();

        
        

        








        try {
            udp.out(s, host, port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    public float[] get(String measurement, long from, long to) {
        return get(measurement, "*", from, to);
    }

    static final DateFormat RFCTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

    public float[] get(String measurement, String value, long from, long to) {
        char c = value.charAt(0);
        if ((c !='*') && (c!='\"'))
            value = '"' + value + '"';

        String query = "SELECT " + value + " FROM \"" + measurement + "\" WHERE \"time\" >= \"" + rfc(from) + "\" AND \"time\" <= \"" + rfc(to) + '"';

        String epoch = "ms";

        URL u = null;
        try {
            u = new URL("http://" + host + ':' + port + "/query?db=" + db + "&epoch=" + epoch + "&q=" + UrlEscapers.urlFragmentEscaper().escape( query) );
            System.out.println(u);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return ArrayUtil.EMPTY_FLOAT_ARRAY;
        }

        try {

            JsonNode x = Util.jsonMapper.readTree(new InputStreamReader(u.openStream()));
            System.out.println(x);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new float[] { };
    }

    static String rfc(long from) {
        return RFCTime.format(new Date(from));
    }

    public static void main(String[] args) {

        System.out.println(Arrays.toString(
            new InfluxDB("nar1").get("cpu", "*",
                System.currentTimeMillis() - 16 * 24 * 60 * 60 * 1000, System.currentTimeMillis()))
        );
    }
}
