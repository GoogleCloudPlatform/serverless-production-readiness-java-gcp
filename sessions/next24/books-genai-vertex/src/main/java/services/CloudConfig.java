package services;

import com.google.cloud.MetadataConfig;
import java.util.Arrays;
import java.util.List;

public class CloudConfig {
  static final String projectID = MetadataConfig.getProjectId();
  static final String zone = MetadataConfig.getZone();

  static final List<String> requiredFields = Arrays.asList("ce-id", "ce-source", "ce-type", "ce-specversion");
}
