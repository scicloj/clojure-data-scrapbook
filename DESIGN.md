# Design [DRAFT]

## Rationale

There is a need for documentation on the modern Clojure Data Science stack.

[Clojure for Data Science in the Real World - Kira McLean](https://www.youtube.com/watch?v=MguatDl5u2Q)

[Real @toms with Clojure! - Thomas Clark and Daniel Slutsky](https://www.youtube.com/watch?v=SE5Ge4QP4oY)

[Cooking up a workflow for data - Kira McLean, Daniel Slutsky, and Timothy Prately](https://www.youtube.com/watch?v=skMMvxWjmNM&t=3749s)

## Roles

We anticipate that this project will be used by people in 3 roles:

* users: Reading the book. Running code examples
* contributors: Adding chapters, fixing bugs
* editors: Organizing, standardising, tidying up, helping the other roles.

## Working with Markdown

Quarto can compile the book and slides (reveal.js), and is customizable.

Babaska -> .qmd would be useful

## Multiple projects vs mono project

The scrapbook will contain multiple projects to support curated setups with specific dependencies. Small projects with precise dependencies can serve as standalone examples which are more didactic.
There will be some big projects that contain multiple examples of the same setup (e.g., using [Noj](https://github.com/scicloj/noj)).

## Hierarchy

Each project will have its own structure of book/notebook/slideshow. 

One main index maintained by the editors will link to all projects.

Projects which are book will have one namespace (or markdown file for each chapter of the book).

```
    Scrapbook index
        Noj documentation project
            Data Visualization topic
                "Chapter 1: Intro to Visualization" -- scrapbook.noj.visualization.intro namespace
                    Heading 1, Heading 2, etc all in this namespace
```

## Curatind dependencies

* An opinionated, curated list of good starting libraries is useful. We encourage such projects.
* This may be done using Branded/opinionated sets of defaults like [Noj](https://github.com/scicloj/noj).
