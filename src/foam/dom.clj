(ns foam.dom
  (:require [clojure.string :as str]
            [foam.core :as foam]))

(defprotocol ReactDOMRender
  (-render-to-string [this]))

(defrecord Element [tag attrs children]
  ReactDOMRender
  (-render-to-string [this]
    (assert (keyword? tag))
    (format "<%s>%s</%s>" tag (str/join " " (map -render-to-string (:children this))) tag)))

(defrecord Text [s]
  ReactDOMRender
  (-render-to-string [this]
    (assert (string? s))
    s))

(defn element [{:keys [tag attrs children]}]
  (assert (keyword? tag))
  (assert (or (nil? attrs) (map? attrs)))
  (assert (every? (fn [c]
                    (when (not (satisfies? ReactDOMRender c))
                      (inspect c))
                    (satisfies? ReactDOMRender c)) children))
  (map->Element {:tag tag
                 :attrs attrs
                 :children children}))

(defn text [s]
  (map->Text {:s s}))

(defn render-to-string [com]
  (let [elems (foam/react-render com)]
    (assert (= 1 (count elems)))
    (-render-to-string (first elems))))
