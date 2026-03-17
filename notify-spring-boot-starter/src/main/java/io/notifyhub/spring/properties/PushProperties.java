package io.notifyhub.spring.properties;

public class PushProperties {
    private String projectId;
    private String serviceAccountJson;
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getServiceAccountJson() { return serviceAccountJson; }
    public void setServiceAccountJson(String serviceAccountJson) { this.serviceAccountJson = serviceAccountJson; }
}
