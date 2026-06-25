/* ── Config ── */
const API = 'http://localhost:8080/api/central';
const INTERVAL = 5000;

/* ── Clock ── */
function tick() {
  const d = new Date();
  document.getElementById('clock').textContent =
    d.toTimeString().slice(0, 8);
}
setInterval(tick, 1000); tick();

/* ── Helpers ── */
function sev(s) {
  if (!s || s === 'null') return '<span class="badge badge-white">—</span>';
  const m = { RED: 'badge-red', YELLOW: 'badge-yellow', GREEN: 'badge-green', WHITE: 'badge-white' };
  return `<span class="badge ${m[s?.toUpperCase()] || 'badge-grey'}">${s}</span>`;
}

function missionStateBadge(s) {
  const m = {
    Triaging: 'badge-triaging', Dispatched: 'badge-dispatched', OnScene: 'badge-onscene',
    Transporting: 'badge-transporting', Completed: 'badge-completed'
  };
  return `<span class="badge ${m[s] || 'badge-grey'}">${s || '—'}</span>`;
}

function vehicleStateBadge(s) {
  if (!s) return '<span class="badge badge-grey">—</span>';
  const sl = s.toLowerCase();
  if (sl === 'atrest') return `<span class="badge badge-atrest">Disponibile</span>`;
  if (sl.includes('moving') || sl.includes('returning')) return `<span class="badge badge-moving">${s}</span>`;
  if (sl.includes('maintenance') || sl.includes('sanitiz')) return `<span class="badge badge-maint">${s}</span>`;
  return `<span class="badge badge-busy">${s}</span>`;
}

function vehicleTypeLabel(t) {
  const m = { ambulance: 'Ambulanza', medcar: 'Automedica', medhelicopter: 'Elisoccorso' };
  return m[t] || t;
}

function fuelBar(f) {
  if (f === null || f === undefined) {
    return `<div class="fuel-bar-wrap">
      <div class="fuel-bar"><div class="fuel-fill ok" style="width:100%"></div></div>
      <div class="fuel-pct">—</div>
    </div>`;
  }
  const pct = Math.round(f * 100);
  const cls = pct > 40 ? 'ok' : pct > 20 ? 'warn' : 'crit';
  return `<div class="fuel-bar-wrap">
    <div class="fuel-bar"><div class="fuel-fill ${cls}" style="width:${pct}%"></div></div>
    <div class="fuel-pct">${pct}%</div>
  </div>`;
}

function boolBadge(v, warnOnTrue) {
  if (v === true) return warnOnTrue
    ? '<span class="badge badge-red">Sì</span>'
    : '<span class="badge badge-green">Sì</span>';
  return '<span class="badge badge-white">No</span>';
}

function nullish(v) {
  return (!v || v === 'null') ? '—' : v;
}

function fmt(v, decimals = 0) {
  if (v === undefined || v === null) return '—';
  return typeof v === 'number' ? v.toFixed(decimals) : v;
}

/* ── Fetch all ── */
async function fetchAll() {
  try {
    const [state, missions, vehicles, hospitals] = await Promise.all([
      fetch(`${API}/state`).then(r => r.json()),
      fetch(`${API}/missions`).then(r => r.json()),
      fetch(`${API}/vehicles`).then(r => r.json()),
      fetch(`${API}/hospitals`).then(r => r.json()),
    ]);

    renderState(state);
    renderMissions(missions);
    renderVehicles(vehicles);
    renderHospitals(hospitals);

    const now = new Date().toLocaleTimeString('it-IT');
    document.getElementById('status-bar').className = '';
    document.getElementById('status-bar').innerHTML =
      `<div class="status-dot"></div><span>Ultimo aggiornamento: ${now}</span>`;

  } catch (e) {
    document.getElementById('status-bar').className = 'error';
    document.getElementById('status-bar').innerHTML =
      `<div class="status-dot"></div><span>Errore di connessione: ${e.message} — server disponibile su localhost:8080?</span>`;
  }
}

/* ── Render state ── */
function renderState(s) {
  if (!s) return;

  // Active missions
  const am = s.activeMissionsCount ?? 0;
  document.getElementById('val-active-missions').textContent = am;
  document.getElementById('val-completed').textContent = `${s.missionsCompleted ?? 0} completate`;

  const kpiM = document.getElementById('kpi-missions');
  kpiM.className = am > 5 ? 'kpi-card alert' : 'kpi-card';

  // D09Z
  const d09z = s.avgD09zSeconds ?? 0;
  const d09zEl = document.getElementById('val-d09z');
  d09zEl.textContent = d09z > 0 ? fmt(d09z, 0) + ' s' : '—';
  d09zEl.className = 'kpi-value' + (d09z > 1080 ? ' red' : d09z > 0 ? ' green' : '');

  // Maintenance / fuel
  const maint = s.vehiclesNeedingMaintenance ?? 0;
  const fuel  = s.vehiclesLowFuel ?? 0;
  document.getElementById('val-maint').textContent = maint;
  document.getElementById('val-maint').className = 'kpi-value' + (maint > 0 ? ' red' : ' green');
  document.getElementById('val-fuel').textContent = `${fuel} carburante critico`;
  document.getElementById('kpi-maint').className = (maint > 0 || fuel > 0) ? 'kpi-card alert' : 'kpi-card';

  // Triage
  const total = s.triageTotalAssessed ?? 0;
  const over  = s.overTriageCount ?? 0;
  const under = s.underTriageCount ?? 0;
  const exact = total - over - under;
  document.getElementById('val-triage-total').textContent = total;
  document.getElementById('val-over').textContent = over;
  document.getElementById('val-under').textContent = under;

  document.getElementById('triage-badge').textContent = `${total} valutati`;
  document.getElementById('cnt-exact').textContent = exact;
  document.getElementById('cnt-over').textContent  = over;
  document.getElementById('cnt-under').textContent = under;

  if (total > 0) {
    document.getElementById('bar-exact').style.width = (exact / total * 100) + '%';
    document.getElementById('bar-over').style.width  = (over  / total * 100) + '%';
    document.getElementById('bar-under').style.width = (under / total * 100) + '%';
  }

  // Status
  document.getElementById('val-status').textContent = s.status || 'OPERATIONAL';

  // Saturation
  const sat = s.saturationScore ?? 0;
  const satPct = Math.round(sat * 100);
  document.getElementById('sat-fill').style.width = satPct + '%';
  document.getElementById('sat-value').textContent = satPct + '%  (' + am + ' missioni / flotta)';
}

/* ── Render missions ── */
function renderMissions(list) {
  const tbody = document.getElementById('missions-tbody');
  document.getElementById('missions-badge').textContent = list.length;

  if (!list.length) {
    tbody.innerHTML = '<tr class="empty-row"><td colspan="10">Nessuna missione attiva</td></tr>';
    return;
  }

  tbody.innerHTML = list.map(m => `
    <tr>
      <td class="mono">${m.missionId}</td>
      <td>${missionStateBadge(m.state)}</td>
      <td class="mono">${nullish(m.patientId)}</td>
      <td class="mono">${nullish(m.hospitalId)}</td>
      <td>${sev(m.severityCode)}</td>
      <td>${sev(m.confirmedSeverityCode)}</td>
      <td>${nullish(m.pathology)}</td>
      <td class="mono">${m.kpiD09zSeconds > 0 ? fmt(m.kpiD09zSeconds, 0) : '—'}</td>
      <td class="mono">${m.kpiTotalDurationSeconds > 0 ? fmt(m.kpiTotalDurationSeconds, 0) : '—'}</td>
      <td>${m.clinicalDeteriorated ? '<span class="badge badge-red">Sì</span>' : '<span class="badge badge-white">No</span>'}</td>
    </tr>
  `).join('');
}

/* ── Render vehicles ── */
function renderVehicles(list) {
  const tbody = document.getElementById('vehicles-tbody');
  document.getElementById('vehicles-badge').textContent = list.length;

  if (!list.length) {
    tbody.innerHTML = '<tr class="empty-row"><td colspan="9">Nessun veicolo registrato</td></tr>';
    return;
  }

  tbody.innerHTML = list.map(v => {
    const dest = nullish(v.hospitalId !== undefined ? v.hospitalId : v.homeBaseId);
    return `
    <tr>
      <td class="mono">${v.agentId}</td>
      <td>${vehicleTypeLabel(v.type)}</td>
      <td>${vehicleStateBadge(v.state)}</td>
      <td>${fuelBar(v.fuelLevel)}</td>
      <td class="mono">${v.missionsSinceMaintenance ?? '—'}</td>
      <td class="mono">${nullish(v.patientId)}</td>
      <td class="mono">${dest}</td>
      <td>${boolBadge(v.needsMaintenance, true)}</td>
      <td>${boolBadge(v.needsRefueling, true)}</td>
    </tr>
  `;
  }).join('');
}

/* ── Render hospitals ── */
function renderHospitals(list) {
  const grid = document.getElementById('hospitals-grid');
  document.getElementById('hospitals-badge').textContent = list.length;

  if (!list.length) {
    grid.innerHTML = '<div style="padding:24px;color:var(--ink-mute);font-style:italic">Nessun ospedale registrato</div>';
    return;
  }

  grid.innerHTML = list.map(h => `
    <div class="hosp-card">
      <div class="hosp-id">${h.hospitalId}</div>
      <div class="hosp-level">${h.assistanceLevel ?? '—'}</div>
      <div class="hosp-label">Livello assistenza</div>
      <div class="hosp-patients">Pazienti assistiti: <strong>${h.patientAssisted ?? 0}</strong></div>
      ${h.lat && h.lon ? `<div style="font-size:10px;color:var(--ink-mute);margin-top:4px;font-family:var(--mono)">${Number(h.lat).toFixed(4)}, ${Number(h.lon).toFixed(4)}</div>` : ''}
    </div>
  `).join('');
}

/* ── Bootstrap ── */
fetchAll();
setInterval(fetchAll, INTERVAL);