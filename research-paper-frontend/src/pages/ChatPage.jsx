import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import {
    ArrowLeft,
    ArrowUp,
    BookOpen,
    FileText,
    GitCompare,
    Layers3,
    Loader2,
    LogOut,
    MoreHorizontal,
    Plus,
    Sparkles,
    X,
} from "lucide-react";

function ChatPage() {
    const { projectId } = useParams();
    const navigate = useNavigate();

    const [messages, setMessages] = useState([]);
    const [papers, setPapers] = useState([]);

    const [selectedDocumentId, setSelectedDocumentId] = useState("");

    const [summaryDocumentId, setSummaryDocumentId] = useState("");
    const [showTools, setShowTools] = useState(false);
    const [showSummaryModal, setShowSummaryModal] = useState(false);

    const [compareDocumentId1, setCompareDocumentId1] = useState("");
    const [compareDocumentId2, setCompareDocumentId2] = useState("");
    const [showCompareModal, setShowCompareModal] = useState(false);

    const [message, setMessage] = useState("");

    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);
    const [summarizing, setSummarizing] = useState(false);
    const [comparing, setComparing] = useState(false);
    const [error, setError] = useState("");

    const chatEndRef = useRef(null);
    const textareaRef = useRef(null);

    const token = localStorage.getItem("token");

    const busy = sending || summarizing || comparing;

    const selectedPaperName = useMemo(() => {
        if (!selectedDocumentId) {
            return "All papers in this project";
        }

        return (
            papers.find(
                (paper) => paper.documentId === selectedDocumentId
            )?.originalFileName || "Selected paper"
        );
    }, [papers, selectedDocumentId]);

    const formatMessageTime = (createdAt) => {
        if (!createdAt) {
            return "";
        }

        const date = new Date(createdAt);

        if (Number.isNaN(date.getTime())) {
            return "";
        }

        return date.toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
        });
    };

    useEffect(() => {
        if (!token) {
            navigate("/");
            return;
        }

        const loadData = async () => {
            try {
                setLoading(true);
                setError("");

                const [chatResponse, papersResponse] = await Promise.all([
                    fetch(
                        `http://localhost:8081/api/projects/${projectId}/chat/messages`,
                        {
                            method: "GET",
                            headers: {
                                Authorization: `Bearer ${token}`,
                            },
                        }
                    ),
                    fetch(
                        `http://localhost:8081/api/projects/${projectId}/papers`,
                        {
                            method: "GET",
                            headers: {
                                Authorization: `Bearer ${token}`,
                            },
                        }
                    ),
                ]);

                if (
                    chatResponse.status === 401 ||
                    chatResponse.status === 403 ||
                    papersResponse.status === 401 ||
                    papersResponse.status === 403
                ) {
                    localStorage.removeItem("token");
                    navigate("/");
                    return;
                }

                if (!chatResponse.ok) {
                    throw new Error("Failed to load chat history");
                }

                if (!papersResponse.ok) {
                    throw new Error("Failed to load research papers");
                }

                const chatData = await chatResponse.json();
                const papersData = await papersResponse.json();

                setMessages(chatData);
                setPapers(papersData);
                setSelectedDocumentId("");

                if (papersData.length > 0) {
                    setSummaryDocumentId(papersData[0].documentId);
                } else {
                    setSummaryDocumentId("");
                }
            } catch (err) {
                setError(
                    err.message || "Failed to load research chat"
                );
            } finally {
                setLoading(false);
            }
        };

        loadData();
    }, [projectId, navigate, token]);

    useEffect(() => {
        chatEndRef.current?.scrollIntoView({
            behavior: "smooth",
            block: "end",
        });
    }, [messages, sending, summarizing, comparing]);

    useEffect(() => {
        if (textareaRef.current) {
            textareaRef.current.style.height = "auto";
            textareaRef.current.style.height = `${Math.min(
                textareaRef.current.scrollHeight,
                180
            )}px`;
        }
    }, [message]);

    const sendMessage = async () => {
        const trimmedMessage = message.trim();

        if (!trimmedMessage || busy) {
            return;
        }

        setError("");

        const temporaryUserMessage = {
            id: `temp-${Date.now()}`,
            role: "USER",
            content: trimmedMessage,
            documentId: selectedDocumentId || null,
            createdAt: new Date().toISOString(),
        };

        setMessages((prev) => [...prev, temporaryUserMessage]);
        setMessage("");
        setSending(true);

        try {
            const response = await fetch(
                `http://localhost:8081/api/projects/${projectId}/chat/messages`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify({
                        message: trimmedMessage,
                        documentId: selectedDocumentId || null,
                    }),
                }
            );

            if (response.status === 401 || response.status === 403) {
                localStorage.removeItem("token");
                navigate("/");
                return;
            }

            if (!response.ok) {
                let errorMessage = "Failed to send message";

                try {
                    const errorData = await response.json();
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                } catch {
                    // Ignore JSON parsing error.
                }

                throw new Error(errorMessage);
            }

            const assistantMessage = await response.json();

            setMessages((prev) => [...prev, assistantMessage]);
        } catch (err) {
            setError(err.message || "Failed to send message");
        } finally {
            setSending(false);
        }
    };

    const openSummaryTool = () => {
        setError("");
        setShowTools(false);

        if (!papers.length) {
            setError(
                "Please upload at least one research paper before summarizing."
            );
            return;
        }

        const defaultPaper =
            papers.find(
                (paper) => paper.documentId === selectedDocumentId
            ) || papers[0];

        setSummaryDocumentId(defaultPaper.documentId);
        setShowSummaryModal(true);
    };

    const handleSummarize = async () => {
        if (!summaryDocumentId) {
            setError("Please select a research paper.");
            return;
        }

        const selectedPaper = papers.find(
            (paper) => paper.documentId === summaryDocumentId
        );

        if (!selectedPaper) {
            setError("Selected research paper was not found.");
            return;
        }

        setError("");
        setSummarizing(true);

        try {
            const response = await fetch(
                "http://localhost:8081/api/ai/summary",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify({
                        projectId: Number(projectId),
                        documentId: summaryDocumentId,
                    }),
                }
            );

            if (response.status === 401 || response.status === 403) {
                localStorage.removeItem("token");
                navigate("/");
                return;
            }

            if (!response.ok) {
                let errorMessage = "Failed to generate summary";

                try {
                    const errorData = await response.json();
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    } else if (errorData.error) {
                        errorMessage = errorData.error;
                    }
                } catch {
                    // Ignore JSON parsing error.
                }

                throw new Error(errorMessage);
            }

            const rawData = await response.json();
            let summaryData = rawData;

            if (typeof rawData === "string") {
                try {
                    summaryData = JSON.parse(rawData);
                } catch {
                    summaryData = { summary: rawData };
                }
            }

            const summaryText =
                summaryData?.summary ||
                summaryData?.message ||
                summaryData?.result ||
                "No summary was returned by the AI service.";

            const summaryMessage = {
                id: `summary-${Date.now()}`,
                role: "ASSISTANT",
                content: summaryText,
                documentId: summaryDocumentId,
                createdAt: new Date().toISOString(),
                isSummary: true,
            };

            setMessages((prev) => [...prev, summaryMessage]);
            setShowSummaryModal(false);
        } catch (err) {
            setError(
                err.message || "Failed to generate summary"
            );
        } finally {
            setSummarizing(false);
        }
    };

    const openCompareTool = () => {
        setError("");
        setShowTools(false);

        if (papers.length < 2) {
            setError(
                "Please upload at least two research papers before comparing."
            );
            return;
        }

        const defaultPaper1 =
            papers.find(
                (paper) => paper.documentId === selectedDocumentId
            ) || papers[0];

        const defaultPaper2 =
            papers.find(
                (paper) => paper.documentId !== defaultPaper1.documentId
            ) || papers[1];

        setCompareDocumentId1(defaultPaper1.documentId);
        setCompareDocumentId2(defaultPaper2.documentId);
        setShowCompareModal(true);
    };

    const handleCompare = async () => {
        if (!compareDocumentId1 || !compareDocumentId2) {
            setError("Please select two research papers.");
            return;
        }

        if (compareDocumentId1 === compareDocumentId2) {
            setError("Please select two different research papers.");
            return;
        }

        const selectedPaper1 = papers.find(
            (paper) => paper.documentId === compareDocumentId1
        );

        const selectedPaper2 = papers.find(
            (paper) => paper.documentId === compareDocumentId2
        );

        if (!selectedPaper1 || !selectedPaper2) {
            setError(
                "One or both selected research papers were not found."
            );
            return;
        }

        setError("");
        setComparing(true);

        try {
            const response = await fetch(
                "http://localhost:8081/api/ai/compare",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify({
                        projectId: Number(projectId),
                        documentId1: compareDocumentId1,
                        documentId2: compareDocumentId2,
                    }),
                }
            );

            if (response.status === 401 || response.status === 403) {
                localStorage.removeItem("token");
                navigate("/");
                return;
            }

            if (!response.ok) {
                let errorMessage = "Failed to compare research papers";

                try {
                    const errorData = await response.json();
                    if (errorData.message) {
                        errorMessage = errorData.message;
                    } else if (errorData.error) {
                        errorMessage = errorData.error;
                    }
                } catch {
                    // Ignore JSON parsing error.
                }

                throw new Error(errorMessage);
            }

            const rawData = await response.json();
            let comparisonData = rawData;

            if (typeof rawData === "string") {
                try {
                    comparisonData = JSON.parse(rawData);
                } catch {
                    comparisonData = { comparison: rawData };
                }
            }

            const comparisonText =
                comparisonData?.comparison ||
                comparisonData?.message ||
                comparisonData?.result ||
                "No comparison was returned by the AI service.";

            const comparisonMessage = {
                id: `comparison-${Date.now()}`,
                role: "ASSISTANT",
                content: comparisonText,
                documentId: null,
                createdAt: new Date().toISOString(),
                isComparison: true,
                comparisonDocumentId1: compareDocumentId1,
                comparisonDocumentId2: compareDocumentId2,
            };

            setMessages((prev) => [...prev, comparisonMessage]);
            setShowCompareModal(false);
        } catch (err) {
            setError(
                err.message || "Failed to compare research papers"
            );
        } finally {
            setComparing(false);
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    };

    const handleLogout = () => {
        localStorage.removeItem("token");
        navigate("/");
    };

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

        console.log("PDF response status:", response.status);

        if (!response.ok) {
            const errorText = await response.text();

            console.log("PDF error response:", errorText);

            throw new Error(
                `PDF download failed (${response.status})`
            );
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

    if (loading) {
        return (
            <div className="chat-loading-page">
                <div className="chat-loading-card">
                    <div className="chat-loading-icon">
                        <Sparkles size={21} />
                    </div>
                    <Loader2 className="chat-loading-spinner" size={18} />
                    <strong>Loading research workspace</strong>
                    <span>Preparing your papers and chat history...</span>
                </div>
            </div>
        );
    }

    return (
        <div className="chat-page">
            <div className="chat-page-grid"></div>
            <div className="chat-page-orb chat-page-orb-one"></div>
            <div className="chat-page-orb chat-page-orb-two"></div>

            <header className="chat-header">
                <div className="chat-header-brand">
                    <div className="chat-header-brand-icon">✦</div>

                    <div>
                        <p className="chat-header-kicker">RESEARCH AI</p>
                        <h1>Research Chat</h1>
                        <span>Project #{projectId}</span>
                    </div>
                </div>

                <div className="chat-header-actions">
                    <button
                        className="chat-header-button chat-header-button-secondary"
                        onClick={() => navigate(`/projects/${projectId}`)}
                    >
                        <ArrowLeft size={15} />
                        <span>Project</span>
                    </button>

                    <button
                        className="chat-header-button chat-header-button-secondary"
                        onClick={handleDownloadChatPdf}
                    >
                        <FileText size={15} />
                        <span>Export PDF</span>
                    </button>

                    <button
                        className="chat-header-button chat-header-button-dark"
                        onClick={handleLogout}
                    >
                        <LogOut size={15} />
                        <span>Logout</span>
                    </button>
                </div>
            </header>

            <main className="chat-main">
                <section className="chat-intro">
                    <div>
                        <div className="chat-intro-kicker">
                            <Sparkles size={13} />
                            RAG WORKSPACE
                        </div>

                        <h2>
                            Ask your papers.
                            <span>Understand faster.</span>
                        </h2>

                        <p>
                            Search your research library with natural language,
                            then summarize or compare papers without leaving the workspace.
                        </p>
                    </div>

                    <div className="chat-intro-status">
                        <div className="chat-status-dot"></div>
                        <div>
                            <strong>{papers.length}</strong>
                            <span>papers loaded</span>
                        </div>
                    </div>
                </section>

                <section className="chat-scope-card">
                    <div className="chat-scope-topline">
                        <div className="chat-scope-title-wrap">
                            <div className="chat-scope-icon">
                                <Layers3 size={18} />
                            </div>

                            <div>
                                <p className="chat-scope-kicker">SEARCH SCOPE</p>
                                <h3>Where should the AI search?</h3>
                            </div>
                        </div>

                        <span className="chat-scope-badge">
                            {selectedDocumentId ? "1 PAPER" : "ALL PAPERS"}
                        </span>
                    </div>

                    <div className="chat-scope-controls">
                        <div className="chat-scope-copy">
                            <span>
                                {selectedDocumentId
                                    ? "Questions will use only the selected paper."
                                    : "Questions can search across your entire project library."}
                            </span>
                        </div>

                        <select
                            className="chat-scope-select"
                            value={selectedDocumentId}
                            onChange={(e) =>
                                setSelectedDocumentId(e.target.value)
                            }
                            disabled={busy}
                        >
                            <option value="">All Papers</option>
                            {papers.map((paper) => (
                                <option
                                    key={paper.documentId}
                                    value={paper.documentId}
                                >
                                    {paper.originalFileName}
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="chat-scope-current">
                        <BookOpen size={13} />
                        <span>Using: {selectedPaperName}</span>
                    </div>
                </section>

                <section className="chat-workspace">
                    <div className="chat-workspace-toolbar">
                        <div className="chat-workspace-heading">
                            <span className="chat-workspace-live-dot"></span>
                            <span>Conversation</span>
                        </div>

                        <div className="chat-workspace-tool-count">
                            <span>{messages.length}</span>
                            messages
                        </div>
                    </div>

                    <div className="chat-box">
                        {messages.length === 0 ? (
                            <div className="chat-empty-state">
                                <div className="chat-empty-visual">
                                    <div className="chat-empty-icon">
                                        <Sparkles size={25} />
                                    </div>
                                    <div className="chat-empty-mark">?</div>
                                </div>

                                <p className="chat-empty-kicker">READY WHEN YOU ARE</p>

                                <h3>Start a research conversation</h3>

                                <p>
                                    Ask about a method, result, concept, finding,
                                    or any other detail in your uploaded papers.
                                </p>

                                <div className="chat-suggestion-row">
                                    <span>What is the main objective?</span>
                                    <span>Explain the methodology</span>
                                    <span>What are the key findings?</span>
                                </div>
                            </div>
                        ) : (
                            messages.map((msg) => {
                                const isUser = msg.role === "USER";
                                const isSummary = msg.isSummary === true;
                                const isComparison = msg.isComparison === true;

                                const selectedPaperNameForMessage = msg.documentId
                                    ? papers.find(
                                          (paper) =>
                                              paper.documentId === msg.documentId
                                      )?.originalFileName
                                    : null;

                                const comparisonPaperName1 =
                                    msg.comparisonDocumentId1
                                        ? papers.find(
                                              (paper) =>
                                                  paper.documentId ===
                                                  msg.comparisonDocumentId1
                                          )?.originalFileName
                                        : null;

                                const comparisonPaperName2 =
                                    msg.comparisonDocumentId2
                                        ? papers.find(
                                              (paper) =>
                                                  paper.documentId ===
                                                  msg.comparisonDocumentId2
                                          )?.originalFileName
                                        : null;

                                return (
                                    <article
                                        key={msg.id}
                                        className={`chat-message-row ${
                                            isUser
                                                ? "chat-message-row-user"
                                                : "chat-message-row-assistant"
                                        }`}
                                    >
                                        <div
                                            className={`chat-message-card ${
                                                isUser
                                                    ? "chat-message-user"
                                                    : isComparison
                                                      ? "chat-message-comparison"
                                                      : isSummary
                                                        ? "chat-message-summary"
                                                        : "chat-message-assistant"
                                            }`}
                                        >
                                            <div className="chat-message-meta">
                                                <div className="chat-message-author">
                                                    {isUser ? (
                                                        <span className="chat-avatar chat-avatar-user">
                                                            Y
                                                        </span>
                                                    ) : isComparison ? (
                                                        <span className="chat-avatar chat-avatar-comparison">
                                                            <GitCompare size={13} />
                                                        </span>
                                                    ) : isSummary ? (
                                                        <span className="chat-avatar chat-avatar-summary">
                                                            <Sparkles size={13} />
                                                        </span>
                                                    ) : (
                                                        <span className="chat-avatar chat-avatar-ai">
                                                            ✦
                                                        </span>
                                                    )}

                                                    <span>
                                                        {isUser
                                                            ? "You"
                                                            : isComparison
                                                              ? "AI Comparison"
                                                              : isSummary
                                                                ? "AI Summary"
                                                                : "AI Assistant"}
                                                    </span>
                                                </div>

                                                <span className="chat-message-time">
                                                    {formatMessageTime(msg.createdAt)}
                                                </span>
                                            </div>

                                            {selectedPaperNameForMessage && (
                                                <div className="chat-message-source">
                                                    <FileText size={12} />
                                                    <span>
                                                        {selectedPaperNameForMessage}
                                                    </span>
                                                </div>
                                            )}

                                            {isComparison &&
                                                (comparisonPaperName1 ||
                                                    comparisonPaperName2) && (
                                                    <div className="chat-message-source chat-message-comparison-source">
                                                        <GitCompare size={12} />
                                                        <span>
                                                            {comparisonPaperName1 ||
                                                                "Paper 1"}
                                                            <b>↔</b>
                                                            {comparisonPaperName2 ||
                                                                "Paper 2"}
                                                        </span>
                                                    </div>
                                                )}

                                            <div className="chat-message-content">
                                                {isUser ? (
                                                    <p className="chat-user-text">
                                                        {msg.content}
                                                    </p>
                                                ) : (
                                                    <ReactMarkdown
                                                        remarkPlugins={[remarkGfm]}
                                                    >
                                                        {msg.content}
                                                    </ReactMarkdown>
                                                )}
                                            </div>
                                        </div>
                                    </article>
                                );
                            })
                        )}

                        {sending && (
                            <div className="chat-message-row chat-message-row-assistant">
                                <div className="chat-progress-message">
                                    <span className="chat-avatar chat-avatar-ai">✦</span>
                                    <div>
                                        <strong>AI is thinking</strong>
                                        <span className="chat-typing-dots">
                                            <i></i>
                                            <i></i>
                                            <i></i>
                                        </span>
                                    </div>
                                </div>
                            </div>
                        )}

                        {summarizing && (
                            <div className="chat-message-row chat-message-row-assistant">
                                <div className="chat-progress-message chat-progress-summary">
                                    <span className="chat-avatar chat-avatar-summary">
                                        <Sparkles size={13} />
                                    </span>
                                    <div>
                                        <strong>Preparing paper summary</strong>
                                        <span>
                                            Reading representative sections...
                                        </span>
                                    </div>
                                </div>
                            </div>
                        )}

                        {comparing && (
                            <div className="chat-message-row chat-message-row-assistant">
                                <div className="chat-progress-message chat-progress-comparison">
                                    <span className="chat-avatar chat-avatar-comparison">
                                        <GitCompare size={13} />
                                    </span>
                                    <div>
                                        <strong>Comparing research papers</strong>
                                        <span>
                                            Analysing the selected pair...
                                        </span>
                                    </div>
                                </div>
                            </div>
                        )}

                        <div ref={chatEndRef}></div>
                    </div>

                    {error && (
                        <div className="chat-error-banner">
                            <div className="chat-error-icon">!</div>
                            <span>{error}</span>
                        </div>
                    )}

                    <div className="chat-composer-shell">
                        <div className="chat-composer-topline">
                            <div className="chat-composer-context">
                                <BookOpen size={13} />
                                <span>
                                    {selectedDocumentId
                                        ? "Selected paper"
                                        : "Project library"}
                                </span>
                            </div>

                            <span className="chat-composer-shortcut">
                                Enter to send · Shift + Enter for a new line
                            </span>
                        </div>

                        <div className="chat-composer">
                            <div className="chat-tools-wrap">
                                <button
                                    className={`chat-tools-button ${
                                        showTools ? "is-open" : ""
                                    }`}
                                    onClick={() =>
                                        setShowTools((prev) => !prev)
                                    }
                                    disabled={busy || papers.length === 0}
                                    title={
                                        papers.length === 0
                                            ? "Upload a paper first"
                                            : "More tools"
                                    }
                                    aria-label="More tools"
                                >
                                    {showTools ? (
                                        <X size={19} />
                                    ) : (
                                        <Plus size={19} />
                                    )}
                                </button>

                                {showTools && (
                                    <div className="chat-tools-menu">
                                        <div className="chat-tools-menu-head">
                                            <div>
                                                <span>TOOLS</span>
                                                <strong>Research actions</strong>
                                            </div>
                                            <MoreHorizontal size={17} />
                                        </div>

                                        <button
                                            className="chat-tool-item"
                                            onClick={openSummaryTool}
                                        >
                                            <span className="chat-tool-item-icon chat-tool-item-summary">
                                                <FileText size={17} />
                                            </span>
                                            <span>
                                                <strong>Summarize Paper</strong>
                                                <small>
                                                    Generate a focused AI summary
                                                </small>
                                            </span>
                                            <ArrowUp size={14} />
                                        </button>

                                        <button
                                            className="chat-tool-item"
                                            onClick={openCompareTool}
                                        >
                                            <span className="chat-tool-item-icon chat-tool-item-comparison">
                                                <GitCompare size={17} />
                                            </span>
                                            <span>
                                                <strong>Compare Papers</strong>
                                                <small>
                                                    Compare two papers side by side
                                                </small>
                                            </span>
                                            <ArrowUp size={14} />
                                        </button>
                                    </div>
                                )}
                            </div>

                            <textarea
                                ref={textareaRef}
                                className="chat-composer-textarea"
                                value={message}
                                onChange={(e) => setMessage(e.target.value)}
                                onKeyDown={handleKeyDown}
                                placeholder={
                                    selectedDocumentId
                                        ? "Ask anything about the selected paper..."
                                        : "Ask anything about your research papers..."
                                }
                                rows={1}
                                disabled={busy}
                                aria-label="Research question"
                            />

                            <button
                                className="chat-send-button"
                                onClick={sendMessage}
                                disabled={busy || !message.trim()}
                            >
                                <span>
                                    {sending ? "Sending" : "Send"}
                                </span>
                                {sending ? (
                                    <Loader2 className="chat-send-spinner" size={16} />
                                ) : (
                                    <ArrowUp size={17} />
                                )}
                            </button>
                        </div>
                    </div>
                </section>
            </main>

            {showSummaryModal && (
                <div
                    className="chat-modal-overlay"
                    onMouseDown={(e) => {
                        if (e.target === e.currentTarget && !summarizing) {
                            setShowSummaryModal(false);
                        }
                    }}
                >
                    <div className="chat-modal chat-modal-summary">
                        <div className="chat-modal-accent chat-modal-accent-summary"></div>

                        <div className="chat-modal-header">
                            <div className="chat-modal-title-block">
                                <div className="chat-modal-icon chat-modal-icon-summary">
                                    <Sparkles size={20} />
                                </div>
                                <div>
                                    <p className="chat-modal-kicker">AI TOOL</p>
                                    <h3>Summarize Research Paper</h3>
                                    <p>
                                        Generate a structured overview of one
                                        uploaded paper.
                                    </p>
                                </div>
                            </div>

                            <button
                                className="chat-modal-close"
                                onClick={() => setShowSummaryModal(false)}
                                disabled={summarizing}
                                aria-label="Close summary dialog"
                            >
                                <X size={18} />
                            </button>
                        </div>

                        <div className="chat-modal-field">
                            <label htmlFor="summary-paper">
                                Research paper
                            </label>
                            <select
                                id="summary-paper"
                                value={summaryDocumentId}
                                onChange={(e) =>
                                    setSummaryDocumentId(e.target.value)
                                }
                                disabled={summarizing}
                            >
                                {papers.map((paper) => (
                                    <option
                                        key={paper.documentId}
                                        value={paper.documentId}
                                    >
                                        {paper.originalFileName}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {summaryDocumentId && (
                            <div className="chat-modal-selection chat-modal-selection-summary">
                                <BookOpen size={15} />
                                <div>
                                    <span>Selected paper</span>
                                    <strong>
                                        {papers.find(
                                            (paper) =>
                                                paper.documentId ===
                                                summaryDocumentId
                                        )?.originalFileName ||
                                            "Selected paper"}
                                    </strong>
                                </div>
                            </div>
                        )}

                        <div className="chat-modal-actions">
                            <button
                                className="chat-modal-button chat-modal-button-secondary"
                                onClick={() => setShowSummaryModal(false)}
                                disabled={summarizing}
                            >
                                Cancel
                            </button>

                            <button
                                className="chat-modal-button chat-modal-button-primary chat-modal-button-summary"
                                onClick={handleSummarize}
                                disabled={summarizing || !summaryDocumentId}
                            >
                                {summarizing ? (
                                    <>
                                        <Loader2
                                            className="chat-send-spinner"
                                            size={16}
                                        />
                                        Summarizing...
                                    </>
                                ) : (
                                    <>
                                        <Sparkles size={16} />
                                        Summarize
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {showCompareModal && (
                <div
                    className="chat-modal-overlay"
                    onMouseDown={(e) => {
                        if (e.target === e.currentTarget && !comparing) {
                            setShowCompareModal(false);
                        }
                    }}
                >
                    <div className="chat-modal chat-modal-compare">
                        <div className="chat-modal-accent chat-modal-accent-compare"></div>

                        <div className="chat-modal-header">
                            <div className="chat-modal-title-block">
                                <div className="chat-modal-icon chat-modal-icon-compare">
                                    <GitCompare size={20} />
                                </div>
                                <div>
                                    <p className="chat-modal-kicker">AI TOOL</p>
                                    <h3>Compare Research Papers</h3>
                                    <p>
                                        Select two different papers to compare
                                        their research and findings.
                                    </p>
                                </div>
                            </div>

                            <button
                                className="chat-modal-close"
                                onClick={() => setShowCompareModal(false)}
                                disabled={comparing}
                                aria-label="Close comparison dialog"
                            >
                                <X size={18} />
                            </button>
                        </div>

                        <div className="chat-compare-grid">
                            <div className="chat-modal-field">
                                <div className="chat-compare-label-row">
                                    <label htmlFor="compare-paper-one">
                                        Paper 1
                                    </label>
                                    <span>01</span>
                                </div>

                                <select
                                    id="compare-paper-one"
                                    value={compareDocumentId1}
                                    onChange={(e) =>
                                        setCompareDocumentId1(e.target.value)
                                    }
                                    disabled={comparing}
                                >
                                    {papers.map((paper) => (
                                        <option
                                            key={paper.documentId}
                                            value={paper.documentId}
                                        >
                                            {paper.originalFileName}
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="chat-compare-arrow">
                                <GitCompare size={16} />
                            </div>

                            <div className="chat-modal-field">
                                <div className="chat-compare-label-row">
                                    <label htmlFor="compare-paper-two">
                                        Paper 2
                                    </label>
                                    <span>02</span>
                                </div>

                                <select
                                    id="compare-paper-two"
                                    value={compareDocumentId2}
                                    onChange={(e) =>
                                        setCompareDocumentId2(e.target.value)
                                    }
                                    disabled={comparing}
                                >
                                    {papers.map((paper) => (
                                        <option
                                            key={paper.documentId}
                                            value={paper.documentId}
                                        >
                                            {paper.originalFileName}
                                        </option>
                                    ))}
                                </select>
                            </div>
                        </div>

                        {compareDocumentId1 && compareDocumentId2 && (
                            <div
                                className={`chat-compare-selection ${
                                    compareDocumentId1 === compareDocumentId2
                                        ? "is-invalid"
                                        : ""
                                }`}
                            >
                                <div className="chat-compare-selection-item">
                                    <span>A</span>
                                    <strong>
                                        {papers.find(
                                            (paper) =>
                                                paper.documentId ===
                                                compareDocumentId1
                                        )?.originalFileName || "Paper 1"}
                                    </strong>
                                </div>

                                <GitCompare size={17} />

                                <div className="chat-compare-selection-item">
                                    <span>B</span>
                                    <strong>
                                        {papers.find(
                                            (paper) =>
                                                paper.documentId ===
                                                compareDocumentId2
                                        )?.originalFileName || "Paper 2"}
                                    </strong>
                                </div>
                            </div>
                        )}

                        {compareDocumentId1 === compareDocumentId2 && (
                            <div className="chat-modal-inline-error">
                                Select two different papers.
                            </div>
                        )}

                        <div className="chat-modal-info">
                            <Sparkles size={15} />
                            <span>
                                The AI uses a limited set of representative
                                sections from each paper to keep comparisons focused.
                            </span>
                        </div>

                        <div className="chat-modal-actions">
                            <button
                                className="chat-modal-button chat-modal-button-secondary"
                                onClick={() => setShowCompareModal(false)}
                                disabled={comparing}
                            >
                                Cancel
                            </button>

                            <button
                                className="chat-modal-button chat-modal-button-primary chat-modal-button-compare"
                                onClick={handleCompare}
                                disabled={
                                    comparing ||
                                    !compareDocumentId1 ||
                                    !compareDocumentId2 ||
                                    compareDocumentId1 === compareDocumentId2
                                }
                            >
                                {comparing ? (
                                    <>
                                        <Loader2
                                            className="chat-send-spinner"
                                            size={16}
                                        />
                                        Comparing...
                                    </>
                                ) : (
                                    <>
                                        <GitCompare size={16} />
                                        Compare
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default ChatPage;
