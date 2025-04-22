# IntelliSurf

IntelliSurf is a Java-based application featuring user registration, login, and a chatbot interface powered by Ollama API.

## Project Structure

```
IntelliSurf/
├── src/
│   └── intellisurf/
│       └── Main.java
├── IntelliSurf.iml
├── README.md
└── .idea/ (IDE config)
```

- **src/intellisurf/Main.java**: Main application source code, organized in the `intellisurf` package.
- **IntelliSurf.iml**: IntelliJ IDEA module file.
- **.idea/**: IDE configuration files.

## How to Run

1. Open the project in IntelliJ IDEA or any Java IDE.
2. Ensure dependencies for `io.github.ollama4j` are available (add to your build system if needed).
3. Run `Main.java` from the `intellisurf` package.

## Features
- User registration with strong password enforcement
- Secure password hashing (SHA-256)
- Login authentication
- Chatbot interface using Ollama API

---
