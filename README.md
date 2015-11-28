# Foam

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

### Server-side Rendering

In clj, call

```clojure
(ns foo.bar
  (:require [foam.core :as foam
             foam.dom :as dom]))

(let [state (your-app-state)
      cursor (foam/root-cursor state)
      com (foam/build your-component cursor)]
 (dom/render-to-string com)
```

Serve that in your http response.

## Limitations

Lots. This is experimental, proof of concept. This list of limitations is not complete.

- Not all Om protocols have been ported over yet
- Bugs everywhere
- Most unusual options aren't supported yet. Cursors and app-state can currently only be maps.
- because CLJ doesn't support Javascript's ... special... arity-overloading, your om component functions must use the 3 arg constructor: `(defn foo [app owner opts])`. `foam.core/build` only provides the 3 arity version, to reduce confusion.

## License

Copyright © 2015 Allen Rohner

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

Contains code adapted from [hiccup](https://github.com/weavejester/hiccup)

Contains code adapted from [sablono](https://github.com/r0man/sablono)