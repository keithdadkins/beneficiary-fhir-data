#!/usr/bin/env bash
DOCS_DIR="${DOCS_DIR:-../../../../docs}"
ANNOTATIONS_FILE="${ANNOTATIONS_FILE:-./annotations.md}"
ABBREVIATIONS_FILE="${ABBREVIATIONS_FILE:-$DOCS_DIR/about/glossary/abbreviations.md}"


# exit if no glossary
[[ ! -f $ABBREVIATIONS_FILE ]] && echo "Error: cannot parse glossary to build annotations." && exit 1

# rebuild the annotations.md file
rm -f "$ANNOTATIONS_FILE"
echo "<!-- this file is auto generated - please do not edit directly -->" > "$ANNOTATIONS_FILE"

# parse the glossary
grep -E ^- "$ABBREVIATIONS_FILE" \
  | cut -d' ' -f2- \
  | sort \
  | sed 's/[[:space:]]*=[[:space:]]*/]: /g' \
  | awk '{print "*[" $0}' >> "$ANNOTATIONS_FILE"

# ensure each doc has the abbreviations embedded at the end of the file.. ie
# foo.md
# ...
#
# --8<-- "utils/mkdocs/.includes/annotations.md"
#
docs=()
while IFS='' read -r line; do
  docs+=("$line")
done < <(find "$DOCS_DIR" -type f -name "*.md" -not -path "$ABBREVIATIONS_FILE" -not -path './runbooks/README.md' -not -path '*.venv*' -not -path '*.includes*' -exec echo {} \;)

for doc in "${docs[@]}"; do
  if ! grep -axE '^--8<--\ \"ops\/utils\/mkdocs\/\includes\/annotations\.md\"$' "$doc" >/dev/null 2>&1; then
      {
        echo ""
        echo "<!-- adds annotations to abbreviations -->"
        echo '--8<-- "ops/utils/mkdocs/includes/annotations.md"'
      } >> "$doc"
  fi
done
