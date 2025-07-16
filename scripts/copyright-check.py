#!/usr/bin/env python3

import argparse
import os
import re
import difflib
from datetime import datetime
from typing import Tuple, List, Dict

CURRENT_YEAR = datetime.now().year

# File type to comment format mapping
FILE_TYPE_PATTERNS = {
    # Java/Kotlin/C-style comments
    'java': {
        'spdx_pattern': re.compile(
            r"/\*\n"
            r" \* SPDX-FileCopyrightText: (?P<years>\d{4}(?:-\d{4})?) (?P<owner>.+)\n"
            r" \* SPDX-License-Identifier: (?P<license>.+)\n"
            r" \*/",
            re.MULTILINE
        ),
        'non_spdx_pattern': re.compile(
            r"/\*[\s\S]{0,5}?"
            r"(Copyright|\(c\))\s+(?P<years>\d{4}(?:-\d{4})?)\s+(?P<owner>[^.\n]+)[.\n]"
            r".*?Licensed under (the )?(?P<license>[^.\n]+)",
            re.IGNORECASE
        ),
        'header_template': (
            "/*\n"
            " * SPDX-FileCopyrightText: {years} {owner}\n"
            " * SPDX-License-Identifier: {license}\n"
            " */"
        )
    },
    # Python/Shell/YAML style comments
    'python': {
        'spdx_pattern': re.compile(
            r"#\n"
            r"# SPDX-FileCopyrightText: (?P<years>\d{4}(?:-\d{4})?) (?P<owner>.+)\n"
            r"# SPDX-License-Identifier: (?P<license>.+)\n"
            r"#",
            re.MULTILINE
        ),
        'non_spdx_pattern': re.compile(
            r"#[\s\S]{0,5}?"
            r"(Copyright|\(c\))\s+(?P<years>\d{4}(?:-\d{4})?)\s+(?P<owner>[^.\n]+)[.\n]"
            r".*?Licensed under (the )?(?P<license>[^.\n]+)",
            re.IGNORECASE
        ),
        'header_template': (
            "#\n"
            "# SPDX-FileCopyrightText: {years} {owner}\n"
            "# SPDX-License-Identifier: {license}\n"
            "#"
        )
    },
    # XML/HTML style comments
    'xml': {
        'spdx_pattern': re.compile(
            r"<!--\n"
            r"  SPDX-FileCopyrightText: (?P<years>\d{4}(?:-\d{4})?) (?P<owner>.+)\n"
            r"  SPDX-License-Identifier: (?P<license>.+)\n"
            r"-->",
            re.MULTILINE
        ),
        'non_spdx_pattern': re.compile(
            r"<!--[\s\S]{0,5}?"
            r"(Copyright|\(c\))\s+(?P<years>\d{4}(?:-\d{4})?)\s+(?P<owner>[^.\n]+)[.\n]"
            r".*?Licensed under (the )?(?P<license>[^.\n]+)",
            re.IGNORECASE
        ),
        'header_template': (
            "<!--\n"
            "  SPDX-FileCopyrightText: {years} {owner}\n"
            "  SPDX-License-Identifier: {license}\n"
            "-->"
        )
    }
}

# File extension to comment type mapping
EXTENSION_MAP = {
    '.java': 'java',
    '.kt': 'java',
    '.kts': 'java',
    '.js': 'java',
    '.ts': 'java',
    '.c': 'java',
    '.cpp': 'java',
    '.h': 'java',
    '.hpp': 'java',
    '.css': 'java',
    '.py': 'python',
    '.sh': 'python',
    '.yaml': 'python',
    '.yml': 'python',
    '.toml': 'python',
    '.conf': 'python',
    '.properties': 'python',
    '.xml': 'xml',
    '.html': 'xml',
    '.xhtml': 'xml',
    '.svg': 'xml'
}

LICENSE_NAME_MAP = {
    "Apache License, Version 2.0": "Apache-2.0",
    "Apache 2.0": "Apache-2.0",
    "MIT": "MIT",
    "GPL v3": "GPL-3.0-only",
    "GPLv3": "GPL-3.0-only",
}

def normalize_license(name: str, fallback: str) -> str:
    return LICENSE_NAME_MAP.get(name.strip(), fallback)

def get_file_type(file_path: str) -> str:
    """Determine the comment style based on file extension."""
    ext = os.path.splitext(file_path)[1].lower()
    return EXTENSION_MAP.get(ext, 'java')  # Default to Java-style comments

def get_updated_years(existing_years: str) -> str:
    if "-" in existing_years:
        start_year, _ = existing_years.split("-")
        return f"{start_year}-{CURRENT_YEAR}"
    return f"{existing_years}-{CURRENT_YEAR}" if existing_years != str(CURRENT_YEAR) else existing_years

def update_or_insert_header(content: str, file_path: str, fallback_license: str, fallback_owner: str) -> Tuple[str, str]:
    file_type = get_file_type(file_path)
    patterns = FILE_TYPE_PATTERNS[file_type]
    
    spdx_pattern = patterns['spdx_pattern']
    non_spdx_pattern = patterns['non_spdx_pattern']
    header_template = patterns['header_template']
    
    match = spdx_pattern.search(content)
    if match:
        years = get_updated_years(match.group("years"))
        license = match.group("license")
        owner = match.group("owner")
        new_header = header_template.format(years=years, license=license, owner=owner)
        return (spdx_pattern.sub(new_header, content, count=1), "spdx_updated")

    match = non_spdx_pattern.search(content)
    if match:
        years = get_updated_years(match.group("years"))
        owner = match.group("owner").strip()
        license = normalize_license(match.group("license"), fallback_license)
        new_header = header_template.format(years=years, license=license, owner=owner)
        return (non_spdx_pattern.sub(new_header, content, count=1), "non_spdx_converted")

    new_header = header_template.format(years=str(CURRENT_YEAR), license=fallback_license, owner=fallback_owner)
    return (new_header + "\n\n" + content, "header_inserted")

def process_file(file_path: str, fallback_license: str, fallback_owner: str, dry_run: bool) -> str:
    with open(file_path, 'r', encoding='utf-8') as f:
        original = f.read()

    updated, change_type = update_or_insert_header(original, file_path, fallback_license, fallback_owner)

    if original != updated:
        print(f"{'[DRY-RUN]' if dry_run else '[UPDATED]'} {file_path}")
        if dry_run:
            diff = difflib.unified_diff(
                original.splitlines(),
                updated.splitlines(),
                fromfile='original',
                tofile='updated',
                lineterm=''
            )
            for line in list(diff)[:12]:
                print(line)
        else:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(updated)
        return change_type
    return "unchanged"

def collect_files(root_dir: str, extensions: List[str], recursive: bool) -> List[str]:
    files = []
    for dirpath, _, filenames in os.walk(root_dir):
        for file in filenames:
            if any(file.endswith(ext) for ext in extensions):
                files.append(os.path.join(dirpath, file))
        if not recursive:
            break
    return files

def format_summary(stats: Dict[str, int], dry_run: bool, file_types: Dict[str, int]) -> str:
    lines = [
        "\n----------------------------------------",
        "SPDX Header Update Summary",
        "----------------------------------------",
        f"Total files scanned:     {stats['total']}",
        f"Dry-run mode:            {'Yes' if dry_run else 'No'}",
        f"Files with changes:      {stats['spdx_updated'] + stats['non_spdx_converted'] + stats['header_inserted']}",
        f" - SPDX header updated:   {stats['spdx_updated']}",
        f" - Non-SPDX converted:    {stats['non_spdx_converted']}",
        f" - Header inserted:       {stats['header_inserted']}",
        f"Files unchanged:         {stats['unchanged']}",
        "",
        "File types processed:",
    ]
    
    for file_type, count in sorted(file_types.items()):
        lines.append(f" - {file_type} files:       {count}")
    
    lines.append("----------------------------------------")
    return "\n".join(lines)

def main():
    parser = argparse.ArgumentParser(description="Update or insert SPDX headers in source files.")
    parser.add_argument("extensions", nargs="+", help="File extensions to match (e.g. java js)")
    parser.add_argument("-r", "--recursive", action="store_true", help="Recursively process subdirectories")
    parser.add_argument("-p", "--path", default=".", help="Path to start processing (default: current directory)")
    parser.add_argument("-l", "--fallback-license", default="Apache-2.0", help="Fallback license identifier")
    parser.add_argument("-o", "--fallback-owner", default="Lucimber UG", help="Fallback copyright holder")
    parser.add_argument("-d", "--dry-run", action="store_true", help="Preview changes without modifying files")
    parser.add_argument("-s", "--summary", action="store_true", help="Show summary after execution")
    parser.add_argument("-f", "--summary-file", help="Write summary output to specified file (e.g. summary.txt)")

    args = parser.parse_args()
    extensions = [ext if ext.startswith('.') else f'.{ext}' for ext in args.extensions]
    files = collect_files(args.path, extensions, args.recursive)

    stats = {
        "total": 0,
        "spdx_updated": 0,
        "non_spdx_converted": 0,
        "header_inserted": 0,
        "unchanged": 0,
    }
    
    file_types = {}

    for file_path in files:
        # Track file type statistics
        file_type = get_file_type(file_path)
        file_types[file_type] = file_types.get(file_type, 0) + 1
        
        result = process_file(file_path, args.fallback_license, args.fallback_owner, args.dry_run)
        stats["total"] += 1
        if result == "spdx_updated":
            stats["spdx_updated"] += 1
        elif result == "non_spdx_converted":
            stats["non_spdx_converted"] += 1
        elif result == "header_inserted":
            stats["header_inserted"] += 1
        elif result == "unchanged":
            stats["unchanged"] += 1

    if args.summary or args.summary_file:
        summary_text = format_summary(stats, args.dry_run, file_types)
        if args.summary:
            print(summary_text)
        if args.summary_file:
            with open(args.summary_file, 'w', encoding='utf-8') as f:
                f.write(summary_text + "\n")
            print(f"Summary written to: {args.summary_file}")
    
    # Exit with code 1 if files need updating (in dry-run mode)
    if args.dry_run:
        files_needing_update = stats['spdx_updated'] + stats['non_spdx_converted'] + stats['header_inserted']
        if files_needing_update > 0:
            exit(1)
    
    # Exit with code 0 (success) in all other cases
    exit(0)

if __name__ == "__main__":
    main()
