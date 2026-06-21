package it.ausl.emergency.twin;

import it.ausl.emergency.adapter.digital.AmbulanceDigitalAdapter;
import it.ausl.emergency.adapter.physical.AmbulancePhysicalAdapter;
import it.ausl.emergency.shadowing.AmbulanceShadowingFunction;
import it.wldt.core.engine.DigitalTwin;
import it.wldt.exception.EventBusException;
import it.wldt.exception.ModelException;
import it.wldt.exception.WldtConfigurationException;
import it.wldt.exception.WldtDigitalTwinStateException;
import it.wldt.exception.WldtRuntimeException;
import it.wldt.exception.WldtWorkerException;

/**
 * Digital Twin dell'Ambulanza.
 *
 * A differenza del PatientDigitalTwin, le istanze di AmbulanceDigitalTwin
 * vengono create una volta sola all'avvio del sistema (bootstrap), rispecchiando
 * il fatto che le ambulanze sono risorse permanenti della simulazione.
 *
 * Il ciclo di vita è quindi:
 *   1. AmbulanceTwinManager le crea tutte al ricevimento dei messaggi
 *      ces/registry con action=CREATED e type=ambulance.
 *   2. I messaggi ces/ambulance/{id}/state aggiornano lo stato della
 *      singola istanza per tutta la durata della simulazione.
 *   3. Non viene mai effettuato un cleanup: il twin rimane attivo finché
 *      il processo non viene terminato.
 */
public class AmbulanceDigitalTwin extends DigitalTwin {

    private final String id;
    private final AmbulancePhysicalAdapter physicalAdapter;
    private final AmbulanceDigitalAdapter  digitalAdapter;
    private final AmbulanceShadowingFunction shadowingFunction;

    public AmbulanceDigitalTwin(String digitalTwinId, AmbulanceShadowingFunction shadowingFunction)
            throws ModelException, EventBusException, WldtRuntimeException,
                   WldtWorkerException, WldtDigitalTwinStateException {

        super(digitalTwinId, shadowingFunction);
        this.id               = digitalTwinId;
        this.shadowingFunction = shadowingFunction;
        this.physicalAdapter  = new AmbulancePhysicalAdapter(id);
        this.digitalAdapter   = new AmbulanceDigitalAdapter(id);

        try {
            this.addPhysicalAdapter(physicalAdapter);
            this.addDigitalAdapter(digitalAdapter);
        } catch (WldtConfigurationException | WldtWorkerException e) {
            e.printStackTrace();
        }
    }

    public AmbulancePhysicalAdapter getPhysicalAdapter() {
        return physicalAdapter;
    }

    public AmbulanceDigitalAdapter getDigitalAdapter() {
        return digitalAdapter;
    }

    public AmbulanceShadowingFunction getShadowingFunction() {
        return shadowingFunction;
    }
}