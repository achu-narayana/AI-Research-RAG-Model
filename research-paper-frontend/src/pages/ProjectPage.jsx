import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

function ProjectPage() {

    const { projectId } = useParams();
    const navigate = useNavigate();

    const [project, setProject] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {

        const fetchProject = async () => {

            const token = localStorage.getItem("token");

            if (!token) {
                navigate("/");
                return;
            }

            try {

                // We already have GET /api/projects,
                // so find the selected project from the user's projects.
                const response = await fetch(
                    "http://localhost:8081/api/projects",
                    {
                        headers: {
                            Authorization: `Bearer ${token}`,
                        },
                    }
                );

                if (response.status === 401 ||
                    response.status === 403) {

                    localStorage.removeItem("token");
                    navigate("/");
                    return;
                }

                if (!response.ok) {
                    throw new Error("Failed to load project");
                }

                const projects = await response.json();

                const selectedProject = projects.find(
                    (item) =>
                        String(item.id) === String(projectId)
                );

                if (!selectedProject) {
                    throw new Error("Project not found");
                }

                setProject(selectedProject);

            } catch (err) {

                setError(err.message);

            } finally {

                setLoading(false);
            }
        };

        fetchProject();

    }, [projectId, navigate]);

    const handleDownloadChatPdf = async () => {
    const token = localStorage.getItem("token");

    if (!token) {
        navigate("/");
        return;
    }

    try {
        const response = await fetch(
            `http://localhost:8081/api/projects/${projectId}/chat/pdf`,
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
            throw new Error("Failed to download chat PDF");
        }

        const blob = await response.blob();

        const url = window.URL.createObjectURL(blob);

        const link = document.createElement("a");
        link.href = url;
        link.download = "research-project-chat.pdf";

        document.body.appendChild(link);
        link.click();

        link.remove();
        window.URL.revokeObjectURL(url);

    } catch (error) {
        alert(error.message);
    }
};
    const handleLogout = () => {
        localStorage.removeItem("token");
        navigate("/");
    };


    if (loading) {
        return (
            <div className="project-status-page">
                <p>Loading project...</p>
            </div>
        );
    }


    if (error) {
        return (
            <div className="project-status-page">
                <div className="project-error-box">
                    <h2>Unable to load project</h2>
                    <p>{error}</p>

                    <button
                        onClick={() => navigate("/dashboard")}
                    >
                        Back to Dashboard
                    </button>
                </div>
            </div>
        );
    }


    return (
        <div className="project-page">

            {/* Header */}
            <header className="project-header">

                <div>
                    <h1>AI Research Paper Assistant</h1>
                    <p>Research Workspace</p>
                </div>

                <div className="project-header-actions">

                    <button
                        onClick={() => navigate("/dashboard")}
                    >
                        Dashboard
                    </button>

                    <button
                        onClick={handleLogout}
                    >
                        Logout
                    </button>

                </div>

            </header>


            {/* Main content */}
            <main className="project-content">

                {/* Project information */}
                <section className="project-info-card">

                    <div>

                        <p className="project-label">
                            RESEARCH PROJECT
                        </p>

                        <h2>{project.title}</h2>

                        <p className="project-description">
                            {project.description ||
                                "No description provided."}
                        </p>

                    </div>

                    <div className="project-created">

                        Created{" "}
                        {new Date(
                            project.createdAt
                        ).toLocaleDateString()}

                    </div>

                </section>


                {/* Workspace sections */}
                <section className="workspace-grid">

                    {/* Papers */}
                    <div className="workspace-card">

                        <div className="workspace-icon">
                            📄
                        </div>

                        <h3>Research Papers</h3>

                        <p>
                            Upload and manage the research papers
                            for this project.
                        </p>

                        <button
    className="workspace-button"
    onClick={() =>
        navigate(`/projects/${projectId}/papers`)
    }
>
    View Papers
</button>

                    </div>


                    {/* Chat */}
                    <div className="workspace-card">

                        <div className="workspace-icon">
                            💬
                        </div>

                        <h3>AI Research Chat</h3>

                        <p>
                            Ask questions about your uploaded
                            research papers using AI.
                        </p>

                        <button
    className="workspace-button"
    onClick={() =>
        navigate(`/projects/${projectId}/chat`)
    }
>
    Open Chat
</button>

                    </div>


                    {/* PDF */}
                    <div className="workspace-card">

                        <div className="workspace-icon">
                            📥
                        </div>

                        <h3>Chat PDF</h3>

                        <p>
                            Download your complete project chat
                            as a PDF document.
                        </p>

                        <button
    className="workspace-button"
    onClick={handleDownloadChatPdf}
>
    Download Chat PDF
</button>

                    </div>

                </section>

            </main>

        </div>
    );
}

export default ProjectPage;