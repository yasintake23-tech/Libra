export interface Env {
  BUCKET: R2Bucket
  CORS_ORIGIN: string
  FIREBASE_PROJECT_ID: string
  FIREBASE_WEB_API_KEY: string
}

type VerifiedUser = {
  uid: string
  email?: string
}

const jsonHeaders = (env: Env) => ({
  "Content-Type": "application/json; charset=UTF-8",
  "Access-Control-Allow-Origin": env.CORS_ORIGIN || "*",
  "Access-Control-Allow-Headers": "Authorization, Content-Type",
  "Access-Control-Allow-Methods": "GET, PUT, DELETE, OPTIONS",
})

function json(env: Env, body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: jsonHeaders(env),
  })
}

function getBearerToken(request: Request) {
  const header = request.headers.get("Authorization") || ""
  return header.startsWith("Bearer ") ? header.slice(7).trim() : ""
}

async function verifyFirebaseToken(request: Request, env: Env): Promise<VerifiedUser | null> {
  const idToken = getBearerToken(request)
  if (!idToken || !env.FIREBASE_WEB_API_KEY) return null

  const response = await fetch(
    "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=" +
      encodeURIComponent(env.FIREBASE_WEB_API_KEY),
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idToken }),
    }
  )

  if (!response.ok) return null

  const payload = (await response.json()) as {
    users?: Array<{
      localId?: string
      email?: string
      disabled?: boolean
    }>
  }

  const user = payload.users?.[0]
  if (!user?.localId || user.disabled) return null

  return {
    uid: user.localId,
    email: user.email,
  }
}

function normalizeKey(key: string) {
  return key.replace(/^/+/, "").replace(//+/g, "/")
}

function isSafeKey(key: string) {
  return Boolean(
    key &&
      key.length <= 512 &&
      !key.includes("..") &&
      !key.startsWith("/") &&
      !key.includes("\\")
  )
}

function publicUrl(request: Request, key: string) {
  const url = new URL(request.url)
  return url.origin + "/media/" + key
}

async function handleUpload(request: Request, env: Env) {
  const user = await verifyFirebaseToken(request, env)
  if (!user) return json(env, { error: "Unauthorized" }, 401)

  const requested = normalizeKey(
    new URL(request.url).searchParams.get("path") || ""
  )

  if (
    !isSafeKey(requested) ||
    !requested.startsWith("users/" + user.uid + "/")
  ) {
    return json(env, { error: "Invalid object path" }, 400)
  }

  const contentType =
    request.headers.get("Content-Type") || "application/octet-stream"
  const length = Number(request.headers.get("Content-Length") || "0")

  if (length <= 0) return json(env, { error: "Empty upload" }, 400)
  if (length > 50 * 1024 * 1024) {
    return json(
      env,
      { error: "File is too large. Maximum is 50 MB." },
      413
    )
  }

  const object = await env.BUCKET.put(requested, request.body, {
    httpMetadata: { contentType },
  })

  if (!object) {
    return json(env, { error: "R2 upload failed" }, 500)
  }

  return json(env, {
    key: requested,
    url: publicUrl(request, requested),
  })
}

async function handleDelete(request: Request, env: Env) {
  const user = await verifyFirebaseToken(request, env)
  if (!user) return json(env, { error: "Unauthorized" }, 401)

  const requested = normalizeKey(
    new URL(request.url).searchParams.get("path") || ""
  )

  if (
    !isSafeKey(requested) ||
    !requested.startsWith("users/" + user.uid + "/")
  ) {
    return json(env, { error: "Invalid object path" }, 400)
  }

  await env.BUCKET.delete(requested)
  return json(env, { ok: true })
}

async function handleMedia(request: Request, env: Env) {
  const url = new URL(request.url)
  const encoded = url.pathname.slice("/media/".length)

  const key = normalizeKey(
    encoded
      .split("/")
      .map(part => decodeURIComponent(part))
      .join("/")
  )

  if (!isSafeKey(key)) {
    return json(env, { error: "Invalid object path" }, 400)
  }

  const object = await env.BUCKET.get(key)
  if (!object) return new Response("Not Found", { status: 404 })

  const headers = new Headers()
  object.writeHttpMetadata(headers)
  headers.set("etag", object.httpEtag)
  headers.set("Cache-Control", "public, max-age=31536000, immutable")
  headers.set("Access-Control-Allow-Origin", env.CORS_ORIGIN || "*")

  return new Response(object.body, { headers })
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: jsonHeaders(env),
      })
    }

    const url = new URL(request.url)

    try {
      if (request.method === "PUT" && url.pathname === "/upload") {
        return handleUpload(request, env)
      }

      if (request.method === "DELETE" && url.pathname === "/delete") {
        return handleDelete(request, env)
      }

      if (request.method === "GET" && url.pathname.startsWith("/media/")) {
        return handleMedia(request, env)
      }

      return json(env, { service: "Libra R2 Worker", ok: true })
    } catch (error) {
      return json(
        env,
        {
          error: error instanceof Error ? error.message : "Unknown error",
        },
        500
      )
    }
  },
}
