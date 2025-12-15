#!/bin/bash
# Helper script to create a new feature branch from master
# Usage: ./scripts/create-branch.sh <branch-type> <branch-name>
# Example: ./scripts/create-branch.sh feature add-student-dashboard

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if arguments are provided
if [ $# -lt 2 ]; then
    print_error "Usage: $0 <branch-type> <branch-name>"
    echo ""
    echo "Branch types:"
    echo "  feature  - For new features"
    echo "  bugfix   - For bug fixes"
    echo "  hotfix   - For critical production fixes"
    echo "  docs     - For documentation changes"
    echo ""
    echo "Example: $0 feature add-student-dashboard"
    exit 1
fi

BRANCH_TYPE=$1
BRANCH_NAME=$2
FULL_BRANCH_NAME="${BRANCH_TYPE}/${BRANCH_NAME}"

# Validate branch type
case $BRANCH_TYPE in
    feature|bugfix|hotfix|docs)
        ;;
    *)
        print_error "Invalid branch type: $BRANCH_TYPE"
        echo "Valid types: feature, bugfix, hotfix, docs"
        exit 1
        ;;
esac

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    print_error "Not in a git repository"
    exit 1
fi

# Check for uncommitted changes
if ! git diff-index --quiet HEAD -- 2>/dev/null; then
    print_warning "You have uncommitted changes. Please commit or stash them first."
    git status --short
    exit 1
fi

print_info "Fetching latest changes from origin..."
git fetch origin

print_info "Checking out master branch..."
git checkout master

print_info "Pulling latest changes from master..."
git pull origin master

print_info "Creating new branch: $FULL_BRANCH_NAME"
git checkout -b "$FULL_BRANCH_NAME"

print_info "Successfully created and switched to branch: $FULL_BRANCH_NAME"
echo ""
print_info "Next steps:"
echo "  1. Make your changes"
echo "  2. Commit your changes: git add . && git commit -m 'Your message'"
echo "  3. Push your branch: git push -u origin $FULL_BRANCH_NAME"
echo "  4. Create a Pull Request on GitHub"
echo ""
print_info "Happy coding! 🚀"
