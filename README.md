# School Management App (schmng)

An AI-powered school operations management solution with fully functional landing layouts and activities.

## Features

- Student & Teacher Management
- Attendance and Scheduling
- AI-driven analytics (future)
- Modern Android UI/UX

## Getting Started

1. Clone the repository:
   ```
   git clone https://github.com/YeswanthKasi/schmng.git
   ```
2. Open in Android Studio and build the project.

## Contributing

We follow a feature branch workflow to keep the main branch stable. See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

### Quick Start for Contributors

Create a new feature branch:
```bash
./scripts/create-branch.sh feature your-feature-name
```

This will:
- Fetch the latest changes from master
- Create a new branch based on master
- Switch to your new branch

Make your changes, review them, commit, and push:
```bash
git status  # Review your changes
git add <files>  # Stage specific files
git commit -m "Description of your changes"
git push -u origin feature/your-feature-name
```

Then create a Pull Request on GitHub!

## Tech Stack

- Kotlin
- Android Jetpack Libraries
- Material Design

## Project Structure

- `/app` – Main Android app source code
- `/fastlane` – Automation scripts (see fastlane/README.md)

## License

Proprietary. For demo and educational use.

## Docker Development Environment

### Prerequisites
- Docker
- Docker Compose

### Building with Docker

1. Build the Docker image:
```bash
docker-compose build
```

2. Run a build:
```bash
docker-compose run --rm android-build ./gradlew build
```

3. Run tests:
```bash
docker-compose run --rm android-build ./gradlew test
```

4. Clean build:
```bash
docker-compose run --rm android-build ./gradlew clean build
```

### Benefits of Docker Setup
- Consistent build environment across all developers
- No need to install Android SDK locally
- Cached Gradle dependencies in a Docker volume
- Isolated build environment
- Easy CI/CD integration

### Notes
- The first build might take longer as it downloads all dependencies
- Gradle cache is preserved between builds in a Docker volume
- Your source code changes are immediately reflected due to volume mounting

---

Built by Yeswanth Kasi
