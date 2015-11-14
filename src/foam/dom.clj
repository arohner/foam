(ns foam.dom
  (:require [clojure.string :as str]
            [foam.core :as foam]))

(defprotocol ReactDOMRender
  (-render-to-string [this]))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. ^String (clojure.core/name text)
    (replace "&"  "&amp;")
    (replace "<"  "&lt;")
    (replace ">"  "&gt;")
    (replace "\"" "&quot;")))

(defn xml-attribute [name value]
  (str " " (clojure.core/name name) "=\"" (escape-html value) "\""))

(defn render-attribute [[name value]]
  (cond
    (true? value)
    (str " " (clojure.core/name name))
    (not value)
    ""
    :else
    (xml-attribute name value)))

(defn render-attr-map [attrs]
  (apply str
         (sort (map render-attribute attrs))))

(def ^{:doc "A list of tags that need an explicit ending tag when rendered."}
  container-tags
  #{"a" "b" "body" "canvas" "dd" "div" "dl" "dt" "em" "fieldset" "form" "h1" "h2" "h3"
    "h4" "h5" "h6" "head" "html" "i" "iframe" "label" "li" "ol" "option" "pre"
    "script" "span" "strong" "style" "table" "textarea" "ul"})

(defn render-element
  "Render an tag vector as a HTML element."
  [{:keys [tag attrs children]}]
  (if (or (seq children) (container-tags tag))
    (str "<" tag (render-attr-map attrs) ">"
         (apply str (map -render-to-string children))
         "</" tag ">")
    (str "<" tag (render-attr-map attrs) ">")))

(defrecord Element [tag attrs children]
  ReactDOMRender
  (-render-to-string [this]
    (render-element this)))

(defrecord Text [s]
  ReactDOMRender
  (-render-to-string [this]
    (assert (string? s))
    s))

(defn element [{:keys [tag attrs children]}]
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
