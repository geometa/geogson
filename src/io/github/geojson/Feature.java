package io.github.geojson;

import com.vividsolutions.jts.geom.Geometry;
import java.util.Map;

public class Feature {
  private final String type = "Feature";
  private Map<String, Object> properties;
  private Geometry geometry;
  public String id;
  public long numericId;

  public Feature() {
  }

  public Feature(Geometry geometry) {
    this.geometry = geometry;
  }

  public Feature(String id,Geometry geometry) {
    this.id = id;
    this.geometry = geometry;
  }

  public Feature(String id, long numericId, Geometry geometry) {
    this.id = id;
    this.numericId = numericId;
    this.geometry = geometry;
  }

  public String getType() {
    return type;
  }

  public Geometry getGeometry() {
    return geometry;
  }

  public void setGeometry(Geometry geometry) {
    this.geometry = geometry;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public long getNumericId() {
    return numericId;
  }

  public void setNumericId(long numericId) {
    this.numericId = numericId;
  }
}
