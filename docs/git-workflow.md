# Git Playbook: Branch, PR & Merge Workflow

A simple guide for Kevin and Michael to avoid merge conflicts and keep our repos clean.

## Why Squash-and-Merge Causes Conflicts (The Trap)

Squash-and-merge is great because it keeps the git history on `main` clean and linear (one commit per PR). However, it has one major catch:

When GitHub squash-merges a PR, it compresses all branch commits into a brand new commit with a new SHA on `main`. The original branch commits do not exist on `main`.

Conflicts happen when:
1. **Stacked branches**: Branch B is created off Branch A before Branch A is merged. When Branch A is squash-merged, `main` gets the new squashed commit. Branch B still has the old commits. When Branch B tries to merge with `main`, Git sees conflicting histories on every file touched in Branch A.
2. **Branching from stale `main`**: Starting a new feature branch without running `git pull origin main` first.
3. **Merging locally instead of pulling**: Merging a feature branch into local `main` on your machine instead of pulling the squashed commit down from GitHub.

---

## Kevin's Workflow (Upstream Lead)

### 1. Starting a Feature Branch
Always start from an up-to-date `main`:
```bash
git checkout main
git pull origin main
git checkout -b my-feature
```

### 2. PR and Merge on GitHub
- Push branch and open PR against `main`.
- Review and discuss.
- Click **Squash and merge** on GitHub.
- Delete the remote branch on GitHub after merging.

### 3. Wrapping Up Locally
Immediately update local `main` from GitHub and delete the local feature branch:
```bash
git checkout main
git pull origin main
git branch -d my-feature
```
Never run `git merge my-feature` into local `main`.

### 4. Rule on Stacked Work
Avoid branching from an unmerged feature branch. Wait for the PR to squash-merge into `main`, pull `main`, then start the next branch. If you must start before review is done, treat the second branch as an extension and do not merge `main` into it until the first PR lands and you can rebase onto fresh `main`.

---

## Michael's Workflow (Fork Collaborator)

Michael maintains a fork (`MichaelPaulukonis/kevins-todo_app`) with local agent configurations (`AGENTS.md`, `.agents/`, `GEMINI.md`).

### 1. Contributing Code / PRs to Kevin
Never branch off your fork's `main` to create a PR for Kevin. Always branch directly from `upstream/main`:
```bash
git fetch upstream
git checkout -b my-feature upstream/main
# work, commit, push to origin
git push origin my-feature
# open PR from my-feature -> kkraft714:main
```
This guarantees Kevin's PR has zero fork baggage or merge commits.

### 2. Reviewing Kevin's PRs
Fetch the PR directly into a clean tracking branch:
```bash
git fetch upstream pull/<N>/head:pr-<N>
git checkout pr-<N>
```
Test and review on this clean branch. When done:
```bash
git checkout main
git branch -D pr-<N>
```

### 3. Syncing the Fork's `main`
To keep your working tree up to date with Kevin's latest code:
```bash
git checkout main
git pull --no-rebase upstream main
git push origin main
```
Merge commits on your fork's `main` are completely fine and expected because your fork carries local agent docs.
