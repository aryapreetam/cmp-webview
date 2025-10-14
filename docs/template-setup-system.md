# Template Setup System

This document describes the automated template setup system for the Compose Multiplatform Library Template.

## Overview

When someone uses this template to create a new library, they need to customize:
- Project name (e.g., `cmp-mediaviewer`)
- Maven coordinates (group ID, artifact name, version)
- Package structure
- Developer information
- GitHub URLs

Instead of manual find-replace, we provide an **automated setup script** that handles everything.

## Components

### 1. Setup Scripts

#### `setup-template.sh` (Primary - Unix/Linux/macOS)
- Interactive shell script
- Prompts for all configuration values
- Automatically updates all files
- Creates package directory structure
- Can be run multiple times safely
- Saves configuration to `.template-config.json`

#### `setup-template.bat` (Windows fallback)
- Basic batch script for Windows users
- Limited functionality - recommends using Git Bash or WSL

### 2. Configuration File

#### `.template-config.json`
- Stores user's configuration
- Allows script to detect if already configured
- Shows current values when re-running
- **Gitignored** - each user has their own config

Example:
```json
{
  "configured": true,
  "repo_name": "cmp-mediaviewer",
  "artifact_name": "mediaviewer",
  "github_org": "johnsmith",
  "group_id": "io.github.johnsmith",
  "developer_name": "John Smith",
  "library_description": "A modern media viewer",
  "package_name": "io.github.johnsmith.mediaviewer",
  "package_path": "io/github/johnsmith/mediaviewer",
  "version": "1.0.0"
}
```

### 3. Gradle Validation

#### `gradle/check-template-setup.gradle.kts`
- Gradle task that checks if template is configured
- Looks for "cmp-lib-template" in `settings.gradle.kts`
- **Automatically runs** before build tasks
- Shows prominent error if not configured

Applied in `build.gradle.kts`:
```kotlin
apply(from = "gradle/check-template-setup.gradle.kts")
```

### 4. README Warning

#### Conditional warning banner in `README.MD`
- Shows prominent warning at top of README
- Visible on GitHub repository page
- Instructs users to run `./setup-template.sh`
- User removes manually after setup

## User Flow

### First-Time Setup

1. User clicks "Use this template" on GitHub
2. Creates repository: `cmp-mediaviewer`
3. Clones the repository
4. Opens README → sees big warning banner
5. Runs `./setup-template.sh`
6. Script prompts for values (with smart defaults)
7. Script updates all files automatically
8. Script creates package structure
9. User commits changes
10. User removes README warning banner

### What Gets Updated Automatically

The script updates:
- ✅ `settings.gradle.kts` - Project name
- ✅ `lib/build.gradle.kts` - Maven coordinates (group, artifact, version), URLs, POM metadata
- ✅ `CONTRIBUTING.md` - Repository URLs and examples
- ✅ `README.MD` - All references to template names

The script creates:
- ✅ `lib/src/commonMain/kotlin/{package-path}/` - New package directory
- ✅ `lib/src/commonTest/kotlin/{package-path}/` - New test directory
- ✅ `lib/src/commonMain/kotlin/{package-path}/README.md` - Instructions

The script preserves:
- ✅ `lib/src/commonMain/kotlin/fiblib/` - Example code (as reference)
- ✅ `sample/` - Working sample app (uses fiblib example)

## Design Decisions

### Why Keep Example Code?

**Decision:** Don't delete or refactor the `fiblib` example code

**Reasons:**
1. **Working reference** - Users can see a complete example
2. **Sample app works** - No broken imports immediately after setup
3. **Less complex** - No need to update imports across multiple files
4. **User choice** - They delete it when ready
5. **Safer** - No risk of breaking things during setup

### Why Not Use GitHub Template Variables?

**Decision:** Use local setup script instead of GitHub's template system

**Reasons:**
1. **GitHub doesn't support template variables** - No native find-replace during creation
2. **Can't rename directories** - Package structure needs manual creation
3. **Need user input** - Maven coordinates, developer info, etc.
4. **More flexible** - Can run multiple times, validate inputs

### Why Check in Gradle?

**Decision:** Add automatic validation before builds

**Reasons:**
1. **Fail fast** - Don't let users build unconfigured template
2. **Clear message** - Explain exactly what to do
3. **Automatic** - No need to remember to run check
4. **Visible** - Shows up immediately on first build attempt

## Testing the Setup

### Test 1: Fresh Template (Current State)
```bash
./gradlew checkTemplateSetup
# Expected: ❌ Error with instructions to run setup script
```

### Test 2: After Running Setup
```bash
./setup-template.sh
# Follow prompts...
./gradlew checkTemplateSetup
# Expected: ✅ "Template is configured"
```

### Test 3: Re-running Setup
```bash
./setup-template.sh
# Expected: Shows current config, asks to reconfigure
```

## Future Enhancements

Potential improvements:
- [ ] PowerShell version of setup script
- [ ] Validation of package names (no invalid characters)
- [ ] GitHub Action to auto-run setup (if possible)
- [ ] Option to delete example code during setup
- [ ] Option to update sample app imports during setup

## Files Reference

### New Files Added
```
setup-template.sh          - Main setup script (Unix/Linux/macOS)
setup-template.bat         - Windows batch script
.template-config.json      - Configuration storage (gitignored)
gradle/check-template-setup.gradle.kts - Gradle validation task
```

### Modified Files
```
README.MD                  - Added warning banner
build.gradle.kts          - Applied validation script
.gitignore                - Ignore .template-config.json
docs/using-this-template.md - Updated with setup instructions
```

## Maintenance Notes

When updating the template:

1. **Adding new configurable values:**
   - Add to setup script prompts
   - Add to .template-config.json structure
   - Add find-replace logic in script

2. **Adding new files that need updates:**
   - Add sed/replace commands to setup script
   - Document in README

3. **Testing:**
   - Test on fresh clone
   - Test re-running script
   - Test Gradle validation
   - Test on Windows (Git Bash)

## Support

For users experiencing issues:
- Check `docs/using-this-template.md` for detailed guide
- Verify Git Bash or WSL is being used on Windows
- Check `.template-config.json` exists after running script
- Run `./gradlew checkTemplateSetup` to verify
