const input   = document.getElementById('search-box');
const status  = document.getElementById('status');
const results = document.getElementById('results');
let debounce;

input.addEventListener('input', () => {
  clearTimeout(debounce);
  const q = input.value.trim();
  if (!q) { results.innerHTML = ''; status.textContent = ''; return; }
  debounce = setTimeout(() => search(q), 250);
});

async function search(q) {
  status.textContent = 'Searching\u2026';
  results.innerHTML = '';
  try {
    const res  = await fetch('/api/search?q=' + encodeURIComponent(q));
    const data = await res.json();
    if (!res.ok) {
      status.textContent = '';
      results.innerHTML = `<li class="error">${esc(data.message ?? 'Error ' + res.status)}</li>`;
      return;
    }
    const hits = data.hits ?? [];
    status.textContent = `${data.estimatedTotalHits ?? hits.length} result(s)`;
    results.innerHTML = hits.map(hit => `
      <li>
        <span>
          <span class="title">${esc(hit.title ?? '')}</span>
          ${hit.genres?.length ? `<br><span class="genres">${hit.genres.map(esc).join(', ')}</span>` : ''}
        </span>
        <span class="year">${esc(String(hit.year ?? ''))}</span>
      </li>`).join('');
  } catch (err) {
    status.textContent = '';
    results.innerHTML = `<li class="error">Request failed: ${esc(err.message)}</li>`;
  }
}

function esc(s) {
  return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}
