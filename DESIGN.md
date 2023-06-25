# Design [DRAFT]

## Rationale

There is a need for documentation on the modern Clojure Data Science stack.

[Clojure for Data Science in the Real World - Kira McLean](https://www.youtube.com/watch?v=MguatDl5u2Q)

[Real @toms with Clojure! - Thomas Clark and Daniel Slutsky](https://www.youtube.com/watch?v=SE5Ge4QP4oY)

## Roles

We anticipate that this project will be used by people in 3 roles:

* users: Reading the book. Running code examples
* contributors: Adding chapters, fixing bugs
* editors: TBD

## Working with Markdown

Quarto can compile the book and slides (reveal.js), and is customizable.

Babaska -> .qmd would be useful

## Multiple projects vs mono project

* Dependency conflicts are possible
* Might be dependency heavy
* Less didactic to teach - would like a standalone example
* main - easier?

## Hierarchy of namespace

Data Visualization
book: Each namespace is a chapter

Alternatives?

* Each project is a chapter?
* Namespace is a heading?
* Just a list of things?

```
SciCloj
  DataScience
    Scrapbook [Book]
      Data Visualization [Section/volume]
        "Chapter 1: Visualization" -- visualization.clj [Chapter]
          Heading 1, Heading 2, etc all in this namespace
```

Many namespaces about visualization, adhoc.
Order of chapters could be by convention for nesting and ordering of the chapters.
Should there be a hierarchy? or a flat list, labels? tags?
Maybe we should avoid hierarchy because it cannot be maintained.
Rather rely on editor controlled config.

Turning namespaces into qmd files is currently manually; could be another automated step prior to prepare.

Why main? Common tasks e.g. tidyverse

* Opinionated, curated list of good starting libraries is useful
    - where to begin? here's a default set of good stuff
* Branded/opinionated defaults see [noj](https://github.com/scicloj/noj)
