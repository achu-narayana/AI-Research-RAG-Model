import { useState } from "react";
import { useNavigate } from "react-router-dom";

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

        const token = localStorage.getItem("token");

        if (!token) {
            navigate("/");
            return;
        }

        try {

            const response = await fetch(
                "http://localhost:8081/api/projects",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify({
                        title,
                        description,
                    }),
                }
            );

            if (response.status === 401 || response.status === 403) {
                localStorage.removeItem("token");
                navigate("/");
                return;
            }

            const data = await response.json();

            if (!response.ok) {
                throw new Error(
                    data.message || "Failed to create project"
                );
            }

            // Project created successfully
            navigate("/dashboard");

        } catch (err) {

            setError(err.message);

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

                    <label>Project Title</label>

                    <input
                        type="text"
                        placeholder="Enter project title"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        required
                    />

                    <label>Description</label>

                    <textarea
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