package gov.cms.model.rda.codegen.plugin.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ModelUtil {
  private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  public static List<MappingBean> loadMappingsFromYamlFile(String filename) throws IOException {
    return objectMapper.readValue(new File(filename), RootBean.class).getMappings();
  }
}
