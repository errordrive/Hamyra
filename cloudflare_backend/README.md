# Hamyra Cloudflare Worker Backend

This is the complete, deployable Cloudflare Worker backend for the **Hamyra** Android application. It implements all backend APIs required by the app:

1. **`GET /channels`**: Channels list with HMAC-SHA256 signature verification (`X-Hamyra-Ts` & `X-Hamyra-Sig`).
2. **`GET /ai/recommend?q=...`**: Gemini AI recommendations with automatic key rotation.
3. **`GET /tmdb/flaxfer`**: Flaxfer / Stela / VZ streaming resolver proxy.
4. **`GET /tmdb/movieboxnoob`**: MovieBox / Cinejoy stream resolver proxy.
5. **`GET /auth/token`**: Generates signed authentication tokens.
6. **`GET /banglasub/catalog`**: Bangla subtitle series & episode streams.
7. **`GET /mlbd/category`**: MLBD catalog categories.
8. **`GET /?get_cookie=1`**: Live streaming cookie/token generator.

---

## Prerequisites

- **Node.js**: v18.0.0 or higher.
- A free or paid **Cloudflare Account**.

---

## Quick Start & Deployment

### 1. Install Dependencies
```bash
cd /storage/emulated/0/ApkLab/MovieApp/cloudflare_backend
npm install
```

### 2. Login to Cloudflare
```bash
npx wrangler login
```
*(This opens a browser tab to authenticate your Cloudflare account)*

### 3. Test Locally
Run the local development server:
```bash
npm run dev
# or: npx wrangler dev
```
You can test the endpoints in your browser or with curl:
```bash
# Health check
curl http://localhost:8787/

# Cookie generator
curl http://localhost:8787/?get_cookie=1
```

### 4. Deploy to Production
To deploy to your `*.workers.dev` subdomain:
```bash
npm run deploy
# or: npx wrangler deploy
```

Once deployed, Wrangler will output your live URL:
```
Published hamyra-api (x.xx sec)
  https://hamyra-api.<your-subdomain>.workers.dev
```

---

## Configuration & Environment Variables

All variables are defined in [`wrangler.toml`](file:///storage/emulated/0/ApkLab/MovieApp/cloudflare_backend/wrangler.toml):

| Variable | Description | Default |
| :--- | :--- | :--- |
| `HMAC_SECRET` | HMAC-SHA256 signing secret for client requests | `streambd_K9mR2pL8nT5qY3wJ7cS4uE6oA0bF` |
| `TMDB_API_KEY` | The Movie Database API key | `0b3d17a4fe3dd52593a48d9a0dad4bd6` |
| `GEMINI_API_KEYS` | Comma-separated Google Gemini AI API keys | Configured with your 3 active keys |
| `PROXY_BASE_URL` | Base proxy worker URL | `https://streambd-proxy.aininjaibrahim.workers.dev` |

### Adding or Updating Channels
Edit [`src/channels.json`](file:///storage/emulated/0/ApkLab/MovieApp/cloudflare_backend/src/channels.json) to add, remove, or modify live TV channels, and then run `npx wrangler deploy`.
