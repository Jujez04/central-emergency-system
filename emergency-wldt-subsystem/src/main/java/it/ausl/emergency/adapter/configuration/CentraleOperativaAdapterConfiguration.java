package it.ausl.emergency.adapter.configuration;

public class CentraleOperativaAdapterConfiguration {

    private String initialStatus = "OPERATIONAL";
    private int defaultActiveMissions = 0;

    public CentraleOperativaAdapterConfiguration() {}

    public String getInitialStatus() {
        return initialStatus;
    }

    public void setInitialStatus(String initialStatus) {
        this.initialStatus = initialStatus;
    }

    public int getDefaultActiveMissions() {
        return defaultActiveMissions;
    }

    public void setDefaultActiveMissions(int defaultActiveMissions) {
        this.defaultActiveMissions = defaultActiveMissions;
    }
}