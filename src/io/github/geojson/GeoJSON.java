package io.github.geojson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Geometry;
import java.io.Reader;
import java.lang.reflect.Type;

/**
 * 数据的常用方法的工具类。
 * </pre>
 */
public class GeoJSON {
  /**
   * 默认的 {@code JSON} 日期/时间字段的格式化模式。
   */

  public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
  private static final Gson GSON = createGson(true);
  private static final Gson GSON_NO_NULLS = createGson(false);

  /**
   * Create the standard {@link com.google.gson.Gson} configuration
   *
   * @return created gson, never null
   */

  public static final Gson createGson() {
    return createGson(true);
  }

  /**
   * Create the standard {@link com.google.gson.Gson} configuration
   *
   * @param serializeNulls whether nulls should be serialized
   * @return created gson, never null
   */

  public static final Gson createGson(final boolean serializeNulls) {
    final GsonBuilder builder = new GsonBuilder();
    //设置Jts空间对象转换为GeoJSON格式的适配器
    builder.registerTypeHierarchyAdapter(Geometry.class, new GeometryAdapter());
    //设置JSON格式化的日期格式
    builder.setDateFormat(DEFAULT_DATE_PATTERN);
    if (serializeNulls) {
      builder.serializeNulls();
    }
    return builder.create();
  }

  /**
   * Get reusable pre-configured {@link com.google.gson.Gson} instance
   *
   * @return Gson instance
   */

  public static final Gson getGson() {
    return GSON;
  }

  /**
   * Get reusable pre-configured {@link com.google.gson.Gson} instance
   *
   * @return Gson instance
   */

  public static final Gson getGson(final boolean serializeNulls) {
    return serializeNulls ? GSON : GSON_NO_NULLS;
  }

  /**
   * 将给定的目标对象转换成 {@code JSON} 格式的字符串。<strong>此方法只用来转换普通的 {@code JavaBean} 对象。</strong> <ul> <li>该方法转换时使用默认的 日期/时间 格式化模式
   * - {@code yyyy-MM-dd HH:mm:ss SSS}；</li> </ul>
   *
   * @param object 要转换成 {@code JSON} 的目标对象。
   * @return 目标对象的 {@code JSON} 格式的字符串。
   */

  public static final String toJson(final Object object) {
    return toJson(object, true);
  }

  /**
   * 将给定的目标对象转换成 {@code JSON} 格式的字符串。<strong>此方法只用来转换普通的 {@code JavaBean} 对象。</strong>
   *
   * @param object 要转换成 {@code JSON} 的目标对象。
   * @return 目标对象的 {@code JSON} 格式的字符串。
   */

  public static final String toJson(final Object object, final boolean includeNulls) {
    return includeNulls ? GSON.toJson(object) : GSON_NO_NULLS.toJson(object);
  }

  /**
   * 将给定的目标对象转换成 {@code JSON} 格式的字符串。<strong>此方法只用来转换普通的 {@code JavaBean} 对象。</strong>
   *
   * @param target 要转换成 {@code JSON} 的目标对象。
   * @param datePattern 日期字段的格式化模式。
   * @return 目标对象的 {@code JSON} 格式的字符串。
   */

  public static String toJson(Object target, String datePattern) {
    final GsonBuilder builder = new GsonBuilder();
    if (datePattern == null || datePattern.length() <= 0) {
      datePattern = DEFAULT_DATE_PATTERN;
    }
    builder.setDateFormat(datePattern);
    return builder.create().toJson(target);
  }

  /**
   * 将给定的目标对象转换成 {@code JSON} 格式的字符串。<strong>此方法通常用来转换使用泛型的对象。</strong> <ul> <li>该方法转换时使用默认的 日期/时间 格式化模式 - {@code
   * yyyy-MM-dd HH:mm:ss SSSS}；</li> </ul>
   *
   * @param target 要转换成 {@code JSON} 的目标对象。
   * @param targetType 目标对象的类型。
   * @return 目标对象的 {@code JSON} 格式的字符串。
   * @since 1.0
   */

  public static String toJson(Object target, Type targetType) {
    return GSON_NO_NULLS.toJson(target, targetType);
  }

  /**
   * 将给定的 {@code JSON} 字符串转换成指定的类型对象。<strong>此方法通常用来转换普通的 {@code JavaBean} 对象。</strong>
   *
   * @param <V> 要转换的目标类型。
   * @param json 给定的 {@code JSON} 字符串。
   * @param type 要转换的目标类。
   * @return 给定的 {@code JSON} 字符串表示的指定的类型对象。
   */

  public static final <V> V parse(String json, Class<V> type) {
    return GSON.fromJson(json, type);
  }

  /**
   * 将给定的 {@code JSON} 字符串转换成指定的类型对象。
   *
   * @param <V> 要转换的目标类型。
   * @param json 给定的 {@code JSON} 字符串。
   * @param type {@code java.lang.reflect.Type} 的类型指示类对象。
   * @return 给定的 {@code JSON} 字符串表示的指定的类型对象。
   */

  public static final <V> V parse(String json, Type type) {
    return GSON.fromJson(json, type);
  }

  /**
   * 将给定的 {@code JSON} 字符串转换成指定的类型对象。<strong>此方法通常用来转换普通的 {@code JavaBean} 对象。</strong>
   *
   * @param <V> 要转换的目标类型。
   * @param reader 给定的 {@code JSON} reader对象。
   * @param type 要转换的目标类。
   * @return 给定的 {@code JSON} 字符串表示的指定的类型对象。
   */

  public static final <V> V parse(Reader reader, Class<V> type) {
    return GSON.fromJson(reader, type);
  }

  /**
   * 将给定的 {@code JSON} Reader对象转换成指定的类型对象。
   *
   * @param <V> 要转换的目标类型。
   * @param reader 给定的 {@code JSON} reader对象。
   * @param type {@code java.lang.reflect.Type} 的类型指示类对象。
   * @return 给定的 {@code JSON} 字符串表示的指定的类型对象。
   */

  public static final <V> V parse(Reader reader, Type type) {
    return GSON.fromJson(reader, type);
  }
}
