^:kindly/hide-code?
(ns notebooks.combining-tools
  (:require [scicloj.kindly.v4.kind :as kind]))

(kind/md "# Combining tools for data exploration")

(kind/md "##
![](https://scicloj.github.io/sci-cloj-logo-transparent.png)

[scicloj.org](http://scicloj.org)
")

(kind/md "##
![](https://raw.githubusercontent.com/practicalli-john/london-clojurians-logo/master/london-clojurians-logo.svg)
")

(kind/md "##
![](https://sessionize.com/image/68c6-1140o400o3-Boo2pA28xQfvKskyXqwcmd.jpg)")


(kind/md "## Plan
- Dynamic report making with slow processes
- Data processing & visualization in harmony
")

(kind/md "## Use case: analysing commit logs
- Which projects are encouraging community contributions?")

(kind/md "## Data pipeline
- take the 1000 most popular Clojure repos (according to Github's API)
- .. and also the Python ones
- clone them
- look into the commit logs
- process & visualize")

(kind/md "## Limitations
- a biased data sample
- descriptive statistics, not statistical inference
- interpretation is not obvious")

(kind/md "# Dynamic report making with slow processes")

(kind/md "## Slow processes
- collecting remote data
- fetching API responses
- applying slow AI models")

(kind/md "# Data processing & visualization in harmony")

(kind/md "## Lots of existing functionality

- dtype-next - array processing
- tech.ml.dataset - high-performance datasets
- Tablecloth - dataset ergonomics
- Fasthmath - lots of math & stats
- scicloj.ml - machine learning, data modelling
- Hanami - data visualization
- Libpython-clj, ClojisR - interop
- Visual tools
- etc.")

(kind/md "## Noj
- collecting the relevant dependencies
- composing them for common use cases")

(kind/md "# Thanks")
