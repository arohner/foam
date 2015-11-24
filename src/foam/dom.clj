(ns foam.dom
  (:require [clojure.string :as str]
            [foam.core :as foam]))

;; tags that om.dom defines functions for
(def om-tags
  '[a
    abbr
    address
    area
    article
    aside
    audio
    b
    base
    bdi
    bdo
    big
    blockquote
    body
    br
    button
    canvas
    caption
    cite
    code
    col
    colgroup
    data
    datalist
    dd
    del
    dfn
    div
    dl
    dt
    em
    embed
    fieldset
    figcaption
    figure
    footer
    form
    h1
    h2
    h3
    h4
    h5
    h6
    head
    header
    hr
    html
    i
    iframe
    img
    ins
    kbd
    keygen
    label
    legend
    li
    link
    main
    map
    mark
    marquee
    menu
    menuitem
    meta
    meter
    nav
    noscript
    object
    ol
    optgroup
    output
    p
    param
    pre
    progress
    q
    rp
    rt
    ruby
    s
    samp
    script
    section
    select
    small
    source
    span
    strong
    style
    sub
    summary
    sup
    table
    tbody
    td
    tfoot
    th
    thead
    time
    title
    tr
    track
    u
    ul
    var
    video
    wbr

    ;; svg
    circle
    ellipse
    g
    line
    path
    polyline
    rect
    svg
    text
    defs
    linearGradient
    polygon
    radialGradient
    stop
    tspan
    use])

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
    (true? value) (str " " (clojure.core/name name))
    (fn? value) ""
    (not value) ""
    :else (xml-attribute name value)))

(defn render-attr-map [attrs]
  (apply str
         (sort (clojure.core/map render-attribute attrs))))

(def ^{:doc "A list of tags that need an explicit ending tag when rendered."}
  container-tags
  #{"a" "b" "body" "canvas" "dd" "div" "dl" "dt" "em" "fieldset" "form" "h1" "h2" "h3"
    "h4" "h5" "h6" "head" "html" "i" "iframe" "label" "li" "ol" "option" "pre"
    "script" "span" "strong" "style" "table" "textarea" "ul"})

(defn react-id-str [react-id]
  (assert (vector? react-id))
  (str "." (str/join "." react-id)))

(defn render-element
  "Render an tag vector as a HTML element."
  [{:keys [tag attrs react-id children]}]
  (assert react-id)
  (let [attrs (merge {:data-react-id (react-id-str react-id)} attrs)]
    (if (or (seq children) (container-tags tag))
      (str "<" tag (render-attr-map attrs) ">"
           (apply str (clojure.core/map foam/-render-to-string children))
           "</" tag ">")
      (str "<" tag (render-attr-map attrs) ">"))))

(defrecord Element [tag attrs react-id children]
  foam/ReactRender
  (react-render [this]
    (update-in this [:children] (fn [children]
                                  (clojure.core/map (fn [c]
                                                      (cond
                                                        (satisfies? foam/ReactRender c) (foam/react-render c)
                                                        :else c)) children))))
  foam/ReactDOMRender
  (-children [this]
    children)
  (-render-to-string [this]
    (render-element this)))

(defrecord Text [s]
  foam/ReactDOMRender
  (-children [this]
    nil)
  (-render-to-string [this]
    (assert (string? s))
    s))

(defn text-node
  "HTML text node"
  [s]
  (map->Text {:s s}))

(defn valid-element? [e]
  (assert (satisfies? foam/ReactDOMRender e))
  (assert (every? (fn [c]
                    (or (satisfies? foam/ReactDOMRender c)
                        (satisfies? foam/ReactRender c))) (foam/-children e)))
  (and (satisfies? foam/ReactDOMRender e)
       (every? (fn [c]
                 (or (satisfies? foam/ReactDOMRender c)
                     (satisfies? foam/ReactRender c))) (foam/-children e))))

(defn element
  "Creates a dom node."
  [{:keys [tag attrs children] :as elem}]
  {:post [(valid-element? %)]}
  (assert (name tag))
  (assert (or (nil? attrs) (map? attrs)) (format "elem %s attrs invalid" elem))
  (let [children (clojure.core/map (fn [c]
                                     (cond
                                       (satisfies? foam/ReactDOMRender c) c
                                       (satisfies? foam/ReactRender c) c
                                       (string? c) (text-node c)
                                       :else (do
                                               (inspect c)
                                               (assert false c)))) children)]

    (map->Element {:tag (name tag)
                   :attrs attrs
                   :children children})))

(defn def-tag-fn [tag]
  `(defn ~tag [~'attrs & ~'children]
     (element {:tag (quote ~tag)
               :attrs ~'attrs
               :children ~'children})))

(defmacro def-all-tags []
  `(do
     ~@(clojure.core/map def-tag-fn om-tags)))

(def-all-tags)

(defn assign-react-ids
  ([elem]
   (assign-react-ids elem [0]))
  ([elem id]
   (assert (vector? id))
   (let [elem (assoc elem :react-id id)]
     (update-in elem [:children] (fn [children]
                                   (map-indexed (fn [i c]
                                                  (assign-react-ids c (conj id i))) children))))))
(defn render-to-string [com]
  (let [elem (foam/react-render com)
        elem (assign-react-ids elem)]
    (foam/-render-to-string elem)))
