# mkdocs annotations

Note: The `.includes` directory is not served or visible when viewing the docs site.

As described in the [docs](https://squidfunk.github.io/mkdocs-material/reference/abbreviations/#adding-abbreviations), there are two ways to implement annotations with abbreviations in mkdocs-material:

    1. By appending the annotations to the end of each file. This is tedius to maintain. Example:
        ```markdown
        Please install WSL

        <!-- below here will be hidden in the browser and the WSL above will be underlined -->
        *[WSL]: Windows Subsystem for Linux
        ```
    2. Or, move all annotiations/abbreviations in a dedicated file and then embed the file using the --8<-- notation at the end of each document.

The `build-and-include-annotations.sh` script automatically generates and appends our annotatations to implement option 2.
