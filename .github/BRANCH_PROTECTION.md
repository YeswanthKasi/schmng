# Branch Protection Setup Guide

This document provides recommendations for setting up branch protection rules to maintain code quality and prevent direct commits to the main branch.

## Recommended Branch Protection Rules for `master`

To protect the master branch and ensure all changes go through pull requests, follow these steps:

### 1. Navigate to Branch Protection Settings

1. Go to your repository on GitHub: https://github.com/YeswanthKasi/schmng
2. Click on **Settings**
3. Click on **Branches** in the left sidebar
4. Click **Add branch protection rule**

### 2. Configure Protection Rules

#### Branch Name Pattern
```
master
```

#### Recommended Settings

**Protect matching branches:**
- ✅ **Require a pull request before merging**
  - ✅ Require approvals: 1 (or more for critical projects)
  - ✅ Dismiss stale pull request approvals when new commits are pushed
  - ⬜ Require review from Code Owners (optional, requires CODEOWNERS file)

- ✅ **Require status checks to pass before merging**
  - ✅ Require branches to be up to date before merging
  - Add status checks if you have CI/CD set up (e.g., "build", "test")

- ✅ **Require conversation resolution before merging**
  - Ensures all review comments are addressed

- ✅ **Require signed commits** (optional but recommended for security)

- ⬜ **Require linear history** (optional, forces rebase instead of merge commits)

- ✅ **Do not allow bypassing the above settings**
  - This prevents even admins from pushing directly (recommended)
  - Or: Allow administrators to bypass (for emergency hotfixes)

- ⬜ **Restrict who can push to matching branches** (optional)
  - Use this to limit who can merge PRs

- ⬜ **Allow force pushes** (NOT recommended)
  - Keep this disabled to prevent history rewrites

- ⬜ **Allow deletions** (NOT recommended)
  - Keep this disabled to prevent accidental branch deletion

### 3. Additional Best Practices

#### Create a CODEOWNERS file (Optional)
Create `.github/CODEOWNERS` to automatically request reviews from specific people:

```
# Default owner for everything
* @YeswanthKasi

# Android specific code
/app/ @YeswanthKasi

# Documentation
*.md @YeswanthKasi
```

#### Status Checks
If you have CI/CD workflows, make sure to select them as required status checks:
- Build workflow
- Test workflow
- Lint checks
- Security scans

## Workflow After Protection

Once branch protection is enabled:

1. Developers cannot push directly to `master`
2. All changes must go through pull requests
3. Pull requests must be reviewed and approved
4. All required status checks must pass
5. Only then can the PR be merged

## Benefits

✅ **Code Quality**: All code is reviewed before merging
✅ **Stability**: Prevents accidental or untested changes to master
✅ **Collaboration**: Encourages discussion and knowledge sharing
✅ **Audit Trail**: Clear history of what changed and why
✅ **CI/CD Integration**: Automated testing before merge

## Emergency Procedures

If you need to make an emergency fix:

1. Create a hotfix branch:
   ```bash
   ./scripts/create-branch.sh hotfix critical-security-fix
   ```

2. Make the fix and test it

3. Create a PR with "URGENT" or "HOTFIX" in the title

4. If admin bypass is enabled, the hotfix can be merged faster

5. Alternatively, enable "Allow administrators to bypass" in settings for emergency situations

## Testing the Setup

After enabling branch protection, test it:

```bash
# Try to push directly to master (should fail)
git checkout master
git commit --allow-empty -m "Test commit"
git push origin master
# Expected: Error message about branch protection
```

## References

- [GitHub Branch Protection Documentation](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- [CODEOWNERS Documentation](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners)

---

**Note**: These settings can only be configured by repository administrators through the GitHub web interface.
