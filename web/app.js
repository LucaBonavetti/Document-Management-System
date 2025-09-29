const qs = (sel) => document.querySelector(sel);

const uploadForm = qs("#uploadForm");
const fileInput = qs("#fileInput");
const uploadResult = qs("#uploadResult");

const getForm = qs("#getForm");
const docIdInput = qs("#docId");
const getResult = qs("#getResult");

// POST /api/documents  (multipart form-data)
uploadForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    uploadResult.textContent = "Uploading…";

    const file = fileInput.files?.[0];
    if (!file) {
        uploadResult.textContent = "Please select a file first.";
        return;
    }

    try {
        const form = new FormData();
        form.append("file", file);

        const res = await fetch("/api/documents", {
            method: "POST",
            body: form
        });

        if (res.status === 201) {
            const json = await res.json();
            uploadResult.textContent =
                `✅ Uploaded!\nLocation: /api/documents/${json.id}\n` +
                `Name: ${json.filename}\nSize: ${json.size} bytes\nType: ${json.contentType || "n/a"}`;
            docIdInput.value = json.id; // convenience for the Get form
        } else {
            const text = await res.text();
            uploadResult.textContent = `❌ Upload failed (${res.status}).\n${text || ""}`;
        }
    } catch (err) {
        uploadResult.textContent = `❌ Network error: ${err}`;
    }
});

// GET /api/documents/{id}
getForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const id = docIdInput.value?.trim();
    if (!id) {
        getResult.textContent = "Please enter a document ID.";
        return;
    }
    getResult.textContent = "Fetching…";

    try {
        const res = await fetch(`/api/documents/${encodeURIComponent(id)}`);
        if (res.ok) {
            const json = await res.json();
            getResult.textContent = JSON.stringify(json, null, 2);
        } else if (res.status === 404) {
            getResult.textContent = "Not found (404).";
        } else {
            getResult.textContent = `Error ${res.status}: ${await res.text()}`;
        }
    } catch (err) {
        getResult.textContent = `❌ Network error: ${err}`;
    }
});