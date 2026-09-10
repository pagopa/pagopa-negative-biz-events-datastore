package it.gov.pagopa.negativebizeventsdatastore.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtil {

  public static <T> T readModelFromFile(String relativePath, Class<T> clazz) throws IOException {
    ClassLoader classLoader = TestUtil.class.getClassLoader();
    File file = new File(Objects.requireNonNull(classLoader.getResource(relativePath)).getPath());
    var content = Files.readString(file.toPath());
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper.readValue(content, clazz);
  }

  public static <T, clazz> List<clazz> readListModelFromFile(String relativePath, Class<T> clazz) throws IOException {

	  ClassLoader classLoader = TestUtil.class.getClassLoader();

	  URL resource = Objects.requireNonNull(classLoader.getResource(relativePath),
			  "Test resource not found: " + relativePath);

	  Path resourcePath;
	  try {
		  resourcePath = Path.of(resource.toURI());
	  } catch (URISyntaxException e) {
		  throw new IOException("Unable to resolve test resource: " + relativePath, e);
	  }

	  var content = Files.readString(resourcePath);

	  ObjectMapper objectMapper = new ObjectMapper();
	  objectMapper.registerModule(new JavaTimeModule());

	  return objectMapper.readValue(content,
			  objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
  }

  /**
   * @param object to map into the Json string
   * @return object as Json string
   * @throws JsonProcessingException if there is an error during the parsing of the object
   */
  public String toJson(Object object) throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(object);
  }
}
