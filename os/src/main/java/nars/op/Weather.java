package nars.op;

import com.fasterxml.jackson.databind.JsonNode;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.v2;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.control.Part;
import nars.task.NALTask;
import nars.time.clock.RealTime;
import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import static nars.$.$$;
import static nars.Op.BELIEF;

/**
 * weather and meteorlogical model
 */
public class Weather extends Part {
    public final v2 lonLat;

    /** current day daylight param */
    transient Date sunrise, sunset, noon;

    /**
     * updated with the latest events
     */
    private final List<Task> events = new FasterList();

    /** TODO GeoIP */
    public Weather(NAR nar, float lon, float lat) {
        super($.func($.the(Weather.class.getSimpleName()), $.the(lon), $.the(lat)));
        this.lonLat = new v2(lon, lat);

        assert (nar.time instanceof RealTime.MS && ((RealTime)nar.time).relativeToStart);

        nar.add(this);
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);

        update();
    }

    protected void update() {

        synchronized (events) {
            events.forEach(t -> t.delete());
            events.clear();

            updateSunRiseSetTime();
            updateWeatherGov();

            events.forEach(x -> x.pri(nar));

            nar.input(events);
        }
    }

    private void updateSunRiseSetTime() {
        //https://sunrise-sunset.org/api
        String url = "https://api.sunrise-sunset.org/json?lat=" + lonLat.y + "&lng=" + lonLat.x;
        try {

            JsonNode w = json(url);

            synchronized (events) {

                //{"results":{"sunrise":"11:28:31 AM","sunset":"9:58:36 PM","solar_noon":"4:43:34 PM","day_length":"10:30:05","civil_twilight_begin":"11:01:13 AM","civil_twilight_end":"10:25:54 PM","nautical_twilight_begin":"10:30:00 AM","nautical_twilight_end":"10:57:07 PM","astronomical_twilight_begin":"9:59:15 AM","astronomical_twilight_end":"11:27:52 PM"},"status":"OK"}

                w = w.get("results");

                sunrise = DateTimeFormat.forPattern("hh:mm:ss a").withZoneUTC().parseDateTime(w.get("sunrise").asText()).toDateTime().withDate(LocalDate.now()).toDate();
                sunset = DateTimeFormat.forPattern("hh:mm:ss a").withZoneUTC().parseDateTime(w.get("sunset").asText()).toDateTime().withDate(LocalDate.now()).toDate();
                noon = DateTimeFormat.forPattern("hh:mm:ss a").withZoneUTC().parseDateTime(w.get("solar_noon").asText()).toDateTime().withDate(LocalDate.now()).toDate();


                long HALF_HOUR_ms = 3600/2 * 1000;
                /* (parameters) */
                events.add(
                        NALTask.the($$("sunrise" /* (parameters) */), BELIEF, $.t(1, 0.9f), nar.time(), sunrise.getTime() - HALF_HOUR_ms, sunrise.getTime() + HALF_HOUR_ms, nar.evidence())
                );
                /* (parameters) */
                events.add(
                        NALTask.the($$("sunset" /* (parameters) */), BELIEF, $.t(1, 0.9f), nar.time(), sunset.getTime() - HALF_HOUR_ms, sunset.getTime() + HALF_HOUR_ms, nar.evidence())
                );
                /* (parameters) */
                events.add(
                        NALTask.the($$("daylight" /* (parameters) */), BELIEF, $.t(1, 0.9f), nar.time(), sunrise.getTime(), sunset.getTime(), nar.evidence())
                );
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

                //System.out.println(Util.jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(w));

                //parse temperature forecast
                JsonNode times = w.get("time").get("startValidTime");
                JsonNode temperatures = w.get("data").get("temperature");
                int n = times.size();
                DateTimeFormatter df = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ");
                //2018-11-05T18:00:00-05:00

                //12 hour intervals
                for (int i = 0; i < n; i++) {
                    Date ww = df.parseDateTime(times.get(i).asText()).toDate();
                    int tempFahrenheit = Integer.valueOf(temperatures.get(i).asText()); //a string really??

                    forecastTemperature(ww, 3600*1000*6, tempFahrenheit);
                }

                //{"operationalMode":"Production","srsName":"WGS 1984","creationDate":"2018-11-03T06:43:03-04:00","creationDateLocal":"1 Jan 00:00 am EDT","productionCenter":"Wakefield, VA","credit":"http://www.weather.gov/akq/","moreInformation":"http://weather.gov","location":{"region":"erh","latitude":"37.99","longitude":"-75.01","elevation":"0","wfo":"AKQ","timezone":"E|Y|5","areaDescription":"17NM E Chincoteague VA","radar":"KDOX","zone":"ANZ650","county":"marine","firezone":"","metar":""},"time":{"layoutKey":"k-p12h-n9-1","startPeriodName":["Today","Tonight","Sunday","Sunday Night","Monday","Monday Night","Tuesday","Tuesday Night","Wednesday"],"startValidTime":["2018-11-03T06:00:00-04:00","2018-11-03T18:00:00-04:00","2018-11-04T06:00:00-05:00","2018-11-04T18:00:00-05:00","2018-11-05T06:00:00-05:00","2018-11-05T18:00:00-05:00","2018-11-06T06:00:00-05:00","2018-11-06T18:00:00-05:00","2018-11-07T06:00:00-05:00"],"tempLabel":["High","Low","High","Low","High","Low","High","Low","High"]},"data":{"temperature":["61","50","62","57","66","61","68","62","67"],"pop":[null,null,null,null,null,null,null,null,null],"weather":[" "," "," "," "," "," "," "," "," "],"iconLink":["http://forecast.weather.gov/images/wtf/medium/m_hi_shwrs.png","http://forecast.weather.gov/images/wtf/medium/m_nskc.png","http://forecast.weather.gov/images/wtf/medium/m_few.png","http://forecast.weather.gov/images/wtf/medium/m_nshra.png","http://forecast.weather.gov/images/wtf/medium/m_shra.png","http://forecast.weather.gov/images/wtf/medium/m_nshra.png","http://forecast.weather.gov/images/wtf/medium/m_tsra.png","http://forecast.weather.gov/images/wtf/medium/m_ntsra.png","http://forecast.weather.gov/images/wtf/medium/m_shra.png"],"hazard":["Small Craft Advisory"],"hazardUrl":["http://forecast.weather.gov/showsigwx.php?warnzone=ANZ650&amp;warncounty=marine&amp;firewxzone=&amp;local_place1=17NM+E+Chincoteague+VA&amp;product1=Small+Craft+Advisory"],"text":["WNW wind 23 to 26 kt decreasing to 20 to 23 kt in the afternoon. Winds could gust as high as 32 kt. A slight chance of showers before 10am.   Seas 5 to 6 ft.","WNW wind around 14 kt becoming N after midnight. Clear. Seas 3 to 4 ft.","NNE wind 10 to 13 kt becoming E in the afternoon. Sunny. Seas around 3 ft.","E wind 13 to 15 kt. A chance of showers, mainly after 2am.   Seas around 3 ft.","E wind 16 to 19 kt decreasing to 13 to 16 kt in the afternoon. Showers likely, mainly between 8am and 2pm.   Seas 4 ft building to 6 ft.","SSE wind 5 to 9 kt. A chance of showers.   Seas around 5 ft.","SE wind 6 to 9 kt becoming S 11 to 16 kt in the afternoon. A chance of showers and thunderstorms.   Seas around 4 ft.","S wind 19 to 21 kt becoming SW after midnight. A chance of showers and thunderstorms.   Seas 5 to 6 ft.","W wind 11 to 14 kt. A chance of showers.   Seas 4 to 5 ft."]},"currentobservation":{"id":"","name":"","elev":"","latitude":"","longitude":"","Date":"1 Jan 00:00 am EDT","datetime":"","Temp":"0","AirTemp":"","WaterTemp":"","Dewp":"","Winds":"","Windd":"","Gust":"","Weather":"","WaveHeight":"","Visibility":"","Pressure":"","timezone":"","state":"","DomWavePeriod":""}}
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void forecastTemperature(Date at, long radiusMS, float degreesF) {
        //System.out.println(at + " " + degreesF + " deg F");
        /* parameters, */
        events.add(
                NALTask.the($.func("temperature", /* parameters, */ $.the(degreesF)), BELIEF, $.t(1, 0.9f), nar.time(), at.getTime() - radiusMS, at.getTime() + radiusMS, nar.evidence())
        );
    }

    private static JsonNode json(String url) throws IOException {
        return Util.jsonMapper.readValue(IOUtils.toString(new URL(url)), JsonNode.class);
    }


//    public static void main(String[] args) {
//        NAR n = NARS.realtime(1f).withNAL(1,8).get();
//
////        Param.DEBUG = true;
//
//        new STMLinkage(n, 1);
//        //new Arithmeticize.ArithmeticIntroduction(n, 32);
//
//        Weather w = new Weather(n, -75, 38);
//        w.events.forEach(System.out::println);
//
//        n.run(4);
//
//        List<Task> tasks = w.events;
//        Iterable<Task> allTasks = ()->n.tasks().iterator();
//
//        allTasks.forEach(t -> {
//            System.out.println(t.proof());
//        });
//
//        Timeline2D t = TasksView.timeline(allTasks).view(n.time()-3600*1000,n.time()+3600*1000);
//
//
//        window(t.withControls(), 800, 800);
//
//    }
}
