import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

function Register() {

    const navigate = useNavigate();

    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const handleRegister = async (e) => {
    e.preventDefault();

    setError("");
    setLoading(true);

    try {
        const response = await fetch(
            "http://localhost:8081/api/auth/register",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    name,
                    email,
                    password,
                }),
            }
        );

        // Read response safely
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
            if (response.status === 409) {
                throw new Error(
                    "An account with this email already exists."
                );
            }

            if (response.status === 400) {
                throw new Error(
                    data.message ||
                    "Please check your details and try again."
                );
            }

            throw new Error(
                data.message ||
                data.error ||
                "Unable to create account. Please try again."
            );
        }

        navigate("/");

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
        <div className="auth-page register-page">

            {/* ==================================================
                BACKGROUND
            ================================================== */}

            <div className="auth-background-grid"></div>

            <div className="auth-decoration auth-decoration-one"></div>
            <div className="auth-decoration auth-decoration-two"></div>
            <div className="auth-decoration auth-decoration-three"></div>


            {/* ==================================================
                MAIN LAYOUT
            ================================================== */}

            <div className="auth-layout">


                {/* ==================================================
                    LEFT SHOWCASE
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

                            Build your workspace

                        </div>

                    </div>


                    <div className="auth-showcase-main">

                        <p className="auth-eyebrow">
                            YOUR RESEARCH · YOUR WORKSPACE
                        </p>


                        <h2>
                            Start building
                            <span> smarter research.</span>
                        </h2>


                        <p className="auth-showcase-description">
                            Create your account and organize your
                            research papers in one intelligent workspace
                            designed for faster reading and analysis.
                        </p>


                        {/* ==================================================
                            REGISTER BENEFITS
                        ================================================== */}

                        <div className="auth-feature-list">

                            <div className="auth-feature-card">

                                <div className="auth-feature-icon">
                                    01
                                </div>

                                <div>

                                    <strong>
                                        Create projects
                                    </strong>

                                    <span>
                                        Keep papers organized by
                                        research project.
                                    </span>

                                </div>

                            </div>


                            <div className="auth-feature-card">

                                <div className="auth-feature-icon">
                                    02
                                </div>

                                <div>

                                    <strong>
                                        Build your library
                                    </strong>

                                    <span>
                                        Upload and manage multiple
                                        research papers.
                                    </span>

                                </div>

                            </div>


                            <div className="auth-feature-card">

                                <div className="auth-feature-icon">
                                    03
                                </div>

                                <div>

                                    <strong>
                                        Explore with AI
                                    </strong>

                                    <span>
                                        Ask questions, summarize and
                                        compare research.
                                    </span>

                                </div>

                            </div>

                        </div>

                    </div>


                    <div className="auth-showcase-footer">

                        <span>
                            Research workspace
                        </span>

                        <span className="auth-footer-line"></span>

                        <span>
                            RAG-powered analysis
                        </span>

                    </div>

                </section>


                {/* ==================================================
                    REGISTER CARD
                ================================================== */}

                <section className="auth-form-section">

                    <div className="auth-card register-card">

                        <div className="auth-card-accent"></div>


                        {/* ==================================================
                            HEADER
                        ================================================== */}

                        <div className="auth-card-header">

                            <div className="auth-card-icon register-card-icon">
                                +
                            </div>

                            <div>

                                <p className="auth-card-kicker">
                                    GET STARTED
                                </p>

                                <h1>
                                    Create account
                                </h1>

                            </div>

                        </div>


                        <p className="auth-subtitle">
                            Set up your research workspace in a few seconds.
                        </p>


                        {/* ==================================================
                            FORM
                        ================================================== */}

                        <form onSubmit={handleRegister}>


                            {/* NAME */}

                            <div className="auth-field">

                                <label htmlFor="register-name">
                                    Name
                                </label>

                                <input
                                    id="register-name"
                                    type="text"
                                    placeholder="Enter your name"
                                    value={name}
                                    onChange={(e) =>
                                        setName(e.target.value)
                                    }
                                    required
                                    autoComplete="name"
                                />

                            </div>


                            {/* EMAIL */}

                            <div className="auth-field">

                                <label htmlFor="register-email">
                                    Email
                                </label>

                                <input
                                    id="register-email"
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


                            {/* PASSWORD */}

                            <div className="auth-field">

                                <label htmlFor="register-password">
                                    Password
                                </label>

                                <input
                                    id="register-password"
                                    type="password"
                                    placeholder="Create a password"
                                    value={password}
                                    onChange={(e) =>
                                        setPassword(e.target.value)
                                    }
                                    required
                                    autoComplete="new-password"
                                />

                            </div>


                            {/* ERROR */}

                            {error && (
                                <p className="error-message">
                                    {error}
                                </p>
                            )}


                            {/* SUBMIT */}

                            <button
                                type="submit"
                                disabled={loading}
                                className="auth-submit-button"
                            >

                                <span>
                                    {loading
                                        ? "Creating account..."
                                        : "Create Account"
                                    }
                                </span>


                                {!loading && (
                                    <span className="auth-submit-arrow">
                                        →
                                    </span>
                                )}

                            </button>

                        </form>


                        {/* ==================================================
                            DIVIDER
                        ================================================== */}

                        <div className="auth-divider">

                            <span></span>

                            <small>OR</small>

                            <span></span>

                        </div>


                        {/* ==================================================
                            SIGN IN
                        ================================================== */}

                        <p className="auth-switch">

                            Already have an account?

                            <Link to="/">
                                Sign In
                            </Link>

                        </p>


                        {/* ==================================================
                            SECURITY NOTE
                        ================================================== */}

                        <div className="auth-security-note">

                            <span className="auth-security-icon">
                                ✓
                            </span>

                            <span>
                                Your account is securely authenticated
                            </span>

                        </div>

                    </div>

                </section>

            </div>

        </div>
    );
}

export default Register;