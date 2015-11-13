# Foam

A hack to add server-side rendering to Om

Assumes you have a working CLJS Om app, and you want to add server-side rendering, without using selenium or node.

## Usage

foam provides a minimal reimplementation of the om.core, om.dom
namespaces in .clj code. Port your Om components and
app-state to .cljc code. In your component namespaces,

```clojure
(:require #?(:clj [foam.core :as om]
             :cljs [om.core :as om])
          #?(:clj [foam.dom :as dom]
             :cljs [om.dom :as dom]))
```

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

Lots. This is experimental, proof of concept.

## License

Copyright © 2015 Allen Rohner

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
