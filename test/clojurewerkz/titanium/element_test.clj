(ns clojurewerkz.titanium.element-test
  (:use [clojure.test]
        [clojurewerkz.titanium.test.support :only [*graph* graph-fixture]])
  (:import [com.thinkaurelius.titan.graphdb.relations RelationIdentifier])
  (:require [clojurewerkz.titanium.graph :as g]
            [clojurewerkz.titanium.vertices :as v]
            [clojurewerkz.titanium.edges :as e]
            [clojurewerkz.ogre.core :as oc]
            [clojurewerkz.titanium.elements :as elem]))

(use-fixtures :once graph-fixture)

(deftest element-test
  #_(testing "Get keys."
    (g/with-transaction [tx *graph*]
                        (let [a (v/create! tx {:name "v1" :a 1 :b 1})
             b (v/create! tx {:name "v2" :a 1 :b 1})
             c (e/connect! a :test-label b {:prop "e1" :a 1 :b 1})
             coll-a (keys (oc/properties a))
             coll-b (keys (oc/properties b))
             coll-c (keys (oc/properties c))]
         (is (= #{:name :a :b} coll-a coll-b))
         (is (= #{:prop :a :b} coll-c))
         (is (= clojure.lang.PersistentHashSet (type coll-a))))))

  (testing "Get id."
    (g/with-transaction [tx *graph*]
       (let [a (v/create! tx {})
             b (v/create! tx {})
             c (e/connect! a :test-label b )]
         (is (= java.lang.Long (type (elem/id-of a))))
         (is (= RelationIdentifier (type (elem/id-of c)))))))

  (testing "Remove property!"
    (g/with-transaction [tx *graph*]
                        (let [a (v/create! tx {:a 1})
             b (v/create! tx {})
             c (e/connect! a :test-label b {:a 1})]
         (elem/remove-properties! a :a)
         (elem/remove-properties! c :a)
         (is (nil? (elem/value c :a)))
         (is (nil? (elem/value a :a)))))))
