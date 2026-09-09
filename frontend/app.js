const { useState, useEffect } = React;

// Point this at your live backend
const API = "https://distributed-file-storage-b26g.onrender.com";

function App() {
  const [token, setToken] = useState(localStorage.getItem("token") || "");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [files, setFiles] = useState([]);
  const [msg, setMsg] = useState(null);
  const [busy, setBusy] = useState(false);

  const show = (text, type = "error") => {
    setMsg({ text, type });
    setTimeout(() => setMsg(null), 4000);
  };

  function isValidEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }

  async function auth(path) {
    if (!isValidEmail(email)) {
      show("Please enter a valid email address (e.g. name@example.com)");
      return;
    }
    if (password.length < 8) {
      show("Password must be at least 8 characters");
      return;
    }
    setBusy(true);
    try {
      const res = await fetch(`${API}/api/auth/${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      const data = await res.json();
      if (!res.ok) { show(data.message || "Failed"); return; }
      setToken(data.token);
      localStorage.setItem("token", data.token);
      show(`${path === "register" ? "Registered" : "Logged in"} as ${data.email}`, "success");
    } catch (e) { show("Network error — is the backend awake? (first request may take 30s)"); }
    finally { setBusy(false); }
  }

  async function loadFiles() {
    if (!token) return;
    try {
      const res = await fetch(`${API}/api/files`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (res.status === 401) { logout(); return; }
      setFiles(await res.json());
    } catch (e) { show("Could not load files"); }
  }

  async function upload(e) {
    const file = e.target.files[0];
    if (!file) return;
    setBusy(true);
    const form = new FormData();
    form.append("file", file);
    try {
      const res = await fetch(`${API}/api/files`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: form,
      });
      const data = await res.json();
      if (!res.ok) { show(data.message || "Upload failed"); return; }
      show(`Uploaded ${data.filename}`, "success");
      loadFiles();
    } catch (e) { show("Upload error"); }
    finally { setBusy(false); e.target.value = ""; }
  }

  async function download(id, filename) {
    const res = await fetch(`${API}/api/files/${id}/download`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url; a.download = filename; a.click();
    URL.revokeObjectURL(url);
  }

  async function del(id) {
    const res = await fetch(`${API}/api/files/${id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.ok) { show("Deleted", "success"); loadFiles(); }
    else show("Delete failed");
  }

  function logout() {
    setToken(""); localStorage.removeItem("token"); setFiles([]);
  }

  useEffect(() => { loadFiles(); }, [token]);

  const fmtSize = (bytes) => {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / (1024 * 1024)).toFixed(1) + " MB";
  };

  return (
    <div className="container">
      <h1>Distributed File Storage</h1>
      <p className="subtitle">Upload, store, and retrieve files — backed by cloud object storage with deduplication.</p>

      {msg && <div className={`msg ${msg.type}`}>{msg.text}</div>}

      {!token ? (
        <div className="card">
          <h2>Sign in / Register</h2>
          <input type="email" placeholder="Email" value={email}
            onChange={(e) => setEmail(e.target.value)} />
          <div className="row" style={{ marginBottom: "0.75rem" }}>
            <input type={showPassword ? "text" : "password"} placeholder="Password (min 8 chars)"
              value={password} onChange={(e) => setPassword(e.target.value)} />
            <button type="button" className="secondary"
              onClick={() => setShowPassword(!showPassword)}
              style={{ whiteSpace: "nowrap" }}>
              {showPassword ? "Hide" : "Show"}
            </button>
          </div>
          <div className="row">
            <button onClick={() => auth("login")} disabled={busy}>Login</button>
            <button className="secondary" onClick={() => auth("register")} disabled={busy}>Register</button>
          </div>
        </div>
      ) : (
        <>
          <div className="card">
            <button className="logout" onClick={logout}>Logout</button>
            <h2>Upload a file</h2>
            <input type="file" onChange={upload} disabled={busy} />
            {busy && <p className="file-meta" style={{ marginTop: "0.5rem" }}>Working…</p>}
          </div>

          <div className="card">
            <h2>My files ({files.length})</h2>
            {files.length === 0 ? (
              <p className="empty">No files yet — upload one above.</p>
            ) : (
              files.map((f) => (
                <div className="file-item" key={f.id}>
                  <div>
                    <div className="file-name">{f.filename}</div>
                    <div className="file-meta">{fmtSize(f.size)} · {f.contentType}</div>
                  </div>
                  <div className="row">
                    <button className="secondary" onClick={() => download(f.id, f.filename)}>Download</button>
                    <button className="danger" onClick={() => del(f.id)}>Delete</button>
                  </div>
                </div>
              ))
            )}
          </div>
        </>
      )}
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<App />);
