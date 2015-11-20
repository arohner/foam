(ns foam.core)

(defprotocol IDisplayName
  (display-name [this]))

(defprotocol IInitState
  (init-state [this]))

(defprotocol IShouldUpdate
  (should-update [this next-props next-state]))

(defprotocol IWillMount
  (will-mount [this]))

(defprotocol IDidMount
  (did-mount [this]))

(defprotocol IWillUnmount
  (will-unmount [this]))

(defprotocol IWillUpdate
  (will-update [this next-props next-state]))

(defprotocol IDidUpdate
  (did-update [this prev-props prev-state]))

(defprotocol IWillReceiveProps
  (will-receive-props [this next-props]))

(defprotocol IRender
  (render [this]))

(defprotocol IRenderProps
  (render-props [this props state]))

(defprotocol IRenderState
  (render-state [this state]))

(defprotocol ICursor
  (-path [cursor])
  (-state [cursor]))

(defprotocol IValue
  (-value [x]))

(defprotocol IToCursor
  (-to-cursor [value state] [value state path]))

(declare to-cursor)

(defn cursor? [x]
  (satisfies? ICursor x))

(defprotocol ICursorDerive
  (-derive [cursor derived state path]))

(deftype MapCursor [value state path]
  clojure.lang.IDeref
  (deref [this]
    (get-in @state path ::invalid))
  IValue
  (-value [_] value)
  ICursor
  (-path [_] path)
  (-state [_] state)
  clojure.lang.ILookup
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k not-found]
    (let [v (get value k ::not-found)]
      (if-not (= v ::not-found)
        (-derive this v state (conj path k))
        not-found))))

(defn to-cursor
  ([val] (to-cursor val nil []))
  ([val state] (to-cursor val state []))
  ([val state path]
    (cond
      (cursor? val) val
      (map? val) (MapCursor. val state path)
      :else val)))

(extend-type Object
  ICursorDerive
  (-derive [this derived state path]
    (to-cursor derived state path)))

(defn valid-component? [x f]
  (assert
   (or (satisfies? IRender x)
       (satisfies? IRenderProps x)
       (satisfies? IRenderState x))
   (str "Invalid Om component fn, " f
        " does not return valid instance")))

(defn path [cursor]
  (-path cursor))

(defn value [cursor]
  (-value cursor))

(defn state [cursor]
  (-state cursor))

(defprotocol IGetState
  (-get-state [this] [this ks]))

(defprotocol ISetState
  (-set-state! [this val] [this ks val]))

(defprotocol ReactRender
  (react-render
    [this]
    "must return a ReactDOMRender instance"))

(defprotocol ReactDOMRender
  "represents a DOM node, implements render-to-string"
  (-children [this])
  (-render-to-string [this]))

(defn get-state
  "Returns the component local state of an owning component. owner is
   the component. An optional key or sequence of keys may be given to
   extract a specific value. Always returns pending state."
  ([owner]
   {:pre [(satisfies? IGetState owner)]}
   (-get-state owner))
  ([owner korks]
   {:pre [(satisfies? IGetState owner)]}
   (let [ks (if (sequential? korks) korks [korks])]
     (-get-state owner ks))))

(defn set-state!
  "Takes a pure owning component, a sequential list of keys and value and
   sets the state of the component. Conceptually analagous to React
   setState. Will schedule an Om re-render."
  ([owner v]
   {:pre [(satisfies? ISetState owner)]}
   (-set-state! owner v))
  ([owner korks v]
   {:pre [(satisfies? ISetState owner)]}
   (let [ks (if (sequential? korks) korks [korks])]
     (-set-state! owner ks v))))

(defn children [node]
  (let [c (-> node :children deref)]
    (if (ifn? c)
      (reset! (:children node) (c node))
      c)))

(defn valid-dom-tree? [x]
  (assert (satisfies? ReactDOMRender x))
  (assert (every? valid-dom-tree? (-children x)))
  (and (satisfies? ReactDOMRender x)
       (every? valid-dom-tree? (-children x))))

(defrecord OmComponent [cursor state children init-state]
  IGetState
  (-get-state
    [this]
    (-> this :state deref))
  (-get-state
    [this ks]
    (-> this :state deref (get-in ks)))
  ISetState
  (-set-state!
    [this val]
    (-> this :state (reset! val)))
  (-set-state!
    [this ks val]
    (swap! (:state this) assoc-in ks val))
  ReactRender
  (react-render [this]
    (let [c (foam.core/children this)
          ret (cond
                (satisfies? IRender c)
                (render c)

                (satisfies? IRenderState c)
                (render-state c (get-state this))
                :else c)
          ret (if (-children ret)
                (update-in ret [:children] (fn [children]
                                             (map (fn [c]
                                                    (cond
                                                      (satisfies? ReactRender c) (react-render c)
                                                      :else (assert false))) children)))
                ret)]
      (assert (valid-dom-tree? ret))
      ret)))

(defn component? [x]
  (instance? OmComponent x))

(defn valid-opts? [m]
  (every? #{:key :react-key :key-fn :fn :init-state :state
            :opts :shared ::index :instrument :descriptor}
          (keys m)))

(defn set-init-state! [com]
  (when (satisfies? IInitState (children com))
    (assert (-> com :state))
    (reset! (-> com :state) (-> com children init-state))))

(defn build
  ([f cursor m]
   {:pre [(ifn? f) (or (nil? m) (map? m))]}
   (assert (satisfies? ICursor cursor))
   (assert (valid-opts? m)
           (apply str "build options contains invalid keys, only :key, :key-fn :react-key, "
                  ":fn, :init-state, :state, and :opts allowed, given "
                  (interpose ", " (keys m))))
   (let [create-om-child (atom
                          (fn [this]
                            (let [ret (f cursor this nil)]
                              (valid-component? ret f)
                              ret)))
         com (cond
               (nil? m)
               (map->OmComponent {:cursor cursor
                                  :children create-om-child})

               :else
               (let [{:keys [key key-fn state init-state opts]} m
                     dataf   (get m :fn)
                     cursor' (if-not (nil? dataf)
                               (if-let [i (::index m)]
                                 (dataf cursor i)
                                 (dataf cursor))
                               cursor)
                     rkey    (cond
                               (not (nil? key)) (get cursor' key)
                               (not (nil? key-fn)) (key-fn cursor')
                               :else (get m :react-key))
                     _ (assert (or (nil? state) (map? state)))
                     state (atom state)]
                 (map->OmComponent {:cursor cursor'
                                    :state state
                                    :key (or rkey nil) ;; annoying
                                    :children create-om-child})))]
     (set-init-state! com)
     com)))

(defn root [f value {:keys [] :as options}])

(defn root-cursor
  "Given an application state atom return a root cursor for it."
  [atom]
  {:pre [(instance? clojure.lang.IDeref atom)]}
  (to-cursor @atom atom []))

(declare notify*)
(defn transact!
  "Given a tag, a cursor, an optional list of keys ks, mutate the tree
  at the path specified by the cursor + the optional keys by applying
  f to the specified value in the tree. An Om re-render will be
  triggered."
  ([cursor f]
   (transact! cursor [] f nil))
  ([cursor korks f]
   (transact! cursor korks f nil))
  ([cursor korks f tag]
   (let [state (foam.core/state cursor)
         old-state @state
         path (if (keyword? korks)
                [korks]
                (into (foam.core/path cursor) korks))
         ret (cond
               (empty? path) (swap! state f)
               :else (swap! state update-in path f))]
     (when-not (= ret ::defer)
       (let [tx-data {:path path
                      :old-value (get-in old-state path)
                      :new-value (get-in @state path)
                      :old-state old-state
                      :new-state @state}]
         ;; (if-not (nil? tag)
         ;;   (notify* cursor (assoc tx-data :tag tag))
         ;;   (notify* cursor tx-data))
         )))))

(defn update!
  "Like transact! but no function provided, instead a replacement
  value is given."
  ([cursor v]
   {:pre [(cursor? cursor)]}
   (transact! cursor [] (fn [_] v) nil))
  ([cursor korks v]
   {:pre [(cursor? cursor)]}
   (transact! cursor korks (fn [_] v) nil))
  ([cursor korks v tag]
   {:pre [(cursor? cursor)]}
   (transact! cursor korks (fn [_] v) tag)))

(defn update-state!
  "Takes a pure owning component, a sequential list of keys and a
   function to transition the state of the component. Conceptually
   analagous to React setState. Will schedule an Om re-render."
  ([owner f]
   {:pre [(component? owner) (ifn? f)]}
   (set-state! owner (f (get-state owner))))
  ([owner korks f]
   {:pre [(component? owner) (ifn? f)]}
   (set-state! owner korks (f (get-state owner korks)))))

(defn refresh!
  "Utility to re-render an owner."
  [owner]
  {:pre [(component? owner)]}
  (update-state! owner identity))

(defn get-props
  "Given an owning Pure node return the Om props. Analogous to React
  component props."
  ([x]
   {:pre [(component? x)]}
   (-> x :cursor))
  ([x korks]
   {:pre [(component? x)]}
   (let [korks (if (sequential? korks) korks [korks])]
     (cond-> (:cursor x)
       (seq korks) (get-in korks)))))

(defn get-node
  "A helper function to get at React DOM refs. Given a owning pure node
  extract the DOM ref specified by name."
  ([owner])
  ([owner node]
   ;; not sure it makes sense for foam to support this. Certainly DOM methods won't work on the node
   ))
