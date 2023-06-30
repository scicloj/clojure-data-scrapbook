# The Clojure Data Scrapbook project [DRAFT]

<img src="//scicloj.github.io/sci-cloj-logo-transparent.png" alt="SciCloj logo" width="100" align="right"/>

This project builds [The Clojure Data Scrapbook](https://scicloj.github.io/clojure-data-scrapbook/) that contains
Community-contributed examples for the emerging Clojure data stack.

You can interact with the examples by cloning this project and starting a REPL.

This project is part of the [SciCloj community](https://scicloj.github.io/docs/community/about/).

## Status

Work in progress.
Change are likely after more thought and sharing.

## Using this book

You can read the book online [The Clojure Data Scrapbook](https://scicloj.github.io/clojure-data-scrapbook/).

### Starting a REPL

To run this project you will need to first [Install Clojure](https://clojure.org/guides/install_clojure).

To run code examples, clone this repo and start a [REPL](https://clojure.org/guides/repl/introduction).
A browser window will open with the HTML of the book.

### Editor setup

[Clojure Editor Guide](https://clojure.org/guides/editors) covers general editor integration.

This project uses Clay for interactive examples.
Please see the [Clay Setup Guide](https://scicloj.github.io/clay/#setup)
for some additional configuration.

## Project overview

[./projects/](./projects) contains code, and will be of most interest to readers

> :bulb: [./projects/main/notebooks](./projects/main/notebooks) contains most of the miscellaneous examples

[./book/](./book) contains Markdown configuration and the `prepare.sh` script

> :bulb: [./book/_quarto.yml](./book/_quarto.yml) configures the ordering of the examples

[./docs](./docs) is the published part (generated from the markdown)

The code in `projects` is compiled to `.qmd` Markdown files, and then [Quarto](https://quarto.org/) converts those into the book HTML.

```mermaid
graph LR
namespace --> qmd --> html
```

## Troubleshooting

If you encounter:
_Error building classpath. Could not acquire write lock for 'artifact:org.bytedeco:mkl'_

Try: `clj -P -Sthreads 1`

See [deps issue report](https://clojurians-log.clojureverse.org/tools-deps/2021-09-16).

## Contributing

Please see [CONTRIBUTING.md](./CONTRIBUTING.md)

## Rationale and Design

Please see [DESIGN.md](./DESIGN.md).

## License

Copyright © 2022 Scicloj

Distributed under the Eclipse Public License version 1.0.
