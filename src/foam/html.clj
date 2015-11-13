(ns foam.html
  (:require [foam.dom :as dom]
            [hiccup.core :as html]
            [schema.core :as s]))

;; hiccup-alike DSL, for foam.core

(declare html)

(defn eval-vector [expr]
  (let [tag (first expr)
        attrs (if (map? (second expr))
                (second expr)
                nil)
        body (if attrs
               (rest (rest expr))
               (rest expr))]
    (assert (keyword? tag))
    (dom/element {:tag tag
                  :attrs attrs
                  :children (mapcat html body)})))

(defn html [& content]
  (for [expr content]
    (do
      (cond
        (vector? expr) (eval-vector expr)
        (string? expr) (dom/text expr)
        :else expr))))
