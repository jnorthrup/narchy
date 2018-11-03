package nars.op;

import com.fasterxml.jackson.databind.JsonNode;
import jcog.Util;
import jcog.data.list.FasterList;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.control.NARService;
import nars.time.clock.RealTime;
import org.apache.commons.io.IOUtils;
import spacegraph.util.math.v2;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * weather and meteorlogical model
 */
public class Weather extends NARService {
    public final v2 lonLat;

    /**
     * updated with the latest events
     */
    private final List<Task> events = new FasterList();

    public Weather(NAR nar, float lon, float lat) {
        super($.func($.the(Weather.class.getSimpleName()), $.the(lon), $.the(lat)));
        this.lonLat = new v2(lon, lat);

        assert (nar.time instanceof RealTime.MS);

        nar.on(this);
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);

        updateSunRiseSetTime();
        updateWeatherGov();
    }

    private void updateSunRiseSetTime() {
        //https://sunrise-sunset.org/api
        String url = "https://api.sunrise-sunset.org/json?lat=" + lonLat.y + "&lng=" + lonLat.x;
        try {

            JsonNode w = json(url);

            synchronized (events) {
                System.out.println(w);
                //{"results":{"sunrise":"11:28:31 AM","sunset":"9:58:36 PM","solar_noon":"4:43:34 PM","day_length":"10:30:05","civil_twilight_begin":"11:01:13 AM","civil_twilight_end":"10:25:54 PM","nautical_twilight_begin":"10:30:00 AM","nautical_twilight_end":"10:57:07 PM","astronomical_twilight_begin":"9:59:15 AM","astronomical_twilight_end":"11:27:52 PM"},"status":"OK"}
                //TODO
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void updateWeatherGov() {

        String url =
                "https://marine.weather.gov/MapClick.php?lat=" +
                        lonLat.y + "&lon=" + lonLat.x +
                        "&unit=0&lg=english&FcstType=json";
        try {

            JsonNode w = json(url);

            synchronized (events) {
                System.out.println(w);
                //{"operationalMode":"Production","srsName":"WGS 1984","creationDate":"2018-11-03T06:43:03-04:00","creationDateLocal":"1 Jan 00:00 am EDT","productionCenter":"Wakefield, VA","credit":"http://www.weather.gov/akq/","moreInformation":"http://weather.gov","location":{"region":"erh","latitude":"37.99","longitude":"-75.01","elevation":"0","wfo":"AKQ","timezone":"E|Y|5","areaDescription":"17NM E Chincoteague VA","radar":"KDOX","zone":"ANZ650","county":"marine","firezone":"","metar":""},"time":{"layoutKey":"k-p12h-n9-1","startPeriodName":["Today","Tonight","Sunday","Sunday Night","Monday","Monday Night","Tuesday","Tuesday Night","Wednesday"],"startValidTime":["2018-11-03T06:00:00-04:00","2018-11-03T18:00:00-04:00","2018-11-04T06:00:00-05:00","2018-11-04T18:00:00-05:00","2018-11-05T06:00:00-05:00","2018-11-05T18:00:00-05:00","2018-11-06T06:00:00-05:00","2018-11-06T18:00:00-05:00","2018-11-07T06:00:00-05:00"],"tempLabel":["High","Low","High","Low","High","Low","High","Low","High"]},"data":{"temperature":["61","50","62","57","66","61","68","62","67"],"pop":[null,null,null,null,null,null,null,null,null],"weather":[" "," "," "," "," "," "," "," "," "],"iconLink":["http://forecast.weather.gov/images/wtf/medium/m_hi_shwrs.png","http://forecast.weather.gov/images/wtf/medium/m_nskc.png","http://forecast.weather.gov/images/wtf/medium/m_few.png","http://forecast.weather.gov/images/wtf/medium/m_nshra.png","http://forecast.weather.gov/images/wtf/medium/m_shra.png","http://forecast.weather.gov/images/wtf/medium/m_nshra.png","http://forecast.weather.gov/images/wtf/medium/m_tsra.png","http://forecast.weather.gov/images/wtf/medium/m_ntsra.png","http://forecast.weather.gov/images/wtf/medium/m_shra.png"],"hazard":["Small Craft Advisory"],"hazardUrl":["http://forecast.weather.gov/showsigwx.php?warnzone=ANZ650&amp;warncounty=marine&amp;firewxzone=&amp;local_place1=17NM+E+Chincoteague+VA&amp;product1=Small+Craft+Advisory"],"text":["WNW wind 23 to 26 kt decreasing to 20 to 23 kt in the afternoon. Winds could gust as high as 32 kt. A slight chance of showers before 10am.   Seas 5 to 6 ft.","WNW wind around 14 kt becoming N after midnight. Clear. Seas 3 to 4 ft.","NNE wind 10 to 13 kt becoming E in the afternoon. Sunny. Seas around 3 ft.","E wind 13 to 15 kt. A chance of showers, mainly after 2am.   Seas around 3 ft.","E wind 16 to 19 kt decreasing to 13 to 16 kt in the afternoon. Showers likely, mainly between 8am and 2pm.   Seas 4 ft building to 6 ft.","SSE wind 5 to 9 kt. A chance of showers.   Seas around 5 ft.","SE wind 6 to 9 kt becoming S 11 to 16 kt in the afternoon. A chance of showers and thunderstorms.   Seas around 4 ft.","S wind 19 to 21 kt becoming SW after midnight. A chance of showers and thunderstorms.   Seas 5 to 6 ft.","W wind 11 to 14 kt. A chance of showers.   Seas 4 to 5 ft."]},"currentobservation":{"id":"","name":"","elev":"","latitude":"","longitude":"","Date":"1 Jan 00:00 am EDT","datetime":"","Temp":"0","AirTemp":"","WaterTemp":"","Dewp":"","Winds":"","Windd":"","Gust":"","Weather":"","WaveHeight":"","Visibility":"","Pressure":"","timezone":"","state":"","DomWavePeriod":""}}
                //TODO
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static JsonNode json(String url) throws IOException {
        return Util.jsonMapper.readValue(IOUtils.toString(new URL(url)), JsonNode.class);
    }


    public static void main(String[] args) {
        Weather w = new Weather(NARS.realtime(1f).get(), -75, 38);
        w.events.forEach(System.out::println);
    }
}
