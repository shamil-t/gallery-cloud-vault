import re
import json
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

def main():
    parser = argparse.ArgumentParser(description="Manage Gradle and update.json versions")
    parser.add_argument("version", help="New version name (e.g., 1.2.2)")
    args = parser.parse_args()

    new_version_name = args.version
    try:
        old_version_name, old_version_code = get_current_gradle_version()

        if new_version_name == old_version_name:
            print(f"Version {new_version_name} is already set. No changes made.")
            return

        new_version_code = old_version_code + 1
        update_gradle_version(new_version_name, new_version_code)
        update_update_json(new_version_name, old_version_name)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
