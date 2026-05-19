
# Ubiquitous Language v1.0

![[img/DS_v1.1.png]]
![[img/DS_v1.2.png]]

| Termine                      | Definizione                                                                                                                                                                                                               |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Centrale Operativa / CEU** | Unità operativa che abilita un Soccorritore e un Veicolo di Soccorso (Ambulanza o Elisoccorso) per dirigersi verso il luogo in cui prestare soccorso                                                                      |
| **Soccorritore**             | Unità in grado di operare nel Scenario di Soccorso. Può stabilire una Valutazione GCS sul Paziente e avviare la Missione in caso di Trauma                                                                                |
| **Valutazione** **GCS**      | Valutazione preventiva che viene fatta per valutare la situazione critica del Paziente. Superata la soglia critica si avvia la Missione                                                                                   |
| **Missione**                 | L'unità di lavoro che inizia quando il Paziente rientra nel caso di Trauma e termina con l'Handover                                                                                                                       |
| **Ambulanza**                | Veicolo di soccorso via terra dotato di un mezzo di comunicazione con il PS e tiene a bordo un Soccorritore (che non è quello che guida). Al momento non è dotato di dispositivi per tenere traccia dei parametri vitali. |
| **Automedica**               | Veicolo di soccorso in cui a bordo è presente un medico e un infermiere che forniscono supporto all'ambulanza in caso di codice di massima gravità.                                                                       |
| **Cinematica del trauma**    | Il che cosa ha provocato il Trauma, i.e. incidente stradale, incendio, ...                                                                                                                                                |
| **Segnalazione**             | Evento di innesco del sistema in cui sono presenti possibili casi di Trauma.                                                                                                                                              |
| **Scenario di Soccorso**     | Il luogo fisico in cui "è stato innescato" il Trauma nel Paziente                                                                                                                                                         |
| **Elisoccorso**              | Veicolo di soccorso aereo dotato di dispositivi per tenere traccia di Parametri Vitali rilevanti per l'altitudine                                                                                                         |
| **Parametri vitali PreH**    | Misure fatte sul paziente per tenere traccia di informazioni importanti sull'incolumità del Paziente                                                                                                                      |
| **Paziente**                 | Entità principale del dominio di cui si deve monitorare la sua situazione in caso di Trauma.                                                                                                                              |
| **Evento PreH**              | Qualsiasi evento registrato dall'inizio della chiamata del CEU fino all'Handover, i.e. arrivo mezzo di soccorso, avvio missione, ... (solitamente con un timestamp dell'evento)                                           |
| **Report PreH**              | Documento in cui vengono registrati tutti gli eventi rilevati e rilevanti sia da un punto di vista medico che legale                                                                                                      |
| **Handover**                 | Fase di intermezzo dalla fase extra-ospedaliera a quella intra-ospedaliera, in cui l'evento più importante è l'arrivo del Paziente al PS                                                                                  |
| **Pronto Soccorso**          | Luogo di destinazione finale della fase PreH                                                                                                                                                                              |

## Scenario principale

### Attivazione

Tutto il sistema viene attivato da una **Segnalazione**, definita come l'evento di innesco in cui sono presenti possibili casi di Trauma. Questa segnalazione viene recepita dalla **Centrale Operativa / CEU**, la quale raccoglie le prime informazioni per comprendere la **Cinematica del trauma** (ovvero il "che cosa" ha provocato il trauma, come un incidente stradale o un incendio). Sulla base di questi dati, la CEU abilita un **Soccorritore** e un veicolo di soccorso per dirigerli verso il luogo dell'emergenza. La destinazione finale comunicata ai mezzi è lo **Scenario di Soccorso**, ovvero il luogo fisico in cui è stato innescato il trauma nel Paziente.

### Valutazione

I veicoli inviati, che possono essere un'**Ambulanza** (veicolo via terra con un soccorritore a bordo non alla guida) o un **Elisoccorso** (veicolo aereo), raggiungono lo Scenario di Soccorso. Una volta arrivati, interviene il **Soccorritore**, l'unità in grado di operare in questo scenario. Il suo primo compito è stabilire una **Valutazione GCS** sul Paziente. Se il punteggio indica una situazione critica (superamento della soglia), si avvia ufficialmente la **Missione**.

### Tracciamento

Da questo momento in poi si tiene traccia del **Paziente** e tutta la missione ruota attorno a questa entità. Durante l'intero svolgimento della missione, ogni azione significativa (arrivo del mezzo, avvio missione, ecc.) viene registrata come un **Evento PreH**, dotato di un suo timestamp. Tutti gli Eventi PreH, che riguardano direttamente il Paziente e il veicolo di soccorso coinvolto, vengono raccolti e storicizzati in un **Report PreH**, un documento che ha valenza sia medica che legale e mostra un resoconto della fase di tracciamento.

### Consegna

La Missione termina con la fase di **Handover**. Questo è il momento di intermezzo che segna il passaggio dalla fase extra-ospedaliera a quella intra-ospedaliera, il cui evento scatenante è l'arrivo fisico del veicolo e del Paziente al **Pronto Soccorso**. Il Pronto Soccorso rappresenta il luogo di destinazione finale di tutta la fase pre-ospedaliera (PreH).

## Scenario secondario: Elisoccorso

### Attivazione

Se durante la segnalazione emergono dinamiche complesse da gestire in Ambulanza, viene mandato anche l'Elisoccorso. Nonostante l'invio del soccorso aereo, non viene mandata l'Automedica, in quanto l'Elisoccorso trasporta con sé un medico.

### Tracciamento

Se per il trasporto viene impiegato un Elisoccorso, verranno tenuti traccia dei **Parametri vitali PreH** (misure sull'incolumità del paziente rilevanti per l'altitudine) tramite appositi dispositivi a bordo.

## Scenario secondario: Automedica

### Attivazione

Nel momento in cui viene inviata una Segnalazione al CEU ed emergono casi di trauma grave che non richiedono l'invio dell'Elisoccorso, allora è possibile mandare l'**Automedica**.

### Valutazione

In questa fase occorre non solo la valutazione del Soccorritore ma anche del medico dell'Automedica, di conseguenza bisogna aspettare che entrambi arrivino sul posto.
