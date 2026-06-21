package it.ausl.emergency.model.payload;

public record AmbulanceTelemetryPayload(
        String state,
        double lat,
        double lon,
        String patientId,
        String hospitalId,
        String homeBaseId,
        double fuelLevel,
        int    missionsSinceMaintenance,
        boolean needsRefueling,
        boolean needsMaintenance,
        double timestamp,
        double tripDistanceSinceEmergency
) {
 
    /** Restituisce true se patientId è valorizzato con un agentId reale. */
    public boolean hasPatient() {
        return patientId != null && !patientId.isBlank() && !"null".equalsIgnoreCase(patientId);
    }
 
    /** Restituisce true se hospitalId è valorizzato con un ospedale reale. */
    public boolean hasHospital() {
        return hospitalId != null && !hospitalId.isBlank() && !"null".equalsIgnoreCase(hospitalId);
    }
}
 