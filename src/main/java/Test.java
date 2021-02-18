import io.github.geohash.GeoHash;
import io.github.geohash.GeoHashUtils;
import io.github.geojson.GeoJSON;
import io.github.geom.Geom;
import org.locationtech.jts.geom.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class Test {
    public static void main(String[] args) throws IOException {
//        System.out.println(GeoHash.geoHash(30.723514, 104.123245,9));
//        System.out.println(GeoHash.fromGeohashString("wm6nc3h1r").getNorthernNeighbour());
        double[][] dd = {{43.82682, 87.561062}, {43.82603, 87.56118}, {43.825584, 87.561228}, {43.824812, 87.559267}, {43.823374, 87.559966}, {43.822403, 87.55697}, {43.822063, 87.557242}, {43.82196, 87.557343}, {43.821884, 87.557411}, {43.821541, 87.557739}, {43.821297, 87.557968}, {43.821026, 87.558212}, {43.821026, 87.55822}, {43.82095, 87.558289}, {43.820648, 87.55854}, {43.820457, 87.558701}, {43.820316, 87.558784}, {43.819336, 87.55941}, {43.818413, 87.559868}, {43.818369, 87.559888}, {43.818079, 87.560069}, {43.816741, 87.560534}, {43.816735, 87.560536}, {43.816155, 87.560759}, {43.816258, 87.561499}, {43.816504, 87.562881}, {43.817145, 87.562698}, {43.818107, 87.567492}, {43.818326, 87.569616}, {43.813458, 87.571201}, {43.813561, 87.571781}, {43.813934, 87.573748}, {43.813909, 87.573757}, {43.814606, 87.577705}, {43.819122, 87.575989}, {43.819328, 87.576653}, {43.819881, 87.576721}, {43.820751, 87.576462}, {43.821248, 87.577614}, {43.821629, 87.577293}, {43.821831, 87.577744}, {43.823769, 87.577217}, {43.82365, 87.575882}, {43.825039, 87.5755}, {43.824669, 87.574402}, {43.82653, 87.573799}, {43.825508, 87.572128}, {43.826324, 87.568733}, {43.828007, 87.569618}, {43.828228, 87.568977}, {43.828635, 87.569151}, {43.828725, 87.569072}, {43.828926, 87.568604}, {43.828968, 87.568535}, {43.829071, 87.568367}, {43.829262, 87.567757}, {43.829269, 87.567688}, {43.829258, 87.567665}, {43.829113, 87.567551}, {43.829102, 87.567505}, {43.829109, 87.567429}, {43.829342, 87.56665}, {43.829315, 87.566544}, {43.829277, 87.56649}, {43.828979, 87.566254}, {43.828751, 87.566055}, {43.828674, 87.56601}, {43.82798, 87.565628}, {43.827759, 87.565498}, {43.827629, 87.565453}, {43.827522, 87.565437}, {43.827412, 87.565437}, {43.827347, 87.565453}, {43.827248, 87.565483}, {43.82753, 87.564476}, {43.827785, 87.56356}, {43.827789, 87.563316}, {43.827779, 87.563052}};
        Coordinate[] cd = new Coordinate[79];
        int i = 0;
        for(double[] d : dd){
            cd[i] = new Coordinate(d[1],d[0]);
            i++;
        }
        cd[78] = cd[0];
        Polygon geom = Geom.polygon(cd);
        System.out.println(GeoJSON.toJson(geom));
        Set<String> hash = new GeoHash().geoHashesPolygon(geom,7);
        System.out.println(hash);
        Geometry[] gms = new Geometry[hash.size()];
        int k=0;
        for(String st : hash){
            Envelope columnBox = GeoHash.decode(st).getEnvelope();
            Geometry geoHashEvp = Geom.polygon(columnBox.getMinX(),columnBox.getMinY(),columnBox.getMaxX(),columnBox.getMinY(),columnBox.getMaxX(),columnBox.getMaxY(),columnBox.getMinX(),columnBox.getMaxY(),columnBox.getMinX(),columnBox.getMinY());
            gms[k] = geoHashEvp;
            k++;
        }
        GeometryCollection gcs = new GeometryCollection(gms,Geom.factory);
        System.out.println(GeoJSON.toJson(gcs));
        FileOutputStream out = new FileOutputStream("d:/gcs2.json");
        out.write(GeoJSON.toJson(gcs).getBytes(StandardCharsets.UTF_8));
        out.close();

//        Set<String> hash = GeoHashUtils.geoHashesPolygon(geom,7);
//        System.out.println(hash);
//        Geometry[] gms = new Geometry[hash.size()];
//        int k=0;
//        for(String st : hash){
////            System.out.println(st);
//            double[] columnBox = GeoHashUtils.decodeBbox(st);
//            Geometry geoHashEvp = Geom.polygon(columnBox[0],columnBox[1],columnBox[2],columnBox[1],columnBox[2],columnBox[3],columnBox[0],columnBox[3],columnBox[0],columnBox[1]);
//            gms[k] = geoHashEvp;
//            k++;
//        }
//        GeometryCollection gcs = new GeometryCollection(gms,Geom.factory);
//        System.out.println(GeoJSON.toJson(gcs));
//        FileOutputStream out = new FileOutputStream("d:/gcs.json");
//        out.write(GeoJSON.toJson(gcs).getBytes(StandardCharsets.UTF_8));
//        out.close();

//        String[] strs = {"tzy328p","tzy32b0","tzy32b1","tzy328r","tzy32b2","tzy32b3","tzy32b6","tzy32b7","tzy3288","tzy328x","tzy32b8","tzy32b9","tzy32bd","tzy32be","tzy322z","tzy328b","tzy328c","tzy328f","tzy328g","tzy328u","tzy328v","tzy328y","tzy328z","tzy32bb","tzy32bc","tzy32bf","tzy323p","tzy3290","tzy3291","tzy3294","tzy3295","tzy329h","tzy329j","tzy329n","tzy329p","tzy32c0","tzy32c1","tzy32c4","tzy323q","tzy323r","tzy3292","tzy3293","tzy3296","tzy3297","tzy329k","tzy329m","tzy329q","tzy329r","tzy32c2","tzy32c3","tzy32c6","tzy32c7","tzy323t","tzy323w","tzy323x","tzy3298","tzy3299","tzy329d","tzy329e","tzy329s","tzy329t","tzy329w","tzy329x","tzy32c8","tzy32c9","tzy32cd","tzy32ce","tzy323z","tzy329b","tzy329c","tzy329f","tzy329g","tzy329u","tzy329v","tzy329y","tzy329z","tzy32cb","tzy32cc","tzy32cf","tzy326p","tzy32d0","tzy32d1","tzy32d4","tzy32d5","tzy32dh","tzy32dj","tzy32dn","tzy32dp","tzy32f0","tzy32f1","tzy32d2","tzy32d3","tzy32d6","tzy32d7","tzy32dk","tzy32dm","tzy32de","tzy32ds","tzy32dt"};
//        Geometry[] gms = new Geometry[strs.length];
//        int k=0;
//        for(String st : strs){
//            double[] columnBox = GeoHashUtils.decodeBbox(st);
//            Geometry geoHashEvp = Geom.polygon(columnBox[0],columnBox[1],columnBox[2],columnBox[1],columnBox[2],columnBox[3],columnBox[0],columnBox[3],columnBox[0],columnBox[1]);
//            gms[k] = geoHashEvp;
//            k++;
//        }
//        GeometryCollection gcs = new GeometryCollection(gms,Geom.factory);
//        FileOutputStream out = new FileOutputStream("d:/gcs1.json");
//        out.write(GeoJSON.toJson(gcs).getBytes(StandardCharsets.UTF_8));
//        out.close();
    }
}
