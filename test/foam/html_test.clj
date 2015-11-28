(ns foam.html-test
  (:require [clojure.test :refer :all]
            [foam.core :as foam]
            [foam.html :as html]))

(deftest parsing-tags
  (are [expr expected] (= expected (html/normalize-element expr))
       [:div] ["div" {} nil]

       [:div#foo] ["div" {:id "foo"} nil]

       [:.bar] ["div" {:class "bar"} nil]

       [:#foo] ["div" {:id "foo"} nil]

       [:div.bar] ["div" {:class "bar"} nil]

       [:div#foo.bar] ["div" {:id "foo"
                              :class "bar"} nil]

       [:div.bar {:display "none"} "hello"] ["div" {:class "bar"
                                                    :display "none"} ["hello"]]

       [:div.bar {:class "foo"} "hello"] ["div" {:class "bar foo"} ["hello"]]

       [:div#foo.bar.baz] ["div" {:id "foo"
                                  :class "bar baz"} nil]

       [:#foo.bar] ["div" {:id "foo"
                           :class "bar"} nil]

       [:.bar#foo] ["div" {:id "foo"
                           :class "bar"} nil]

       [:#foo.bar.baz] ["div" {:id "foo"
                               :class "bar baz"} nil]))

(deftest html-return-types
  (let [ret (html/html
             [:h1 "Hello World"])]
    (is (satisfies? foam/ReactDOMRender ret))
    (is (-> ret :tag (= "h1")))))
