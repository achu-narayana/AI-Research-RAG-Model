export const API_URL =
    import.meta.env.VITE_API_URL || "http://localhost:8081";

export const LOGIN_PATH = "/";

const TOKEN_KEY = "token";
const USER_KEY = "user";

const STATUS_MESSAGES = {
    400: "The request was invalid. Please check your input.",
    401: "Your session has expired. Please login again.",
    403: "You are not allowed to perform this action.",
    404: "The requested item was not found.",
    409: "This conflicts with existing data.",
    413: "The file is too large.",
    500: "Something went wrong on the server.",
    502: "The AI service failed to respond correctly.",
    503: "The AI service is not available right now.",
    504: "The AI service timed out. Please try again.",
};

export function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

function decodeBase64Url(value) {
    let base64 = value.replace(/-/g, "+").replace(/_/g, "/");

    while (base64.length % 4 !== 0) {
        base64 += "=";
    }

    return atob(base64);
}

export function isTokenExpired(token = getToken()) {
    if (!token) {
        return true;
    }

    try {
        const payload = JSON.parse(decodeBase64Url(token.split(".")[1]));

        if (typeof payload.exp !== "number") {
            return false;
        }

        return payload.exp * 1000 <= Date.now();
    } catch {
        return true;
    }
}

export function isLoggedIn() {
    const token = getToken();
    return Boolean(token) && !isTokenExpired(token);
}

export function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

export function logout() {
    clearSession();
    window.location.assign(LOGIN_PATH);
}

function isPlainBody(body) {
    if (typeof body === "string") {
        return true;
    }

    return (
        body !== null &&
        typeof body === "object" &&
        Object.getPrototypeOf(body) === Object.prototype
    );
}

async function parseBody(response) {
    const text = await response.text();

    if (!text.trim()) {
        return null;
    }

    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}

function buildError(status, data) {
    let message = "";

    if (data && typeof data === "object") {
        message = data.message || data.error || "";
    } else if (typeof data === "string" && data.length < 300) {
        message = data;
    }

    const error = new Error(
        message || STATUS_MESSAGES[status] || `Request failed (${status})`
    );
    error.status = status;
    error.data = data;
    return error;
}

/*
 * Sends a request to the backend and returns the raw Response.
 * Handles the Authorization header, JSON bodies and 401 handling.
 * Throws an Error with .status for non-2xx responses.
 */
async function request(path, options = {}) {
    const { skipAuthRedirect = false, headers, body, ...rest } = options;

    const finalHeaders = new Headers(headers || {});
    const token = getToken();

    if (token && !finalHeaders.has("Authorization")) {
        finalHeaders.set("Authorization", `Bearer ${token}`);
    }

    let finalBody = body;

    if (isPlainBody(body)) {
        if (!finalHeaders.has("Content-Type")) {
            finalHeaders.set("Content-Type", "application/json");
        }

        if (typeof body !== "string") {
            finalBody = JSON.stringify(body);
        }
    }

    const response = await fetch(`${API_URL}${path}`, {
        ...rest,
        headers: finalHeaders,
        body: finalBody,
    });

    if (!response.ok) {
        const data = await parseBody(response);

        if (response.status === 401 && !skipAuthRedirect) {
            clearSession();
            window.location.assign(LOGIN_PATH);
        }

        throw buildError(response.status, data);
    }

    return response;
}

/*
 * Calls the backend and returns the parsed body
 * (JSON when possible, otherwise text, or null when empty).
 *
 * Options are passed to fetch. Extra option:
 * - skipAuthRedirect: do not clear the session / redirect on 401
 *   (used by login and register).
 */
export async function apiFetch(path, options = {}) {
    const response = await request(path, options);
    return parseBody(response);
}

export function isAbortError(error) {
    return error?.name === "AbortError";
}

export async function downloadChatPdf(projectId, title) {
    const response = await request(`/api/projects/${projectId}/chat/pdf`, {
        method: "GET",
    });

    const blob = await response.blob();
    const url = URL.createObjectURL(blob);

    const safeTitle = (title || "research-project-chat")
        .replace(/[^a-z0-9-_ ]/gi, "")
        .trim()
        .replace(/\s+/g, "-");

    const link = document.createElement("a");
    link.href = url;
    link.download = `${safeTitle || "research-project-chat"}.pdf`;

    document.body.appendChild(link);
    link.click();
    link.remove();

    setTimeout(() => URL.revokeObjectURL(url), 0);
}
