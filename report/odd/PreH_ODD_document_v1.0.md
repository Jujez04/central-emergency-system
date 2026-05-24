# Overview
## 1. Purpose

Lo scopo principale del modello è simulare la catena di soccorso pre-ospedaliera per analizzare le tempistiche di intervento, l'efficienza nell'allocazione delle risorse e l'impatto di questi fattori sull'evoluzione clinica del Paziente vittima di trauma, dal momento della ricezione della chiamata fino all'arrivo in ospedale.

Gli obiettivi primari del modello sono:

- Analizzare la distribuzione dei tempi: **Response Time** (dalla chiamata all'arrivo sul posto) e **Total Pre-Hospital Time** (Dalla chiamata all'handover in PS).
- Valutare l'impatto clinico e geografico sul dispatching: come la Valutazione GCS (soglia critica $\le$ 8) e la distanza (> 10 km) guidano l'assegnazione dei mezzi.
- Confrontare l'efficienza operativa e il tasso di utilizzo (Utilization Rate) di Ambulanza ed Elisoccorso.
- **Ottimizzare l'allocazione delle risorse** tramite esperimenti di _Parameter Variation_ per individuare il numero ideale di mezzi necessari ad abbattere i tempi critici riducendo gli sprechi.

I *pattern* (criteri per valutare l'utilità e il realismo del modello) che ci si aspetta emergano dalla simulazione sono:
- L'aumento di risorse porta effettivamente alla diminuzione del tempo di risposta, passando da un problema di ottimizzazione risorse a uno fisico
- L'avvento di colli di bottiglia geografici all'aumentare della distanza, in particolare il *response* *time* sarà minore per segnalazioni vicine all'ospedale e maggiore per quelli più lontani.

## 2. Entities, State Variables, and Scales

Il modello è composto da quattro agenti principali, modellati tramite la logica Agent-Based:

| **Agente**                             | **Variabili di stato**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | **Note**                                                                                                                |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------- |
| **Agente di Contesto (Main)**          | param: `gcsThreshold`, `operativeAmbulances`, `operativeHelicopters` (int), `lambdaUrbanSignals`, `lambdaSignals`, `ambulanceSpeed`, `helicopterSpeed`, `distanceThreshold`, `takingPatientTimeUB`, `takingPatientTimeLB`, `timeHandoverHelicopter`, `timeHandoverAmbulance`, `patientReleasedRatio` (double), `signalOnlyUrbanEvent` (bool), var: `ambulancesCollection`, `medCarsCollection`, `hospitalsCollection`, `stationaryPointsCollection`, `urbanRegionCollection` (Collection), `medHelicopters`, `patients` (Population), `operator` (Agent), schedule: `dayRoutine`, event: `signalEvent`, `signalUrbanEvent`, `retriggerSignals`, dataset: `responseTimeData`, `totalPreHTimeData` | Simula l'ambiente geografico (GIS) e genera le chiamate d'emergenza, ma non prende decisioni.                           |
| **Ambulanza (Ambulance)**              | Statechart: `atRest`, `MovingToPatient`, `TakingPatient`, `WaitingForDoctor`, `MovingToHospital`, `Handover`, `Returning`. var: `isH24` (bool), `targetPatient` (Patient), `homeBase` (StationaryPoint), `targetHospital` (Hospital), function: `isInUrbanRegion`, event: `speedControl`                                                                                                                                                                                                                                                                                                                                                                                                         | Agente attivo con routing GIS su rete stradale reale. Incorpora la logica del Soccorritore.                             |
| **Elisoccorso (MedHelicopter)**        | Statechart: `atRest`, `MovingToPatient`, `TakingPatient`, `MovingToHospital`, `Handover`, `Returning`. var: `targetPatient` (Patient), `targetHospital` (Hospital), `homeBase` (StationaryPoint)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 | Agente attivo con routing _Straight-line_ point-to-point.                                                               |
| **Centrale Operativa (CEU)**           | function: `handleSignals`, `getNearestHospital`, `getNearestHospitalPosition`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Coordina il dispatching dei mezzi                                                                                       |
| **Automedica (MedCar)**                | Statechart: `atRest`, `MovingToPatient`, `TreatingPatient`, `Returning`. var: `homeBase` (StationaryPoint), `targetPatient` (Patient)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | Fornisce supporta all'ambulanza in caso di codice di massima gravità                                                    |
| **Punto Stazionari (StationaryPoint)** | coordinate                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Funge da punto di controllo per i mezzi di soccorso e hanno una posizione strategica per diminuire il tempo di risposta |
| **Ospedale (Hospital)**                | param: `assistanceLevel`(int), var: `patientAssisted` (int), event: `patientReleased`, coordinate                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Luogo di destinazione per i pazienti                                                                                    |
| **Paziente (Patient)**                 | Statechart: `Signaled`, `WaitingSupport`, `MovingToHospital`, `AtHospital`, var: `inHelicopter`, `medicalAssisted` (bool), `gcsScore` (int), `timeCalled`, `responseTime`, `totalTime` (double -> ora), coordinate                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | Agente passivo che viene utilizzato per cronometrare i tempi di risposta.                                               |

- **Scale:**
    - Temporale: Continua/Discreta, basata su eventi con timestamp in **secondi**.
    - Spaziale: Mappa GIS reale (provincia di Cesena) misurato in **metri**.

### 2.1 Statecharts
#### Ambulance
![[img/Pasted image 20260515150742.png]]
#### Patient
![[img/Pasted image 20260515163304.png]]
#### MedHelicopter
![[img/Pasted image 20260515150801.png]]

#### MedCar
![[img/Pasted image 20260515150827.png]]

## 3. Process Overview and Scheduling

1. **Signaling**: Generazione di una segnalazione in punto casuale tramite `Main` all'interno della zona operativa. Il `Patient` viene segnalato e passa allo stato `Signaled`.
2. **Dispatching**: `operator`, attraverso una policy relativa alla distanza, indica quale tipo di veicolo di soccorso viene inviato e il veicolo scelto passa da `atRest` allo stato `MovingToPatient`
3. **Caricamento**: una volta che il veicolo è arrivato passa allo stato `TakingPatient` e viene indicato un valore tra `takingPatientTimeLB` e `takingPatientTimeUB` che simula quanto tempo viene impiegato per trasportare il paziente all'interno del veicolo. Nel mentre viene inviato un messaggio dal veicolo al paziente che lo fa passare allo stato `MovingToHospital` e il `operator` indica qual è il `Hospital` più vicino.
	1. Se il paziente ha un `gcsScore` inferiore a `gcsThreshold` e la distanza è troppo elevata (> `distanceThreshold`) per intervenire con una `MedCar` viene chiamato anche il `MedHelicopter` per il supporto medico.
	2. Se non c'è nessun `MedHelicopter` disponibile allora la `Ambulance` sul luogo continua il suo flusso con una `MedCar` a supporto.
	3. Se il paziente è in buone condizioni (`gcsScore` > `gcsThreshold`) allora viene inviata un `Ambulance`
4. **Trasporto in ospedale**: il veicolo passa allo stato `MovingToHospital` una volta caricato il `Patient`.
5. **Handover**: All'arrivo del veicolo in ospedale avviene l'*Handover*, simulato tramite un timeout di pochi minuti, che dipendono dal numero di `Patient` presenti in `Hospital`. Se `Hospital` ha troppi pazienti, il veicolo dovrà aspettare un tempo proporzionale al numero di `Patient` presenti all'interno.
6. **Ritorno**: il veicolo ritorna al proprio deposito diventando disponibile per una nuova missione.

# Design Concepts

## 4.1 Basic Principles

Ogni entità fisica ha una "mente" autonoma, mentre il Main simula eventi reali(i.e. segnalazioni di un ferito in un determinato punto geografico). L'attivazione dei veicoli viene gestita attraverso l'invio di messaggi provenienti dalla funzione di decisione della Centrale Operativa.

## 4.2 Emergence

I fenomeni emergenti di interesse sono:
- Ritardi che non dipendono dal numero di mezzi, ma dalla lontananza del target all'aumentare dei mezzi di soccorso.
- Tempi di risposta maggiori rispetto ai tempi per soccorrere il paziente, anche questo dovuto alla distanza


## 4.3 Adaptation

Il `CEU` implementa una regola di dispatch:
- Se la distanza supera la soglia di vantaggio temporale (20 km) e il `Patient` è in uno stato critico (`gcsScore <= gcsThreshold`), si predilige l'attivazione immediata del `MedHelicopter`.


## 4.4 Interaction

Le interazioni tra agenti nel modello avvengono tramite logica _Event-Driven_ e _Message Passing_:
- Per le segnalazione esiste un evento ciclico con cadenza `lambdaSignals` che indica quanti pazienti si devono soccorrere ogni ora.
- Il `Main` invia un messaggio a `operator` passandole il target.
- Superata la policy di assegnazione, viene inviato al veicolo scelto un messaggio che lo attiva per soccorrere il paziente segnalato
- La Centrale Operativa (CEU) effettua un triage predittivo alla chiamata: se il `gcsScore` è inferiore alla soglia critica, attiva simultaneamente `Ambulance` e il supporto avanzato (`MedHelicopter` o `MedCar`) prima ancora dell'arrivo sul target.

## 4.5 Stochasticity

Le variabili stocastiche del modello:

| **Variabile**            | **Distribuzione**  | **Note**                                                                                                       |
| ------------------------ | ------------------ | -------------------------------------------------------------------------------------------------------------- |
| Coordinate Scenario      | Random             | Generazione casuale all'interno del poligono operativo GIS.                                                    |
| Generazione segnalazioni | Poisson            | Genera $\lambda$ segnalazioni ogni ora.                                                                        |
| Punteggio GCS            | Uniforme discreta  | Assegnato alla creazione del paziente.                                                                         |
| Tempo di Caricamento     | Condizionale       | 20 min se GCS $\le$ 8; 5 min altrimenti.                                                                       |
| Tempo Handover PS        | Equazione Dinamica | Calcolato in base al congestionamento del Pronto Soccorso (es. `timeHandover` + (`patientAssisted` * 10 min)). |
In questo caso sono state riportate solo quelle più significative.
# Details

## 5.1 Initialization

All'avvio della simulazione:
- Vengono selezionati i punti sulla mappa in cui piazzare l'ospedale, il deposito per le ambulanze e la stazione per gli elicotteri.
- Vengono configurati i parametri della simulazione. In caso di *Parameter Variation* viene anche selezionato quale parametro deve essere iterato. Per esempio allo stato attuale viene visualizzato un grafico dove al variare delle ambulanze viene visualizzato il tempo medio di risposta.

## 5.2 Submodels

Questa sezione descrive nel dettaglio la logica algoritmica e le equazioni che governano le decisioni degli agenti, permettendo la replicabilità del modello.

### Submodel 1: Generazione e Dispatching Geografico (Main)
![[img/Pasted image 20260515164305.png]]
Al momento della segnalazione, il `operator` (CEU) riceve una segnalazione da parte dell'evento `signalEvent` e/o `signalUrbanEvent` e utilizza una funzione `handleSignal` per implementare la regola di dispatching dei mezzi:
- Il blocco sorgente genera eventi seguendo una distribuzione di Poisson basata sul parametro `lambdaSignals` (segnalazioni/ora). Le coordinate del Paziente vengono generate con la funzione `randomPointInside()` vincolata a un raggio operativo attorno all'ospedale.
- `CEU` calcola la distanza tramite l'algoritmo interno GIS `distance = targetPatient.distanceTo(ospedale)`.
- *Policy distanza e di intervento primario*:
    - Se `distanza > 10000` (metri) e `gcsScore < gcsThreshold`: `CEU` cerca il primo `MedHelicopter` in stato `atRest` o in stato `Returning`. Se disponibile, invia il target e un `Ambulance` a supporto
    - Se il `gcsScore < gcsThreshold` allora chiama anche un `MedCar` a supporto e un `Ambulance`
    - Altrimenti ripiega sul primo `Ambulance` disponibile
- Se nessun veicolo è compatibile o disponibile, il `Patient` attende in stato `Signaled`. Il sistema gestisce una coda degli interventi in sospeso: non appena un veicolo termina una missione e torna in stato `atRest/Returning`, la `CEU` assegna a quel veicolo le segnalazioni in sospeso.

### Submodel 2: Operazioni sul posto (Veicoli)
![[img/Pasted image 20260515164338.png]]
Il parametro `gcsScore` guida anche le dinamiche di interazione tra i mezzi sul target. Se `Ambulance` arriva sul posto e il paziente risulta critico, si attiva la logica del soccorso congiunto:
- All'arrivo sul target, la transizione di `Ambulance` da `TakingPatient` a `MovingToHospital` è regolata da una logica di sincronizzazione basata sulla variabile `medicalAssisted` di `Patient`.
- Se è previsto l'arrivo di un mezzo avanzato (`MedCar` o `MedHelicopter`), l'Ambulanza transita nello stato di attesa `WaitingForDoctor`.
- Una volta giunto sul posto il mezzo avanzato, `MedCar` transita nello stato `TreatingPatient`, esegue le procedure di stabilizzazione e imposta la variabile `medicalAssisted = true`. Questa azione agisce come trigger, sbloccando `Ambulance` in attesa e permettendo di ripartire verso il `targetHospital`, mentre `MedCar` si disimpegna tornando in stato `Returning`.
- Se è un `MedHelicopter` avviene un rendez-vous tra quest'ultimo e `Ambulance` e `MedHelicopter` dovrà portare `Patient` a `targetHospital`, mentre `Ambulance` simula il supporto.
### Submodel 3: Esperimento di Ottimizzazione (Parameter Variation)
![[img/Pasted image 20260419085144.png]]
Strumento analitico per testare l'ipotesi della legge dei rendimenti decrescenti e trovare il punto di saturazione della flotta in cui i colli di bottiglia diventano puramente geografici/stradali.
- L'esperimento isola il modello dall'animazione visiva ed esegue iterazioni cicliche (es. durata fissa di $x$ giorni simulati per garantire consistenza statistica) disattivando le valutazioni in parallelo per non corrompere la scrittura dei grafici.
- Il parametro iterato è `operativeAmbulances` (es. da Min=2 a Max=10, Step=1). Al termine di ogni singola iterazione ("After simulation run"), l'esperimento estrae la media matematica globale dei tempi di risposta dal contenitore dati del Main e la inietta nel grafico dei risultati.
Per esempio notiamo nel grafico che nonostante l'aumento dei mezzi di soccorso il tempo medio di risposta rimane uguale.


