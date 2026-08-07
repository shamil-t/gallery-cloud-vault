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
        raise Exception(f"Command failed: {description}")
    return result.stdout

def is_tool_available(name):
    from shutil import which
    return which(name) is not None

def main():
    parser = argparse.ArgumentParser(description="Cloud Vault Release Flow")
    parser.add_argument("version", help="New version name (e.g., 1.2.2)")
    args = parser.parse_args()

    new_version_name = args.version

    # Store original contents for revert
    with open(GRADLE_FILE, 'r') as f:
        original_gradle = f.read()
    with open(UPDATE_JSON, 'r') as f:
        original_update = f.read()

    old_version_name, old_version_code = get_current_gradle_version()

    version_changed = new_version_name != old_version_name
    new_version_code = old_version_code + 1 if version_changed else old_version_code

    try:
        if version_changed:
            print(f"Starting release flow for v{new_version_name} (updating from v{old_version_name})...")
            # 1. Update files
            update_gradle_version(new_version_name, new_version_code)
            # Find previous version for release notes comparison
            # If version is changed, old_version_name is the previous one.
            update_update_json(new_version_name, old_version_name)
        else:
            print(f"Version {new_version_name} is already set. Skipping file updates.")

        # 2. Build Signed APK
        gradle_cmd = "gradlew" if os.name == 'nt' else "./gradlew"
        run_command(f"{gradle_cmd} assembleRelease", "Building signed APK")

        apk_path = "app/build/outputs/apk/release/app-release.apk"
        if not os.path.exists(apk_path):
            raise Exception(f"APK not found at {apk_path}")

        # 3. Git Operations
        if version_changed:
            run_command(f"git add {GRADLE_FILE} {UPDATE_JSON}", "Staging changes")
            run_command(f'git commit -m "Release v{new_version_name}"', "Committing release")

        run_command(f"git tag -f v{new_version_name} -m \"Version {new_version_name}\"", "Tagging release")
        run_command("git push origin main --tags", "Pushing to GitHub")

        # 4. GitHub Release
        if is_tool_available("gh"):
            # If version didn't change, we might need to delete the old release first or just let gh handle it
            # But usually we overwrite or create new. Using --overwrite if available or just create.
            gh_command = f'gh release create v{new_version_name} "{apk_path}" --title "Release v{new_version_name}" --notes "Release v{new_version_name}" --clobber'
            run_command(gh_command, "Creating/Updating GitHub release and uploading APK")
            print(f"Successfully released v{new_version_name} via GitHub CLI!")
        else:
            print("\n" + "="*50)
            print("GITHUB CLI (gh) NOT FOUND")
            print("Git push was successful. Please create the release manually:")
            print(f"1. Go to: https://github.com/{REPO_OWNER}/{REPO_NAME}/releases/new")
            print(f"2. Select Tag: v{new_version_name}")
            print(f"3. Release Title: Release v{new_version_name}")
            print(f"4. Upload the APK from: {os.path.abspath(apk_path)}")
            print("="*50 + "\n")

    except Exception as e:
        print(f"\nCRITICAL: Release failed: {e}")
        if version_changed:
            print("Reverting version changes in local files...")
            with open(GRADLE_FILE, 'w') as f:
                f.write(original_gradle)
            with open(UPDATE_JSON, 'w') as f:
                f.write(original_update)
            print("Files restored to original state.")
        sys.exit(1)

if __name__ == "__main__":
    main()
