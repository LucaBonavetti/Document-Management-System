const dropzone = document.getElementById('dropzone');
const fileInput = document.getElementById('fileInput');
const uploadBtn = document.getElementById('uploadBtn');
const uploadStatus = document.getElementById('uploadStatus');

const docIdInput = document.getElementById('docIdInput');
const getBtn = document.getElementById('getBtn');
const getResult = document.getElementById('getResult');
const tagsContainer = document.getElementById('tagsContainer');

const tagInput = document.getElementById('tagInput');
const addTagBtn = document.getElementById('addTagBtn');

let currentDocId = null;

const searchQueryInput = document.getElementById('searchQueryInput');
const searchTable = document.getElementById('searchTable').querySelector('tbody');

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

function debounce(fn, delayMs) {
    let t;
    return (...args) => {
        clearTimeout(t);
        t = setTimeout(() => fn(...args), delayMs);
    };
}
const debouncedSearch = debounce(() => runSearch(), 250);

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
        currentDocId = json.id;
        await loadTags(json.id);
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
        currentDocId = json.id;
        await loadTags(json.id);
    } catch (e) {
        console.error(e);
        getResult.textContent = 'Error loading document';
        showToast('Error loading document', false);
    }
});

async function runSearch() {
    const q = (searchQueryInput.value || '').trim();

    if (!q) {
        searchTable.innerHTML = '<tr><td colspan="4">Type to search…</td></tr>';
        return;
    }

    return await runNormalSearch(q);
}

async function runNormalSearch(queryText) {
    const limit = 20;
    searchTable.innerHTML = '<tr><td colspan="4">Searching…</td></tr>';

    try {
        const url = `/api/search?query=${encodeURIComponent(queryText)}&limit=${limit}`;
        const res = await fetch(url);

        // 🔥 instead of throwing error → treat as no results
        if (!res.ok) {
            console.warn("Search backend error, treating as no results");
            searchTable.innerHTML = '<tr><td colspan="4">No results</td></tr>';
            showToast('No results', false);
            return;
        }

        const arr = await res.json();

        if (!Array.isArray(arr) || arr.length === 0) {
            searchTable.innerHTML = '<tr><td colspan="4">No results</td></tr>';
            showToast('No results', false);
            return;
        }

        searchTable.innerHTML = arr.map(r => `
          <tr style="cursor:pointer" data-id="${r.id}">
            <td>${r.id}</td>
            <td>${r.filename || '-'}</td>
            <td>${(r.score ?? 0).toFixed ? (r.score ?? 0).toFixed(2) : (r.score ?? 0)}</td>
            <td>${r.uploadedAt ? new Date(r.uploadedAt).toLocaleString() : '-'}</td>
          </tr>
        `).join('');

        searchTable.querySelectorAll('tr[data-id]').forEach(tr => {
            tr.addEventListener('click', () => {
                docIdInput.value = tr.dataset.id;
                getBtn.click();
            });
        });

        showToast(`Found ${arr.length} result(s) ✔`);
    } catch (e) {
        console.error(e);
        searchTable.innerHTML = '<tr><td colspan="4">No results</td></tr>';
        showToast('No results', false);
    }
}

async function loadTags(documentId) {
    const res = await fetch(`/api/documents/${encodeURIComponent(documentId)}/tags`);
    if (!res.ok) {
        tagsContainer.innerHTML = '<span class="muted">No tags</span>';
        return;
    }

    const tags = await res.json();
    if (!Array.isArray(tags) || tags.length === 0) {
        tagsContainer.innerHTML = '<span class="muted">No tags</span>';
        return;
    }

    tagsContainer.innerHTML = tags
        .map(t => `<span class="tag-chip">${t.name}</span>`)
        .join('');
}

async function addTagToDocument(documentId, rawName) {
    const res = await fetch(`/api/documents/${encodeURIComponent(documentId)}/tags`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: rawName })
    });

    if (!res.ok) {
        const msg = await res.text();
        throw new Error(msg || 'Failed to add tag');
    }
}

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
          <tr data-id="${d.id}" style="cursor:pointer">
            <td>${d.id}</td>
            <td>${d.filename}</td>
            <td>${d.contentType || '-'}</td>
            <td>${bytesToHuman(d.size || 0)}</td>
            <td>${new Date(d.uploadedAt).toLocaleString()}</td>
          </tr>
        `).join('');
        recentTable.querySelectorAll('tr[data-id]').forEach(tr => {
            tr.addEventListener('click', () => {
                docIdInput.value = tr.dataset.id;
                getBtn.click();
            });
        });
    } catch (e) {
        console.error(e);
        recentTable.innerHTML = '<tr><td colspan="5">Failed to load</td></tr>';
        showToast('Failed to load recent', false);
    }
}

refreshBtn.addEventListener('click', loadRecent);
limitSel.addEventListener('change', loadRecent);
searchQueryInput.addEventListener('input', () => {
    const q = searchQueryInput.value.trim();

    if (!q) {
        searchTable.innerHTML = '<tr><td colspan="4">Type to search…</td></tr>';
        return;
    }

    if (q.length < 3) {
        searchTable.innerHTML = '<tr><td colspan="4">Type at least 3 characters…</td></tr>';
        return;
    }

    debouncedSearch();
});
addTagBtn.addEventListener('click', async () => {
    const name = (tagInput.value || '').trim();
    if (!currentDocId) {
        showToast('Load a document first', false);
        return;
    }
    if (!name) return;

    try {
        await addTagToDocument(currentDocId, name);
        tagInput.value = '';
        showToast('Tag added ✔');
        await loadTags(currentDocId);
    } catch (e) {
        console.error(e);
        showToast('Invalid tag name or failed', false);
    }
});
tagInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') addTagBtn.click();
});

// Initial load
loadRecent();