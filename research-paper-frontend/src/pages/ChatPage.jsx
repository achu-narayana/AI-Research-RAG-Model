import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

function ChatPage() {

    const { projectId } = useParams();
    const navigate = useNavigate();

    const [messages, setMessages] = useState([]);
    const [papers, setPapers] = useState([]);

    const [selectedDocumentId, setSelectedDocumentId] = useState("");

    const [message, setMessage] = useState("");

    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);

    const [error, setError] = useState("");

    const chatEndRef = useRef(null);

    const token = localStorage.getItem("token");


    /*
     * Load chat history and project papers
     */
    useEffect(() => {

        if (!token) {
            navigate("/");
            return;
        }

        const loadData = async () => {

            try {

                setLoading(true);
                setError("");

                const [chatResponse, papersResponse] =
                    await Promise.all([

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


                /*
                 * Authentication check
                 */
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
                    throw new Error(
                        "Failed to load chat history"
                    );
                }


                if (!papersResponse.ok) {
                    throw new Error(
                        "Failed to load research papers"
                    );
                }


                const chatData =
                    await chatResponse.json();

                const papersData =
                    await papersResponse.json();


                setMessages(chatData);
                setPapers(papersData);


                /*
                 * Default = All Papers
                 */
                setSelectedDocumentId("");


            } catch (err) {

                setError(err.message);

            } finally {

                setLoading(false);
            }
        };


        loadData();

    }, [projectId, navigate]);


    /*
     * Automatically scroll to latest message
     */
    useEffect(() => {

        chatEndRef.current?.scrollIntoView({
            behavior: "smooth",
        });

    }, [messages, sending]);


    /*
     * Send question
     */
    const sendMessage = async () => {

        const trimmedMessage =
            message.trim();


        if (!trimmedMessage || sending) {
            return;
        }


        setError("");


        /*
         * Show user's message immediately
         */
        const temporaryUserMessage = {

            id: `temp-${Date.now()}`,

            role: "USER",

            content: trimmedMessage,

            documentId:
                selectedDocumentId || null,

            createdAt:
                new Date().toISOString(),
        };


        setMessages((prev) => [

            ...prev,

            temporaryUserMessage,

        ]);


        setMessage("");
        setSending(true);


        try {

            const response = await fetch(

                `http://localhost:8081/api/projects/${projectId}/chat/messages`,

                {

                    method: "POST",

                    headers: {

                        "Content-Type":
                            "application/json",

                        Authorization:
                            `Bearer ${token}`,
                    },

                    body: JSON.stringify({

                        message: trimmedMessage,

                        /*
                         * null = all papers
                         * documentId = selected paper
                         */
                        documentId:
                            selectedDocumentId || null,
                    }),
                }
            );


            if (
                response.status === 401 ||
                response.status === 403
            ) {

                localStorage.removeItem("token");

                navigate("/");

                return;
            }


            if (!response.ok) {

                let errorMessage =
                    "Failed to send message";


                try {

                    const errorData =
                        await response.json();


                    if (errorData.message) {

                        errorMessage =
                            errorData.message;
                    }

                } catch {

                    // Ignore JSON parsing error

                }


                throw new Error(errorMessage);
            }


            const assistantMessage =
                await response.json();


            setMessages((prev) => [

                ...prev,

                assistantMessage,

            ]);


        } catch (err) {

            setError(err.message);

        } finally {

            setSending(false);
        }
    };


    /*
     * Enter = send
     * Shift + Enter = new line
     */
    const handleKeyDown = (e) => {

        if (
            e.key === "Enter" &&
            !e.shiftKey
        ) {

            e.preventDefault();

            sendMessage();
        }
    };


    /*
     * Logout
     */
    const handleLogout = () => {

        localStorage.removeItem("token");

        navigate("/");
    };


    /*
     * Download chat PDF
     */
    const handleDownloadChatPdf = async () => {

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

                        Authorization:
                            `Bearer ${token}`,
                    },
                }
            );


            if (
                response.status === 401 ||
                response.status === 403
            ) {

                localStorage.removeItem("token");

                navigate("/");

                return;
            }


            if (!response.ok) {

                throw new Error(
                    "Failed to download chat PDF"
                );
            }


            const blob =
                await response.blob();


            const url =
                window.URL.createObjectURL(blob);


            const link =
                document.createElement("a");


            link.href = url;

            link.download =
                "research-project-chat.pdf";


            document.body.appendChild(link);

            link.click();

            link.remove();

            window.URL.revokeObjectURL(url);


        } catch (err) {

            setError(err.message);
        }
    };


    if (loading) {

        return (

            <div
                style={{
                    minHeight: "100vh",
                    display: "flex",
                    justifyContent: "center",
                    alignItems: "center",
                    background: "#f5f7fb",
                }}
            >

                <p>
                    Loading research chat...
                </p>

            </div>
        );
    }


    return (

        <div
            style={{
                minHeight: "100vh",
                background: "#f5f7fb",
                display: "flex",
                flexDirection: "column",
            }}
        >

            {/* ================= HEADER ================= */}

            <header
                style={{
                    background: "#ffffff",
                    borderBottom:
                        "1px solid #e5e7eb",

                    padding: "18px 32px",

                    display: "flex",

                    justifyContent:
                        "space-between",

                    alignItems: "center",
                }}
            >

                <div>

                    <h1
                        style={{
                            margin: 0,
                            fontSize: "22px",
                        }}
                    >
                        AI Research Paper Assistant
                    </h1>

                    <p
                        style={{
                            margin: "4px 0 0",
                            color: "#6b7280",
                            fontSize: "14px",
                        }}
                    >
                        AI Research Chat
                    </p>

                </div>


                <div
                    style={{
                        display: "flex",
                        gap: "10px",
                    }}
                >

                    <button
                        onClick={() =>
                            navigate(
                                `/projects/${projectId}`
                            )
                        }

                        style={headerButtonStyle}
                    >
                        Back to Project
                    </button>


                    <button
                        onClick={handleDownloadChatPdf}

                        style={headerButtonStyle}
                    >
                        Download Chat PDF
                    </button>


                    <button
                        onClick={handleLogout}

                        style={headerButtonStyle}
                    >
                        Logout
                    </button>

                </div>

            </header>


            {/* ================= MAIN ================= */}

            <main
                style={{
                    flex: 1,
                    width: "100%",
                    maxWidth: "1000px",
                    margin: "0 auto",
                    padding: "28px 20px",
                    display: "flex",
                    flexDirection: "column",
                    boxSizing: "border-box",
                }}
            >

                {/* Title */}

                <div
                    style={{
                        marginBottom: "18px",
                    }}
                >

                    <h2
                        style={{
                            margin: 0,
                            fontSize: "26px",
                        }}
                    >
                        Research Chat
                    </h2>

                    <p
                        style={{
                            color: "#6b7280",
                            marginTop: "6px",
                        }}
                    >
                        Ask questions about your
                        uploaded research papers.
                    </p>

                </div>


                {/* ================= RESEARCH SCOPE ================= */}

                <div
                    style={{
                        background: "#ffffff",
                        border:
                            "1px solid #e5e7eb",

                        borderRadius: "12px",

                        padding: "16px 18px",

                        marginBottom: "14px",
                    }}
                >

                    <div
                        style={{
                            display: "flex",
                            alignItems: "center",
                            justifyContent:
                                "space-between",

                            gap: "20px",

                            flexWrap: "wrap",
                        }}
                    >

                        <div>

                            <div
                                style={{
                                    fontWeight: "600",
                                    color: "#111827",
                                    marginBottom: "4px",
                                }}
                            >
                                Research Scope
                            </div>

                            <div
                                style={{
                                    fontSize: "13px",
                                    color: "#6b7280",
                                }}
                            >
                                Choose whether the AI should
                                search all papers or one
                                specific paper.
                            </div>

                        </div>


                        <select
                            value={
                                selectedDocumentId
                            }

                            onChange={(e) =>
                                setSelectedDocumentId(
                                    e.target.value
                                )
                            }

                            style={{
                                minWidth: "280px",
                                maxWidth: "100%",
                                padding:
                                    "10px 12px",

                                border:
                                    "1px solid #d1d5db",

                                borderRadius: "8px",

                                background:
                                    "#ffffff",

                                fontSize: "14px",

                                color:
                                    "#111827",

                                cursor:
                                    "pointer",
                            }}
                        >

                            {/* All papers */}

                            <option value="">
                                All Papers
                            </option>


                            {/* Individual papers */}

                            {papers.map((paper) => (

                                <option
                                    key={
                                        paper.documentId
                                    }

                                    value={
                                        paper.documentId
                                    }
                                >
                                    {paper.originalFileName}
                                </option>

                            ))}

                        </select>

                    </div>


                    {/* Current selection */}

                    <div
                        style={{
                            marginTop: "10px",
                            fontSize: "13px",
                            color: "#2563eb",
                            fontWeight: "500",
                        }}
                    >

                        {selectedDocumentId
                            ? `Using: ${
                                papers.find(
                                    (paper) =>
                                        paper.documentId ===
                                        selectedDocumentId
                                )?.originalFileName ||
                                "Selected paper"
                            }`
                            : "Using: All papers in this project"}

                    </div>

                </div>


                {/* ================= CHAT BOX ================= */}

                <div
                    style={{
                        flex: 1,
                        minHeight: "500px",
                        maxHeight:
                            "calc(100vh - 360px)",

                        overflowY: "auto",

                        background: "#ffffff",

                        border:
                            "1px solid #e5e7eb",

                        borderRadius: "14px",

                        padding: "22px",

                        boxSizing: "border-box",
                    }}
                >

                    {messages.length === 0 ? (

                        <div
                            style={{
                                textAlign: "center",
                                color: "#6b7280",
                                padding:
                                    "80px 20px",
                            }}
                        >

                            <div
                                style={{
                                    fontSize: "42px",
                                    marginBottom:
                                        "12px",
                                }}
                            >
                                💬
                            </div>


                            <h3
                                style={{
                                    marginBottom:
                                        "8px",

                                    color:
                                        "#374151",
                                }}
                            >
                                Start your research chat
                            </h3>


                            <p>
                                Select a research scope
                                and ask a question.
                            </p>

                        </div>

                    ) : (

                        messages.map((msg) => {

                            const isUser =
                                msg.role === "USER";


                            const selectedPaperName =
                                msg.documentId
                                    ? papers.find(
                                        (paper) =>
                                            paper.documentId ===
                                            msg.documentId
                                    )?.originalFileName
                                    : null;


                            return (

                                <div
                                    key={msg.id}

                                    style={{
                                        display:
                                            "flex",

                                        justifyContent:
                                            isUser
                                                ? "flex-end"
                                                : "flex-start",

                                        marginBottom:
                                            "18px",
                                    }}
                                >

                                    <div
                                        style={{
                                            maxWidth:
                                                "78%",

                                            background:
                                                isUser
                                                    ? "#2563eb"
                                                    : "#f1f5f9",

                                            color:
                                                isUser
                                                    ? "#ffffff"
                                                    : "#111827",

                                            padding:
                                                "13px 16px",

                                            borderRadius:
                                                "14px",

                                            borderBottomRightRadius:
                                                isUser
                                                    ? "4px"
                                                    : "14px",

                                            borderBottomLeftRadius:
                                                isUser
                                                    ? "14px"
                                                    : "4px",

                                            lineHeight:
                                                "1.6",

                                            whiteSpace:
                                                "pre-wrap",
                                        }}
                                    >

                                        <div
                                            style={{
                                                fontSize:
                                                    "12px",

                                                fontWeight:
                                                    "600",

                                                marginBottom:
                                                    "5px",

                                                opacity:
                                                    "0.75",
                                            }}
                                        >
                                            {isUser
                                                ? "You"
                                                : "AI Assistant"}
                                        </div>


                                        {/* Paper used */}

                                        {msg.documentId && (
                                            <div
                                                style={{
                                                    fontSize:
                                                        "11px",

                                                    marginBottom:
                                                        "7px",

                                                    opacity:
                                                        "0.7",
                                                }}
                                            >
                                                📄{" "}
                                                {selectedPaperName ||
                                                    "Selected paper"}
                                            </div>
                                        )}


                                        {msg.content}

                                    </div>

                                </div>
                            );
                        })

                    )}


                    {/* AI thinking */}

                    {sending && (

                        <div
                            style={{
                                display:
                                    "flex",

                                justifyContent:
                                    "flex-start",

                                marginBottom:
                                    "18px",
                            }}
                        >

                            <div
                                style={{
                                    background:
                                        "#f1f5f9",

                                    color:
                                        "#6b7280",

                                    padding:
                                        "13px 16px",

                                    borderRadius:
                                        "14px",
                                }}
                            >
                                AI is thinking...
                            </div>

                        </div>
                    )}


                    <div ref={chatEndRef} />

                </div>


                {/* ================= ERROR ================= */}

                {error && (

                    <div
                        style={{
                            marginTop:
                                "12px",

                            padding:
                                "12px 16px",

                            background:
                                "#fee2e2",

                            color:
                                "#b91c1c",

                            borderRadius:
                                "8px",
                        }}
                    >
                        {error}
                    </div>
                )}


                {/* ================= MESSAGE INPUT ================= */}

                <div
                    style={{
                        marginTop:
                            "14px",

                        display:
                            "flex",

                        gap: "10px",

                        alignItems:
                            "flex-end",
                    }}
                >

                    <textarea

                        value={message}

                        onChange={(e) =>
                            setMessage(
                                e.target.value
                            )
                        }

                        onKeyDown={
                            handleKeyDown
                        }

                        placeholder={
                            selectedDocumentId
                                ? "Ask a question about the selected paper..."
                                : "Ask a question about your research papers..."
                        }

                        rows={3}

                        disabled={sending}

                        style={{
                            flex: 1,

                            resize:
                                "none",

                            border:
                                "1px solid #d1d5db",

                            borderRadius:
                                "12px",

                            padding:
                                "13px 15px",

                            fontSize:
                                "15px",

                            outline:
                                "none",

                            fontFamily:
                                "inherit",

                            boxSizing:
                                "border-box",
                        }}
                    />


                    <button

                        onClick={
                            sendMessage
                        }

                        disabled={
                            sending ||
                            !message.trim()
                        }

                        style={{

                            height:
                                "48px",

                            padding:
                                "0 22px",

                            border:
                                "none",

                            borderRadius:
                                "10px",

                            background:
                                sending ||
                                !message.trim()
                                    ? "#9ca3af"
                                    : "#2563eb",

                            color:
                                "#ffffff",

                            fontWeight:
                                "600",

                            cursor:
                                sending ||
                                !message.trim()
                                    ? "not-allowed"
                                    : "pointer",
                        }}
                    >

                        {sending
                            ? "Sending..."
                            : "Send"}

                    </button>

                </div>


                <p
                    style={{
                        margin:
                            "8px 0 0",

                        color:
                            "#9ca3af",

                        fontSize:
                            "12px",
                    }}
                >
                    Press Enter to send.
                    Use Shift + Enter for a new line.
                </p>

            </main>

        </div>
    );
}


const headerButtonStyle = {

    border:
        "1px solid #d1d5db",

    background:
        "#ffffff",

    padding:
        "9px 15px",

    borderRadius:
        "8px",

    cursor:
        "pointer",

    fontWeight:
        "500",
};


export default ChatPage;