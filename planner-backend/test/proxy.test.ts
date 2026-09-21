import { describe, it, before, after } from "node:test";
import assert from "node:assert/strict";
import express from "express";
import type { Server } from "node:http";
import { createProxyRouter } from "../src/proxy.js";

describe("Backend API Proxy Architecture Test Suite", () => {
  let app: express.Express;
  let server: Server;
  let baseUrl: string;

  before(async () => {
    app = express();
    app.use(express.json());
    app.use("/api/proxy", createProxyRouter());

    await new Promise<void>((resolve) => {
      server = app.listen(0, () => {
        const addr = server.address();
        if (typeof addr === "object" && addr) {
          baseUrl = `http://127.0.0.1:${addr.port}`;
        }
        resolve();
      });
    });
  });

  after(async () => {
    await new Promise<void>((resolve) => {
      server.close(() => resolve());
    });
  });

  it("Requirement 1: GET /api/proxy/status returns 200 and operational proxy status", async () => {
    const res = await fetch(`${baseUrl}/api/proxy/status`);
    assert.equal(res.status, 200);
    const data = (await res.json()) as { status: string; mockMode: boolean; upstreamConfigured: boolean };
    assert.equal(data.status, "operational");
    assert.ok("mockMode" in data);
    assert.ok("upstreamConfigured" in data);
  });

  it("Requirement 2: POST /api/proxy/generate without X-App-Secret returns 401 Unauthorized", async () => {
    const res = await fetch(`${baseUrl}/api/proxy/generate`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ prompt: "Plan my day" }),
    });
    assert.equal(res.status, 401);
    const data = (await res.json()) as { success: boolean; error: string };
    assert.equal(data.success, false);
    assert.equal(data.error, "Unauthorized");
  });

  it("Requirement 3: POST /api/proxy/generate with invalid X-App-Secret returns 401 Unauthorized", async () => {
    const res = await fetch(`${baseUrl}/api/proxy/generate`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-App-Secret": "invalid_secret_token_123",
      },
      body: JSON.stringify({ prompt: "Plan my day" }),
    });
    assert.equal(res.status, 401);
    const data = (await res.json()) as { success: boolean; error: string };
    assert.equal(data.success, false);
    assert.equal(data.error, "Unauthorized");
  });

  it("Requirement 4: POST /api/proxy/generate with missing prompt returns 400 Bad Request", async () => {
    const secret = process.env.CLIENT_SHARED_SECRET || "shipaton_hackathon_token";
    const res = await fetch(`${baseUrl}/api/proxy/generate`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-App-Secret": secret,
      },
      body: JSON.stringify({}),
    });
    assert.equal(res.status, 400);
    const data = (await res.json()) as { success: boolean; error: string };
    assert.equal(data.success, false);
    assert.equal(data.error, "Bad Request");
  });

  it("Requirement 5: POST /api/proxy/generate with valid handshake produces valid AI completion", async () => {
    const secret = process.env.CLIENT_SHARED_SECRET || "shipaton_hackathon_token";
    const res = await fetch(`${baseUrl}/api/proxy/generate`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-App-Secret": secret,
      },
      body: JSON.stringify({
        prompt: "Create a 3-step focus schedule for morning study.",
      }),
    });
    assert.equal(res.status, 200);
    const data = (await res.json()) as { success: boolean; text: string; isMock: boolean };
    assert.equal(data.success, true);
    assert.ok(typeof data.text === "string");
    assert.ok(data.text.length > 0);
  });
});
