# Git Workflow: Branches, PRs, and Merges

How Kevin and Michael keep upstream and the fork in sync without generating phantom merge conflicts.

## Why squash-and-merge bites (the stacked-branch trap)

Squash-and-merge keeps `main` linear, but it breaks git's commit lineage.

When GitHub squash-merges a PR, it mints a brand-new commit SHA on `main`. The original branch commits vanish from `main`'s history.

This causes conflicts if:
- **Branches get stacked**: Branch B comes off Branch A before A merges. Once A is squash-merged on GitHub, `main` has the squashed commit, but Branch B still carries A's old commits. When Branch B tries to merge with `main` later, git sees two different sets of commits touching the same lines and calls it a conflict.
- **Local `main` is stale**: Starting a new feature branch without pulling `main` first.
- **Merging locally instead of pulling**: Merging the feature branch into local `main` by hand (`git merge my-feature`) instead of pulling down GitHub's squash commit. Local `main` and remote `main` immediately diverge.

---

## Kevin's side (upstream lead)

### Starting work
Always branch off fresh `main`:
```bash
git checkout main
git pull origin main
git checkout -b my-feature
```

### Landing the PR
1. Open the PR and review.
2. Click **Squash and merge** on GitHub, then delete the remote branch on GitHub.
3. Update local `main` and delete the local branch:
```bash
git checkout main
git pull origin main
git branch -d my-feature
```
Don't run `git merge my-feature` locally. Pull the squash commit down.

### Stacked branches
Avoid branching from an unmerged feature branch. Wait for it to land, pull `main`, then start the next piece. If you have to start early, wait until the first PR merges before touching `main` on the second branch, then rebase onto fresh `main` (`git rebase --onto main branch-a branch-b`).

---

## Michael's side (fork collaborator)

Michael works from a fork (`MichaelPaulukonis/kevins-todo_app`) with local agent rules (`AGENTS.md`, `.agents/`, `GEMINI.md`).

### Contributing PRs to Kevin
Never branch off the fork's `main` for upstream PRs. Branch directly off `upstream/main` so Kevin's PRs don't inherit local agent files or fork merge commits:
```bash
git fetch upstream
git checkout -b my-feature upstream/main
git push origin my-feature
# open PR against kkraft714:main
```

### Reviewing Kevin's PRs
Pull the PR directly into an isolated branch:
```bash
git fetch upstream pull/<N>/head:pr-<N>
git checkout pr-<N>
```
Test and review there. When done, switch back and delete it:
```bash
git checkout main
git branch -D pr-<N>
```

### Syncing the fork's `main`
To bring Kevin's latest code into the fork:
```bash
git checkout main
git pull --no-rebase upstream main
git push origin main
```
Merge commits on the fork's `main` are normal here (it carries local agent docs).
