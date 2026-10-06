import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

function Login() {
    const navigate = useNavigate();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const handleLogin = async (e) => {
    e.preventDefault();

    setError("");
    setLoading(true);

    try {
        const response = await fetch(
            "http://localhost:8081/api/auth/login",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    email,
                    password,
                }),
            }
        );

        // Read response safely even if backend returns empty/non-JSON
        const rawResponse = await response.text();

        let data = {};

        if (rawResponse.trim()) {
            try {
                data = JSON.parse(rawResponse);
            } catch {
                data = {
                    message: rawResponse,
                };
            }
        }

        if (!response.ok) {
            if (response.status === 401) {
                throw new Error("Incorrect email or password.");
            }

            if (response.status === 403) {
                throw new Error("You are not allowed to sign in.");
            }

            if (response.status === 400) {
                throw new Error(
                    data.message || "Please enter valid login details."
                );
            }

            throw new Error(
                data.message ||
                data.error ||
                "Unable to sign in. Please try again."
            );
        }

        if (!data.token) {
            throw new Error("Login failed. Please try again.");
        }

        localStorage.setItem("token", data.token);

        navigate("/dashboard");

    } catch (err) {
        if (err instanceof TypeError) {
            setError(
                "Unable to connect to the server. Please try again."
            );
        } else {
            setError(err.message || "Something went wrong. Please try again.");
        }
    } finally {
        setLoading(false);
    }
};
    return (
        <div className="auth-page">

            {/* ==================================================
                BACKGROUND DECORATION
            ================================================== */}

            <div className="auth-background-grid"></div>

            <div className="auth-decoration auth-decoration-one"></div>
            <div className="auth-decoration auth-decoration-two"></div>
            <div className="auth-decoration auth-decoration-three"></div>


            {/* ==================================================
                MAIN AUTH LAYOUT
            ================================================== */}

            <div className="auth-layout">

                {/* ==================================================
                    LEFT BRANDING PANEL
                ================================================== */}

                <section className="auth-showcase">

                    <div className="auth-showcase-top">

                        <div className="auth-brand-mark">
                            <span className="auth-brand-symbol">
                                ✦
                            </span>

                            <span>
                                Research AI
                            </span>
                        </div>

                        <div className="auth-live-badge">
                            <span className="auth-live-dot"></span>
                            AI Research Workspace
                        </div>

                    </div>


                    <div className="auth-showcase-main">

                        <p className="auth-eyebrow">
                            READ · RETRIEVE · UNDERSTAND
                        </p>

                        <h2>
                            Turn research papers
                            <span> into answers.</span>
                        </h2>

                        <p className="auth-showcase-description">
                            Upload your research papers, ask questions,
                            generate summaries, and compare papers
                            in one intelligent workspace.
                        </p>


                        {/* Feature blocks */}

                        <div className="auth-feature-list">

                            <div className="auth-feature-card">

                                <div className="auth-feature-icon">
                                    01
                                </div>

                                <div>
                                    <strong>
                                        Ask with context
                                    </strong>

                                    <span>
                                        Search across your uploaded
                                        research papers.
                                    </span>
                                </div>

                            </div>


                            <div className="auth-feature-card">

                                <div className="auth-feature-icon">
                                    02
                                </div>

                                <div>
                                    <strong>
                                        Summarize faster
                                    </strong>

                                    <span>
                                        Extract the important ideas
                                        from long papers.
                                    </span>
                                </div>

                            </div>


                            <div className="auth-feature-card">

                                <div className="auth-feature-icon">
                                    03
                                </div>

                                <div>
                                    <strong>
                                        Compare research
                                    </strong>

                                    <span>
                                        Understand similarities and
                                        differences between papers.
                                    </span>
                                </div>

                            </div>

                        </div>

                    </div>


                    {/* Bottom note */}

                    <div className="auth-showcase-footer">

                        <span>
                            RAG-powered research assistant
                        </span>

                        <span className="auth-footer-line"></span>

                        <span>
                            Built for students & researchers
                        </span>

                    </div>

                </section>


                {/* ==================================================
                    LOGIN CARD
                ================================================== */}

                <section className="auth-form-section">

                    <div className="auth-card">

                        <div className="auth-card-accent"></div>


                        {/* Header */}

                        <div className="auth-card-header">

                            <div className="auth-card-icon">
                                ✦
                            </div>

                            <div>

                                <p className="auth-card-kicker">
                                    WELCOME BACK
                                </p>

                                <h1>
                                    Sign in
                                </h1>

                            </div>

                        </div>


                        <p className="auth-subtitle">
                            Continue working on your research.
                        </p>


                        {/* Form */}

                        <form onSubmit={handleLogin}>

                            <div className="auth-field">

                                <label htmlFor="email">
                                    Email
                                </label>

                                <input
                                    id="email"
                                    type="email"
                                    placeholder="Enter your email"
                                    value={email}
                                    onChange={(e) =>
                                        setEmail(e.target.value)
                                    }
                                    required
                                    autoComplete="email"
                                />

                            </div>


                            <div className="auth-field">

                                <label htmlFor="password">
                                    Password
                                </label>

                                <input
                                    id="password"
                                    type="password"
                                    placeholder="Enter your password"
                                    value={password}
                                    onChange={(e) =>
                                        setPassword(e.target.value)
                                    }
                                    required
                                    autoComplete="current-password"
                                />

                            </div>


                            {error && (
                                <p className="error-message">
                                    {error}
                                </p>
                            )}


                            <button
                                type="submit"
                                disabled={loading}
                                className="auth-submit-button"
                            >
                                <span>
                                    {loading
                                        ? "Signing in..."
                                        : "Sign In"
                                    }
                                </span>

                                {!loading && (
                                    <span className="auth-submit-arrow">
                                        →
                                    </span>
                                )}
                            </button>

                        </form>


                        {/* Register */}

                        <div className="auth-divider">
                            <span></span>
                            <small>OR</small>
                            <span></span>
                        </div>


                        <p className="auth-switch">

                            Don't have an account?

                            <Link to="/register">
                                Create account
                            </Link>

                        </p>


                        <div className="auth-security-note">

                            <span className="auth-security-icon">
                                ✓
                            </span>

                            <span>
                                Your research workspace is protected
                            </span>

                        </div>

                    </div>

                </section>

            </div>

        </div>
    );
}

export default Login;