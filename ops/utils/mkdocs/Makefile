.ONESHELL:
.DEFAULT_GOAL := help

.PHONY: help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-30s\033[0m %s\n", $$1, $$2}'

docs: .venv/bin/activate ## Build and serve the docs using python3 virtualenv (live reload)
	@sleep 2 && open http://localhost:8000 >/dev/null 2>&1 &
	@source .venv/bin/activate && mkdocs serve
.PHONY: docs

.venv/bin/activate:
	@python3 -m venv .venv && \
	source .venv/bin/activate && \
	pip3 install -r requirements.txt

build: .venv/bin/activate .docs/index.html ## Just build the docs
	@echo "Building docs..."
	@source .venv/bin/activate && mkdocs build
.PHONY: build

# build if index.html file is not present
.docs/index.html: build

# build annotations from the glossary (see utils/mkdocs/.includes/README.md)
annotations:
	@ABBREVIATIONS_FILE=../../runbooks/glossary/abbreviations.md ANNOTATIONS_FILE=includes/annotations.md includes/build-and-include-annotations.sh
.PHONY: annotations

## Serve built docs
serve: .docs/index.html
	@sleep 2 && open http://localhost:8001 >/dev/null 2>&1 &
	@python3 -m http.server 8001 --bind 127.0.0.1 --directory .docs
.PHONY: serve

clean: ## Remove all mkdocs artifacts
	@printf "Removing mkdocs artifacts.. "
	@rm -rf .docs
	@rm -rf .venv
	@echo "DONE"
.PHONY: clean
