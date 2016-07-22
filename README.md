# Foam

[![Clojars Project](http://clojars.org/arohner/foam/latest-version.svg)](http://clojars.org/arohner/foam)

[![Circle CI](https://circleci.com/gh/arohner/foam.svg?style=svg)](https://circleci.com/gh/arohner/foam)

A hack to add server-side rendering to Om

Assumes you have a working CLJS Om app, and you want to add server-side rendering, without using selenium or node.

## Usage

foam provides a minimal reimplementation of the om.core, om.dom
namespaces in .clj code. Port your Om components and
app-state to .cljc code. In your component namespaces:

```clojure
(:require #?(:clj [foam.core :as om]
             :cljs [om.core :as om])
          #?(:clj [foam.dom :as dom]
             :cljs [om.dom :as dom]))
```

Everything *should* work without needing to modify your component code, with the exception of a few known limitations listed below.

### App-State

Like normal Om, your appstate should be an atom wrapping a map or
vector. We recommend an app-state constructor function in .cljc, so
it's available to both CLJ and CLJS.

### Server-side Rendering

In clj, call

```clojure
(ns foo.bar
  (:require [foam.core :as foam]
            [foam.dom :as dom]))

(let [state (your-app-state)
      cursor (foam/root-cursor state)
      com (foam/build your-component cursor)]
 (dom/render-to-string com)
```

Serve that in your http response.

## Limitations

Lots. This is alpha, though [Rasterize](https://rasterize.io) is using
it in produciton. This list of limitations is not complete.

- Not all Om protocols have been ported over yet
- Bugs everywhere

## Version Compatiblity

React changed its algorithm for assigning react-ids to elements
between 0.14 and 15.0. If you're using react 15 or higher, you'll need
foam 0.1.8 or later. Conversely, use foam 0.1.7 for react < 15. Recent
Om alphas, such as `1.0.0-alpha40` are known to work with foam.

React < 15   | >= 15
Om     0.9   | 1.0.0-alpha40
foam   0.1.7 | 0.1.8


## License

Copyright © 2015 Allen Rohner

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

Contains code adapted from [Om](https://github.com/omcljs/om)

Contains code adapted from [hiccup](https://github.com/weavejester/hiccup)

Contains code adapted from [sablono](https://github.com/r0man/sablono)