package org.energyweb.ddhub.helper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

@Provider
public class LocalDateTimeConverterProvider implements ParamConverterProvider {
  @Override
  public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType,
                                                        Annotation[] annotations) {
      if (rawType == LocalDateTime.class) {
          return (ParamConverter<T>) new LocalDateTimeConverter();
      }
      return null;
  }
}