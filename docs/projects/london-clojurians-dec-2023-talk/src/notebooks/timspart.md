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

## Graphs

::: {layout-ncol=2}
![](https://hummi.app/img/sociogram.png)

![](https://hummi.app/img/factors.png)
:::

::: {style="display: flex; align-items: center; justify-content: center"}

https://Hummi.app

<img src="https://hummi.app/img/hummi.svg" width=100 alt="Hummi logo" style="float:left">

:::

## Books <img alt="books" width=400 src="books-stack-of-three.svg" align="right">

::: {.fragment}
you can learn anything
:::

::: {.fragment}
libraries rule
:::

::: {.fragment}
scicloj: clojure data cookbook
:::

::: {.notes}
In June 2023 I joined a SciCloj visual tools meetup.
Kira and Daniel gave some updates on the Clojure Data Cookbook and Scrapbook.
They requested some early feedback.
I made some suggestions, and we had a followup session to discuss the books further.
Beside the content, there was a troubling question of how to set up the project.
:::

## How to code a book? <img alt="books" width=400 src="books-stack-of-three.svg" align="right" >

::: {.fragment}
::: {.callout-tip appearance="simple"}
keeping the code in sync with prose
:::
:::

::: {.fragment}
literate coding

chapters are namespaces

```clojure
(ns cookbook.chapter1)

;; Welcome to the Clojure Data Cookbook

(defn example1 [] ...)
```

:::

::: {.fragment}
prose, code, and visualizations
:::

::: {.fragment}
explorable, reproducible, testable
:::

## Kindly <img src="Kindly.svg" align="right" width=400>

::: {.fragment }
::: {.callout-tip appearance="simple"}
a standard for requesting visualizations
:::
:::

::: {.fragment}
requests are annotations

```clojure
^:kind/hiccup [:svg [:circle {:r 50}]]
```

:::

::: {.fragment}
with a functional api

```clojure
(kind/hiccup [:svg [:circle {:r 50}]])
```

:::

::: {.fragment}
supported by tools (`clay`),

or adapted to tools (`kind-portal`, `kind-clerk`)
:::

## Kindly promise <img src="Kindly.svg" align="right" width=400>

just works with whatever tools you want to use

no breaking changes

notebooks, blogs, books, library code, application code

easy for toolmakers to support

## Clay <img src="Clay.svg" align="right" width=400>

dynamic visualization

static documents

## Obstacles <img alt="books" width=400 src="books-stack-of-three.svg" align="right">

::: {.fragment}
collating chapters
:::

::: {.fragment}
tool specific code
:::

::: {.fragment}
poor performance
:::

::: {.fragment}
git churn
:::

::: {.fragment}
::: {.callout-warning appearance="simple"}
lots of tools, no complete path
:::
:::

## Quarto <img src="https://avatars.githubusercontent.com/u/67437475?s=2048&v=4" align="right" width=400>

::: {.fragment}
markdown books, slides, and websites
:::

::: {.fragment}
based on pandoc
:::

::: {.fragment}
R and python
:::

::: {.fragment}
::: {.callout-tip appearance="simple"}
focus on creating markdown suitable for Quarto
:::
:::

## Claykind experiment <img src="claykind.svg" align="right" width=400>

::: {.fragment}
can `clay` be simpler?
:::

::: {.fragment}
leave markdown to `quarto`
:::

::: {.fragment}
github flavored markdown (documentation)
:::

::: {.fragment}
eval as data (babashka, testing)
:::

::: {.fragment}
untangle into components
:::

::: {.fragment}
::: {.callout-tip appearance="simple"}
a simpler thing might be more useful
:::
:::

## {background-image="babashka-markdown.png" background-size="contain"}

## {background-image="babashka-book.png" background-size="contain"}

## Eval as data

```clojure
(ns my.example)
(+ 1 2)
...
```

> ```clojure
>[{:code  "(ns my.example)"
>   :form  (ns my.example)
>   :value nil
>   :kind  nil}
>  {:code  "(+ 1 2)"
>   :form  (+ 1 2)
>   :value 3
>   :kind  nil}
>   ...]
>```

::: {.fragment }
::: {.callout-tip appearance="simple"}
`read-kinds`
:::
:::

## Discoveries

::: {.fragment}
`kindly` needs no behavior
:::

::: {.fragment}
tool-makers need `kindly-advice`

multiple ways to annotate, nested annotations, kind inference (images, datasets, user extensible)
:::

::: {.fragment}
users depend on a **tool** version (dev only)
:::

::: {.fragment}
it's good to know when values change `note-to-test`
:::

## Clay v2 <img src="Clay.svg" align="right" width=400>

::: {.fragment}
lightweight (may include JavaScript)
:::

::: {.fragment}
images and data as files
:::

::: {.fragment}
**loads fast**
:::

::: {.fragment}
separation of source from target
:::

::: {.fragment}
render a project, file, or form
:::

::: {.fragment}
serve the file (or not)
:::

::: {.fragment}
::: {.callout-note appearance="simple"}
**books**, slides, and websites!
:::
:::

::: {.notes}

* way faster!
* no ClojureScript
* no embedded images/data
* less git churn

:::

## Configuration <img src="Clay.svg" align="right" width=400>

options file: `clay.edn`

```clojure
{:format      [:html]
 :source-path "notebooks/index.clj"}
```

to render

```clojure
(clay/make! options)
```

extra options are merged

::: {.fragment }
::: {.callout-tip appearance="simple"}
bind a key to

```clojure
(make! {:source-path ~current-file
        :format      [:html]})
```

to view progress
:::
:::

## Write, don't show  <img src="Clay.svg" align="right" width=400>

```clojure
{:show false}
```

## Multiple namespaces <img src="Clay.svg" align="right" width=400>

```clojure
{:source-path ["notebooks/slides.clj"
               "notebooks/index.clj"]}
```

## Single form <img src="Clay.svg" align="right" width=400>

```clojure
{:source-path "notebooks/index.clj"
 :single-form '(kind/cytoscape
                 [{:style {:width  "300px"
                           :height "300px"}}
                  cytoscape-example])}
```

::: {.fragment }
::: {.callout-tip appearance="simple"}
bind a key to

```clojure
(make! {:source-path ~current-file
        :single-form ~form-before-caret
        :show        true})
```

for interactive development
:::
:::

## Use Quarto <img src="Clay.svg" align="right" width=400>

```clojure
{:format [:quarto :html]}
```

## Only Markdown <img src="Clay.svg" align="right" width=400>

```clojure
{:format     [:quarto :html]
 :run-quarto false}
```

## Slides <img src="Clay.svg" align="right" width=400>

```clojure
{:format      [:quarto :revealjs]
 :source-path "notebooks/slides.clj"}
```

## Books <img src="Clay.svg" align="right" width=400>

```clojure
{:format           [:quarto :html]
 :base-source-path "notebooks"
 :source-path      ["index.clj"
                    "chapter.clj"
                    "another_chapter.md"]
 :base-target-path "book"
 :show             false
 :run-quarto       false
 :book             {:title "Book Example"}}
```

::: {.fragment }
::: {.callout-tip appearance="simple"}
so flexible

everything is decoupled

adaptable to your workflow

interactive development

static publishing

```sh
clojure -M:dev -m scicloj.clay.main
```

:::
:::

## How to literate <img src="path.svg" align="right" width=400>

::: {.fragment}
start a namespace
:::

::: {.fragment}
explore some question, idea, data
:::

::: {.fragment}
prose, code, tables, images, charts
:::

::: {.fragment}
visualize from your editor
:::

::: {.fragment}
publish it
:::

::: {.fragment}
read as document or explore code
:::

::: {.fragment }
::: {.callout-tip appearance="simple"}

```clojure
{:deps    {org.scicloj/kindly ...}
 :aliases {:dev {:deps {org.scicloj/clay ...}}}}
```

:::
:::

## What I learnt

## About Data {background-image="TMD.svg" background-size="contain"}

::: {.fragment}
we all have questions
:::

::: {.fragment}
we observe
:::

::: {.fragment}
we write
:::

::: {.fragment }
::: {.callout-tip appearance="simple"}
everyone is a data scientist?

everyone can benefit from a path to literate
:::
:::

## About Writing {background-image="Cookbook.svg" background-size="contain"}

::: {.fragment}
crystallizes thinking
:::

::: {.fragment}
creates knowledge
:::

::: {.fragment}
in code
:::

::: {.fragment }
::: {.callout-tip appearance="simple"}
notebooks are namespaces
:::
:::

## Critical Thinking {background-image="scicloj.ml.svg" background-size="contain"}

::: {.fragment}
requires imagination
:::

::: {.fragment}
what else is possible?
:::

::: {.fragment}
a creative process
:::

::: {.fragment }
::: {.callout-tip appearance="simple"}
it is not enough to be critical, you have to create
:::
:::

## About Simplicity {background-image="Metamorph.svg" background-size="contain"}

::: {.fragment}
brings clarity and focus
:::

::: {.fragment}
and sometimes universality
:::

::: {.fragment }
::: {.callout-tip appearance="simple"}
we make workflows out of simple libraries and tools
:::
:::

::: {.notes}
Universality is being true in all situations
:::

## Standardization {background-image="Kindly.svg" background-size="contain"}

::: {.fragment}
establishes an interface
:::

::: {.fragment}
selectable parts
:::

::: {.fragment }
::: {.callout-tip appearance="simple"}
fosters composition
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

## About Composition {background-image="Clay.svg" background-size="contain"}

::: {.fragment}
unity and variety
:::

::: {.fragment}
experimentation and creativity
:::

::: {.fragment}
crystallization
:::

::: {.fragment }
::: {.callout-tip appearance="simple"}
lightweight, adaptable parts

complete path from ideas to publication
:::
:::

::: {.notes}
Clay
Interactively developing
Publishing
:::

## About Connecting {background-image="clj-djl.svg" background-size="contain"}

::: {.fragment}
provides purpose
:::

::: {.fragment}
provides fulfillment
:::

::: {.fragment}
::: {.callout-tip appearance="simple"}
builds community
:::
:::

## Community {background-image="all.svg" background-size="contain"}

::: {.fragment}
learn together
:::

::: {.fragment}
solve problems together
:::

::: {.fragment}
::: {.callout-tip appearance="simple"}
scicloj was born of a simple vision:
small groups working on problems together

thinking, collaborating, solving, and writing
:::
:::

::: {.fragment}
::: {.callout-note appearance="simple"}
we'd love you to join us in search of answers

https://scicloj.github.io/

consider joining a study group, working group, or suggest a new group

your community needs you!
:::
:::

## EOF {background-image="all.svg" background-size="contain"}
