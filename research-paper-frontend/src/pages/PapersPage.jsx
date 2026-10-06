import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { apiFetch, isAbortError } from "../api";

function getStatusInfo(status) {
    const normalized = (status || "").toUpperCase();

    if (normalized === "PROCESSING") {
        return { label: "Processing…", className: "status-processing" };
    }

    if (normalized === "READY" || normalized === "UPLOADED") {
        return { label: "Ready", className: "status-ready" };
    }

    return {
        label: status,
        className: `status-${normalized.toLowerCase()}`,
    };
}

function PapersPage() {
    const { projectId } = useParams();
    const navigate = useNavigate();
    const fileInputRef = useRef(null);

    const [papers, setPapers] = useState([]);
    const [selectedFiles, setSelectedFiles] = useState([]);
    const [loading, setLoading] = useState(true);
    const [uploading, setUploading] = useState(false);
    const [deletingId, setDeletingId] = useState(null);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    // Incrementing this re-runs the effect below to reload the list.
    const [reloadKey, setReloadKey] = useState(0);

    useEffect(() => {
        // State is reset on project change because App.jsx
        // remounts this page with key={projectId}.
        const controller = new AbortController();

        const fetchPapers = async () => {
            try {
                const data = await apiFetch(
                    `/api/projects/${projectId}/papers`,
                    {
                        method: "GET",
                        signal: controller.signal,
                    }
                );

                setPapers(Array.isArray(data) ? data : []);
                setError("");
                setLoading(false);

            } catch (err) {
                if (isAbortError(err)) {
                    return;
                }

                setError(err.message || "Failed to load papers");
                setLoading(false);
            }
        };

        fetchPapers();

        return () => controller.abort();
    }, [projectId, reloadKey]);

    const handleFileSelection = (event) => {
        setError("");
        setSuccess("");

        const newFiles = Array.from(event.target.files || []);

        if (newFiles.length === 0) {
            return;
        }

        // Check total selection count
        if (selectedFiles.length + newFiles.length > 5) {
            setError("You can select a maximum of 5 PDFs at a time.");
            event.target.value = "";
            return;
        }

        // Check file type
        const invalidFile = newFiles.find(
            (file) =>
                file.type !== "application/pdf" &&
                !file.name.toLowerCase().endsWith(".pdf")
        );

        if (invalidFile) {
            setError("Only PDF files are allowed.");
            event.target.value = "";
            return;
        }

        // Avoid adding the same file twice
        const uniqueNewFiles = newFiles.filter((newFile) => {
            return !selectedFiles.some(
                (existingFile) =>
                    existingFile.name === newFile.name &&
                    existingFile.size === newFile.size &&
                    existingFile.lastModified === newFile.lastModified
            );
        });

        setSelectedFiles((currentFiles) => [
            ...currentFiles,
            ...uniqueNewFiles,
        ]);

        // Allow selecting the same file again later
        event.target.value = "";
    };

    const handleUpload = async () => {
        if (selectedFiles.length === 0) {
            setError("Please select at least one PDF.");
            return;
        }

        setUploading(true);
        setError("");
        setSuccess("");

        try {
            const formData = new FormData();

            selectedFiles.forEach((file) => {
                formData.append("files", file);
            });

            formData.append("projectId", projectId);

            const data = await apiFetch("/api/projects/multiple", {
                method: "POST",
                body: formData,
            });

            // Backend multiple-upload endpoint should return an array
            const uploadedPapers = Array.isArray(data)
                ? data
                : [];

            setSuccess(
                `${uploadedPapers.length} paper(s) uploaded successfully.`
            );

            // Clear selected files
            setSelectedFiles([]);

            if (fileInputRef.current) {
                fileInputRef.current.value = "";
            }

            // Reload papers from backend
            setReloadKey((key) => key + 1);

        } catch (err) {
            setError(err.message || "Failed to upload papers");
        } finally {
            setUploading(false);
        }
    };

    const handleDeletePaper = async (paper) => {
        const confirmed = window.confirm(
            `Delete "${paper.originalFileName}" from this project? ` +
            "This cannot be undone."
        );

        if (!confirmed) {
            return;
        }

        setError("");
        setSuccess("");
        setDeletingId(paper.documentId);

        try {
            await apiFetch(
                `/api/projects/${projectId}/papers/${paper.documentId}`,
                { method: "DELETE" }
            );

            setPapers((currentPapers) =>
                currentPapers.filter(
                    (item) => item.documentId !== paper.documentId
                )
            );
        } catch (err) {
            setError(err.message || "Failed to delete paper");
        } finally {
            setDeletingId(null);
        }
    };

    const handleRemoveSelectedFile = (index) => {
        setSelectedFiles((currentFiles) =>
            currentFiles.filter((_, i) => i !== index)
        );
    };

    return (
        <div className="papers-page">

            <header className="papers-header">

                <div>
                    <h1>Research Papers</h1>
                    <p>
                        Manage papers for this research project
                    </p>
                </div>

                <button
                    className="back-button"
                    onClick={() =>
                        navigate(`/projects/${projectId}`)
                    }
                >
                    ← Back to Project
                </button>

            </header>

            <main className="papers-content">

                {/* Upload section */}
                <section className="upload-card">

                    <div>
                        <h2>Upload Research Papers</h2>

                        <p>
                            Select up to 5 PDF files at a time.
                            You can add files in multiple selections
                            before uploading.
                        </p>
                    </div>

                    <input
                        ref={fileInputRef}
                        type="file"
                        accept=".pdf,application/pdf"
                        multiple
                        onChange={handleFileSelection}
                    />

                    {selectedFiles.length > 0 && (
                        <div className="selected-files">

                            <h3>
                                Selected Files ({selectedFiles.length}/5)
                            </h3>

                            {selectedFiles.map((file, index) => (
                                <div
                                    className="selected-file"
                                    key={`${file.name}-${file.size}-${file.lastModified}`}
                                >
                                    <span>
                                        📄 {file.name}
                                    </span>

                                    <button
                                        type="button"
                                        onClick={() =>
                                            handleRemoveSelectedFile(index)
                                        }
                                        disabled={uploading}
                                    >
                                        Remove
                                    </button>
                                </div>
                            ))}

                        </div>
                    )}

                    {error && (
                        <p className="papers-error">
                            {error}
                        </p>
                    )}

                    {success && (
                        <p className="papers-success">
                            {success}
                        </p>
                    )}

                    <button
                        type="button"
                        className="upload-button"
                        onClick={handleUpload}
                        disabled={
                            uploading ||
                            selectedFiles.length === 0
                        }
                    >
                        {uploading
                            ? "Uploading and processing..."
                            : `Upload ${selectedFiles.length || ""} ${
                                  selectedFiles.length === 1
                                      ? "Paper"
                                      : "Papers"
                              }`}
                    </button>

                </section>


                {/* Papers list */}
                <section className="papers-list-section">

                    <div className="papers-list-header">

                        <div>
                            <h2>Uploaded Papers</h2>

                            <p>
                                {papers.length} paper
                                {papers.length !== 1 ? "s" : ""}
                            </p>
                        </div>

                    </div>


                    {loading ? (

                        <div className="papers-status">
                            Loading papers...
                        </div>

                    ) : papers.length === 0 ? (

                        <div className="papers-empty">

                            <div className="empty-icon">
                                📄
                            </div>

                            <h3>
                                No papers uploaded yet
                            </h3>

                            <p>
                                Upload research papers to start
                                asking questions about them.
                            </p>

                        </div>

                    ) : (

                        <div className="papers-list">

                            {papers.map((paper) => {

                                const statusInfo = getStatusInfo(paper.status);

                                return (

                                <div
                                    className="paper-item"
                                    key={paper.id}
                                >

                                    <div className="paper-icon">
                                        📄
                                    </div>

                                    <div className="paper-details">

                                        <h3>
                                            {paper.originalFileName}
                                        </h3>

                                        <p>
                                            Uploaded{" "}
                                            {new Date(
                                                paper.uploadedAt
                                            ).toLocaleDateString()}
                                        </p>

                                    </div>

                                    <div className="paper-status">

                                        <span
                                            className={`status-badge ${statusInfo.className}`}
                                        >
                                            {statusInfo.label}
                                        </span>

                                        <button
                                            type="button"
                                            className="delete-button"
                                            onClick={() =>
                                                handleDeletePaper(paper)
                                            }
                                            disabled={
                                                deletingId === paper.documentId
                                            }
                                            aria-label={`Delete ${paper.originalFileName}`}
                                        >
                                            {deletingId === paper.documentId
                                                ? "Deleting..."
                                                : "Delete"}
                                        </button>

                                    </div>

                                </div>

                                );
                            })}

                        </div>

                    )}

                </section>

            </main>

        </div>
    );
}

export default PapersPage;