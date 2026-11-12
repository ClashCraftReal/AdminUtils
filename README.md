# Admin Utils

Admin Utils is a Fabric mod for Minecraft 1.21.11, 26.1.2, 26.2, and 26.3. Stonecutter preprocesses the shared source tree for each version, and each version subproject has its own Minecraft, Fabric API, and Loom configuration.

## Requirements

- JDK 25 (Gradle runs on Java 25; the 1.21.11 mod classes target Java 21)
- The included Gradle wrapper

## Build

Run these commands from the repository root using PowerShell:

```powershell
.\gradlew.bat :1.21.11:build
.\gradlew.bat :26.1.2:build
.\gradlew.bat :26.2:build
.\gradlew.bat :26.3:build
.\gradlew.bat buildAllVersions
```

Each version's distributable jar is written to its `versions/<version>/build/libs/` directory.

## Development runs

Each version provides separate Loom `runClient` and `runServer` tasks:

```powershell
.\gradlew.bat :1.21.11:runClient
.\gradlew.bat :1.21.11:runServer
.\gradlew.bat :26.1.2:runClient
.\gradlew.bat :26.1.2:runServer
.\gradlew.bat :26.2:runClient
.\gradlew.bat :26.2:runServer
.\gradlew.bat :26.3:runClient
.\gradlew.bat :26.3:runServer
```

Run data is isolated by version under `versions/<version>/run/`.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your projects.
