const dropzone = document.getElementById('dropzone');
const fileInput = document.getElementById('fileInput');
const uploadBtn = document.getElementById('uploadBtn');
const uploadStatus = document.getElementById('uploadStatus');

const docIdInput = document.getElementById('docIdInput');
const getBtn = document.getElementById('getBtn');
const getResult = document.getElementById('getResult');

const recentTable = document.getElementById('recentTable').querySelector('tbody');
const refreshBtn = document.getElementById('refreshBtn');
const limitSel = document.getElementById('limitSel');

const toast = document.getElementById('toast');

let selectedFile = null;

function showToast(message, ok = true) {
    toast.textContent = message;
    toast.className = 'toast ' + (ok ? 'ok' : 'err');
    toast.style.opacity = '1';
    setTimeout(() => (toast.style.opacity = '0'), 2500);
}

function bytesToHuman(n) {
    if (n < 1024) return n + ' B';
    const units = ['KB', 'MB', 'GB', 'TB'];
    let u = -1;
    do {
        n = n / 1024;
        ++u;
    } while (n >= 1024 && u < units.length - 1);
    return n.toFixed(1) + ' ' + units[u];
}

function setUploading(isUploading) {
    uploadBtn.disabled = !selectedFile || isUploading;
    dropzone.classList.toggle('busy', isUploading);
    uploadStatus.textContent = isUploading ? 'Uploading…' : '';
}

dropzone.addEventListener('click', () => fileInput.click());
dropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropzone.classList.add('hover');
});
dropzone.addEventListener('dragleave', () => dropzone.classList.remove('hover'));
dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('hover');
    if (e.dataTransfer.files.length > 0) {
        selectedFile = e.dataTransfer.files[0];
        uploadBtn.disabled = false;
        dropzone.querySelector('p').textContent = `Ready: ${selectedFile.name} (${bytesToHuman(selectedFile.size)})`;
    }
});

fileInput.addEventListener('change', () => {
    if (fileInput.files.length > 0) {
        selectedFile = fileInput.files[0];
        uploadBtn.disabled = false;
        dropzone.querySelector('p').textContent = `Ready: ${selectedFile.name} (${bytesToHuman(selectedFile.size)})`;
    }
});

uploadBtn.addEventListener('click', async () => {
    if (!selectedFile) return;
    setUploading(true);
    try {
        const form = new FormData();
        form.append('file', selectedFile);
        const res = await fetch('/api/documents', { method: 'POST', body: form });
        if (!res.ok) {
            const msg = await res.text();
            throw new Error(`Upload failed: ${msg || res.status}`);
        }
        const json = await res.json();
        showToast(`Uploaded #${json.id} ✔`);
        docIdInput.value = json.id;
        await loadRecent();
    } catch (e) {
        console.error(e);
        showToast(e.message || 'Upload error', false);
    } finally {
        setUploading(false);
    }
});

getBtn.addEventListener('click', async () => {
    const id = docIdInput.value.trim();
    if (!id) return;
    try {
        const res = await fetch(`/api/documents/${encodeURIComponent(id)}`);
        if (res.status === 404) {
            getResult.textContent = 'Not found';
            showToast('Not found', false);
            return;
        }
        if (!res.ok) throw new Error('Request failed');
        const json = await res.json();
        getResult.textContent = JSON.stringify(json, null, 2);
        showToast(`Loaded #${json.id} ✔`);
    } catch (e) {
        console.error(e);
        getResult.textContent = 'Error loading document';
        showToast('Error loading document', false);
    }
});

async function loadRecent() {
    recentTable.innerHTML = '<tr><td colspan="5">Loading…</td></tr>';
    try {
        const res = await fetch(`/api/documents?limit=${encodeURIComponent(limitSel.value)}`);
        if (!res.ok) throw new Error('Failed to load recent');
        const arr = await res.json();
        if (!Array.isArray(arr) || arr.length === 0) {
            recentTable.innerHTML = '<tr><td colspan="5">No documents yet</td></tr>';
            return;
        }
        recentTable.innerHTML = arr.map(d => `
      <tr>
        <td>${d.id}</td>
        <td>${d.filename}</td>
        <td>${d.contentType || '-'}</td>
        <td>${bytesToHuman(d.size || 0)}</td>
        <td>${new Date(d.uploadedAt).toLocaleString()}</td>
      </tr>
    `).join('');
    } catch (e) {
        console.error(e);
        recentTable.innerHTML = '<tr><td colspan="5">Failed to load</td></tr>';
        showToast('Failed to load recent', false);
    }
}

refreshBtn.addEventListener('click', loadRecent);
limitSel.addEventListener('change', loadRecent);

// Initial load
loadRecent();
