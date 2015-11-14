(ns foam.html-test
  (:require [clojure.test :refer :all]
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
