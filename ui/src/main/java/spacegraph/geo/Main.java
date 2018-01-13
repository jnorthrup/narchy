package spacegraph.geo;

import spacegraph.geo.data.Osm;

import java.net.URL;

public class Main {

    public static void main(String[] args) throws Exception {


        //https://wiki.openstreetmap.org/wiki/API_v0.6
        //http://api.openstreetmap.org/api/0.6/changeset/#id/comment
                                   // /api/0.6/map?bbox=min_lon,min_lat,max_lon,max_lat (W,S,E,N)

            Osm osm = OsmReader.load(new URL("http://api.openstreetmap.org/api/0.6/map?bbox=-80.65,28.58,-80.60,28.63").openStream());
            new OsmViewer(osm).show(800, 800);

    }
}
