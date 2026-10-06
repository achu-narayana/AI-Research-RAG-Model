import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
    apiFetch,
    clearSession,
    downloadChatPdf,
    isAbortError,
} from "../api";

function ProjectPage() {

    const { projectId } = useParams();
    const navigate = useNavigate();

    const [project, setProject] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [pdfError, setPdfError] = useState("");
    const [downloadingPdf, setDownloadingPdf] = useState(false);

    useEffect(() => {

        // State is reset on project change because App.jsx
        // remounts this page with key={projectId}.
        const controller = new AbortController();

        const fetchProject = async () => {

            try {

                const data = await apiFetch(
                    `/api/projects/${projectId}`,
                    { signal: controller.signal }
                );

                if (!data) {
                    throw new Error("Project not found");
                }

                setProject(data);
                setLoading(false);

            } catch (err) {

                if (isAbortError(err)) {
                    return;
                }

                setError(err.message || "Failed to load project");
                setLoading(false);
            }
        };

        fetchProject();

        return () => controller.abort();

    }, [projectId]);

    const handleDownloadChatPdf = async () => {

        setPdfError("");
        setDownloadingPdf(true);

        try {
            await downloadChatPdf(projectId, project?.title);
        } catch (err) {
            setPdfError(err.message || "Failed to download chat PDF");
        } finally {
            setDownloadingPdf(false);
        }
    };

    const handleLogout = () => {
        clearSession();
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
    disabled={downloadingPdf}
>
    {downloadingPdf ? "Downloading..." : "Download Chat PDF"}
</button>

                        {pdfError && (
                            <p className="error-message" role="alert">
                                {pdfError}
                            </p>
                        )}

                    </div>

                </section>

            </main>

        </div>
    );
}

export default ProjectPage;