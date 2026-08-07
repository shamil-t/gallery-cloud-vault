import os
import re
import json
import subprocess
import argparse
import sys

# Configuration
GRADLE_FILE = "app/build.gradle.kts"
UPDATE_JSON = "update.json"
REPO_OWNER = "shamil-t"
REPO_NAME = "gallery-cloud-vault"

def get_current_gradle_version():
    with open(GRADLE_FILE, 'r') as f:
        content = f.read()
        version_name = re.search(r'versionName = "(.*?)"', content).group(1)
        version_code = int(re.search(r'versionCode = (\d+)', content).group(1))
        return version_name, version_code

def update_gradle_version(new_version_name, new_version_code):
    with open(GRADLE_FILE, 'r') as f:
        content = f.read()

    content = re.sub(r'versionName = ".*?"', f'versionName = "{new_version_name}"', content)
    content = re.sub(r'versionCode = \d+', f'versionCode = {new_version_code}', content)

    with open(GRADLE_FILE, 'w') as f:
        f.write(content)
    print(f"Updated {GRADLE_FILE} to {new_version_name} ({new_version_code})")

def update_update_json(new_version_name, old_version_name):
    with open(UPDATE_JSON, 'r') as f:
        data = json.load(f)

    data["versionCode"] = new_version_name
    data["versionName"] = f"v{new_version_name}"
    data["latestReleaseUrl"] = f"https://github.com/{REPO_OWNER}/{REPO_NAME}/releases/download/v{new_version_name}/GalleryVault.apk"
    data["releaseNotes"] = f"https://github.com/{REPO_OWNER}/{REPO_NAME}/compare/v{old_version_name}...v{new_version_name}"

    with open(UPDATE_JSON, 'w') as f:
        json.dump(data, f, indent=2)
    print(f"Updated {UPDATE_JSON} for version {new_version_name}")

def run_command(command, description):
    print(f"Running: {description}...")
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error during {description}:")
        print(result.stdout)
        print(result.stderr)
        sys.exit(1)
    return result.stdout

def main():
    parser = argparse.ArgumentParser(description="Cloud Vault Release Flow")
    parser.add_argument("version", help="New version name (e.g., 1.2.2)")
    args = parser.parse_args()

    new_version_name = args.version
    old_version_name, old_version_code = get_current_gradle_version()
    new_version_code = old_version_code + 1

    print(f"Starting release flow for v{new_version_name}...")

    # 1. Update files
    update_gradle_version(new_version_name, new_version_code)
    update_update_json(new_version_name, old_version_name)

    # 2. Build Signed APK
    # Note: Requires RELEASE_STORE_FILE, etc. to be set in local.properties or Env
    run_command("./gradlew assembleRelease", "Building signed APK")

    apk_path = "app/build/outputs/apk/release/app-release.apk"
    if not os.path.exists(apk_path):
        # Check if it was renamed by gradle (sometimes happens)
        print("Warning: Standard APK path not found, checking alternatives...")
        # Add logic if needed, but standard is usually fine

    # 3. Git Operations
    run_command(f"git add {GRADLE_FILE} {UPDATE_JSON}", "Staging changes")
    run_command(f'git commit -m "Release v{new_version_name}"', "Committing release")
    run_command(f"git tag -a v{new_version_name} -m \"Version {new_version_name}\"", "Tagging release")
    run_command("git push origin main --tags", "Pushing to GitHub")

    # 4. GitHub Release (using gh CLI)
    gh_command = f'gh release create v{new_version_name} "{apk_path}" --title "Release v{new_version_name}" --notes "See comparison for changes: {old_version_name}...{new_version_name}"'
    run_command(gh_command, "Creating GitHub release and uploading APK")

    print(f"Successfully released v{new_version_name}!")

if __name__ == "__main__":
    main()
