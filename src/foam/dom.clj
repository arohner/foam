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

(def ^:dynamic *html-mode* :xhtml)

(defn xml-mode? []
  (#{:xml :xhtml} *html-mode*))

(defn html-mode? []
  (#{:html :xhtml} *html-mode*))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  {:pre [(string? text)]
   :post [(string? %)]}
  (.. ^String (clojure.core/name text)
    (replace "&"  "&amp;")
    (replace "<"  "&lt;")
    (replace ">"  "&gt;")
    (replace "\"" "&quot;")))

(defn xml-attribute [name value]
  (let [actual-name (if (= name :className) "class" name)]
    (cond (not (= name :dangerouslySetInnerHTML))
      (str " " (clojure.core/name actual-name) "=\"" (escape-html value) "\""))))

(defn render-attribute [[name value]]
  (cond
    (true? value) (str " " (clojure.core/name name))
    (fn? value) ""
    (not value) ""
    :else (xml-attribute name value)))

(defn render-attr-map [attrs]
  (apply str
         (clojure.core/map render-attribute attrs)))

(defn render-inner [attrs children]
  (if (:dangerouslySetInnerHTML attrs)
    (get-in attrs [:dangerouslySetInnerHTML :__html])
    (apply str (clojure.core/map foam/-render-to-string children))))

(def ^{:doc "A list of elements that must be rendered without a closing tag."
       :private true}
  void-tags
  #{"area" "base" "br" "col" "command" "embed" "hr" "img" "input" "keygen" "link"
    "meta" "param" "source" "track" "wbr"})

(defn container-tag?
  "Returns true if the tag has content or is not a void tag. In non-HTML modes,
  all contentless tags are assumed to be void tags."
  [tag content]
  (or content
      (and (html-mode?) (not (void-tags tag)))))

(defn react-id-str [react-id]
  (assert (vector? react-id))
  (str "." (str/join "." react-id)))

(defn render-element
  "Render an tag vector as a HTML element string."
  [{:keys [tag attrs react-id children]}]
  (let [html
        (str "<" tag
             (render-attr-map attrs)
             (when react-id
               (format " data-reactid=\"%s\"" (cond
                                                (vector? react-id) (react-id-str react-id)
                                                (integer? react-id) (str react-id))))
             ">")]
    (if (container-tag? tag (seq children))
      (str html (render-inner attrs children) "</" tag ">")
      html)))

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

(extend-type clojure.lang.LazySeq
  foam/ReactRender
  (react-render [this]
    (map foam/react-render this))
  foam/ReactDOMRender
  (-children [this]
    this)
  (-render-to-string [this]
    (str/join "" (map foam/-render-to-string this))))

(defn text-node
  "HTML text node"
  [s]
  (map->Text {:s s}))

(defn assert-valid-element! [e]
  (assert (or (satisfies? foam/ReactDOMRender e)
              (satisfies? foam/ReactRender e)) (format "%s is does not satisfy foam/ReactDOMRender" e))

  (when (satisfies? foam/ReactDOMRender e)
    (doseq [c (foam/-children e)]
      (assert-valid-element! c))))

(defn valid-element? [e]
  (assert-valid-element! e)
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
  (let [children (doall (->> (clojure.core/map (fn [c]
                                                 (cond
                                                   (satisfies? foam/ReactDOMRender c) c
                                                   (satisfies? foam/ReactRender c) c
                                                   (string? c) (text-node c)
                                                   (nil? c) nil
                                                   :else (do
                                                           (println "invalid child element:" c (class c))
                                                           (assert false)))) children)
                             (filter identity)))]

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

(declare assign-react-ids)

(defn determine-react-id [index element parent-id]
  (if (associative? element)
    (let [current-part (Integer/toString index 30)]
      (assign-react-ids element (conj parent-id current-part)))
    element))


(defn assign-react-ids
  ([elem]
   (assign-react-ids elem [0]))
  ([elem id]
   (assert (vector? id))
   (let [elem (assoc elem :react-id id)]
     (update-in elem [:children] (fn [children]
                                   (map-indexed (fn [index element] (determine-react-id index element id)) children))))))

(defn get-next-id [counter]
  (let [v @counter]
    (swap! counter inc)
    v))

(defn assign-react-ids-15
  "Algorithm for assigning react-id in react 15 and higher"
  ([elem]
   (let [counter (atom 1)]
     (-> elem
         (assoc-in [:attrs :data-reactroot] "")
         (assign-react-ids-15 counter))))
  ([elem counter]
   (let [elem (assoc elem :react-id (get-next-id counter))]
     (update-in elem [:children] (fn [children]
                                   (mapv (fn [elem]
                                           (assign-react-ids-15 elem counter)) children))))))

(def mod-number 65521)

(defn react-adler32* [data max-index a b i]
  (if (= max-index i)
    (bit-or a (clojure.lang.Numbers/shiftLeftInt b 16))
    (let [a (mod (+ a (Character/codePointAt data i)) mod-number)
          b (mod (+ a b) mod-number)]
      (recur data max-index a b (inc i)))))

(defn react-adler32 [data]
  (react-adler32* data (count data) 1 0 0))

(defn add-checksum-to-markup [markup]
  (let [checksum (react-adler32 markup)]
    (str/replace-first markup ">" (str " " "data-react-checksum" "=\"" checksum "\">"))
  ))

(defn render-to-string [com]
  (-> com
      (foam/react-render)
      (assign-react-ids-15)
      (foam/-render-to-string)
      (add-checksum-to-markup)))
