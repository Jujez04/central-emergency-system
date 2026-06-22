package it.ausl.emergency;

import it.ausl.emergency.adapter.physical.MissionPhysicalAdapter;
import it.ausl.emergency.payload.MissionTelemetryPayload;
import it.ausl.emergency.shadowing.MissionShadowingFunction;
import it.ausl.emergency.twin.MissionDigitalTwin;
import it.ausl.emergency.utils.MissionKeywords;
import it.wldt.core.engine.DigitalTwinEngine;
import it.wldt.core.state.DigitalTwinState;
import it.wldt.core.state.DigitalTwinStateProperty;
import it.wldt.exception.WldtEngineException;
import org.junit.jupiter.api.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 Integration Test Suite for MissionDigitalTwin and MissionShadowingFunction.
 * Simulates an end-to-end aggregated pre-hospital medical emergency mission roadmap,
 * validating transactional properties tracking, relationship indexing, and KPI augmentation.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MissionDigitalTwinTest {

    private static final String MISSION_ID = "M-991823";
    private static final long PROPAGATION_MS = 2_000L;
    private static final long BOOT_WAIT_MS = 3_000L;

    private DigitalTwinEngine engine;
    private MissionDigitalTwin missionTwin;
    private MissionPhysicalAdapter physicalAdapter;

    @BeforeEach
    public void setUp() throws Exception {
        MissionShadowingFunction shadowingFunction =
                new MissionShadowingFunction("mission-shadowing-" + MISSION_ID);

        // Core aggregate instance wrapper
        missionTwin = new MissionDigitalTwin("dt-mission-" + MISSION_ID, shadowingFunction);
        physicalAdapter = missionTwin.getPhysicalAdapter();

        engine = new DigitalTwinEngine();
        engine.addDigitalTwin(missionTwin);
        engine.startAll();

        System.out.println("\n[TEST-SETUP] MissionDigitalTwin engine boot initiated. Waiting for worker synchronization thread sync...");
        Thread.sleep(BOOT_WAIT_MS);
    }

    @AfterEach
    public void tearDown() {
        if (engine != null) {
            try {
                engine.stopAll();
            } catch (WldtEngineException e) {
                e.printStackTrace();
            }
            System.out.println("\n[TEST-TEARDOWN] Mission engine lifecycle environments successfully stopped.\n");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Aggregated Mission Lifecycle Workflow -> State transitions, semantic bounds, and analytical KPI updates")
    public void testAggregatedMissionLifecycleFlow() throws Exception {

        // ── STEP 1: Triaging initialization and implicit structure checks ──
        log("Step 1: Emergency Call Log Registered — Dispatch Triaging Phase");
        physicalAdapter.onMissionTelemetryReceived(new MissionTelemetryPayload(
                MissionKeywords.STATE_TRIAGING, "YELLOW", "WHITE", "NONE", "P-88716", "null", false, 200.0, 0.0, 0.0, 0.0
        ));

        Thread.sleep(PROPAGATION_MS);
        assertPropertyEquals(MissionKeywords.STATE_PROPERTY_KEY, MissionKeywords.STATE_TRIAGING);
        assertPropertyEquals(MissionKeywords.PATIENT_ID_PROPERTY_KEY, "P-88716");
        assertPropertyEquals(MissionKeywords.HOSPITAL_ID_PROPERTY_KEY, "null");
        assertPropertyEquals(MissionKeywords.KPI_D09Z_PROPERTY_KEY, 0.0);

        // ── STEP 2: Dispatched state and structural relationship instantiation ──
        log("Step 2: Fleet Unit Dispatched to Target Coordinates");
        physicalAdapter.onMissionTelemetryReceived(new MissionTelemetryPayload(
                MissionKeywords.STATE_DISPATCHED, "YELLOW", "WHITE", "NONE", "P-88716", "null", false, 200.0, 0.0, 0.0, 0.0
        ));

        Thread.sleep(PROPAGATION_MS);
        assertPropertyEquals(MissionKeywords.STATE_PROPERTY_KEY, MissionKeywords.STATE_DISPATCHED);

        // ── STEP 3: Arrival on Scene and evaluation of Alarm-to-Target KPI ──
        log("Step 3: Rescue Team Reached Scene — Processing KPI D09Z");
        physicalAdapter.onMissionTelemetryReceived(new MissionTelemetryPayload(
                MissionKeywords.STATE_ON_SCENE, "YELLOW", "YELLOW", "SEVERE_TRAUMA", "P-88716", "null", false, 200.0, 680.0, 0.0, 0.0
        ));

        Thread.sleep(PROPAGATION_MS);
        assertPropertyEquals(MissionKeywords.STATE_PROPERTY_KEY, MissionKeywords.STATE_ON_SCENE);
        assertPropertyEquals(MissionKeywords.CONFIRMED_SEVERITY_PROPERTY_KEY, "YELLOW");
        assertPropertyEquals(MissionKeywords.PATHOLOGY_PROPERTY_KEY, "SEVERE_TRAUMA");
        
        // Analytical Check: KPI D09Z = timeOnScene (680.0) - timeCalled (200.0) = 480.0 seconds
        assertPropertyEquals(MissionKeywords.KPI_D09Z_PROPERTY_KEY, 480.0);

        // ── STEP 4: Loading & Transport — Dynamic clinical deterioration checks ──
        log("Step 4: Critical Transport Inbound — Rerouting due to Clinical Deterioration");
        physicalAdapter.onMissionTelemetryReceived(new MissionTelemetryPayload(
                MissionKeywords.STATE_TRANSPORTING, "YELLOW", "RED", "SEVERE_TRAUMA", "P-88716", "hospitalCesena", true, 200.0, 680.0, 810.0, 0.0
        ));

        Thread.sleep(PROPAGATION_MS);
        assertPropertyEquals(MissionKeywords.STATE_PROPERTY_KEY, MissionKeywords.STATE_TRANSPORTING);
        assertPropertyEquals(MissionKeywords.HOSPITAL_ID_PROPERTY_KEY, "hospitalCesena");
        assertPropertyEquals(MissionKeywords.CLINICAL_DETERIORATED_PROPERTY_KEY, true);

        // ── STEP 5: Terminal Handover — Validation of the entire duration metric ──
        log("Step 5: Hospital Emergency Unit Handover Achieved — Evaluating Total Mission Duration");
        physicalAdapter.onMissionTelemetryReceived(new MissionTelemetryPayload(
                MissionKeywords.STATE_COMPLETED, "YELLOW", "RED", "SEVERE_TRAUMA", "P-88716", "hospitalCesena", true, 200.0, 680.0, 810.0, 2140.0
        ));

        Thread.sleep(PROPAGATION_MS);
        assertPropertyEquals(MissionKeywords.STATE_PROPERTY_KEY, MissionKeywords.STATE_COMPLETED);
        
        // Analytical Check: KPI Total Duration = timeHandover (2140.0) - timeCalled (200.0) = 1940.0 seconds
        assertPropertyEquals(MissionKeywords.KPI_TOTAL_DURATION_PROPERTY_KEY, 1940.0);

        System.out.println("\n[TEST] === Mission Aggregated Digital Twin timeline operations fully validated. ===\n");
    }

    /**
     * Type-safe utility to check assertions directly against the structural state transaction ledger.
     */
    private <T> void assertPropertyEquals(String propertyKey, T expectedValue) throws Exception {
        DigitalTwinState state = missionTwin
                .getShadowingFunction()
                .getDigitalTwinStateManager()
                .getDigitalTwinState();

        assertNotNull(state, "Active DigitalTwinState core runtime model object cannot be null.");

        Optional<DigitalTwinStateProperty<?>> optProp = state.getProperty(propertyKey);
        assertTrue(optProp.isPresent(), "Aggregated resource attribute matching key '" + propertyKey + "' was missing from twin map topology.");

        Object actualValue = optProp.get().getValue();
        assertEquals(expectedValue, actualValue, "Ledger property verification mismatch for key: '" + propertyKey + "'");

        System.out.println("[ASSERT ✓] Property boundary synchronized: " + propertyKey + " = " + actualValue);
    }

    private static void log(String phase) {
        System.out.println("\n[TEST-MISSION-PHASE] ─── " + phase.toUpperCase() + " ───\n");
    }
}