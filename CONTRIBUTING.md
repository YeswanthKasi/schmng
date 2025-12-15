# Contributing to School Management App (schmng)

Thank you for your interest in contributing to schmng! This document provides guidelines and best practices for contributing to this project.

## Branching Strategy

To keep the main branch stable and avoid conflicts, we follow a feature branch workflow:

### Main Branch
- The `master` branch is our primary branch
- It should always contain stable, tested code
- Never commit directly to `master`

### Working on Features or Fixes

1. **Create a new branch from master:**
   ```bash
   git checkout master
   git pull origin master
   git checkout -b feature/your-feature-name
   ```

2. **Branch naming conventions:**
   - Feature branches: `feature/description`
   - Bug fixes: `bugfix/description`
   - Hotfixes: `hotfix/description`
   - Documentation: `docs/description`
   
   Examples:
   - `feature/add-student-dashboard`
   - `bugfix/fix-attendance-calculation`
   - `docs/update-api-documentation`

3. **Make your changes:**
   - Write clean, well-documented code
   - Follow the existing code style
   - Test your changes thoroughly

4. **Commit your changes:**
   ```bash
   git status  # Review your changes
   git add <files>  # Stage specific files
   git commit -m "Brief description of your changes"
   ```

5. **Push your branch:**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Create a Pull Request:**
   - Go to the GitHub repository
   - Click "New Pull Request"
   - Select your branch as the source
   - Provide a clear description of your changes
   - Link any related issues

## Quick Start Script

We've provided a helper script to quickly create a new feature branch. See `scripts/create-branch.sh`.

## Code Standards

### Kotlin Code Style
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic

### Commit Messages
- Use clear, descriptive commit messages
- Start with a verb in present tense (e.g., "Add", "Fix", "Update")
- Keep the first line under 50 characters
- Add detailed description if needed

Example:
```
Add student attendance tracking feature

- Implement attendance API endpoints
- Create UI for marking attendance
- Add database migrations for attendance table
```

## Testing

Before submitting a pull request:

1. **Run tests:**
   ```bash
   ./gradlew test
   ```

2. **Build the project:**
   ```bash
   ./gradlew build
   ```

3. **Test manually:**
   - Run the app on an emulator or device
   - Test the specific features you changed
   - Verify no existing features broke

## Pull Request Process

1. Ensure your code builds and all tests pass
2. Update documentation if you changed functionality
3. Request review from maintainers
4. Address any feedback from code review
5. Once approved, your PR will be merged

## Getting Help

If you have questions or need help:
- Open an issue on GitHub
- Reach out to the maintainers

## Additional Resources

- [Android Development Documentation](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Git Branching Guide](https://git-scm.com/book/en/v2/Git-Branching-Basic-Branching-and-Merging)

---

Thank you for contributing to schmng!
