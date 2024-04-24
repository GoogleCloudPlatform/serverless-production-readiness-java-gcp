package com.mycompany.app;

import java.util.HashMap;
import java.util.Map;
import com.hashicorp.cdktf.providers.google.compute_network.ComputeNetwork;
import com.hashicorp.cdktf.providers.google.vpc_access_connector.VpcAccessConnector;

public class ApplicationConfig {
    private String environment;
    private String project;
    private String region;
    private String imageName;
    private ComputeNetwork serviceVpc;
    private VpcAccessConnector connector;
    private Map<String, String> memory = new HashMap<>();

    public ApplicationConfig(String environment, String project, String region) {
        this.environment = environment;
        this.project = project;
        this.region = region;
        // Default memory size for Cloud Run
        memory.put("memory", "2Gi");
    }

    public Map<String, String> getMemory() {
        return memory;
    }

    public void setMemory(Map<String, String> memory) {
        this.memory = memory;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getProject() {
        return project;
    }

    public String getRegion() {
        return region;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public ComputeNetwork getServiceVpc() {
        return serviceVpc;
    }

    public void setServiceVpc(ComputeNetwork serviceVpc) {
        this.serviceVpc = serviceVpc;
    }

    public VpcAccessConnector getConnector() {
        return connector;
    }

    public void setConnector(VpcAccessConnector connector) {
        this.connector = connector;
    }

    public String getImagePrefix() {
        return "gcr.io/" + project + "/";
    }
}
