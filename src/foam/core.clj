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

(deftype MapCursor [value state path]
  clojure.lang.IDeref
  (deref [this]
    (get-in @state path ::invalid))
  IValue
  (-value [_] value)
  ICursor
  (-path [_] path)
  (-state [_] state))

(defn cursor? [x]
  (satisfies? ICursor x))

(defn to-cursor
  ([val] (to-cursor val nil []))
  ([val state] (to-cursor val state []))
  ([val state path]
    (cond
      (cursor? val) val
      (map? val) (MapCursor. val state path)
      :else val)))

(defprotocol IValue
  (-value [x]))

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
  (react-render [this]))

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
    (let [c (children this)]
      (cond
        (satisfies? IRender c)
        (render c)

        (satisfies? IRenderState c)
        (render-state c (get-state this))
        :else c))))

(defn valid-opts? [m]
  (every? #{:key :react-key :key-fn :fn :init-state :state
            :opts :shared ::index :instrument :descriptor}
          (keys m)))

(defn build
  ([f cursor m]
   {:pre [(ifn? f) (or (nil? m) (map? m))]}
   (assert (satisfies? ICursor cursor))
   (assert (valid-opts? m)
           (apply str "build options contains invalid keys, only :key, :key-fn :react-key, "
                  ":fn, :init-state, :state, and :opts allowed, given "
                  (interpose ", " (keys m))))
   (cond
     (nil? m)
     (map->OmComponent {:cursor cursor
                        :children
                        (fn [this]
                          (let [ret (f cursor this nil)]
                            (valid-component? ret f)
                            ret))})

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
                          :init_state init-state
                          :state state
                          :key (or rkey nil) ;; annoying
                          :children
                          (if (nil? opts)
                            (fn [this]
                              (let [ret (f cursor' this nil)]
                                (valid-component? ret f)
                                ret))
                            (fn [this]
                              (let [ret (f cursor' this opts)]
                                (valid-component? ret f)
                                ret)))})))))

(defn root [f value {:keys [] :as options}])

(defn root-cursor
  "Given an application state atom return a root cursor for it."
  [atom]
  {:pre [(instance? clojure.lang.IDeref atom)]}
  (to-cursor @atom atom []))

(defn get-state
  ([owner]
   (println "foam get-state owner"))
  ([owner korks]
   (println "foam get-state owner korks")))

(defn set-state!
  ([owner v]
   (println "foam.core/set-state owner v"))
  ([owner korks v]
   (println "foam.core/set-state owner korks v")))

(defn transact!
  "Given a tag, a cursor, an optional list of keys ks, mutate the tree
  at the path specified by the cursor + the optional keys by applying
  f to the specified value in the tree. An Om re-render will be
  triggered."
  ([cursor f])
  ([cursor korks f])
  ([cursor korks f tag]
   (println "transact!")))

(defn update!
  ([cursor v]
   (println "foam update! cursor v"))
  ([cursor korks v]
   (println "foam update! cursor korks v"))
  ([cursor korks v tag]
   (println "foam update! cursor korks v tag")))
