package spacegraph.geo;

import spacegraph.geo.data.Osm;

public class Main {

    public static void main(String[] args) {
        String filename = "map.osm";
//        String filename = "./osm/coastline_islands.osm";
//        String filename = "./osm/coastline_multiple_coasts.osm";
//        String filename = "./osm/tessellation.osm";
//        String filename = "./osm/tessellation_broken.osm";
//        String filename = "./osm/sakurada_moat.osm";

        if (args.length > 0) {
            filename = args[0];
        } else {
            filename = "/home/me/myosm/osm/" + filename; //HACK
        }

        try {
            OsmReader osmReader = new OsmReader(filename);
            Osm osm = osmReader.parse();
            OsmReader.printOsm(osm);
            new OsmViewer(osm);
        }
        catch (Exception e) {
            System.out.print("Usage: myosm filename\n");
            System.out.print(e);
        }
    }
}
