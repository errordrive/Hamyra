package com.example.config;

/**
 * Reconstructed Central Configuration and API Keys for Hamyra Application (v2.1).
 * All keys, secrets, and endpoints recovered from APK binary and decrypted from XOR routines.
 */
public final class AppConfig {
    private AppConfig() {}

    // Versioning
    public static final String APP_VERSION = "2.1";
    public static final int APP_VERSION_CODE = 14;

    // Active Live Cloudflare Worker Endpoints (Hosted on user's Cloudflare account)
    public static final String WORKER_PRIMARY = "https://hamyra-api.hamyra.workers.dev";
    public static final String WORKER_SECONDARY = "https://hamyra-api.mdibrahimkhalil516.workers.dev";
    public static final String WORKER_TERTIARY = "https://hamyra-api.aininjaibrahim.workers.dev";

    // Dynamic Backends
    public static final String BACKEND_PRIMARY = "https://api.hamyra.xyz";
    public static final String BACKEND_BACKUP = "https://backup.hamyra.xyz";

    // Direct & Stream Proxy Endpoints
    public static final String CHANNELS_URL = WORKER_PRIMARY + "/channels";
    public static final String PROXY_BASE_URL = "https://streambd-proxy.hamyra.workers.dev/";
    public static final String TOFFEE_COOKIE_URL = PROXY_BASE_URL + "?get_cookie=1";

    // Dedicated Subtitle Services
    public static final String BNSUB_BASE_URL = "https://px.talkoraai.com";

    // Authentication, HMAC & Internal Auth Secrets
    public static final String HMAC_SECRET = "streambd_K9mR2pL8nT5qY3wJ7cS4uE6oA0bF";
    public static final String INTERNAL_AUTH_KEY = "45a34053c6367e18d4d9b79f53ad2a6b5d867a9d7dbd7480287064518f8c2e8a";
    public static final String USER_AGENT_HAMYRA = "Hamyra/2.1";
    public static final String USER_AGENT_MOBILE = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36";

    // Google AI / Gemini API Keys (Rotated for recommendations)
    public static final String[] GEMINI_API_KEYS = new String[] {
        "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w",
        "AIzaSyCzCx_T_y3dz-teDIEas1he6nfW4EwRj5s",
        "AIzaSyCv82eHAx0LaNvcJqOwumExKJen0_CzT1M"
    };

    // Third-party API Keys & Endpoints
    public static final String TMDB_API_KEY = "0b3d17a4fe3dd52593a48d9a0dad4bd6";
    public static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";
    public static final String TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p";

    public static final String STORYTELLER_KEY = "ad4a7794-f3b2-4062-aa29-476bca9dc7c0";
    public static final String STORYTELLER_COLLECTION = "wc-2026-all-clips";

    public static final String STREAMED_BASE = "https://streamed.pk/api";
    public static final String AD_NETWORK_URL = "https://www.effectivecpmnetwork.com/reiz0vka5?key=fb8037f0ea86a3e0b33f63577dc2b4fa";
    public static final String AD_NETWORK_KEY = "fb8037f0ea86a3e0b33f63577dc2b4fa";
}
