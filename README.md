# AI Research Paper Assistant

An AI-powered research workspace for uploading research papers, managing
projects, and asking questions about uploaded papers using
Retrieval-Augmented Generation (RAG).

## Project Architecture

``` text
React Frontend
      |
      v
Spring Boot Backend
      |
      +---- H2 Database
      |
      v
FastAPI AI Service
      |
      +---- PDF Text Extraction / OCR
      +---- Text Chunking
      +---- HuggingFace Embeddings
      +---- ChromaDB
      +---- Ollama + Llama 3.2
```

The application is divided into three parts:

``` text
research-paper-assistant/
├── frontend/       # React + Vite
├── backend/        # Spring Boot + Java
├── ai-service/     # FastAPI + Python RAG service
└── README.md
```

> Folder names can be adjusted to match the repository structure.

------------------------------------------------------------------------

# What Has Been Implemented

## 1. User Authentication

The Spring Boot backend currently provides:

-   User registration
-   User login
-   Password hashing using BCrypt
-   JWT-based authentication
-   Stateless Spring Security configuration
-   Protected API endpoints
-   Authentication using the logged-in user's email

The frontend stores the JWT token and sends it with protected API
requests.

------------------------------------------------------------------------

## 2. Research Projects

Users can:

-   Create research projects
-   Provide a project title
-   Provide a project description
-   View their projects
-   Open an individual project
-   Access project-specific papers and AI chat

Projects are associated with their owner, so users cannot access another
user's project.

------------------------------------------------------------------------

## 3. Research Paper Upload

The application supports uploading PDF research papers to a project.

Implemented functionality includes:

-   Single PDF upload
-   Multiple PDF upload
-   Uploading up to 5 papers in one request
-   Project ownership validation
-   PDF validation
-   Unique document IDs
-   Original filename storage
-   Paper metadata storage
-   Paper status tracking
-   Sending uploaded PDFs from Spring Boot to the FastAPI AI service

The frontend supports selecting multiple PDFs before clicking the upload
button.

------------------------------------------------------------------------

## 4. PDF Processing and OCR

The FastAPI service processes uploaded PDFs.

Current processing includes:

-   PDF text extraction using PyMuPDF
-   OCR using Tesseract when required
-   Text cleaning
-   Text chunking using LangChain's RecursiveCharacterTextSplitter

### Tesseract

Tesseract OCR is required on the machine because it is an external
system dependency and is not installed through `requirements.txt`.

On Windows, the expected installation is:

``` text
C:\Program Files\Tesseract-OCR\
```

The `tesseract.exe` executable and English trained data should be
available.

------------------------------------------------------------------------

## 5. Embeddings and Vector Storage

The AI service uses:

-   HuggingFace sentence-transformer embeddings
-   `all-MiniLM-L6-v2`
-   384-dimensional embeddings
-   ChromaDB for vector storage

Paper chunks are stored with metadata such as:

``` text
document_id
project_id
paper_name
```

This metadata allows the system to retrieve information either from:

-   All papers belonging to a project
-   One specifically selected paper

Generated Chroma/vector data is local runtime data and should not be
committed to Git.

------------------------------------------------------------------------

## 6. RAG-Based AI Chat

The core AI feature is Retrieval-Augmented Generation.

Users can ask questions about their research papers.

Two modes are supported:

### Project-wide chat

The question searches across all papers uploaded to the selected
project.

### Selected-paper chat

The question searches only the selected research paper.

The flow is:

``` text
User Question
      |
      v
Spring Boot
      |
      v
FastAPI AI Service
      |
      v
ChromaDB Retrieval
      |
      v
Relevant Paper Chunks
      |
      v
Ollama + Llama 3.2
      |
      v
Generated Answer
      |
      v
Spring Boot
      |
      v
React
```

The AI service is designed to answer using the retrieved research-paper
context rather than general unrelated knowledge.

------------------------------------------------------------------------

## 7. Persistent Project Chat

Each project has a persistent chat.

The Spring Boot backend stores:

-   User messages
-   Assistant responses
-   Selected document ID when applicable
-   Message creation time

Chat history can be retrieved when the user opens the project again.

------------------------------------------------------------------------

## 8. Chat PDF Export

The backend includes an endpoint for exporting the project chat as a
PDF.

The exported document contains the project's chat conversation in a
readable PDF format.

------------------------------------------------------------------------

# Technology Stack

## Frontend

-   React
-   Vite
-   JavaScript
-   Axios
-   React Router
-   Lucide React

## Backend

-   Java 17
-   Spring Boot 4.1.1
-   Spring Security
-   JWT
-   Spring Data JPA
-   Hibernate
-   H2 Database
-   Maven

## AI Service

-   Python
-   FastAPI
-   Uvicorn
-   PyMuPDF
-   Tesseract OCR
-   LangChain
-   HuggingFace Sentence Transformers
-   ChromaDB
-   Ollama
-   Llama 3.2

------------------------------------------------------------------------

# Requirements Before Running the Project

A new developer/team member should install the following before running
the application.

## 1. Git

Install Git:

``` text
Git
```

Verify:

``` powershell
git --version
```

------------------------------------------------------------------------

## 2. Node.js and npm

Required for the React frontend.

Verify:

``` powershell
node --version
npm --version
```

After cloning the repository:

``` powershell
cd frontend
npm install
```

Then run:

``` powershell
npm run dev
```

Frontend:

``` text
http://localhost:5173
```

------------------------------------------------------------------------

## 3. Java 17

Java 17 is required for the Spring Boot backend.

Verify:

``` powershell
java --version
```

The project currently uses Java 17.

------------------------------------------------------------------------

## 4. Maven

The Spring Boot project uses Maven.

The project includes Maven Wrapper files (`mvnw` and `mvnw.cmd`), so
Maven does not necessarily need to be installed globally.

From the backend directory on Windows:

``` powershell
.\mvnw.cmd spring-boot:run
```

Backend:

``` text
http://localhost:8081
```

------------------------------------------------------------------------

## 5. Python

Python is required for the FastAPI AI service.

Verify:

``` powershell
python --version
```

A Python virtual environment should be created inside `ai-service`:

``` powershell
cd ai-service
python -m venv .venv
```

Activate it on Windows PowerShell:

``` powershell
.\.venv\Scripts\activate
```

Install dependencies:

``` powershell
pip install -r requirements.txt
```

Run FastAPI:

``` powershell
uvicorn main:app --reload
```

AI service:

``` text
http://127.0.0.1:8000
```

------------------------------------------------------------------------

## 6. Tesseract OCR

Tesseract must be installed separately because it is not a Python
package.

On Windows, install Tesseract OCR and make sure it is available at:

``` text
C:\Program Files\Tesseract-OCR\
```

The system should have:

``` text
C:\Program Files\Tesseract-OCR\tesseract.exe
C:\Program Files\Tesseract-OCR\tessdata\eng.traineddata
```

Add the following to the Windows environment variables if necessary:

### PATH

``` text
C:\Program Files\Tesseract-OCR
```

### TESSDATA_PREFIX

``` text
C:\Program Files\Tesseract-OCR\tessdata
```

After changing environment variables, restart PowerShell/VS Code/STS.

Verify:

``` powershell
tesseract --version
```

And:

``` powershell
tesseract --list-langs
```

`eng` should be listed.

------------------------------------------------------------------------

## 7. Ollama

Ollama is required for the local Llama model.

Install Ollama before running the AI chat.

Verify:

``` powershell
ollama --version
```

Pull the model used by the project:

``` powershell
ollama pull llama3.2
```

Verify the model:

``` powershell
ollama list
```

Ollama should be running before asking AI questions.

The AI service communicates with the local Ollama service.

------------------------------------------------------------------------

# Running the Complete Application

Open three terminals.

## Terminal 1 --- AI Service

``` powershell
cd ai-service
.\.venv\Scripts\activate
uvicorn main:app --reload
```

------------------------------------------------------------------------

## Terminal 2 --- Spring Boot Backend

``` powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend runs on:

``` text
http://localhost:8081
```

------------------------------------------------------------------------

## Terminal 3 --- React Frontend

``` powershell
cd frontend
npm install
npm run dev
```

Frontend runs on:

``` text
http://localhost:5173
```

------------------------------------------------------------------------

# Recommended Startup Order

For normal development:

``` text
1. Start Ollama
       ↓
2. Start FastAPI AI service
       ↓
3. Start Spring Boot backend
       ↓
4. Start React frontend
```

------------------------------------------------------------------------

# Local Data

The following are local/generated files and should not be committed to
Git:

``` text
frontend/node_modules/
frontend/dist/

backend/target/
backend/data/
backend/uploads/

ai-service/.venv/
ai-service/__pycache__/
ai-service/data/
```

The H2 database, uploaded PDFs, ChromaDB data, Python cache files, and
build files can be recreated locally.

Do not commit personal PDFs, uploaded research papers, resumes, or other
private documents.

------------------------------------------------------------------------

# Important Development Notes

## Backend

Spring Boot:

``` text
Port: 8081
```

H2 database is configured for local development.

## AI Service

FastAPI:

``` text
Port: 8000
```

The Spring Boot backend communicates with:

``` text
http://127.0.0.1:8000
```

## Frontend

React/Vite:

``` text
Port: 5173
```

The frontend communicates with the Spring Boot backend on port `8081`.

------------------------------------------------------------------------

# Current Main API Areas

Authentication:

``` text
/api/auth/**
```

Projects:

``` text
/api/projects
```

Papers:

``` text
/api/projects/{projectId}/papers
/api/projects/multiple
```

Chat:

``` text
/api/projects/{projectId}/chat/messages
/api/projects/{projectId}/chat/pdf
```

The AI service provides endpoints for paper ingestion and question
answering.

------------------------------------------------------------------------

# Team Development

The repository should contain all three components:

``` text
frontend/
backend/
ai-service/
```

Each developer should create their own Git branch rather than directly
working on `main`.

Example:

``` powershell
git checkout -b your-name-feature
```

Before starting work:

``` powershell
git pull
```

After making changes:

``` powershell
git add .
git commit -m "Describe your changes"
git push -u origin your-name-feature
```

------------------------------------------------------------------------

# Important: What Git Should NOT Contain

Do not commit:

``` text
node_modules
target
.venv
__pycache__
H2 database files
ChromaDB files
uploaded PDFs
personal documents
API keys
passwords
.env files
```

These should be excluded using `.gitignore`.

------------------------------------------------------------------------

# Project Status

The current implementation includes:

-   User registration/login
-   JWT authentication
-   Research project creation
-   Project listing
-   Project-specific access control
-   Single PDF upload
-   Multiple PDF upload
-   PDF text extraction
-   OCR support
-   Text chunking
-   Embedding generation
-   ChromaDB vector storage
-   Project-wide RAG questions
-   Selected-paper RAG questions
-   Persistent project chat
-   Chat history
-   Chat PDF export
-   React frontend
-   Spring Boot backend
-   FastAPI AI service

Further UI/UX improvements and additional AI features can be added as
development continues.
