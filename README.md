# IntelliSurf

IntelliSurf is a Java-based application featuring user registration, login, and a chatbot interface powered by Ollama API.

## Project Structure

```
IntelliSurf/
├── packages/ (Dependencies: ollama4j, jackson, guava, slf4j, etc.)
├── src/
│   └── intellisurf/
│       └── Main.java
├── config.properties
├── IntelliSurf.iml
├── README.md
├── out/ (Compiled output)
└── .idea/ (IDE config)
```

- **packages/**: Contains required JAR dependencies.
- **src/intellisurf/Main.java**: Main application source code, organized in the `intellisurf` package.
- **config.properties**: Configuration file for API and app settings.
- **IntelliSurf.iml**: IntelliJ IDEA module file.
- **.idea/**: IDE configuration files.

## Configuration

The application can be configured via `config.properties` or through environment variables (which take precedence). Available settings include:
- `OLLAMA_HOST`: The URL of the Ollama API (default: `http://localhost:11434/`)
- `OLLAMA_MODEL`: The Ollama model to use (default: `qwen2.5-coder:7b`)
- `OLLAMA_TIMEOUT`: API timeout in seconds (default: `60`)
- `USER_FILE`: File to store user credentials (default: `users.txt`)
- `POLL_INTERVAL_MS`: Polling interval for chat streaming (default: `100`)

## How to Run

1. Open the project in IntelliJ IDEA or any Java IDE.
2. Ensure the JAR files in the `packages/` directory are added to your project's classpath as dependencies.
3. Run `Main.java` from the `intellisurf` package.

## Features
- User registration with strong password enforcement
- Secure password hashing (PBKDF2 with HMAC-SHA256 and per-user salt)
- Login authentication
- Chatbot interface using Ollama API with asynchronous streaming
- Configurable settings via `config.properties` or environment variables

---
