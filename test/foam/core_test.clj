(ns foam.core-test
  (:require [clojure.test :refer :all]
            [foam.core :as foam]
            [foam.dom :as dom]))

(defn app-state []
  (atom {:foo 42}))

(defn simple-component [app owner opts]
  (reify
    foam/IRender
    (render [this]
      (dom/h1 nil "Hello World"))))

(defn nested-component [app owner opts]
  (reify
    foam/IRender
    (render [this]
      (dom/div nil
       (dom/h1 nil "Hello World")
       (dom/p nil "Some text")))))

(deftest can-build
  (is (foam/build simple-component (foam/root-cursor (app-state)) {})))

(deftest valid-dom-tree-works
  (let [com (foam/build simple-component (foam/root-cursor (app-state)) {})
        tree (foam/react-render com)]
    (foam/valid-dom-tree? tree)))

(deftest simple-render
  (let [com (foam/build simple-component (foam/root-cursor (app-state)) {})
        s (dom/render-to-string com)]
    (is s)
    (is (string? s))))

(deftest nested-render-works
  (let [com (foam/build nested-component (foam/root-cursor (app-state)) {})
        s (dom/render-to-string com)]
    (is s)
    (is (string? s))))

(defn init-state-component [app owner opts]
  (reify
    foam/IInitState
    (init-state [this]
      {:foo :bar})
    foam/IRender
    (render [this]
      (dom/h1 nil "Hello World"))))

(deftest init-state-works
  (let [state (app-state)
        cursor (foam/root-cursor state)
        com (foam/build init-state-component cursor {})]
    (is (= {:foo :bar} (foam/get-state com)))
    (is (= :bar (foam/get-state com [:foo])))))

(deftest get-set-state-works
  (let [state (app-state)
        cursor (foam/root-cursor state)
        com (foam/build init-state-component cursor {})]
    (is (nil? (foam/get-state com [:bbq])))
    (foam/set-state! com [:bbq] 42)
    (is (= 42 (foam/get-state com [:bbq])))))

(defn render-state-component [app owner opts]
  (reify
    foam/IInitState
    (init-state [this]
      {:who "CLJS"})
    foam/IRenderState
    (render-state [this state]
      (dom/h1 nil (str "Hello, " (:who state))))))

(deftest render-state-works
  (let [com (foam/build render-state-component (foam/root-cursor (app-state)) {})
        s (dom/render-to-string com)]
    (is s)
    (is (string? s))
    (is (re-find #"Hello, CLJS" s))))

(defn app-state-component [app owner opts]
  (reify
    foam/IRender
    (render [this]
      (dom/h1 nil (str "Hello, " (get-in app [:who]))))))

(deftest app-state-works
  (let [state (app-state)
        cursor (foam/root-cursor state)
        _ (foam/update! cursor :who "CLJS")
        com (foam/build app-state-component cursor {})
        s (dom/render-to-string com)]
    (is (re-find #"Hello, CLJS" s))))

(deftest transact-works
  (let [state (app-state)
        cursor (foam/root-cursor state)]
    (foam/transact! cursor :who (fn [_] "CLJS"))
    (is (= "CLJS" (get @state :who)))))

(deftest update-works
  (let [state (app-state)
        cursor (foam/root-cursor state)]
    (is (= 42 (:foo @state)))
    (is (nil? (:bar @state)))
    (foam/update! cursor [:bar] 17)
    (is (= 17 (:bar @state)))))

(deftest cursors-work
  (let [state (atom {:foo :bar})
        cursor (foam/root-cursor state)]
    (is (= :bar (get cursor :foo)))))

(deftest sub-cursors-work
  (let [state (atom {:foo {:bar {:bbq 3}}})
        cursor (foam/root-cursor state)]

    (is (= {:bar {:bbq 3}} (foam/value (get cursor :foo))))
    (is (foam/cursor? (get cursor :foo)))

    (is (= 3 (get-in cursor [:foo :bar :bbq])))
    (is (not (foam/cursor? (get-in cursor [:foo :bar :bbq]))))))

(defn clj-expr-component [app owner opts]
  (reify
    foam/IRender
    (render [this]
      (dom/div nil
       (if (even? (rand-int 2))
         (foam/build simple-component app {})
         (foam/build simple-component app {}))))))

(deftest render-handles-clj-exprs
  (let [com (foam/build clj-expr-component (foam/root-cursor (app-state)) {})
        tree (foam/react-render com)]
    (is (foam/valid-dom-tree? tree))
    (is (seq (foam/-children tree)))
    (re-find #"Hello" (dom/render-to-string com))))

(deftest get-props-works
  (let [state (atom {:foo :bar})
        cursor (foam/root-cursor state)
        com (foam/build simple-component cursor {})]
    (is (= :bar (foam/get-props com [:foo])))

    ))
