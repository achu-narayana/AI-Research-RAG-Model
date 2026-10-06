# AI Research Paper Assistant

An AI-powered research workspace for uploading research papers, organizing them into projects, and interacting with them using Retrieval-Augmented Generation (RAG).

The application combines a React frontend, Spring Boot backend, and Python FastAPI AI service.

## Project Architecture

```text
                    +-----------------------+
                    |     React Frontend    |
                    |       Vite + JS       |
                    +-----------+-----------+
                                | HTTP
                                v
                    +-----------------------+
                    |   Spring Boot Backend |
                    | Java 17 + Spring Boot |
                    |   JWT + Spring Data   |
                    +----+-------------+----+
                         |             |
                         |             +-------------> H2 Database
                         |
                         | HTTP
                         v
                    +-----------------------+
                    |   FastAPI AI Service  |
                    |       Python          |
                    +-----------+-----------+
                                |
                +---------------+---------------+
                |               |               |
                v               v               v
          PDF / OCR       RAG Retrieval        LLM
          PyMuPDF         ChromaDB +           OpenRouter
          Tesseract       HuggingFace          openrouter/free
                          Embeddings
```

## Repository Structure

```text
research-paper-assistant-ALL/
├── ai-service/
│   ├── main.py
│   ├── rag\\\_service.py
│   ├── retrieval\\\_service.py
│   ├── summary\\\_service.py
│   ├── compare\\\_service.py
│   ├── llm\\\_service.py
│   └── requirements.txt
│
├── research-paper-assistant/
│   └── Spring Boot backend
│
├── research-paper-frontend/
│   ├── src/
│   ├── package.json
│   └── vite.config.js
│
├── .gitignore
├── README.md
└── ...
```

\---

# Main Features

## 1\. User Authentication

The Spring Boot backend provides:

* User registration
* User login
* BCrypt password hashing
* JWT-based authentication
* Stateless Spring Security
* Protected API endpoints
* Project access based on the authenticated user

The frontend stores the JWT token in local storage and uses it for protected requests.

The Login and Register pages also provide simple frontend-friendly error messages for common failures such as invalid credentials, duplicate accounts, invalid requests, and server connection problems.

\---

## 2\. Research Project Management

Users can:

* Create research projects
* Add a project title
* Add a project description
* View their projects
* Open a project
* Access project-specific papers
* Access project-specific AI chat

Projects are associated with their owner, preventing users from accessing another user's project through the protected backend APIs.

\---

## 3\. Research Paper Upload

Research papers are uploaded as PDFs and associated with a project.

Implemented functionality includes:

* Single PDF upload
* Multiple PDF upload
* Up to 5 papers in one upload request
* PDF validation
* Project ownership validation
* Unique document IDs
* Original filename storage
* Paper metadata storage
* Paper status tracking
* Sending uploaded PDFs from Spring Boot to the FastAPI AI service

The frontend also supports selecting multiple PDFs before starting the upload.

\---

## 4\. PDF Text Extraction and OCR

The FastAPI service processes uploaded PDFs.

Processing includes:

* PDF text extraction using PyMuPDF
* OCR using Tesseract when readable text is not sufficient
* Text cleaning
* Text chunking using LangChain's `RecursiveCharacterTextSplitter`

### Tesseract OCR

Tesseract is an external system dependency and must be installed separately.

For Windows, the expected installation is:

```text
C:\\\\Program Files\\\\Tesseract-OCR\\\\
```

Required files include:

```text
C:\\\\Program Files\\\\Tesseract-OCR\\\\tesseract.exe
C:\\\\Program Files\\\\Tesseract-OCR\\\\tessdata\\\\eng.traineddata
```

If necessary, add these Windows environment variables:

### PATH

```text
C:\\\\Program Files\\\\Tesseract-OCR
```

### TESSDATA\_PREFIX

```text
C:\\\\Program Files\\\\Tesseract-OCR\\\\tessdata
```

After changing environment variables, restart PowerShell, VS Code, or STS.

Verify:

```powershell
tesseract --version
tesseract --list-langs
```

`eng` should be listed.

\---

# 5\. Embeddings and ChromaDB

The AI service uses:

* HuggingFace Sentence Transformers
* `all-MiniLM-L6-v2`
* 384-dimensional embeddings
* ChromaDB persistent vector storage

Research paper chunks are stored with metadata such as:

```text
document\\\_id
project\\\_id
paper\\\_name
```

This metadata allows retrieval to be limited to:

* All papers in the selected project
* One specific paper

The local ChromaDB data is runtime data and should not be committed to Git.

\---

# 6\. RAG-Based AI Chat

The main AI feature is Retrieval-Augmented Generation.

Users can ask questions about their uploaded research papers.

Two chat scopes are supported.

### Project-wide chat

The question searches across the papers in the selected research project.

### Selected-paper chat

The question searches only the selected research paper.

### RAG Flow

```text
User Question
      |
      v
React Frontend
      |
      v
Spring Boot Backend
      |
      v
FastAPI AI Service
      |
      v
Query Embedding
      |
      v
ChromaDB Similarity Search
      |
      v
Relevant Paper Chunks
      |
      v
OpenRouter LLM
(openrouter/free)
      |
      v
Generated Answer
      |
      v
Spring Boot
      |
      v
React UI
```

The RAG prompt is designed to keep research-related answers grounded in the retrieved paper context and to avoid inventing unsupported information.

Simple conversational messages such as greetings and acknowledgements are handled separately rather than forcing them through paper retrieval.

\---

# 7\. AI Paper Summarization

The application can generate a structured summary for a selected research paper.

The summary process:

```text
Selected Paper
      |
      v
ChromaDB
      |
      v
Representative Paper Chunks
      |
      v
OpenRouter LLM
      |
      v
Structured Summary
```

The generated summary focuses on information such as:

* Overview
* Research problem and objective
* Methodology / approach
* Techniques, models and data
* Results / findings
* Limitations
* Conclusion

The summary service uses a limited set of representative chunks to reduce prompt size and avoid unnecessary repeated LLM requests.

\---

# 8\. Research Paper Comparison

The application supports comparing two different research papers.

The user selects two papers from the current project.

The comparison process:

```text
Paper 1 -------+
               +----> ChromaDB / Paper Content
Paper 2 -------+
                         |
                         v
                 Representative Chunks
                         |
                         v
                   OpenRouter LLM
                         |
                         v
                   Paper Comparison
```

The comparison covers, when available in the supplied paper content:

* Overall comparison
* Similarities
* Research problem and objectives
* Methodology / approach
* Techniques, models and data
* Results / findings
* Key differences
* Limitations
* Final comparison

Only the selected two papers are used for the comparison.

\---

# 9\. Persistent Project Chat

Each research project has one persistent chat.

The Spring Boot backend stores:

* User messages
* Assistant responses
* Selected document ID when applicable
* Message creation time

The chat history can be loaded again when the project is reopened.

\---

# 10\. Chat PDF Export

The application provides an endpoint for exporting a project's chat conversation as a PDF.

The exported document contains the chat history in a readable format.

\---

# Technology Stack

## Frontend

* React
* Vite
* JavaScript
* Axios
* React Router
* Lucide React
* React Markdown
* Remark GFM
* CSS

The frontend includes responsive UI styling combining neumorphism and neo-brutalist visual elements.

## Backend

* Java 17
* Spring Boot 4.1.1
* Spring Security
* JWT
* Spring Data JPA
* Hibernate
* H2 Database
* Maven
* Java HTTP Client

## AI Service

* Python
* FastAPI
* Uvicorn
* PyMuPDF
* Tesseract OCR
* LangChain
* HuggingFace Sentence Transformers
* ChromaDB
* OpenRouter
* `openrouter/free`

\---

# Requirements

Before running the project, install the following:

* Git
* Node.js and npm
* Java 17
* Python
* Tesseract OCR

An OpenRouter API key is also required for the current LLM integration.

\---

# Setup

## 1\. Clone the Repository

```powershell
git clone https://github.com/achu-narayana/AI-Research-RAG-Model.git
cd AI-Research-RAG-Model
```

Make sure your working directory contains:

```text
ai-service/
research-paper-assistant/
research-paper-frontend/
```

\---

# 2\. Frontend Setup

Go to the React frontend:

```powershell
cd research-paper-frontend
npm install
```

Run:

```powershell
npm run dev
```

Frontend:

```text
http://localhost:5173
```

\---

# 3\. Backend Setup

Open another terminal and go to:

```powershell
cd research-paper-assistant
```

The project includes Maven Wrapper files, so Maven does not necessarily need to be installed globally.

On Windows:

```powershell
.\\\\mvnw.cmd spring-boot:run
```

Backend:

```text
http://localhost:8081
```

\---

# 4\. AI Service Setup

Open another terminal:

```powershell
cd ai-service
```

Create a virtual environment:

```powershell
python -m venv .venv
```

Activate it:

```powershell
.\\\\.venv\\\\Scripts\\\\activate
```

Install Python dependencies:

```powershell
pip install -r requirements.txt
```

\---

# 5\. OpenRouter Configuration

The current AI service uses OpenRouter for LLM generation.

Create a file:

```text
ai-service/.env
```

Add your API key:

```env
OPENROUTER\\\_API\\\_KEY=your\\\_openrouter\\\_api\\\_key
```

Do not commit this file.

The model router currently used by the service is:

```text
openrouter/free
```

The `.env` file and API keys are excluded by `.gitignore`.

\---

# 6\. Run FastAPI

With the Python virtual environment activated:

```powershell
uvicorn main:app --reload
```

AI service:

```text
http://127.0.0.1:8000
```

\---

# Recommended Startup Order

For normal local development:

```text
1. Start FastAPI AI service
          |
          v
2. Start Spring Boot backend
          |
          v
3. Start React frontend
```

Tesseract must already be installed and configured on the machine.

\---

# Application Ports

|Component|Port|
|-|-:|
|React / Vite|`5173`|
|Spring Boot|`8081`|
|FastAPI|`8000`|

Spring Boot communicates with the AI service through:

```text
http://127.0.0.1:8000
```

The React frontend communicates with Spring Boot on:

```text
http://localhost:8081
```

\---

# Main API Areas

## Authentication

```text
/api/auth/\\\*\\\*
```

## Projects

```text
/api/projects
```

## Papers

```text
/api/projects/{projectId}/papers
/api/projects/multiple
```

## Chat

```text
/api/projects/{projectId}/chat/messages
/api/projects/{projectId}/chat/pdf
```

## AI

```text
/api/ai/ask
/api/ai/summary
/api/ai/compare
```

The FastAPI service also exposes endpoints for PDF ingestion and AI processing.

\---

# Local Runtime Data

The following files/directories are local or generated data and should not be committed:

```text
research-paper-frontend/node\\\_modules/
research-paper-frontend/dist/

research-paper-assistant/target/
research-paper-assistant/data/
research-paper-assistant/uploads/

ai-service/.venv/
ai-service/\\\_\\\_pycache\\\_\\\_/
ai-service/data/
```

This includes:

* H2 database files
* Uploaded PDFs
* ChromaDB vector data
* Python cache files
* Node modules
* Build output

Do not commit:

* API keys
* Passwords
* `.env` files
* Personal documents
* Private research papers
* Resumes or other confidential files

\---

# Current Project Status

The current implementation includes:

* User registration and login
* BCrypt password hashing
* JWT authentication
* Protected backend APIs
* Research project creation
* Project listing
* Project ownership validation
* Single PDF upload
* Multiple PDF upload
* PDF validation
* PDF text extraction
* OCR support
* Text cleaning
* Text chunking
* HuggingFace embeddings
* ChromaDB vector storage
* Project-wide RAG questions
* Selected-paper RAG questions
* Persistent project chat
* Chat history
* Chat PDF export
* AI paper summarization
* Two-paper comparison
* OpenRouter LLM integration
* Responsive React frontend
* Research-focused UI redesign

\---

# Notes for Team Development

The repository contains three cooperating applications:

```text
React Frontend
      |
      v
Spring Boot Backend
      |
      v
FastAPI AI Service
```

The Spring Boot application is responsible for application-level functionality such as:

* Users
* Authentication
* Projects
* Paper metadata
* Access control
* Chat persistence

The Python service is responsible for AI/RAG-related processing such as:

* PDF extraction
* OCR
* Chunking
* Embeddings
* ChromaDB retrieval
* Question answering
* Summarization
* Paper comparison
* LLM calls

For team work, use feature branches rather than directly committing experimental changes to `main`.

Example:

```powershell
git checkout -b your-name-feature
```

Before starting work:

```powershell
git pull
```

After making changes:

```powershell
git add .
git commit -m "Describe your changes"
git push -u origin your-name-feature
```

\---

# Important Git Reminder

Never commit:

```text
.env
.env.\\\*
API keys
passwords
node\\\_modules/
target/
.venv/
\\\_\\\_pycache\\\_\\\_/
database files
uploaded PDFs
ChromaDB data
```

The repository `.gitignore` is configured to exclude these files.

\---

# Project Goal

The goal of the project is to provide students and researchers with a single workspace where they can upload research papers, search them using RAG, ask questions, generate summaries, and compare papers without manually reading every document from start to finish.



