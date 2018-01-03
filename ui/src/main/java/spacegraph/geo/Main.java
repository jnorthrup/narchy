package spacegraph.geo;

import spacegraph.geo.data.Osm;

public class Main {

    public static void main(String[] args) {
//        String filename = "p.osm.gz";
        String filename = "tokyo.osm";
//        String filename = "map.osm";
//        String filename = "coastline_islands.osm";
//        String filename = "coastline_multiple_coasts.osm";
//        String filename = "tessellation.osm";
//        String filename = "tessellation_broken.osm";
//        String filename = "sakurada_moat.osm";

        if (args.length > 0) {
            filename = args[0];
        } else {
            filename = "/home/me/myosm/osm/" + filename; //HACK
        }

        try {
            OsmReader osmReader = new OsmReader(filename);
            Osm osm = osmReader.parse();
            //OsmReader.printOsm(osm);
            new OsmViewer(osm).show(800, 800);
        }
        catch (Exception e) {
            System.out.print("Usage: myosm filename\n");
            System.out.print(e);
        }
    }
}
