import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

function Dashboard() {
    const navigate = useNavigate();

    const [projects, setProjects] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const fetchProjects = async () => {
            const token = localStorage.getItem("token");

            if (!token) {
                navigate("/");
                return;
            }

            try {
                const response = await fetch(
                    "http://localhost:8081/api/projects",
                    {
                        method: "GET",
                        headers: {
                            Authorization: `Bearer ${token}`,
                        },
                    }
                );

                if (response.status === 401 || response.status === 403) {
                    localStorage.removeItem("token");
                    navigate("/");
                    return;
                }

                if (!response.ok) {
                    throw new Error("Failed to load projects");
                }

                const data = await response.json();
                setProjects(data);

            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchProjects();
    }, [navigate]);

    const handleLogout = () => {
        localStorage.removeItem("token");
        navigate("/");
    };

    const openProject = (projectId) => {
        navigate(`/projects/${projectId}`);
    };

    return (
        <div className="dashboard-page">

            <header className="dashboard-header">

                <div>
                    <h1>AI Research Paper Assistant</h1>
                    <p>Manage your research projects</p>
                </div>

                <button
                    className="logout-button"
                    onClick={handleLogout}
                >
                    Logout
                </button>

            </header>

            <main className="dashboard-content">

                <div className="dashboard-title-row">
                    <div>
                        <h2>My Research Projects</h2>
                        <p>
                            Your saved research projects
                        </p>
                    </div>

                    <button
    className="new-project-button"
    onClick={() => navigate("/projects/new")}
>
    + New Project
</button>
                </div>

                {loading && (
                    <div className="status-message">
                        Loading projects...
                    </div>
                )}

                {error && (
                    <div className="error-message">
                        {error}
                    </div>
                )}

                {!loading && !error && projects.length === 0 && (
                    <div className="empty-state">
                        <h3>No research projects yet</h3>
                        <p>
                            Create your first research project
                            to get started.
                        </p>
                    </div>
                )}

                {!loading && !error && projects.length > 0 && (
                    <div className="projects-grid">

                        {projects.map((project) => (
                            <div
                                className="project-card"
                                key={project.id}
                            >

                                <h3>{project.title}</h3>

                                <p>
                                    {project.description ||
                                        "No description provided."}
                                </p>

                                <div className="project-card-footer">

                                    <span>
                                        Created{" "}
                                        {new Date(
                                            project.createdAt
                                        ).toLocaleDateString()}
                                    </span>

                                    <button
                                        onClick={() =>
                                            openProject(project.id)
                                        }
                                    >
                                        Open Project →
                                    </button>

                                </div>

                            </div>
                        ))}

                    </div>
                )}

            </main>

        </div>
    );
}

export default Dashboard;