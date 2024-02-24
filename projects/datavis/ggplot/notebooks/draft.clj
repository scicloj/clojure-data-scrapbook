(ns draft
  (:require [clojisr.v1.r :as r :refer [r r$ r->clj]]
            [clojisr.v1.applications.plotting :as plotting]
            [representing]))

;; # Draft explorations

(-> "(ggplot(mpg, aes(cty, hwy)))"
    r
    representing/ggplot->clj
    (dissoc :data))


(-> "(ggplot(mpg, aes(cty, hwy)))"
    r
    representing/ggplot->clj
    (dissoc :data))

(-> "(ggplot(mpg, mapping=aes(x=cty, y=hwy)))"
    r
    representing/ggplot->clj
    (dissoc :data))

(-> "(ggplot(mpg, mapping=aes(x=cty, y=hwy))
      + geom_point(mapping=aes(color=cyl), size=5)
      + ylim(c(20,30)))"
    r
    representing/ggplot->clj
    (dissoc :data))

(-> "(ggplot(mpg, mapping=aes(x=cty, y=hwy))
      + geom_point(mapping=aes(color=factor(cyl)), size=20))"
    r
    plotting/plot->buffered-image)

(-> "(ggplot(mpg, mapping=aes(x=cty, y=hwy))
      + geom_point(mapping=aes(color=cyl), size=5)
      + ylim(c(20,30))
      + scale_x_log10())"
    r
    representing/ggplot->clj
    (dissoc :data))

(-> "(ggplot(mpg, mapping=aes(x=cty, y=hwy))
      + geom_point(mapping=aes(color=cyl), size=5)
      + ylim(c(20,30))
      + scale_x_log10()
      + theme_linedraw())"
    r
    representing/ggplot->clj
    (dissoc :data))

(-> "(ggplot(data.frame(x = rnorm(1000, 2, 2)), aes(x)) +
      geom_histogram(aes(y=..density..)) +  # scale histogram y
      geom_density(col = 'red', size=5))"
    r
    plotting/plot->buffered-image)

(-> "(ggplot(data.frame(x = rnorm(1000, 2, 2)), aes(x)) +
      geom_histogram(aes(y=..density..)) +  # scale histogram y
      geom_density(col = 'red'))"
    r
    representing/ggplot->clj
    (dissoc :data))
