#!/usr/bin/env python3
"""
extract_screenshots.py

Extracts screenshot attachments from an .xcresult bundle using
xcresulttool export attachments (Xcode 16+).

Reads the generated manifest.json to rename files from UUIDs to
their deterministic names (e.g., "05_lowlight_color0.png").

Usage:
    python3 extract_screenshots.py <path/to.xcresult> <output_dir>
"""

import json
import os
import re
import shutil
import subprocess
import sys
import tempfile


# Matches a UUID pattern: 8-4-4-4-12 hex chars
UUID_RE = re.compile(r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")


def main():
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <xcresult_path> <output_dir>")
        sys.exit(1)

    xcresult_path = sys.argv[1]
    output_dir = sys.argv[2]

    if not os.path.exists(xcresult_path):
        print(f"  [WARN] Result bundle not found: {xcresult_path}")
        sys.exit(1)

    os.makedirs(output_dir, exist_ok=True)

    # Export attachments to a temp directory
    with tempfile.TemporaryDirectory() as tmp_dir:
        result = subprocess.run(
            [
                "xcrun", "xcresulttool", "export", "attachments",
                "--path", xcresult_path,
                "--output-path", tmp_dir,
            ],
            capture_output=True,
            text=True,
        )

        if result.returncode != 0:
            print(f"  [WARN] xcresulttool export failed: {result.stderr.strip()}")
            sys.exit(1)

        # Read the manifest to map UUID filenames to attachment names
        manifest_path = os.path.join(tmp_dir, "manifest.json")
        if not os.path.exists(manifest_path):
            print("  [WARN] No manifest.json found in export output")
            sys.exit(1)

        with open(manifest_path) as f:
            manifest = json.load(f)

        extracted = 0
        for entry in manifest:
            attachments = entry.get("attachments", [])
            for att in attachments:
                uuid_filename = att.get("exportedFileName", "")
                suggested_name = att.get("suggestedHumanReadableName", "")

                if not uuid_filename:
                    continue

                src_path = os.path.join(tmp_dir, uuid_filename)
                if not os.path.exists(src_path):
                    continue

                # Only process image files
                _, ext = os.path.splitext(uuid_filename)
                if ext.lower() not in (".png", ".jpg", ".jpeg"):
                    continue

                # Clean the suggested name to get deterministic filename
                final_name = clean_name(suggested_name, ext)
                dst_path = os.path.join(output_dir, final_name)

                shutil.copy2(src_path, dst_path)
                extracted += 1

        print(f"  [EXTRACT] {extracted} screenshots -> {output_dir}")


def clean_name(suggested: str, fallback_ext: str) -> str:
    """
    Convert suggested name to a clean deterministic filename.

    xcresulttool produces names like:
        "05_lowlight_color0_0_6FEABA28-1FA6-4752-BD68-72B86456FDF9.png"

    We want:
        "05_lowlight_color0.png"

    Strategy: remove the trailing "_0_<UUID>" suffix.
    """
    if not suggested:
        return f"unknown{fallback_ext}"

    base, ext = os.path.splitext(suggested)
    if not ext:
        ext = fallback_ext

    # Remove trailing _<run-index>_<UUID>
    # Pattern: _0_XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
    cleaned = re.sub(r"_\d+_" + UUID_RE.pattern + r"$", "", base)

    if not cleaned:
        return f"unknown{ext}"

    return cleaned + ext


if __name__ == "__main__":
    main()
