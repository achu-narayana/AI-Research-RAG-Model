import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
    ArrowRight,
    FolderPlus,
    LogOut,
    Plus,
    Sparkles,
    BookOpen,
    Layers3
} from "lucide-react";

function Dashboard() {

    const navigate = useNavigate();

    const [projects, setProjects] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");


    // =========================================================
    // FETCH PROJECTS
    // =========================================================

    useEffect(() => {

        const fetchProjects = async () => {

            const token =
                localStorage.getItem("token");

            if (!token) {
                navigate("/");
                return;
            }

            try {

                setLoading(true);

                const response =
                    await fetch(
                        "http://localhost:8081/api/projects",
                        {
                            method: "GET",
                            headers: {
                                Authorization:
                                    `Bearer ${token}`,
                            },
                        }
                    );


                // ---------------------------------------------
                // AUTHENTICATION
                // ---------------------------------------------

                if (
                    response.status === 401 ||
                    response.status === 403
                ) {

                    localStorage.removeItem("token");
                    navigate("/");

                    return;
                }


                // ---------------------------------------------
                // ERROR
                // ---------------------------------------------

                if (!response.ok) {

                    throw new Error(
                        "Failed to load projects"
                    );
                }


                // ---------------------------------------------
                // DATA
                // ---------------------------------------------

                const data =
                    await response.json();

                setProjects(data);

            } catch (err) {

                setError(
                    err.message ||
                    "Failed to load projects"
                );

            } finally {

                setLoading(false);
            }
        };


        fetchProjects();

    }, [navigate]);


    // =========================================================
    // LOGOUT
    // =========================================================

    const handleLogout = () => {

        localStorage.removeItem("token");

        navigate("/");
    };


    // =========================================================
    // OPEN PROJECT
    // =========================================================

    const openProject = (projectId) => {

        navigate(
            `/projects/${projectId}`
        );
    };


    return (

        <div className="dashboard-page">

            {/* =====================================================
                BACKGROUND
            ===================================================== */}

            <div className="dashboard-grid-background"></div>

            <div className="dashboard-orb dashboard-orb-one"></div>
            <div className="dashboard-orb dashboard-orb-two"></div>


            {/* =====================================================
                HEADER
            ===================================================== */}

            <header className="dashboard-header">

                <div className="dashboard-brand">

                    <div className="dashboard-brand-icon">
                        ✦
                    </div>

                    <div>

                        <h1>
                            AI Research Paper Assistant
                        </h1>

                        <p>
                            Research workspace
                        </p>

                    </div>

                </div>


                <div className="dashboard-header-actions">

                    <button
                        className="dashboard-logout-button"
                        onClick={handleLogout}
                    >

                        <LogOut size={16} />

                        <span>
                            Logout
                        </span>

                    </button>

                </div>

            </header>


            {/* =====================================================
                MAIN CONTENT
            ===================================================== */}

            <main className="dashboard-content">


                {/* =================================================
                    WELCOME / HERO
                ================================================= */}

                <section className="dashboard-hero">

                    <div className="dashboard-hero-content">

                        <div className="dashboard-hero-kicker">

                            <Sparkles size={14} />

                            RESEARCH WORKSPACE

                        </div>


                        <h2>
                            Your research,
                            <span>
                                organized and searchable.
                            </span>
                        </h2>


                        <p>

                            Create projects to organize your research
                            papers and explore them using AI-powered
                            question answering, summaries and comparison.

                        </p>


                        <button
                            className="dashboard-hero-button"
                            onClick={() =>
                                navigate("/projects/new")
                            }
                        >

                            <Plus size={17} />

                            Create Research Project

                            <ArrowRight size={17} />

                        </button>

                    </div>


                    {/* Decorative visual */}

                    <div className="dashboard-hero-visual">

                        <div className="dashboard-visual-card dashboard-visual-card-main">

                            <div className="dashboard-visual-card-top">

                                <div className="dashboard-visual-icon">
                                    <BookOpen size={19} />
                                </div>

                                <span>
                                    RESEARCH
                                </span>

                            </div>


                            <div className="dashboard-visual-lines">

                                <span></span>
                                <span></span>
                                <span></span>
                                <span className="short"></span>

                            </div>


                            <div className="dashboard-visual-chip">
                                AI assisted
                            </div>

                        </div>


                        <div className="dashboard-floating-card dashboard-floating-card-one">

                            <Layers3 size={18} />

                            <div>

                                <strong>
                                    {projects.length}
                                </strong>

                                <span>
                                    Projects
                                </span>

                            </div>

                        </div>


                        <div className="dashboard-floating-card dashboard-floating-card-two">

                            <Sparkles size={16} />

                            <span>
                                RAG powered
                            </span>

                        </div>

                    </div>

                </section>


                {/* =================================================
                    PAGE TITLE / STATS
                ================================================= */}

                <section className="dashboard-section-heading">

                    <div>

                        <p className="dashboard-section-kicker">
                            YOUR WORKSPACE
                        </p>

                        <h3>
                            My Research Projects
                        </h3>

                        <p>
                            Open an existing project or create
                            a new research workspace.
                        </p>

                    </div>


                    <div className="dashboard-project-count">

                        <span>
                            TOTAL PROJECTS
                        </span>

                        <strong>
                            {loading ? "—" : projects.length}
                        </strong>

                    </div>

                </section>


                {/* =================================================
                    LOADING
                ================================================= */}

                {loading && (

                    <div className="dashboard-state-card">

                        <div className="dashboard-loader">
                            <span></span>
                            <span></span>
                            <span></span>
                        </div>

                        <strong>
                            Loading your research workspace
                        </strong>

                        <p>
                            Fetching your saved projects...
                        </p>

                    </div>

                )}


                {/* =================================================
                    ERROR
                ================================================= */}

                {error && (

                    <div className="dashboard-error-card">

                        <div className="dashboard-error-icon">
                            !
                        </div>

                        <div>

                            <strong>
                                Something went wrong
                            </strong>

                            <p>
                                {error}
                            </p>

                        </div>

                    </div>

                )}


                {/* =================================================
                    EMPTY STATE
                ================================================= */}

                {!loading &&
                    !error &&
                    projects.length === 0 && (

                        <div className="dashboard-empty-card">

                            <div className="dashboard-empty-visual">

                                <div className="dashboard-empty-icon">
                                    <FolderPlus size={25} />
                                </div>

                                <div className="dashboard-empty-accent">
                                    +
                                </div>

                            </div>


                            <p className="dashboard-empty-kicker">
                                START YOUR WORKSPACE
                            </p>

                            <h3>
                                No research projects yet
                            </h3>

                            <p>
                                Create your first project and start
                                building your research library.
                            </p>


                            <button
                                className="dashboard-empty-button"
                                onClick={() =>
                                    navigate("/projects/new")
                                }
                            >

                                <Plus size={17} />

                                Create your first project

                                <ArrowRight size={17} />

                            </button>

                        </div>

                    )}


                {/* =================================================
                    PROJECT GRID
                ================================================= */}

                {!loading &&
                    !error &&
                    projects.length > 0 && (

                        <div className="dashboard-project-grid">

                            {projects.map(
                                (project, index) => (

                                    <article
                                        className="dashboard-project-card"
                                        key={project.id}
                                    >

                                        {/* Number */}

                                        <div className="dashboard-project-number">
                                            {String(index + 1).padStart(
                                                2,
                                                "0"
                                            )}
                                        </div>


                                        {/* Accent */}

                                        <div
                                            className={
                                                `dashboard-project-accent dashboard-project-accent-${index % 4}`
                                            }
                                        ></div>


                                        {/* Header */}

                                        <div className="dashboard-project-card-header">

                                            <div className="dashboard-project-icon">

                                                <BookOpen size={19} />

                                            </div>

                                            <span className="dashboard-project-label">
                                                RESEARCH PROJECT
                                            </span>

                                        </div>


                                        {/* Content */}

                                        <div className="dashboard-project-card-content">

                                            <h3>
                                                {project.title}
                                            </h3>

                                            <p>
                                                {project.description ||
                                                    "No description provided."}
                                            </p>

                                        </div>


                                        {/* Footer */}

                                        <div className="dashboard-project-footer">

                                            <div>

                                                <span>
                                                    Created
                                                </span>

                                                <strong>
                                                    {new Date(
                                                        project.createdAt
                                                    ).toLocaleDateString(
                                                        undefined,
                                                        {
                                                            day: "2-digit",
                                                            month: "short",
                                                            year: "numeric"
                                                        }
                                                    )}
                                                </strong>

                                            </div>


                                            <button
                                                onClick={() =>
                                                    openProject(
                                                        project.id
                                                    )
                                                }
                                            >

                                                Open

                                                <ArrowRight
                                                    size={16}
                                                />

                                            </button>

                                        </div>

                                    </article>

                                )
                            )}


                            {/* Create another card */}

                            <button
                                className="dashboard-add-project-card"
                                onClick={() =>
                                    navigate("/projects/new")
                                }
                            >

                                <div className="dashboard-add-project-icon">

                                    <Plus size={23} />

                                </div>


                                <strong>
                                    New Research Project
                                </strong>


                                <span>
                                    Start another research workspace
                                </span>


                                <ArrowRight
                                    size={17}
                                    className="dashboard-add-project-arrow"
                                />

                            </button>

                        </div>

                    )}

            </main>


            {/* =====================================================
                FOOTER
            ===================================================== */}

            <footer className="dashboard-footer">

                <span>
                    AI Research Paper Assistant
                </span>

                <span className="dashboard-footer-dot">
                    •
                </span>

                <span>
                    RAG-powered research workspace
                </span>

            </footer>

        </div>
    );
}

export default Dashboard;