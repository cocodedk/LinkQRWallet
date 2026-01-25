## Product requirements: “Link QR Wallet” (Android)

### 1) Goal

A personal Android app that turns URLs into QR codes and stores them as a searchable, sortable “wallet” of links. Each saved item auto-fetches the web page title (and optional metadata) for easier recognition.

### 2) Target user

* Single user (you).
* Fast capture of links (GitHub repos, docs, product pages, etc.) and easy re-find later.

### 3) Core use cases

1. Paste a URL → generate QR → fetch page title → save.
2. Share a URL into the app from another app (browser, GitHub, Slack) → same flow.
3. Browse saved QRs as a list or grid; sort and search.
4. Tap an item → show QR full-screen + open URL.
5. Edit item (rename title, add notes/tags), delete, favorite.
6. Offline access to saved items (title/QR stored locally).

---

## 4) Scope

### Must-have (MVP)

* **Add URL**

  * Input: paste/type URL
  * Validate URL (basic sanity + scheme normalization)
  * Generate QR code
  * Fetch **page title** from URL (best-effort)
  * Save entry locally
* **List view**

  * Display: title, domain, created date, small QR thumbnail
  * Tap opens detail
* **Detail view**

  * Large QR, title, URL
  * Actions: Copy URL, Open in browser, Share QR, Delete
* **Search**

  * Search by title + URL + domain (case-insensitive)
* **Sorting**

  * Sort by: newest/oldest, A–Z title, domain
* **Persistence**

  * Local database, works offline
* **Import via Android Share**

  * “Share → Link Wallet QR” to create entry quickly

### Nice-to-have (v1.1+)

* Tags + filter by tag
* Favorites
* Notes field
* Automatic favicon fetch for domain
* QR scanning (import from an existing QR)
* Export/import backup (JSON) or Android auto-backup
* Duplicate detection and “merge/skip”
* Bulk actions (multi-select delete/tag)

### Explicitly out of scope (for now)

* Accounts, cloud sync, multi-device sync
* Collaboration/sharing collections
* Tracking clicks/analytics

---

## 5) Screens & user flows

### A) Home / Library

* Default view: list (toggle grid optional)
* Top: search bar
* Sort button (dropdown)
* Floating Action Button: **+ Add**
* Item row: Title, domain, small QR, created date

**Flow:** Open app → scroll/search/sort → tap item

### B) Add Link

* URL input field
* “Fetch Title” auto on paste/submit (with spinner)
* Preview card: Title (editable), domain
* QR preview
* Save button

**Flow:** Paste URL → QR generates immediately → title fetch completes → Save

### C) Detail

* Large QR (tap to fullscreen)
* Title (editable)
* URL (copy)
* Buttons: Open, Share (QR image), Copy, Delete

---

## 6) Data model (local)

**Entity: LinkItem**

* id (uuid/int)
* url (string, normalized)
* title (string, fetched; editable)
* domain (string, derived)
* createdAt (datetime)
* updatedAt (datetime)
* qrPayload (string = url) OR stored QR image bytes/cache key
* notes (string, optional)
* tags (list, optional)
* favorite (bool, optional)

**Indexing**

* Full-text-ish index on title/url/domain for fast search.
* Sort fields indexed: createdAt, title, domain.

---

## 7) Title fetching rules

* Use HTTP GET (or HEAD + GET fallback) and parse `<title>`.
* Timeouts + user agent set.
* If title fetch fails:

  * fallback title = domain or URL path
  * still allow saving immediately
* Cache title; allow manual edit.

---

## 8) Search & sort behavior

* Search matches:

  * title contains query
  * url contains query
  * domain contains query
* Sort options:

  * Newest first (default)
  * Oldest first
  * Title A–Z / Z–A
  * Domain A–Z

---

## 9) Non-functional requirements

* **Performance:** library loads in <300ms for ~1k items (local DB + lazy QR thumbnails).
* **Offline-first:** everything usable without network; title fetch is best-effort.
* **Privacy:** all data stored locally; no telemetry by default.
* **Reliability:** never lose items on crash; DB transactions on save.
* **Accessibility:** QR contrast, large text support, TalkBack labels.

---

## 10) Edge cases

* Invalid URL → show error and prevent save (or auto-add `https://` when missing).
* Same URL added twice → prompt: “Already saved. Add duplicate?” (configurable).
* URL redirects → store final URL or original? (recommend: store **final resolved URL** + keep original in notes optional).
* Pages with no `<title>` or dynamic titles → fallback to domain/path.
* Very long URLs → QR still generated (ensure QR version supports it).

---

## 11) MVP acceptance criteria (done means)

* You can add a URL, see QR and fetched title, and it persists after app restart.
* Library shows all saved items, searchable and sortable.
* Detail screen displays scannable QR and can open/copy/share/delete.
* Sharing a URL from another app creates a saved item.

---

## 12) Suggested build plan (practical)

1. Local DB + LinkItem model + basic list
2. Add-link screen: QR generation + save
3. Title fetch worker + editable title
4. Search + sort
5. Share intent integration
6. Polish: empty states, error handling, performance (thumbnail caching)
