/* e-Adalat web client — vanilla JS. All API calls go through the gateway. */
'use strict';

const API = 'http://localhost:8080';
const WS_URL = 'http://localhost:8080/ws-hearing';

/* ---------- helpers ---------- */
function esc(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;')
    .replace(/>/g, '&gt;').replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function toast(msg) {
  const el = document.getElementById('toast');
  if (!el) return;
  el.textContent = msg;
  el.classList.remove('hidden');
  clearTimeout(toast._t);
  toast._t = setTimeout(() => el.classList.add('hidden'), 3000);
}

function getToken() { return localStorage.getItem('eadalat_token'); }
function getUser() {
  try { return JSON.parse(localStorage.getItem('eadalat_user') || 'null'); }
  catch (e) { return null; }
}
function isLoggedIn() { return !!getToken(); }

function authHeaders(extra) {
  const h = Object.assign({}, extra);
  const t = getToken();
  if (t) h['Authorization'] = 'Bearer ' + t;
  return h;
}

/** JSON API helper. Throws on non-2xx; logs out on 401. */
async function api(path, method, body) {
  method = method || 'GET';
  const res = await fetch(API + path, {
    method: method,
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: body == null ? undefined : JSON.stringify(body)
  });
  if (res.status === 401) { logout(); throw new Error('Session expired — please log in again.'); }
  if (!res.ok) {
    let msg = '';
    try { msg = await res.text(); } catch (e) { /* ignore */ }
    throw new Error(msg || ('Request failed: HTTP ' + res.status));
  }
  if (res.status === 204) return null;
  const ct = res.headers.get('content-type') || '';
  return ct.indexOf('application/json') >= 0 ? res.json() : res.text();
}

/** Multipart upload helper (documents). */
async function apiUpload(path, formData) {
  const res = await fetch(API + path, {
    method: 'POST',
    headers: authHeaders(),
    body: formData
  });
  if (res.status === 401) { logout(); throw new Error('Session expired — please log in again.'); }
  if (!res.ok) {
    let msg = '';
    try { msg = await res.text(); } catch (e) { /* ignore */ }
    throw new Error(msg || ('Upload failed: HTTP ' + res.status));
  }
  const ct = res.headers.get('content-type') || '';
  return ct.indexOf('application/json') >= 0 ? res.json() : res.text();
}

function logout() {
  leaveRoom();
  localStorage.removeItem('eadalat_token');
  localStorage.removeItem('eadalat_user');
  location.hash = '#/login';
}

/* ---------- routing ---------- */
const VIEWS = ['view-auth', 'view-dashboard', 'view-filecase', 'view-case', 'view-hearing'];

function showView(id) {
  VIEWS.forEach(v => {
    const el = document.getElementById(v);
    if (el) el.classList.toggle('hidden', v !== id);
  });
  const topbar = document.getElementById('topbar');
  if (topbar) topbar.classList.toggle('hidden', id === 'view-auth');
}

function route() {
  const hash = location.hash || '#/login';
  const parts = hash.replace(/^#\//, '').split('/');
  leaveRoom(); // leave any active room on navigation

  if (!isLoggedIn()) { showView('view-auth'); return; }
  renderUserChip();

  if (parts[0] === 'cases' && parts.length === 1) { showView('view-dashboard'); loadCases(); }
  else if (parts[0] === 'cases' && parts[1] === 'new') { showView('view-filecase'); }
  else if (parts[0] === 'cases' && parts[1]) { showView('view-case'); loadCaseDetail(parts[1]); }
  else if (parts[0] === 'hearing' && parts[1]) { showView('view-hearing'); joinRoom(parts[1]); }
  else { location.hash = '#/cases'; }
}

window.addEventListener('hashchange', route);

/* ---------- auth ---------- */
function renderUserChip() {
  const chip = document.getElementById('user-chip');
  const u = getUser();
  if (chip) chip.textContent = u && (u.name || u.email) ? (u.name || u.email) : '';
}

function showAuthError(msg) {
  const el = document.getElementById('auth-error');
  if (!el) return;
  el.textContent = msg;
  el.classList.toggle('hidden', !msg);
}

function bindAuth() {
  const tabLogin = document.getElementById('tab-login');
  const tabRegister = document.getElementById('tab-register');
  const loginForm = document.getElementById('login-form');
  const registerForm = document.getElementById('register-form');

  if (tabLogin && tabRegister) {
    tabLogin.addEventListener('click', () => {
      tabLogin.classList.add('active'); tabRegister.classList.remove('active');
      loginForm.classList.remove('hidden'); registerForm.classList.add('hidden');
    });
    tabRegister.addEventListener('click', () => {
      tabRegister.classList.add('active'); tabLogin.classList.remove('active');
      registerForm.classList.remove('hidden'); loginForm.classList.add('hidden');
    });
  }

  if (loginForm) loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    showAuthError('');
    try {
      const data = await api('/api/auth/login', 'POST', {
        email: document.getElementById('login-email').value.trim(),
        password: document.getElementById('login-password').value
      });
      saveSession(data);
      location.hash = '#/cases';
    } catch (err) { showAuthError(err.message); }
  });

  if (registerForm) registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    showAuthError('');
    try {
      const data = await api('/api/auth/register', 'POST', {
        name: document.getElementById('reg-name').value.trim(),
        email: document.getElementById('reg-email').value.trim(),
        password: document.getElementById('reg-password').value,
        role: document.getElementById('reg-role').value
      });
      saveSession(data);
      location.hash = '#/cases';
    } catch (err) { showAuthError(err.message); }
  });

  const logoutBtn = document.getElementById('logout-btn');
  if (logoutBtn) logoutBtn.addEventListener('click', logout);
}

function saveSession(data) {
  if (!data || !data.token) throw new Error('Invalid auth response from server.');
  localStorage.setItem('eadalat_token', data.token);
  if (data.user) localStorage.setItem('eadalat_user', JSON.stringify(data.user));
}

/* ---------- dashboard ---------- */
async function loadCases() {
  const list = document.getElementById('case-list');
  if (!list) return;
  list.innerHTML = '<p class="muted">Loading…</p>';
  try {
    const cases = await api('/api/cases');
    if (!Array.isArray(cases) || cases.length === 0) {
      list.innerHTML = '<p class="muted">No cases yet. <a href="#/cases/new">File your first case</a>.</p>';
      return;
    }
    list.innerHTML = cases.map(c => (
      '<div class="card">' +
        '<h3><a class="case-link" href="#/cases/' + esc(c.id) + '">' + esc(c.title || 'Untitled case') + '</a></h3>' +
        (c.status ? '<span class="pill">' + esc(c.status) + '</span>' : '') +
        (c.category ? '<p>' + esc(c.category) + '</p>' : '') +
        (c.createdAt ? '<p class="small">' + esc(String(c.createdAt).slice(0, 10)) + '</p>' : '') +
      '</div>'
    )).join('');
  } catch (err) {
    list.innerHTML = '<p class="error">' + esc(err.message) + '</p>';
  }
}

/* ---------- file a case ---------- */
function bindFileCase() {
  const form = document.getElementById('filecase-form');
  if (!form) return;
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const errEl = document.getElementById('filecase-error');
    if (errEl) errEl.classList.add('hidden');
    try {
      const created = await api('/api/cases', 'POST', {
        title: document.getElementById('fc-title').value.trim(),
        category: document.getElementById('fc-category').value.trim(),
        description: document.getElementById('fc-description').value.trim()
      });
      toast('Case filed successfully.');
      form.reset();
      location.hash = created && created.id ? '#/cases/' + created.id : '#/cases';
    } catch (err) {
      if (errEl) { errEl.textContent = err.message; errEl.classList.remove('hidden'); }
    }
  });
}

/* ---------- case detail ---------- */
async function loadCaseDetail(id) {
  const root = document.getElementById('case-detail');
  if (!root) return;
  root.innerHTML = '<p class="muted">Loading case…</p>';
  let c;
  try {
    c = await api('/api/cases/' + encodeURIComponent(id));
  } catch (err) {
    root.innerHTML = '<p class="error">' + esc(err.message) + '</p>';
    return;
  }
  const u = getUser() || {};
  const role = String(u.role || '').toUpperCase();
  const canSchedule = role === 'ADMIN' || role === 'JUDGE';

  root.innerHTML =
    '<div class="detail-grid">' +
      '<div class="card"><h3>' + esc(c.title || 'Untitled case') + '</h3>' +
        (c.status ? '<span class="pill">' + esc(c.status) + '</span> ' : '') +
        (c.category ? '<span class="pill">' + esc(c.category) + '</span>' : '') +
        '<p>' + esc(c.description || '') + '</p>' +
        (c.createdAt ? '<p class="small muted">Filed: ' + esc(String(c.createdAt).slice(0, 10)) + '</p>' : '') +
      '</div>' +
      '<div class="card"><h3>🤖 AI Summary</h3><div class="summary-box" id="case-summary"><span class="muted">Loading summary…</span></div></div>' +
      '<div class="card"><h3>📄 Documents</h3><div id="doc-list"><span class="muted">Loading…</span></div>' +
        '<form id="doc-upload" class="stack" style="margin-top:0.8rem">' +
          '<input type="file" id="doc-file" required />' +
          '<button class="btn btn-primary btn-sm" type="submit">Upload</button>' +
        '</form></div>' +
      '<div class="card"><h3>📅 Hearings</h3><div id="hearing-list"><span class="muted">Loading…</span></div>' +
        (canSchedule
          ? '<form id="schedule-form" class="stack" style="margin-top:0.8rem">' +
              '<input type="datetime-local" id="sch-at" required />' +
              '<input type="text" id="sch-title" placeholder="Hearing title (optional)" />' +
              '<button class="btn btn-primary btn-sm" type="submit">Schedule Hearing</button>' +
            '</form>'
          : '<p class="muted small">Only judges/admins can schedule hearings.</p>') +
      '</div>' +
    '</div>';

  // AI summary
  api('/api/cases/' + encodeURIComponent(id) + '/summary').then(s => {
    const box = document.getElementById('case-summary');
    if (!box) return;
    box.textContent = (s && (s.summary || s.text)) || (typeof s === 'string' ? s : 'No summary available.');
  }).catch(() => {
    const box = document.getElementById('case-summary');
    if (box) box.innerHTML = '<span class="muted">Summary unavailable.</span>';
  });

  loadDocuments(id);
  loadHearings(id);

  const up = document.getElementById('doc-upload');
  if (up) up.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fileInput = document.getElementById('doc-file');
    const file = fileInput && fileInput.files && fileInput.files[0];
    if (!file) return;
    const fd = new FormData();
    fd.append('caseId', id);
    fd.append('file', file);
    try {
      await apiUpload('/api/documents', fd);
      toast('Document uploaded.');
      up.reset();
      loadDocuments(id);
    } catch (err) { toast(err.message); }
  });

  const sf = document.getElementById('schedule-form');
  if (sf) sf.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await api('/api/hearings', 'POST', {
        caseId: id,
        scheduledAt: document.getElementById('sch-at').value,
        title: document.getElementById('sch-title').value.trim()
      });
      toast('Hearing scheduled.');
      sf.reset();
      loadHearings(id);
    } catch (err) { toast(err.message); }
  });
}

async function loadDocuments(caseId) {
  const el = document.getElementById('doc-list');
  if (!el) return;
  try {
    const docs = await api('/api/documents?caseId=' + encodeURIComponent(caseId));
    if (!Array.isArray(docs) || docs.length === 0) {
      el.innerHTML = '<p class="muted">No documents yet.</p>';
      return;
    }
    el.innerHTML = docs.map(d => {
      const dl = d.downloadUrl || (API + '/api/documents/' + encodeURIComponent(d.id) + '/download');
      return '<div class="doc-row"><span>📄 ' + esc(d.fileName || d.name || 'document') + '</span>' +
        '<a class="btn btn-ghost btn-sm" style="color:var(--primary)" href="' + esc(dl) + '" target="_blank" rel="noopener">Download</a></div>';
    }).join('');
  } catch (err) {
    el.innerHTML = '<p class="error">' + esc(err.message) + '</p>';
  }
}

async function loadHearings(caseId) {
  const el = document.getElementById('hearing-list');
  if (!el) return;
  try {
    const hs = await api('/api/hearings?caseId=' + encodeURIComponent(caseId));
    if (!Array.isArray(hs) || hs.length === 0) {
      el.innerHTML = '<p class="muted">No hearings scheduled yet.</p>';
      return;
    }
    el.innerHTML = hs.map(h => {
      const join = h.roomId
        ? ' <a class="btn btn-primary btn-sm" href="#/hearing/' + esc(h.roomId) + '">Join Room</a>'
        : '';
      return '<div class="hearing-row"><span>🕒 ' + esc(h.scheduledAt || h.date || '') +
        (h.title ? ' — ' + esc(h.title) : '') +
        (h.status ? ' <span class="pill">' + esc(h.status) + '</span>' : '') + '</span>' + join + '</div>';
    }).join('');
  } catch (err) {
    el.innerHTML = '<p class="error">' + esc(err.message) + '</p>';
  }
}

/* ---------- notifications ---------- */
async function refreshNotifBadge() {
  const badge = document.getElementById('notif-count');
  if (!badge || !isLoggedIn()) return;
  try {
    const ns = await api('/api/notifications?unreadOnly=true');
    const count = Array.isArray(ns) ? ns.length : 0;
    badge.textContent = count;
    badge.classList.toggle('hidden', count === 0);
  } catch (e) { /* non-fatal */ }
}

async function loadNotifications() {
  const list = document.getElementById('notif-list');
  if (!list) return;
  list.innerHTML = '<p class="muted">Loading…</p>';
  try {
    const ns = await api('/api/notifications?unreadOnly=true');
    if (!Array.isArray(ns) || ns.length === 0) {
      list.innerHTML = '<p class="muted">No unread notifications.</p>';
      return;
    }
    list.innerHTML = ns.map(n =>
      '<div class="notif-row"><span>' + esc(n.message || n.title || 'Notification') + '</span>' +
      '<button class="btn btn-sm" data-read="' + esc(n.id) + '">Mark read</button></div>'
    ).join('');
    list.querySelectorAll('[data-read]').forEach(btn => {
      btn.addEventListener('click', async () => {
        try {
          await api('/api/notifications/' + encodeURIComponent(btn.getAttribute('data-read')) + '/read', 'POST');
          loadNotifications();
          refreshNotifBadge();
        } catch (err) { toast(err.message); }
      });
    });
  } catch (err) {
    list.innerHTML = '<p class="error">' + esc(err.message) + '</p>';
  }
}

function bindNotifications() {
  const btn = document.getElementById('notif-btn');
  const panel = document.getElementById('notif-panel');
  const close = document.getElementById('notif-close');
  if (btn && panel) btn.addEventListener('click', () => {
    panel.classList.toggle('hidden');
    if (!panel.classList.contains('hidden')) loadNotifications();
  });
  if (close && panel) close.addEventListener('click', () => panel.classList.add('hidden'));
}

/* ---------- hearing room (WebRTC + STOMP/SockJS signaling) ---------- */
let pc = null, stompClient = null, localStream = null, currentRoom = null;
const myId = 'peer-' + Math.random().toString(36).slice(2, 10);

function setRoomStatus(msg) {
  const el = document.getElementById('room-status');
  if (el) el.textContent = msg;
}

function signal(payload) {
  if (stompClient && stompClient.active) {
    stompClient.publish({
      destination: '/app/signal/' + currentRoom,
      body: JSON.stringify(Object.assign({ from: myId }, payload))
    });
  }
}

async function handleSignal(frame) {
  let msg;
  try { msg = JSON.parse(frame.body); } catch (e) { return; }
  if (!msg || msg.from === myId || !pc) return;
  try {
    if (msg.type === 'JOIN') {
      // A new peer joined — we initiate the offer.
      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);
      signal({ type: 'OFFER', sdp: offer.sdp });
    } else if (msg.type === 'OFFER' && msg.sdp) {
      await pc.setRemoteDescription({ type: 'offer', sdp: msg.sdp });
      const answer = await pc.createAnswer();
      await pc.setLocalDescription(answer);
      signal({ type: 'ANSWER', sdp: answer.sdp });
    } else if (msg.type === 'ANSWER' && msg.sdp) {
      await pc.setRemoteDescription({ type: 'answer', sdp: msg.sdp });
    } else if (msg.type === 'ICE' && msg.candidate) {
      const cand = typeof msg.candidate === 'string' ? JSON.parse(msg.candidate) : msg.candidate;
      await pc.addIceCandidate(new RTCIceCandidate(cand));
    } else if (msg.type === 'LEAVE') {
      setRoomStatus('The other participant left the room.');
      const rv = document.getElementById('remote-video');
      if (rv) rv.srcObject = null;
    }
  } catch (err) { setRoomStatus('Signaling error: ' + err.message); }
}

function joinRoom(roomId) {
  leaveRoom(); // clean slate
  currentRoom = roomId;
  const label = document.getElementById('room-label');
  if (label) label.textContent = 'Room: ' + roomId;
  setRoomStatus('Getting camera/mic…');

  if (typeof SockJS === 'undefined' || typeof StompJs === 'undefined') {
    setRoomStatus('Signaling libraries failed to load (CDN unavailable).');
    return;
  }

  pc = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] });

  pc.onicecandidate = (e) => {
    if (e.candidate) signal({ type: 'ICE', candidate: JSON.stringify(e.candidate.toJSON()) });
  };
  pc.ontrack = (e) => {
    const rv = document.getElementById('remote-video');
    if (rv && e.streams && e.streams[0]) rv.srcObject = e.streams[0];
    setRoomStatus('Connected — hearing in progress.');
  };
  pc.onconnectionstatechange = () => {
    if (pc && (pc.connectionState === 'failed' || pc.connectionState === 'disconnected')) {
      setRoomStatus('Connection ' + pc.connectionState + '.');
    }
  };

  const gotMedia = navigator.mediaDevices && navigator.mediaDevices.getUserMedia
    ? navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    : Promise.reject(new Error('Camera/mic not available.'));

  gotMedia.then((stream) => {
    localStream = stream;
    const lv = document.getElementById('local-video');
    if (lv) lv.srcObject = stream;
    stream.getTracks().forEach(t => pc && pc.addTrack(t, stream));
  }).catch((err) => {
    setRoomStatus('Media unavailable: ' + err.message + ' (joining listen-only).');
  }).finally(connectSignaling);

  function connectSignaling() {
    setRoomStatus('Connecting to signaling…');
    const sock = new SockJS(WS_URL);
    stompClient = StompJs.Stomp.over(sock);
    stompClient.connect(
      { Authorization: 'Bearer ' + getToken() },
      () => {
        stompClient.subscribe('/topic/room/' + currentRoom, handleSignal);
        signal({ type: 'JOIN' });
        setRoomStatus('In room — waiting for the other participant…');
      },
      (err) => setRoomStatus('Signaling failed: ' + (err && err.message ? err.message : err))
    );
  }
}

function leaveRoom() {
  if (currentRoom && stompClient && stompClient.active) {
    try { signal({ type: 'LEAVE' }); } catch (e) { /* ignore */ }
  }
  if (stompClient) { try { stompClient.deactivate(); } catch (e) { /* ignore */ } stompClient = null; }
  if (pc) { try { pc.close(); } catch (e) { /* ignore */ } pc = null; }
  if (localStream) { localStream.getTracks().forEach(t => { try { t.stop(); } catch (e) { /* ignore */ } }); localStream = null; }
  const lv = document.getElementById('local-video');
  const rv = document.getElementById('remote-video');
  if (lv) lv.srcObject = null;
  if (rv) rv.srcObject = null;
  currentRoom = null;
}

function bindHearingRoom() {
  const btn = document.getElementById('leave-room-btn');
  if (btn) btn.addEventListener('click', () => { leaveRoom(); location.hash = '#/cases'; });
}

/* ---------- boot ---------- */
document.addEventListener('DOMContentLoaded', () => {
  bindAuth();
  bindFileCase();
  bindNotifications();
  bindHearingRoom();
  if (!isLoggedIn()) location.hash = '#/login';
  route();
  setInterval(refreshNotifBadge, 30000);
  refreshNotifBadge();
});
