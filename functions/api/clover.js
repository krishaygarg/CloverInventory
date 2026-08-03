export async function onRequest(context) {
  const url = new URL(context.request.url);
  const path = url.pathname.replace(/^\/api\/clover/, "");
  const targetUrl = `https://apisandbox.dev.clover.com${path}${url.search}`;

  if (context.request.method === "OPTIONS") {
    return new Response(null, {
      status: 204,
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
        "Access-Control-Allow-Headers": "Authorization, Content-Type, Accept",
      },
    });
  }

  try {
    const response = await fetch(targetUrl, {
      method: context.request.method,
      headers: context.request.headers,
      body: context.request.method !== "GET" && context.request.method !== "HEAD" ? context.request.body : null,
    });

    const newResponse = new Response(response.body, response);
    newResponse.headers.set("Access-Control-Allow-Origin", "*");
    newResponse.headers.set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
    newResponse.headers.set("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
    return newResponse;
  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 500,
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
      },
    });
  }
}
