(ns foam.html-test
  (:require [clojure.test :refer :all]
            [foam.core :as foam]
            [foam.html :as html]))

(deftest parsing-tags
  (are [expr expected] (= expected (html/normalize-element expr))
       [:div] ["div" {:id nil
                      :class nil} nil]
       [:div#foo.bar] ["div" {:id "foo"
                              :class "bar"} nil]

       [:div#foo.bar.baz] ["div" {:id "foo"
                                  :class "bar baz"} nil]

       [:#foo.bar.baz] ["div" {:id "foo"
                               :class "bar baz"} nil]
       [:.bar] ["div" {:id nil
                       :class "bar"} nil]))

(deftest html-return-types
  (let [ret (html/html
             [:h1 "Hello World"])]
    (is (satisfies? foam/ReactDOMRender ret))
    (is (-> ret :tag (= "h1")))))
