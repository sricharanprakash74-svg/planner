import { Router, type Request, type Response, type NextFunction } from "express";

export function createProxyRouter(): Router {
  const router = Router();

  const UPSTREAM_BASE_URL = process.env.UPSTREAM_BASE_URL || "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
  const UPSTREAM_API_KEY = process.env.UPSTREAM_API_KEY;
  const CLIENT_SHARED_SECRET = process.env.CLIENT_SHARED_SECRET || "shipaton_hackathon_token";
  const UPSTREAM_TIMEOUT_MS = parseInt(process.env.UPSTREAM_TIMEOUT_MS || "30000", 10);

  // 1. Client Authorization & Handshake Verification
  const verifyClientSecret = (req: Request, res: Response, next: NextFunction): void => {
    if (!CLIENT_SHARED_SECRET) {
      return next();
    }

    const clientSecretHeader = req.header("X-App-Secret");
    if (!clientSecretHeader || clientSecretHeader !== CLIENT_SHARED_SECRET) {
      res.status(401).json({
        success: false,
        error: "Unauthorized",
        message: "Invalid or missing application client token."
      });
      return;
    }
    next();
  };

  // 2. Simple In-Memory Sliding-Window Rate Limiter (30 req/min per IP)
  const rateLimitWindowMs = 60 * 1000;
  const maxRequestsPerWindow = 30;
  const requestCounts = new Map<string, { count: number; resetTime: number }>();

  const rateLimiter = (req: Request, res: Response, next: NextFunction): void => {
    const ip = req.ip || req.socket.remoteAddress || "unknown";
    const now = Date.now();
    const entry = requestCounts.get(ip);

    if (!entry || now > entry.resetTime) {
      requestCounts.set(ip, { count: 1, resetTime: now + rateLimitWindowMs });
      return next();
    }

    if (entry.count >= maxRequestsPerWindow) {
      res.status(429).json({
        success: false,
        error: "Too Many Requests",
        message: "Rate limit exceeded. Please wait a minute before making more requests."
      });
      return;
    }

    entry.count += 1;
    next();
  };

  // 3. Health & Status Check Endpoint
  router.get("/status", (_req: Request, res: Response) => {
    res.status(200).json({
      status: "operational",
      mockMode: !UPSTREAM_API_KEY,
      upstreamConfigured: Boolean(UPSTREAM_API_KEY),
      model: UPSTREAM_BASE_URL.includes("generativelanguage") ? "gemini-1.5-flash" : "gpt-4o-mini",
      upstream: UPSTREAM_BASE_URL.split("?")[0],
      timestamp: new Date().toISOString()
    });
  });

  // 4. Upstream Forwarding Endpoint
  router.post("/generate", verifyClientSecret, rateLimiter, async (req: Request, res: Response): Promise<void> => {
    const { prompt, model } = req.body;

    if (!prompt || typeof prompt !== "string" || prompt.trim() === "") {
      res.status(400).json({
        success: false,
        error: "Bad Request",
        message: "Field 'prompt' must be a non-empty string."
      });
      return;
    }

    // Safe Mock Mode for hackathon reviewers when no live key is provided
    if (!UPSTREAM_API_KEY) {
      const mockText = `[Proxy Mock Response] Received prompt: "${prompt.slice(0, 80)}...". Upstream API key is not configured in .env. Supply UPSTREAM_API_KEY to test live upstream completions.`;
      res.status(200).json({
        success: true,
        text: mockText,
        result: mockText,
        isMock: true
      });
      return;
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), UPSTREAM_TIMEOUT_MS);

    try {
      const isGemini = UPSTREAM_BASE_URL.includes("generativelanguage.googleapis.com");
      const targetUrl = isGemini
        ? `${UPSTREAM_BASE_URL}?key=${UPSTREAM_API_KEY}`
        : UPSTREAM_BASE_URL;

      const headers: Record<string, string> = {
        "Content-Type": "application/json"
      };
      if (!isGemini) {
        headers["Authorization"] = `Bearer ${UPSTREAM_API_KEY}`;
      }

      const payload = isGemini
        ? { contents: [{ parts: [{ text: prompt }] }] }
        : {
            model: model || "gpt-4o-mini",
            messages: [{ role: "user", content: prompt }]
          };

      const upstreamResponse = await fetch(targetUrl, {
        method: "POST",
        headers,
        body: JSON.stringify(payload),
        signal: controller.signal
      });

      clearTimeout(timeoutId);

      const data: any = await upstreamResponse.json();

      if (!upstreamResponse.ok) {
        console.error(`Upstream API failure [HTTP ${upstreamResponse.status}]:`, data);
        res.status(upstreamResponse.status).json({
          success: false,
          error: "Upstream Service Error",
          upstreamStatus: upstreamResponse.status,
          details: data
        });
        return;
      }

      let generatedText = "";
      if (isGemini) {
        generatedText = data?.candidates?.[0]?.content?.parts?.[0]?.text || "";
      } else {
        generatedText = data?.choices?.[0]?.message?.content || "";
      }

      res.status(200).json({
        success: true,
        text: generatedText,
        result: generatedText,
        raw: data
      });
    } catch (error: any) {
      clearTimeout(timeoutId);

      if (error.name === "AbortError") {
        res.status(504).json({
          success: false,
          error: "Gateway Timeout",
          message: `Upstream service timed out after ${UPSTREAM_TIMEOUT_MS}ms.`
        });
        return;
      }

      console.error("Internal Proxy Error:", error);
      res.status(500).json({
        success: false,
        error: "Internal Server Error",
        message: "An unexpected error occurred while proxying the request."
      });
    }
  });

  return router;
}
