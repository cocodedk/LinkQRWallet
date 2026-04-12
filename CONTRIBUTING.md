# Contributing to Link QR Wallet

Built by [Cocode](https://cocode.dk).

## Local Setup
1. Install Android Studio (latest stable) and Android SDK.
2. Ensure Java 11 is available.
3. Open the project and sync Gradle.

## Install Git Hooks
```bash
./scripts/install-hooks.sh
```

## Local Git Setup
Run these once after cloning:
```bash
git config pull.rebase true
git config core.autocrlf input
git config push.autoSetupRemote true
git config init.defaultBranch main
```

## Build and Test Commands
```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew buildSmoke
```

## Coding Style
- Kotlin style: official Kotlin conventions.
- Keep files small and focused (200-line maximum).
- Prefer clear naming and explicit behaviour over hidden side effects.
- `allWarningsAsErrors = true` is enforced — all warnings must be fixed.

## Branch Naming

| Branch prefix | Conventional Commit type | Example |
|---|---|---|
| `feature/` | `feat:` | `feature/add-folder-organisation` |
| `fix/` | `fix:` | `fix/crash-on-empty-url` |
| `chore/` | `chore:` | `chore/update-dependencies` |
| `docs/` | `docs:` | `docs/update-contributing` |
| `refactor/` | `refactor:` | `refactor/extract-qr-util` |
| `ci/` | `ci:` | `ci/add-dependabot` |

## PR Checklist
- [ ] `./gradlew buildSmoke` passes.
- [ ] Manual test completed (add link, scan QR, search, sort).
- [ ] No regressions in adjacent features.
- [ ] Updated docs if behaviour changed.
