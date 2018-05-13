package nars.util;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.task.util.TaskRule;
import nars.term.atom.Atomic;
import nars.term.obj.JsonTerm;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

public class JsonTermTest {

    @Test public void testJsonArray() throws Narsese.NarseseException {
        NAR d = new NARS().get();
        d.log();

        new TaskRule("(json,%1):{x(%2)}", "X(%1,%2)", d);

        //d.believe( $.inh( JsonCompound.the("['a', 1, ['b', 'c']]"), $.the("(json,1)")  ) );
        //TaskProcess: $.50;.95$ (("a"),(1),(("b"),("c"))). %1.0;.90% {0: 1}

        d.believe( $.inh( JsonTerm.the("{ x: 3, y: [\"a\",4] }"), $.$("(json,2)") ) );
        //d.believe( $.inh( JsonCompound.the("{ x: 3 }"), $.$("(json,2)") ) );
        //               $.15;.86$ X(3,2). %1.0;.81% {1: 1;;}

        //$.50;.95$ {x(3),y(("a"),("b"))}. %1.0;.90% {0: 2}

        d.run(256);
    }

    @Test public void testBigJSON() {
        NAR d = new NARS().get();
        //d.log();

        int n = 0;
        for (String json : new String[] {
                "{\"coord\":{\"lon\":-0.13,\"lat\":51.51},\"weather\":[{\"id\":300,\"main\":\"Drizzle\",\"description\":\"light intensity drizzle\",\"icon\":\"09d\"}],\"base\":\"stations\",\"main\":{\"temp\":280.32,\"pressure\":1012,\"humidity\":81,\"temp_min\":279.15,\"temp_max\":281.15},\"visibility\":10000,\"wind\":{\"speed\":4.1,\"deg\":80},\"clouds\":{\"all\":90},\"dt\":1485789600,\"sys\":{\"type\":1,\"id\":5091,\"message\":0.0103,\"country\":\"GB\",\"sunrise\":1485762037,\"sunset\":1485794875},\"id\":2643743,\"name\":\"London\",\"cod\":200}",
                "{\"coord\":{\"lon\":139.01,\"lat\":35.02},\"weather\":[{\"id\":800,\"main\":\"Clear\",\"description\":\"clear sky\",\"icon\":\"01n\"}],\"base\":\"stations\",\"main\":{\"temp\":285.514,\"pressure\":1013.75,\"humidity\":100,\"temp_min\":285.514,\"temp_max\":285.514,\"sea_level\":1023.22,\"grnd_level\":1013.75},\"wind\":{\"speed\":5.52,\"deg\":311},\"clouds\":{\"all\":0},\"dt\":1485792967,\"sys\":{\"message\":0.0025,\"country\":\"JP\",\"sunrise\":1485726240,\"sunset\":1485763863},\"id\":1907296,\"name\":\"Tawarano\",\"cod\":200}"
        }) {
            Atomic id = Atomic.the("WEATHER_" + (n++));
            d.believe($.inh(JsonTerm.the(json), id), Tense.Eternal);
            d.believe($.inst(id, Atomic.the("now")), Tense.Present);
        }
        d.run(256);
    }

    @Test public void testBigJSON2() {
        /*
        * https://eonet.sci.gsfc.nasa.gov/api/v2.1/events?limit=5&days=20&source=InciWeb&status=open
        * https://worldview.earthdata.nasa.gov/config/wv.json
        * */
        String j = "{ \"id\": \"EONET_2797\",\n" +
                "   \"title\": \"Snake Ridge Fire, ARIZONA\",\n" +
                "   \"description\": \"\",\n" +
                "   \"link\": \"http://eonet.sci.gsfc.nasa.gov/api/v2.1/events/EONET_2797\",\n" +
                "   \"categories\": [\n" +
                "    {\n" +
                "     \"id\": 8,\n" +
                "     \"title\": \"Wildfires\"\n" +
                "    }\n" +
                "   ] }";
        NAR d = new NARS().get();
        d.log();
        d.believe($.inh($.fromJSON(j), "x"));
        d.run(1000);
    }
}