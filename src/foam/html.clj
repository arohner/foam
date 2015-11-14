(ns foam.html
  (:require [foam.dom :as dom]))

;; hiccup-alike DSL, for foam.core

(declare html)

(def ^{:doc "Regular expression that parses a CSS-style id and class from a tag name." :private true}
  re-tag #"([^\s\.#]+)?(?:#([^\s\.#]+))?(?:\.([^\s#]+))?")

(defn normalize-element
  "Ensure a tag vector is of the form [tag-name attrs content]."
  [[tag & content]]
  (when (not (or (keyword? tag) (symbol? tag) (string? tag)))
    (throw (IllegalArgumentException. (str tag " is not a valid tag name."))))
  (let [[_ tag id class] (re-matches re-tag (name tag))
        tag (or tag "div")
        tag-attrs        {:id id
                          :class (if class (.replace ^String class "." " "))}
        map-attrs        (first content)]
    (if (map? map-attrs)
      [tag (merge tag-attrs map-attrs) (next content)]
      [tag tag-attrs content])))

(defn eval-vector [expr]
  (let [[tag attrs content] (normalize-element expr)]
    (dom/element {:tag tag
                  :attrs attrs
                  :children (mapcat html content)})))

(defn html [& content]
  (for [expr content]
    (do
      (cond
        (vector? expr) (eval-vector expr)
        (string? expr) (dom/text expr)
        :else expr))))
