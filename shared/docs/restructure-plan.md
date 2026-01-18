# TableTorch Project Restructure Plan

## Current Structure
```
TableTorch/
├── .git/
├── .gitignore
├── AGENTS.md
├── TableTorch.xcodeproj/     # Xcode project file
└── TableTorch/               # iOS source code
    ├── Assets.xcassets/
    ├── Preview Content/
    ├── *.swift files
    └── ...
```

## Target Structure
```
TableTorch/
├── ios/                      # Xcode project
│   ├── TableTorch.xcodeproj/
│   └── TableTorch/           # iOS source code
├── android/                  # Android Studio project
│   ├── app/
│   ├── gradle/
│   ├── build.gradle
│   └── ...
├── shared/                   # Shared assets and docs
│   ├── assets/               # Images, icons for both platforms
│   ├── api-specs/            # API documentation
│   └── docs/                 # Project plans and documentation
├── .gitignore
├── AGENTS.md
└── README.md
```

---

## CRITICAL ISSUES

### Issue 1: Xcode Project Path References (HIGH RISK)
The `TableTorch.xcodeproj/project.pbxproj` file contains **hardcoded relative paths** to source files. Moving the project into `ios/` will break these references.

**Options:**
1. **Recommended: Move via Finder/Xcode** - Move the folder using Finder while Xcode is closed, then open the project and fix any broken references in Xcode's GUI
2. **Alternative: Recreate the project** - Create a new Xcode project in `ios/` and add existing source files
3. **Risky: CLI move + manual pbxproj edit** - Move via `git mv` and edit the pbxproj file (complex, error-prone)

### Issue 2: Git History
Moving files will affect git history. Using `git mv` preserves some tracking, but GitHub/GitLab may still show files as deleted + added.

### Issue 3: Documentation Updates Required
`AGENTS.md` contains path references like:
- `TableTorch/` directory
- `open TableTorch.xcodeproj`
- `xcodebuild -scheme TableTorch`

These must be updated to reflect the `ios/` prefix.

---

## IMPLEMENTATION STEPS

### Phase 1: Create New Directory Structure
```bash
mkdir -p ios
mkdir -p android
mkdir -p shared/assets
mkdir -p shared/api-specs
mkdir -p shared/docs
```

### Phase 2: Move iOS Project (MANUAL STEP REQUIRED)
**This should be done manually via Finder:**
1. Close Xcode if open
2. Move `TableTorch.xcodeproj` into `ios/`
3. Move `TableTorch/` (source folder) into `ios/`
4. Open `ios/TableTorch.xcodeproj` in Xcode
5. Verify all files appear correctly in the project navigator
6. If any files show red (missing), re-add them via Xcode

### Phase 3: Update Configuration Files
1. Update `.gitignore` with Android patterns
2. Update `AGENTS.md` with new paths
3. Create/update root `README.md`

### Phase 4: Git Commit
```bash
git add .
git commit -m "Restructure project for multi-platform support"
```

### Phase 5: Android Project Setup
Create Android project in `android/` folder using Android Studio:
1. Open Android Studio
2. Create New Project
3. Set location to `TableTorch/android/`
4. Configure as needed

---

## RECOMMENDED APPROACH

Given the risks with Xcode project paths, I recommend:

1. **I will create** the directory structure (`android/`, `shared/`)
2. **I will update** `.gitignore` and documentation
3. **You manually move** the iOS project via Finder:
   - Move `TableTorch.xcodeproj` to `ios/TableTorch.xcodeproj`
   - Move `TableTorch/` folder to `ios/TableTorch/`
4. **You verify** in Xcode that everything works
5. **You create** the Android project in Android Studio

This hybrid approach minimizes risk of breaking the Xcode project.

---

## GITIGNORE ADDITIONS NEEDED

```gitignore
# Android
*.iml
.gradle/
local.properties
.idea/
*.hprof
android/app/build/
android/build/
android/.gradle/
*.apk
*.aab
*.dex
*.class

# Android Studio
.idea/workspace.xml
.idea/tasks.xml
.idea/libraries/
.idea/caches/
captures/
.externalNativeBuild/
.cxx/
```

---

## Questions Before Proceeding

1. Do you want me to proceed with creating the directory structure and updating configs?
2. Would you prefer to move the iOS project yourself (safer) or should I attempt it via CLI (riskier)?
3. What should the Android app be named? (TableTorch to match iOS?)
