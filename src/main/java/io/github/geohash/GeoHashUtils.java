package io.github.geohash;

import io.github.geom.Geom;
import org.locationtech.jts.geom.*;

import java.util.*;

public class GeoHashUtils {
    private static final int MAX_BIT_PRECISION = 64;
    private static final int DEFAULT_GEO_HASH_LENGTH = 12;

    private static final long serialVersionUID = -8553214249630252175L;
    private static final int[] BITS = { 16, 8, 4, 2, 1 };
    private static final int BASE32_BITS = 5;
    public static final long FIRST_BIT_FLAGGED = 0x8000000000000000L;
    private static final char[] BASE32_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

    private final static Map<Character, Integer> BASE32_DECODE_MAP = new HashMap<>();
    static {
        int sz = BASE32_CHARS.length;
        for (int i = 0; i < sz; i++) {
            BASE32_DECODE_MAP.put(BASE32_CHARS[i], i);
        }
    }

    /**
     * Same as encode but returns a substring of the specified length.
     *
     * @param latitude latitude
     * @param longitude longitude
     * @param length length in characters (1 to 12)
     * @return geo hash of the specified length. The minimum length is 1 and the maximum length is 12.
     */
    public static String encode(double latitude, double longitude, int length){
        if(length <1 || length >12){
            throw new IllegalArgumentException("length must be between 1 and 12");
        }
        double[] latInterval = { -90, 90 };
        double[] lonInterval = { -180, 180 };
        StringBuilder geohash = new StringBuilder();
        boolean isEven = true;
        int bit = 0;
        int ch = 0;
        while(geohash.length() < length){
            if (isEven){
                double mid = (lonInterval[0] + lonInterval[1])/2;
                if(longitude > mid){
                    ch = ch | BITS[bit];
                    lonInterval[0] = mid;
                }else{
                    lonInterval[1] = mid;
                }
            }else{
                double mid = (latInterval[0] + latInterval[1])/2;
                if(latitude > mid){
                    ch = ch | BITS[bit];
                    latInterval[0] = mid;
                }else{
                    latInterval[1] = mid;
                }
            }
            isEven = !isEven;
            if(bit < 4){
                bit++;
            }else{
                geohash.append(BASE32_CHARS[ch]);
                bit = 0;
                ch = 0;
            }
        }
        return geohash.toString();
    }

    /**
     * Encode a geojson style point of [longitude,latitude]
     * @param pt point
     * @param length length in characters (1 to 12)
     * @return geohash
     */
    public static String encode(Point pt, int length){
        return encode(pt.getY(), pt.getX(), length);
    }

    /**
     * @param geoHash valid geoHash
     * @return double array representing the bounding box for the geoHash of [south latitude, north latitude, west
     * longitude, east longitude]
     */
    public static double[] decodeBbox(String geoHash){
        double south = -90.0;
        double north = 90.0;
        double west = -180.0;
        double east = 180.0;
        boolean isEvent = true;
        for (Character ch : geoHash.toCharArray()){
            Integer currentCharacter = BASE32_DECODE_MAP.get(ch);
            if (currentCharacter == null){
                throw new IllegalArgumentException("not a base32 character");
            }
            for (int mask : BITS){
                if(isEvent){
                    if((currentCharacter & mask) != 0){
                        west = (west + east) / 2;
                    }else{
                        east = (west + east) / 2;
                    }
                }else{
                    if((currentCharacter & mask) != 0){
                        south = (south + north) / 2;
                    }else{
                        north = (south + north) / 2;
                    }
                }
                isEvent = !isEvent;
            }
        }
        return new double[]{west,south,east,north};
    }

    public static Polygon decodePolygon(String geoHash){
        double[] columnBox = decodeBbox(geoHash);
        Polygon pg = Geom.polygon(columnBox[0],columnBox[1],columnBox[2],columnBox[1],columnBox[2],columnBox[3],columnBox[0],columnBox[3],columnBox[0],columnBox[1]);
        return pg;
    }

    /**
     * This decodes the geo hash into it's center. Note that the coordinate that you used to generate the geo hash may
     * be anywhere in the geo hash's bounding box and therefore you should not expect them to be identical.
     *
     * The original apache code attempted to round the returned coordinate. I have chosen to remove this 'feature' since
     * it is useful to know the center of the geo hash as exactly as possible, even for very short geo hashes.
     *
     * Should you wish to apply some rounding, you can use the GeoGeometry.roundToDecimals method.
     *
     * @param geoHash valid geo hash
     * @return a Point representing the center of the geohash as a double array of [longitude,latitude]
     */
    public static Point decode(String geoHash){
        double[] bbox = decodeBbox(geoHash);
        double latitude = (bbox[1] + bbox[3]) / 2;
        double longitude = (bbox[0] + bbox[2]) / 2;
        return Geom.point(longitude, latitude);
    }

    /**
     * @param geoHash geohash
     * @return the geo hash of the same length directly south of the bounding box.
     */
    public static String south(String geoHash){
        double[] bbox = decodeBbox(geoHash);
        double latDiff = bbox[3] - bbox[1];
        double lat = bbox[1] - latDiff / 2;
        double lon = (bbox[0] + bbox[2]) / 2;
        return encode(lat, lon, geoHash.length());
    }

    /**
     * @param geoHash geohash
     * @return the geo hash of the same length directly north of the bounding box.
     */
    public static String north(String geoHash){
        double[] bbox = decodeBbox(geoHash);
        double latDiff = bbox[3] - bbox[1];
        double lat = bbox[3] + latDiff / 2;
        double lon = (bbox[0] + bbox[2]) / 2;
        return encode(lat, lon, geoHash.length());
    }

    /**
     * @param geoHash geohash
     * @return the geo hash of the same length directly west of the bounding box.
     */
    public static String west(String geoHash){
        double[] bbox = decodeBbox(geoHash);
        double lonDiff = bbox[2] - bbox[0];
        double lat = (bbox[1] + bbox[3]) / 2;
        double lon = bbox[0] - lonDiff / 2;
        if(lon < -180){
            lon = 180 - (lon + 180);
        }else if(lon > 180){
            lon = 180.0;
        }
        return encode(lat, lon ,geoHash.length());
    }

    /**
     * @param geoHash geohash
     * @return the geo hash of the same length directly east of the bounding box.
     */
    public static String east(String geoHash){
        double[] bbox = decodeBbox(geoHash);
        double lonDiff = bbox[2] - bbox[0];
        double lat = (bbox[1] + bbox[3]) / 2;
        double lon = bbox[2] + lonDiff / 2;
        if(lon > 180){
            lon = -180 + (lon - 180);
        }else if(lon < -180){
            lon = -180.0;
        }
        return encode(lat, lon, geoHash.length());
    }

//    public boolean contains(){
//
//    }

    /**
     * Return the 32 geo hashes this geohash can be divided into.
     *
     * They are returned alpabetically sorted but in the real world they follow this pattern:
     *
     * <pre>
     * u33dbfc0 u33dbfc2 | u33dbfc8 u33dbfcb
     * u33dbfc1 u33dbfc3 | u33dbfc9 u33dbfcc
     * -------------------------------------
     * u33dbfc4 u33dbfc6 | u33dbfcd u33dbfcf
     * u33dbfc5 u33dbfc7 | u33dbfce u33dbfcg
     * -------------------------------------
     * u33dbfch u33dbfck | u33dbfcs u33dbfcu
     * u33dbfcj u33dbfcm | u33dbfct u33dbfcv
     * -------------------------------------
     * u33dbfcn u33dbfcq | u33dbfcw u33dbfcy
     * u33dbfcp u33dbfcr | u33dbfcx u33dbfcz
     </pre> *
     *
     * the first 4 share the north east 1/8th the first 8 share the north east 1/4th the first 16 share the north 1/2
     * and so on.
     *
     * They are ordered as follows:
     *
     * <pre>
     * 0  2  8 10
     * 1  3  9 11
     * 4  6 12 14
     * 5  7 13 15
     * 16 18 24 26
     * 17 19 25 27
     * 20 22 28 30
     * 21 23 29 31
     </pre> *
     *
     * Some useful properties: Anything ending with
     *
     * <pre>
     * 0-g = N
     * h-z = S
     *
     * 0-7 = NW
     * 8-g = NE
     * h-r = SW
     * s-z = SE
     </pre> *
     *
     * @param geoHash geo hash
     * @return String array with the geo hashes.
     */
    public static List<String> subHashes(String geoHash){
        List<String> list = new ArrayList<>();
        for (char c : BASE32_CHARS){
            list.add(geoHash + c);
        }
        return list;
    }

    /**
     * @param geoHash geo hash
     * @return the 16 northern sub hashes of the geo hash
     */
    public static List<String> subHashesNorth(String geoHash){
        List<String> list = new ArrayList<>();
        for (char c : BASE32_CHARS){
            if(c <= 'g'){
                list.add(geoHash + c);
            }
        }
        return list;
    }

    /**
     * @param geoHash geo hash
     * @return the 16 southern sub hashes of the geo hash
     */
    public static List<String> subHashesSouth(String geoHash){
        List<String> list = new ArrayList<>();
        for (char c : BASE32_CHARS){
            if(c >= 'h'){
                list.add(geoHash + c);
            }
        }
        return list;
    }

    /**
     * @param geoHash geo hash
     * @return the 8 north-west sub hashes of the geo hash
     */
    public static List<String> subHashesNorthWest(String geoHash){
        List<String> list = new ArrayList<>();
        for (char c : BASE32_CHARS){
            if(c <= '7'){
                list.add(geoHash + c);
            }
        }
        return list;
    }

    /**
     * @param geoHash geo hash
     * @return the 8 north-east sub hashes of the geo hash
     */
    public static List<String> subHashesNorthEast(String geoHash){
        List<String> list = new ArrayList<>();
        for (char c : BASE32_CHARS){
            if(c >= '8' && c<= 'g'){
                list.add(geoHash + c);
            }
        }
        return list;
    }

    /**
     * @param geoHash geo hash
     * @return the 8 south-west sub hashes of the geo hash
     */
    public static List<String> subHashesSouthWest(String geoHash){
        List<String> list = new ArrayList<>();
        for (char c : BASE32_CHARS){
            if(c >= 'h' && c<= 'r'){
                list.add(geoHash + c);
            }
        }
        return list;
    }

    /**
     * @param geoHash geo hash
     * @return the 8 south-east sub hashes of the geo hash
     */
    public static List<String> subHashesSouthEast(String geoHash){
        List<String> list = new ArrayList<>();
        for (char c : BASE32_CHARS){
            if(c >= 's'){
                list.add(geoHash + c);
            }
        }
        return list;
    }

    /**
     * @param lon1 longitude
     * @param lon2 longitude
     * @return true if l1 is west of l2
     */
    public static boolean isWest(double lon1, double lon2){
        double ll1 = lon1 + 180;
        double ll2 = lon2 + 180;
        if(ll1 < ll2 && (ll2 - ll1)<180){
            return true;
        }else{
            return (ll1>ll2 && (ll2 + 360 - ll1)<180);
        }
    }
    /**
     * @param lon1 longitude
     * @param lon2 longitude
     * @return true if l1 is east of l2
     */
    public static boolean isEast(double lon1, double lon2){
        double ll1 = lon1 + 180;
        double ll2 = lon2 + 180;
        if(ll1 > ll2 && (ll1 - ll2)<180){
            return true;
        }else{
            return (ll1 < ll2 && (ll1 + 360 -ll2)<180);
        }
    }

    /**
     * @param lat1 latitude
     * @param lat2 latitude
     * @return true if l1 is north of l2
     */
    public static boolean isNorth(double lat1, double lat2){
        return lat1 > lat2;
    }

    /**
     * @param lat1 latitude
     * @param lat2 latitude
     * @return true if l1 is south of l2
     */
    public static boolean isSouth(double lat1, double lat2){
        return lat1 < lat2;
    }

    public static Set<String> geoHashesPolygon(int hashLength, Coordinate... pts){
        Polygon geom = Geom.polygon(pts);
        return geoHashesPolygon(geom, hashLength);
    }

    public static Set<String> geoHashesPolygon(Polygon polygon, int hashLength){
        Envelope envelope = polygon.getEnvelopeInternal();
        Set<String> set = new HashSet<>();
        double southLat = envelope.getMinY();
        double northLat = envelope.getMaxY();
        double westLon = envelope.getMinX();
        double eastLon = envelope.getMaxX();
        String rowHash = encode(southLat,westLon, hashLength);
        double[] rowBox = decodeBbox(rowHash);
        while(rowBox[1] < northLat){
            String columnHash = rowHash;
            double[] columnBox = rowBox;
            while (isWest(columnBox[0], eastLon)){
                Geometry geoHashEvp = Geom.polygon(columnBox[0],columnBox[1],columnBox[2],columnBox[1],columnBox[2],columnBox[3],columnBox[0],columnBox[3],columnBox[0],columnBox[1]);
                if(polygon.intersects(geoHashEvp)){
                    set.add(columnHash);
                }
                // move column east
                columnHash = east(columnHash);
                columnBox = decodeBbox(columnHash);
            }
            // move row north
            rowHash = north(rowHash);
            rowBox = decodeBbox(rowHash);
        }
        return set;
    }
}
