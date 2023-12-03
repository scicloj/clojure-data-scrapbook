---
{ title: "Cooking up a data workflow",
  subtitle: "Your community needs you",
  author:
    { name: "Timothy Pratley",
      url: "timothypratley.blogspot.com",
      email: "timothypratley@gmail.com" },
  format:
    { revealjs:
        { theme: [ "default", "custom.scss" ],
          include-in-header:
            { text: '<link rel="shortcut icon" href="Cookbook.svg" />' } } } }
---

## Why am I here?

In June 2023 I joined a SciCloj visual tools meetup.
Kira and Daniel gave some updates on the Clojure Data Cookbook and Scrapbook.
They requested some early feedback.
I made some suggestions, and we had a followup session to discuss the books further.
Beside the content, there was a troubling question of how to set up the project.

## How to code a book?

We want to do literate coding, but there are obstacles.

* Namespaces as notebooks
* Collating
* Large diffs

## Claykind

::: {.fragment style="background:white"}
::: {.callout-tip appearance="simple"}

Reimagining Clay as modular parts in a Kindly pipeline.

:::
:::

::: {.fragment}

* a simpler thing might be more useful
* manage resources instead of embedding everything
* support Babashka
* enable testable notebooks
* continuous integration builds

:::

## Kindly

::: {.fragment style="background:white"}
::: {.callout-tip appearance="simple"}
A standard for requesting visualizations.
:::
:::

::: {.fragment}
Requests are metadata annotations

```clojure
^:kind/hiccup [:svg [:circle {:r 50}]]
```
:::

::: {.fragment}
Which can be made using the functional api

```clojure
(kind/hiccup [:svg [:circle {:r 50}]])
```
:::

::: {.fragment}
Supported by tools (Clay),

or adapted to tools (kind-portal, kind-clerk)
:::

## Kindly goals

* Just works with whatever tools you want to use
* No breaking changes
* Minimal user burden
* Maybe inference

## Discoveries

::: {.fragment}
kindly-advice
:::

::: {.fragment}
kindly-notes
:::

::: {.fragment}
options based api 
:::

::: {.fragment}
need for rendering many files, single file, or single form
:::

::: {.fragment}
decoupling of server
:::

## Philosophy

::: {.fragment}
Sort of...
:::

::: {.fragment}
I think...
:::

## Knowledge {background-image="scicloj.ml.svg" background-size="contain"}

::: {.fragment}
You can learn anything
:::

::: {.fragment}

* Recalling
* Mixing

:::

::: {.fragment style="background:white"}
::: {.callout-tip appearance="simple"}
New knowledge makes old problems solvable
:::
:::

::: {.notes}
Speaker notes go here.
:::

## Critical Thinking {background-image="clj-djl.svg" background-size="contain"}

::: {.fragment}
is a creative process
:::

::: {.fragment}
what else is possible?
:::

::: {.fragment style="background:white"}
::: {.callout-tip appearance="simple"}
It is not enough to be critical, you have to create
:::
:::

## Data-driven {background-image="TMD.svg" background-size="contain"}

::: {.fragment}
We have questions
:::

::: {.fragment}
We make observations
:::

::: {.fragment}
We write
:::

::: {.fragment style="background:white"}
::: {.callout-tip appearance="simple"}
Everyone is a data scientist
:::
:::

## Writing {background-image="Cookbook.svg" background-size="contain"}

::: {.fragment}
crystallizes thinking
:::

::: {.fragment}
creates knowledge
:::

::: {.fragment style="background:white"}

* Interleaving prose, tables, charts, diagrams
* Concise, clear, focused

:::

::: {.fragment}
In code
:::

::: {.fragment style="background:white"}
::: {.callout-tip appearance="simple"}
Namespaces as notebooks
:::
:::

## Standardization {background-image="Kindly.svg" background-size="contain"}

::: {.fragment}
enables individuality
:::

::: {.fragment}

* notebooks, blogs, books
* library code, application code
* keybindings, workflows

:::

::: {.fragment style="background:white"}
::: {.callout-tip appearance="simple"}
Tool agnostic interactive development and static publishing
:::
:::

::: {.notes}
Kindly

Namespaces as notebooks

Interleaving prose, tables, charts and diagrams, in code

Notebooks
Blogs
Books
Library code
Application code
:::

## Composition {background-image="Clay.svg" background-size="contain"}

::: {.fragment}
enables interactivity
:::

::: {.fragment}
enables publishing
:::

::: {.fragment}
enables reproducibility
:::

::: {.fragment style="background:white"}
::: {.callout-tip appearance="simple"}
Lightweight, adaptable tools
:::
:::

::: {.notes}
Clay
Interactively developing
Publishing
:::

## Simplicity {background-image="Metamorph.svg" background-size="contain"}

::: {.fragment}
begets universality
:::

::: {.fragment style="background:white"}
::: {.callout-tip appearance="simple"}
we make our own workflows out of libraries and tools
:::
:::

::: {.notes}
Universality is being true in all situations
:::

## Connecting

::: {.fragment}
provides purpose
:::

::: {.fragment}
provides fulfillment
:::

::: {.fragment}
builds community
:::

## Community {background-image="all.svg" background-size="contain"}

::: {.fragment style="background:white"}
::: {.callout-tip appearance="simple"}
SciCloj was born of a simple vision:
Small groups working on problems together.
Coordinating, collaborating, solving, and writing.
:::
:::

::: {.fragment style="background:white"}
We'd love you to join us in search of answers.

Your community needs you!
:::

## Salute
