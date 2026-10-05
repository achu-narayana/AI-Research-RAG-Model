import { BrowserRouter, Routes, Route } from "react-router-dom";

import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import NewProject from "./pages/NewProject";
import ProjectPage from "./pages/ProjectPage";
import PapersPage from "./pages/PapersPage";
import ChatPage from "./pages/ChatPage";

function App() {
    return (
        <BrowserRouter>
            <Routes>

                <Route
                    path="/"
                    element={<Login />}
                />

                <Route
                    path="/register"
                    element={<Register />}
                />

                <Route
                    path="/dashboard"
                    element={<Dashboard />}
                />

                <Route
                    path="/projects/new"
                    element={<NewProject />}
                />

                <Route
                    path="/projects/:projectId"
                    element={<ProjectPage />}
                />

                <Route
                    path="/projects/:projectId/papers"
                    element={<PapersPage />}
                />

                <Route
                    path="/projects/:projectId/chat"
                    element={<ChatPage />}
                />

            </Routes>
        </BrowserRouter>
    );
}

export default App;