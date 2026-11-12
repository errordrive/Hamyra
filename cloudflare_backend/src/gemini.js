/**
 * Google Gemini AI Recommendation Service with Key Rotation
 */

export async function getAiRecommendations(query, apiKeys, tmdbApiKey) {
  if (!query || !query.trim()) {
    return { ok: false, error: "Empty query" };
  }

  const prompt = `You are a movie and TV series recommendation engine for a streaming app.
The user query is: "${query.trim()}".

Task:
1. If the query is completely unrelated to movies, tv series, cinema, entertainment, actors, or storytelling (for example: math equations, coding, weather, sports scores, recipes), reply ONLY with:
{"offtopic": true}

2. If the query is relevant (mood, genre, actor, similar movie, title, plot description):
Suggest 6 to 8 matching popular movies or TV shows.
Reply ONLY with valid JSON in this exact structure without markdown backticks:
{
  "ok": true,
  "offtopic": false,
  "results": [
    {
      "id": 550,
      "title": "Movie Title",
      "kind": "movie",
      "year": "1999",
      "poster_url": "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
      "reason": "Brief reason why it matches the query",
      "rating": 8.4
    }
  ]
}
Make sure "id" is an approximate valid TMDB movie or TV ID if known, and "kind" is either "movie" or "tv".`;

  let lastError = null;

  for (const key of apiKeys) {
    if (!key || !key.trim()) continue;
    try {
      const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${key.trim()}`;
      const payload = {
        contents: [
          {
            parts: [{ text: prompt }]
          }
        ],
        generationConfig: {
          temperature: 0.4,
          maxOutputTokens: 1200
        }
      };

      const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });

      if (!res.ok) {
        lastError = `Gemini returned HTTP ${res.status}`;
        continue;
      }

      const data = await res.json();
      const rawText = data?.candidates?.[0]?.content?.parts?.[0]?.text;
      if (!rawText) continue;

      // Clean up markdown formatting if present
      const cleaned = rawText
        .replace(/```json/gi, "")
        .replace(/```/g, "")
        .trim();

      const parsed = JSON.parse(cleaned);

      if (parsed.offtopic) {
        return { ok: true, offtopic: true };
      }

      if (Array.isArray(parsed.results) && parsed.results.length > 0) {
        // Enrich poster URLs if missing or placeholder
        const enriched = parsed.results.map((item, idx) => ({
          id: item.id || (1000 + idx),
          title: item.title || "Unknown Title",
          kind: item.kind || "movie",
          year: String(item.year || ""),
          poster_url: item.poster_url && item.poster_url.startsWith("http")
            ? item.poster_url
            : "https://image.tmdb.org/t/p/w500/wwemzKWzjKYJFfCeiB57q3r4Bcm.png",
          reason: item.reason || "Recommended based on your mood",
          rating: Number(item.rating) || 7.5
        }));

        return {
          ok: true,
          offtopic: false,
          results: enriched
        };
      }
    } catch (err) {
      lastError = err.message;
      console.warn("Key error in Gemini:", err.message);
    }
  }

  // Fallback response if all Gemini keys fail or are unavailable
  return {
    ok: true,
    offtopic: false,
    results: [
      {
        id: 157336,
        title: "Interstellar",
        kind: "movie",
        year: "2014",
        poster_url: "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
        reason: "Epic sci-fi journey through space and time.",
        rating: 8.4
      },
      {
        id: 27205,
        title: "Inception",
        kind: "movie",
        year: "2010",
        poster_url: "https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg",
        reason: "Mind-bending heist thriller through dreams.",
        rating: 8.3
      }
    ]
  };
}
