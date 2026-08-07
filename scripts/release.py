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

    new_content, count1 = re.subn(r'versionName = ".*?"', f'versionName = "{new_version_name}"', content)
    new_content, count2 = re.subn(r'versionCode = \d+', f'versionCode = {new_version_code}', new_content)

    if count1 == 0 or count2 == 0:
        raise Exception(f"Failed to update version in {GRADLE_FILE}. Matches found: versionName={count1}, versionCode={count2}")

    with open(GRADLE_FILE, 'w') as f:
        f.write(new_content)
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

def run_command(command, description, exit_on_error=True):
    print(f"Running: {description}...")
    result = subprocess.run(command, shell=True, capture_output=True, text=True)
    if result.returncode != 0 and exit_on_error:
        print(f"Error during {description}:")
        print(result.stdout)
        print(result.stderr)
        raise Exception(f"Command failed: {description}")
    return result.stdout

def is_tool_available(name):
    from shutil import which
    return which(name) is not None

def get_previous_tag():
    try:
        # Get the second most recent tag (the one before the one we are about to create/overwrite)
        tags = subprocess.check_output("git tag --sort=-creatordate", shell=True, text=True).strip().split('\n')
        return tags[0] if tags else "v1.0.0"
    except:
        return "v1.0.0"

def create_github_release_api(tag, apk_path, token):
    print(f"Creating GitHub release {tag} via API...")

    # 1. Create the release
    release_data = {
        "tag_name": tag,
        "name": f"Release {tag}",
        "body": f"Automated release for {tag}",
        "draft": False,
        "prerelease": False,
        "make_latest": "true"
    }

    import json
    import tempfile

    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.json') as tf:
        json.dump(release_data, tf)
        tf_path = tf.name

    try:
        # Create release (or get existing)
        create_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/releases"
        auth_header = f"Authorization: Bearer {token}"

        # Check if release exists
        check_url = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}/releases/tags/{tag}"
        check_res = subprocess.run(f'curl -s -H "{auth_header}" {check_url}', shell=True, capture_output=True, text=True)

        release_id = None
        if check_res.returncode == 0:
            try:
                res_json = json.loads(check_res.stdout)
                if "id" in res_json:
                    release_id = res_json["id"]
                    print(f"Existing release found (ID: {release_id}).")
            except:
                pass

        if not release_id:
            create_res = subprocess.run(f'curl -s -X POST -H "{auth_header}" -H "Accept: application/vnd.github+json" -d @{tf_path} {create_url}', shell=True, capture_output=True, text=True)
            try:
                res_json = json.loads(create_res.stdout)
                release_id = res_json.get("id")
            except:
                raise Exception(f"Failed to create release: {create_res.stdout}")

            if not release_id:
                raise Exception(f"Failed to create release (no ID): {create_res.stdout}")
            print(f"Release created (ID: {release_id}).")

        # 2. Upload APK
        filename = "GalleryVault.apk"
        upload_url = f"https://uploads.github.com/repos/{REPO_OWNER}/{REPO_NAME}/releases/{release_id}/assets?name={filename}"

        print(f"Uploading {apk_path} as {filename}...")
        # Use -L to follow redirects and --data-binary to upload file
        upload_res = subprocess.run(f'curl -s -X POST -H "{auth_header}" -H "Content-Type: application/vnd.android.package-archive" --data-binary @"{apk_path}" "{upload_url}"', shell=True, capture_output=True, text=True)

        if '"id"' in upload_res.stdout:
            print("APK uploaded successfully!")
        elif '"already_exists"' in upload_res.stdout:
            print("Asset already exists. Skipping upload (or delete it manually on GitHub).")
        else:
            print(f"Upload warning/error: {upload_res.stdout}")

    finally:
        os.unlink(tf_path)

def main():
    parser = argparse.ArgumentParser(description="Cloud Vault Release Flow")
    parser.add_argument("version", help="New version name (e.g., 1.2.2)")
    args = parser.parse_args()

    new_version_name = args.version
    github_token = os.environ.get("GITHUB_TOKEN")

    # Store original contents for revert
    with open(GRADLE_FILE, 'r') as f:
        original_gradle = f.read()
    with open(UPDATE_JSON, 'r') as f:
        original_update = f.read()

    old_version_name, old_version_code = get_current_gradle_version()

    # Robust previous version detection for release notes
    previous_tag = get_previous_tag()
    if previous_tag.startswith('v'):
        prev_version_str = previous_tag[1:]
    else:
        prev_version_str = previous_tag

    version_changed = new_version_name != old_version_name
    new_version_code = old_version_code + 1 if version_changed else old_version_code

    try:
        if version_changed:
            print(f"Starting release flow for v{new_version_name} (updating from v{old_version_name})...")
            # 1. Update files
            update_gradle_version(new_version_name, new_version_code)
            # Use the actual previous tag string for the comparison link
            update_update_json(new_version_name, prev_version_str)
        else:
            print(f"Version {new_version_name} is already set. Skipping file updates.")

        # 2. Build Signed APK
        gradle_cmd = "gradlew" if os.name == 'nt' else "./gradlew"
        run_command(f"{gradle_cmd} assembleRelease", "Building signed APK")

        apk_path = "app/build/outputs/apk/release/app-release.apk"
        if not os.path.exists(apk_path):
            raise Exception(f"APK not found at {apk_path}")

        # 3. Git Operations
        current_branch = run_command("git branch --show-current", "Getting current branch").strip()

        if version_changed:
            run_command(f"git add {GRADLE_FILE} {UPDATE_JSON}", "Staging changes")

            # Check if there are actually staged changes to commit
            staged = run_command(f"git status --porcelain {GRADLE_FILE} {UPDATE_JSON}", "Checking staged status", exit_on_error=False).strip()
            if staged:
                run_command(f'git commit -m "Release v{new_version_name}"', "Committing release")
            else:
                print("No changes to commit (files might already be at the target version).")

        run_command(f"git tag -f v{new_version_name} -m \"Version {new_version_name}\"", "Tagging release")
        run_command(f"git push origin {current_branch} --tags", f"Pushing to GitHub ({current_branch})")

        # 4. GitHub Release
        if github_token:
            create_github_release_api(f"v{new_version_name}", apk_path, github_token)
        elif is_tool_available("gh"):
            # If version didn't change, we might need to delete the old release first or just let gh handle it
            # But usually we overwrite or create new. Using --overwrite if available or just create.
            gh_command = f'gh release create v{new_version_name} "{apk_path}" --title "Release v{new_version_name}" --notes "Release v{new_version_name}" --clobber'
            run_command(gh_command, "Creating/Updating GitHub release and uploading APK")
            print(f"Successfully released v{new_version_name} via GitHub CLI!")
        else:
            print("\n" + "="*50)
            print("GITHUB TOKEN OR CLI NOT FOUND")
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
