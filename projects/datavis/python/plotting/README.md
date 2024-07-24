This notebook demonstrates a self-contained workflow for visualizing Python plots in current Clojure tooling using the [Kindly](https://scicloj.github.io/kindly/) convention.

The only dependency necessary is the [Libpython-clj](https://github.com/clj-python/libpython-clj) bridge. Some Kindly-compatible tool is needed to make the visualization visible. This was rendered using [Clay](https://scicloj.github.io/clay/) as an extra dev dependency.

The implementation is inspired by the [Parens for Pyplot](https://gigasquidsoftware.com/blog/2020/01/18/parens-for-pyplot/) tutorial by Carin Meier from Jan 2020. It has been part of the [Noj](https://scicloj.github.io/noj/) library till version `1-alpha34`, but as of July 2024, we are looking for a better place to host these functions, possibly Libpyton-clj itself.


