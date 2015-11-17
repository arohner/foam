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
