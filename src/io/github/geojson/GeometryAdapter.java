package io.github.geojson;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2015/10/11.
 */
public class GeometryAdapter implements JsonSerializer<Geometry>, JsonDeserializer<Geometry> {
  private final String NAME_GEOMETRIES = "geometries";
  private final String NAME_CRS = "crs";
  private final String NAME_PROPERTIES = "properties";
  private final String NAME_NAME = "name";
  private final String NAME_TYPE = "type";
  private final String NAME_POINT = "Point";
  private final String NAME_MULTI_POINT = "MultiPoint";
  private final String NAME_LINE_STRING = "LineString";
  private final String NAME_MULTI_LINE_STRING = "MultiLineString";
  private final String NAME_POLYGON = "Polygon";
  private final String NAME_MULTI_POLYGON = "MultiPolygon";
  private final String NAME_GEOMETRY_COLLECTION = "GeometryCollection";
  private final String EPSG_PREFIX = "EPSG:";
  private final String NAME_COORDINATES = "coordinates";
  private GeometryFactory geomFactory = new GeometryFactory();
  private double scale = Math.pow(10, 8);

  @Override public Geometry deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context)
      throws JsonParseException {
    return toJts(jsonElement);
  }

  @Override public JsonElement serialize(Geometry geometry, Type type, JsonSerializationContext context) {
    return context.serialize(formGeometry(geometry));
  }

  /**
   * JTS对象转换为map对象，进行json序列化
   */
  private Map<String, Object> formGeometry(Geometry geometry) {
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    result.put(NAME_TYPE, geometry.getGeometryType());
    switch (Geom.Type.from(geometry)){
      case POINT:
        result.put(NAME_COORDINATES, fromPoint((Point) geometry));
        break;
      case LINESTRING:
        result.put(NAME_COORDINATES, fromLineString((LineString) geometry));
        break;
      case POLYGON:
        result.put(NAME_COORDINATES, fromPolygon((Polygon) geometry));
        break;
      case MULTIPOINT:
        result.put(NAME_COORDINATES, fromMultiPoint((MultiPoint) geometry));
        break;
      case MULTILINESTRING:
        result.put(NAME_COORDINATES, fromMultiLineString((MultiLineString) geometry));
        break;
      case MULTIPOLYGON:
        result.put(NAME_COORDINATES, fromMulitPolygon((MultiPolygon) geometry));
        break;
      case GEOMETRYCOLLECTION:
        GeometryCollection geometryCollection = (GeometryCollection) geometry;
        List<Map<String, Object>> geometries = new ArrayList<Map<String, Object>>(geometryCollection.getNumGeometries());
        for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
          geometries.add(formGeometry(geometryCollection.getGeometryN(i)));
        }
        result.put(NAME_GEOMETRIES, geometries);
        break;
      default:
        throw new IllegalArgumentException("Unable to encode geometry " + geometry.getGeometryType());
    }
    /*if (encodeCRS) {
      result.put(GeoJsonConstants.NAME_CRS, createCRS(geometry.getSRID()));
    }*/
    return result;
  }

  /**
   * 转换jts点对象到geojson点对象
   */
  private double[] fromPoint(Point pt) {
    Coordinate coord = pt.getCoordinate();
    int size = !Double.isNaN(coord.z) ? 3 : 2;
    double[] coordinates = new double[size];
    coordinates[0] = formatOrdinate(coord.x);
    coordinates[1] = formatOrdinate(coord.y);
    if (!Double.isNaN(coord.z)) {
      coordinates[2] = formatOrdinate(coord.z);
    }
    return coordinates;
  }

  /**
   * 转换jts多点对象到geojson多点对象
   */
  private List<double[]> fromMultiPoint(MultiPoint mpt) {
    return toCoordinates(mpt.getCoordinates());
  }

  /**
   * 转换jts线对象到geojson线对象
   */
  private List<double[]> fromLineString(LineString lineString) {
    return toCoordinates(lineString.getCoordinates());
  }

  /**
   * 转换jts多线对象到geojson多线对象
   */
  private List<List<double[]>> fromMultiLineString(MultiLineString multiLineString) {
    int size = multiLineString.getNumGeometries();
    List<List<double[]>> list = new ArrayList<List<double[]>>(size);
    for (int i = 0; i < size; i++) {
      list.add(toCoordinates(multiLineString.getGeometryN(i).getCoordinates()));
    }
    return list;
  }

  /**
   * 转换jts面对像到geojson面对像
   */
  private List<List<double[]>> fromPolygon(Polygon polygon) {
    int size = polygon.getNumInteriorRing();
    List<List<double[]>> list = new ArrayList<List<double[]>>(size);
    list.add(fromLineString(polygon.getExteriorRing()));
    for (int i = 0; i < size; i++) {
      list.add(fromLineString(polygon.getExteriorRing()));
    }
    return list;
  }

  /**
   * 转换jts多面对象到geojson多面对象
   */
  private List<List<List<double[]>>> fromMulitPolygon(MultiPolygon multiPolygon) {
    int size = multiPolygon.getNumGeometries();
    List<List<List<double[]>>> list = new ArrayList<List<List<double[]>>>(size);
    for (int i = 0; i < size; i++) {
      list.add(fromPolygon((Polygon) multiPolygon.getGeometryN(i)));
    }
    return list;
  }

  /**
   * 把JTS的坐标对象转换为GeoJson坐标对象
   */
  private List<double[]> toCoordinates(Coordinate[] coords) {
    int size = coords.length;
    List<double[]> List = new ArrayList<double[]>(size);
    for (int i = 0; i < size; i++) {
      int len = !Double.isNaN(coords[i].z) ? 3 : 2;
      double[] coordinates = new double[len];
      coordinates[0] = formatOrdinate(coords[i].x);
      coordinates[1] = formatOrdinate(coords[i].y);
      if (!Double.isNaN(coords[i].z)) {
        coordinates[2] = formatOrdinate(coords[i].z);
      }
      List.add(coordinates);
    }
    return List;
  }

  /**
   * 把GeoJson对象转换为JTS对象
   */
  public Geometry toJts(JsonElement jsonElement) {
    String type = jsonElement.getAsJsonObject().get(NAME_TYPE).getAsString();
    JsonElement coordsElement = jsonElement.getAsJsonObject().get(NAME_COORDINATES);
    Geometry geom = null;
    // check type and convert
    if (NAME_POINT.equals(type)) {
      geom = toPoint(coordsElement);
    } else if (NAME_MULTI_POINT.equals(type)) {
      geom = toMultiPoint(coordsElement);
    } else if (NAME_LINE_STRING.equals(type)) {
      geom = toLineString(coordsElement);
    } else if (NAME_MULTI_LINE_STRING.equals(type)) {
      geom = toMultiLineString(coordsElement);
    } else if (NAME_POLYGON.equals(type)) {
      geom = toPolygon(coordsElement);
    } else if (NAME_MULTI_POLYGON.equals(type)) {
      geom = toMultiPolygon(coordsElement);
    } else if (NAME_GEOMETRY_COLLECTION.equals(type)) {
      JsonArray geometrys = jsonElement.getAsJsonObject().get(NAME_GEOMETRIES).getAsJsonArray();
      int size = geometrys.size();
      Geometry[] geometrieList = new Geometry[size];
      for (int i = 0; i < size; i++) {
        geometrieList[i] = toJts(geometrys.get(i));
      }
      geom = new GeometryCollection(geometrieList, geomFactory);
    } else {
      throw new IllegalArgumentException("Unsupported GeoGeometry type: " + jsonElement.getAsString());
    }
    return geom;
  }

  /**
   * 把GeoJson对象转换为JTS的Point对象
   */
  private Geometry toPoint(JsonElement jsonElement) {
    JsonArray pts = jsonElement.getAsJsonArray();
    if (pts.size() > 2) {
      return geomFactory.createPoint(
          new Coordinate(pts.get(0).getAsDouble(), pts.get(1).getAsDouble(), pts.get(2).getAsDouble()));
    } else {
      return geomFactory.createPoint(new Coordinate(pts.get(0).getAsDouble(), pts.get(1).getAsDouble()));
    }
  }

  /**
   * GSON的Element对象转换成JTS的MultiPoint对象
   * @param jsonElement
   * @return
   */
  private MultiPoint toMultiPoint(JsonElement jsonElement) {
    JsonArray array = jsonElement.getAsJsonArray();
    Coordinate[] coords = toCoordinates(array);
    return geomFactory.createMultiPoint(coords);
  }

  /**
   * GSON的Element对象转换成JTS的LIneString对象
   * @param jsonElement
   * @return
   */
  private LineString toLineString(JsonElement jsonElement) {
    JsonArray array = jsonElement.getAsJsonArray();
    Coordinate[] coords = toCoordinates(array);
    return geomFactory.createLineString(coords);
  }

  /**
   * GSON的Element对象转成JTS的MultiLineString对象
   * @param jsonElement
   * @return
   */
  private MultiLineString toMultiLineString(JsonElement jsonElement) {
    JsonArray array = jsonElement.getAsJsonArray();
    int size = array.size();
    LineString[] lineStrings = new LineString[size];
    for (int i = 0; i < size; i++) {
      lineStrings[i] = toLineString(array.get(i));
    }
    return geomFactory.createMultiLineString(lineStrings);
  }

  /**
   * GSON的Element对象转成JTS的Polygon对象
   * @param jsonElement
   * @return
   */
  private Polygon toPolygon(JsonElement jsonElement) {
    return toPolygon(jsonElement.getAsJsonArray());
  }

  /**
   * GSON的Element对象转成JTS MultiPolygon对象
   * @param jsonElement
   * @return
   */
  private MultiPolygon toMultiPolygon(JsonElement jsonElement) {
    JsonArray coordinates = jsonElement.getAsJsonArray();
    int size = coordinates.size();
    Polygon[] polygons = new Polygon[size];
    for (int i = 0; i < size; i++) {
      polygons[i] = toPolygon(coordinates.get(i).getAsJsonArray());
    }
    return geomFactory.createMultiPolygon(polygons);
  }

  /**
   * GSON的array对象转成JTS Polygon对象
   * @param coordinates
   * @return
   */
  private Polygon toPolygon(JsonArray coordinates) {
    int size = coordinates.size();
    LinearRing shell = geomFactory.createLinearRing(toCoordinates(coordinates.get(0).getAsJsonArray()));
    if (size > 1) {
      LinearRing[] holes = new LinearRing[size - 1];
      for (int i = 0; i < size - 1; i++) {
        holes[i] = geomFactory.createLinearRing(toCoordinates(coordinates.get(i + 1).getAsJsonArray()));
      }
      return geomFactory.createPolygon(shell, holes);
    } else {
      return geomFactory.createPolygon(shell);
    }
  }

  /**
   * 把GSON的array对象转换成坐标序列数组
   * @param coordinates
   * @return
   */
  private Coordinate[] toCoordinates(JsonArray coordinates) {
    int size = coordinates.size();
    Coordinate[] coords = new Coordinate[size];
    for (int i = 0; i < size; i++) {
      JsonArray pts = coordinates.get(i).getAsJsonArray();
      if (pts.size() > 2) {
        coords[i] = new Coordinate(pts.get(0).getAsDouble(), pts.get(1).getAsDouble(), pts.get(2).getAsDouble());
      } else {
        coords[i] = new Coordinate(pts.get(0).getAsDouble(), pts.get(1).getAsDouble());
      }
    }
    return coords;
  }

  /**
   * 转换过程中的数据进行小数点保留处理
   * @param x
   * @return
   */
  private double formatOrdinate(double x) {
    if (Math.abs(x) >= Math.pow(10, -3) && x < Math.pow(10, 7)) {
      x = Math.floor(x * scale + 0.5) / scale;
    }
    return x;
  }
}
