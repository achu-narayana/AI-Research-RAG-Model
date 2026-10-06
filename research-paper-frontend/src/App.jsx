import { Fragment } from "react";
import {
    BrowserRouter,
    Routes,
    Route,
    Navigate,
    useParams,
} from "react-router-dom";

import { clearSession, getToken, isLoggedIn } from "./api";

import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import NewProject from "./pages/NewProject";
import ProjectPage from "./pages/ProjectPage";
import PapersPage from "./pages/PapersPage";
import ChatPage from "./pages/ChatPage";

function RequireAuth({ children }) {
    if (!isLoggedIn()) {
        if (getToken()) {
            // Token exists but is expired or malformed.
            clearSession();
        }

        return <Navigate to="/" replace />;
    }

    return children;
}

// Remounts the page when the project changes so that no state
// (messages, papers, errors, selection...) leaks between projects.
function ProjectScoped({ children }) {
    const { projectId } = useParams();

    return <Fragment key={projectId}>{children}</Fragment>;
}

function GuestOnly({ children }) {
    if (isLoggedIn()) {
        return <Navigate to="/dashboard" replace />;
    }

    return children;
}

function App() {
    return (
        <BrowserRouter>
            <Routes>

                <Route
                    path="/"
                    element={
                        <GuestOnly>
                            <Login />
                        </GuestOnly>
                    }
                />

                <Route
                    path="/register"
                    element={
                        <GuestOnly>
                            <Register />
                        </GuestOnly>
                    }
                />

                <Route
                    path="/dashboard"
                    element={
                        <RequireAuth>
                            <Dashboard />
                        </RequireAuth>
                    }
                />

                <Route
                    path="/projects/new"
                    element={
                        <RequireAuth>
                            <NewProject />
                        </RequireAuth>
                    }
                />

                <Route
                    path="/projects/:projectId"
                    element={
                        <RequireAuth>
                            <ProjectScoped>
                                <ProjectPage />
                            </ProjectScoped>
                        </RequireAuth>
                    }
                />

                <Route
                    path="/projects/:projectId/papers"
                    element={
                        <RequireAuth>
                            <ProjectScoped>
                                <PapersPage />
                            </ProjectScoped>
                        </RequireAuth>
                    }
                />

                <Route
                    path="/projects/:projectId/chat"
                    element={
                        <RequireAuth>
                            <ProjectScoped>
                                <ChatPage />
                            </ProjectScoped>
                        </RequireAuth>
                    }
                />

            </Routes>
        </BrowserRouter>
    );
}

export default App;
