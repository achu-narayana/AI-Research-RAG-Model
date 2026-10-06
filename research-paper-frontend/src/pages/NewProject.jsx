import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../api";

function NewProject() {

    const navigate = useNavigate();

    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");

    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const handleCreateProject = async (e) => {

        e.preventDefault();

        setError("");
        setLoading(true);

        try {

            await apiFetch("/api/projects", {
                method: "POST",
                body: {
                    title,
                    description,
                },
            });

            // Project created successfully
            navigate("/dashboard");

        } catch (err) {

            setError(err.message || "Failed to create project");

        } finally {

            setLoading(false);
        }
    };

    return (
        <div className="new-project-page">

            <div className="new-project-card">

                <h1>Create Research Project</h1>

                <p className="new-project-subtitle">
                    Create a workspace for your research papers
                    and AI conversations.
                </p>

                <form onSubmit={handleCreateProject}>

                    <label htmlFor="project-title">Project Title</label>

                    <input
                        id="project-title"
                        type="text"
                        placeholder="Enter project title"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        required
                    />

                    <label htmlFor="project-description">Description</label>

                    <textarea
                        id="project-description"
                        placeholder="Describe your research project"
                        value={description}
                        onChange={(e) =>
                            setDescription(e.target.value)
                        }
                        rows="5"
                    />

                    {error && (
                        <p className="error-message">
                            {error}
                        </p>
                    )}

                    <div className="new-project-actions">

                        <button
                            type="button"
                            className="cancel-button"
                            onClick={() => navigate("/dashboard")}
                        >
                            Cancel
                        </button>

                        <button
                            type="submit"
                            className="create-project-button"
                            disabled={loading}
                        >
                            {loading
                                ? "Creating..."
                                : "Create Project"}
                        </button>

                    </div>

                </form>

            </div>

        </div>
    );
}

export default NewProject;