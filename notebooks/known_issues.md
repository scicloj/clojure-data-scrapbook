# Known Issues

If you encounter:
_Error building classpath. Could not acquire write lock for 'artifact:org.bytedeco:mkl'_

Try: `clj -P -Sthreads 1`

See [deps issue report](https://clojurians-log.clojureverse.org/tools-deps/2021-09-16).

