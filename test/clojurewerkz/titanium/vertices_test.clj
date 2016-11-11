(ns clojurewerkz.titanium.vertices-test
  (:require [clojurewerkz.titanium.graph    :as tg]
            [clojurewerkz.titanium.vertices :as tv]
            [clojurewerkz.titanium.edges    :as ted]
            [clojurewerkz.titanium.schema   :as ts]
            [clojurewerkz.titanium.elements :as elem]
            [clojurewerkz.ogre.core :as q])
  (:use clojure.test
        [clojurewerkz.titanium.test.support :only (*graph* graph-fixture)])
  (:import [com.thinkaurelius.titan.graphdb.vertices StandardVertex]
           [org.apache.tinkerpop.gremlin.structure T]))

(use-fixtures :once graph-fixture)

(defmacro query [g & body]
  `(q/traverse (.traversal ~g) ~@body))

(deftest vertex-test
  (ts/with-management-system [mgmt *graph*]
    (ts/make-property-key mgmt :vname      String)
    (ts/make-property-key mgmt :age        Long)
    (ts/make-property-key mgmt :first-name String)
    (ts/make-property-key mgmt :last-name  String)
    (ts/make-property-key mgmt :lines      String :cardinality :set)
    (ts/build-composite-index mgmt "ixVname" :vertex [:vname] :unique? true)
    (ts/build-composite-index mgmt "ixAge" :vertex [:age])
    (ts/build-composite-index mgmt "ixFirstName" :vertex [:first-name])
    (ts/build-composite-index mgmt "ixLastName" :vertex [:last-name]))

  (testing "Adding a vertex."
    (tg/with-transaction [tx *graph*]
      (let [v (tv/create! tx {:name "Titanium" :language "Clojure"})]
        (is (.id v))
        (is (= "Titanium" (.value (.property v "name")))))))

  (testing "Deletion of vertices."
    (tg/with-transaction [tx *graph*]
      (let [u  (tv/create! tx {:vname "uniquename"})
            id (elem/id-of u)]
        (tv/remove! u)
        (is (empty? (query tx (q/V id) q/into-vec!)))
        (is (not (.hasNext (query tx (q/V) (q/has :vname "uniquename"))))))))

  (testing "Creating and deleting a vertex with properties"
    (tg/with-transaction [tx *graph*]
      (let [v  (tv/create! tx {:name "Gerard" :value "test"})
            id (elem/id-of v)]
        (tv/remove! v)
        (is (empty? (query tx (q/V id) q/into-vec!))))))

  #_(testing "Simple property mutation."
    (tg/with-transaction [tx *graph*]
      (let [u (tv/create! tx {:a 1 :b 1})]
        (elem/set-properties! u :b 2)
        (elem/remove-properties! u :a)
        (is (= 2  (q/properties u :b) q/value))
        (is (empty? (q/properties u :a) q/into-vec!)))))

  #_(testing "Multiple property mutation."
    (tg/with-transaction [tx *graph*]
      (let [u (tv/create! tx {:a 1 :b 1 :d 1})]
        (elem/set-properties! u :b 2 :c 3)
        (elem/remove-properties! u :a :d)
        (is (nil? (q/properties u :a)))
        (is (= 2  (q/properties u :b)))
        (is (= 3  (q/properties u :c)))
        (is (nil? (q/properties u :d))))))

  #_(testing "Merge single map."
    (tg/with-transaction [tx *graph*]
      (let [u (tv/create! tx {:a 0 :b 2})]
        (elem/set-properties! u {:a 1 :b 2 :c 3})
        (is (= 1   (q/properties u :a)))
        (is (= 2   (q/properties u :b)))
        (is (= 3   (q/properties u :c))))))

  
  ;; TODO did/should this really work?
  (comment testing "Updating node properties with fn."
    (tg/with-transaction [tx *graph*]
     (let [data     {:name "Gerard" :age 30}
           v        (tv/create! tx data)
           v'       (elem/set-properties! v :age inc)]
       (is (= v v'))
       (is (= {:name "Gerard" :age 31} (dissoc (tv/to-map v) T/id)))
       (are [k val] (is (= val (q/properties v k)))
            :name "Gerard" :age 31))))

  #_(testing "Clearing node properties."
    (tg/with-transaction [tx *graph*]
     (let [data     {:name "Gerard" :age 30}
           v        (tv/create! tx data)
           v'       (elem/remove-all-properties! v)]
       (is (= v v'))
       (is (= {} (dissoc (q/property-map v) T/id))))))

  #_(testing "Property map."
    (tg/with-transaction [tx *graph*]
      (let [v1 (tv/create! tx {:a 1 :b 2 :c 3})
            prop-map (q/property-map v1)]
        (is (= 1 (prop-map :a)))
        (is (= 2 (prop-map :b)))
        (is (= 3 (prop-map :c))))))

  #_(testing "Associng property map."
    (tg/with-transaction [tx *graph*]
      (let [m  {:station "Boston Manor" :lines #{"Piccadilly"}}
            v  (tv/create! tx m)]
        ;;TODO this should be false, but for some reason it is
        ;;returning null.
        (elem/set-properties! v :opened-in 1883  :has-wifi? "false")
        (is (= (assoc m :opened-in 1883 :has-wifi? "false")
               (dissoc (q/property-map v) T/id)))))

    (testing "Dissocing property map."
      (tg/with-transaction [tx *graph*]
        (let [m {:station "Boston Manor" :lines #{"Piccadilly"}}
              v (tv/create! tx m)]
          (elem/remove-properties! v "lines")
          (is (= {:station "Boston Manor"} (dissoc (q/property-map v) T/id))))))

    (testing "Accessing a non existent node."
      (tg/with-transaction [tx *graph*]
        (is (nil? (query tx (q/V 12388888888))))))

    (testing "Find by single id."
      (tg/with-transaction [tx *graph*]
        (let [v1 (tv/create! tx {:prop 1})
              v1-id (elem/id-of v1)
              v1-maybe (query tx (q/V v1-id))]
          (is (= 1 (q/properties v1-maybe :prop)))))))

  #_(testing "Find by multiple ids."
    (tg/with-transaction [tx *graph*]
     (let [v1 (tv/create! tx {:prop 1})
           v2 (tv/create! tx {:prop 2})
           v3 (tv/create! tx {:prop 3})
           ids (map elem/id-of [v1 v2 v3])
           v-maybes (apply #(query tx (q/V %)) ids)]
       (is (= (range 1 4) (map #(q/properties % :prop) v-maybes))))))

  #_(testing "Find by kv."
    (tg/with-transaction [tx *graph*]
     (let [v1 (tv/create! tx {:age 1 :vname "A"})
           v2 (tv/create! tx {:age 2 :vname "B"})
           v3 (tv/create! tx {:age 2 :vname "C"})]
       (is (= #{"A"}
              (set (map #(q/properties % :vname) (iterator-seq (query tx (q/V) (q/has :age 1)))))))
       (is (= #{"B" "C"}
              (set (map #(q/properties % :vname) (iterator-seq (query tx (q/V) (q/has :age 2))))))))))

  (testing "Get all vertices."
    (tg/with-transaction [tx *graph*]
      (let [v1 (tv/create! tx {:age 28 :name "Michael"})
            v2 (tv/create! tx {:age 26 :name "Alex"})
            xs (set (iterator-seq (query tx (q/V))))]
        ;; TODO CacheVertex's are hanging around
        (is (= #{v1 v2} (set (filter #(= (type %) StandardVertex) xs)))))))

  (testing "Creating then immediately accessing a node without properties."
    (tg/with-transaction [tx *graph*]
      (let [created (tv/create! tx {})
            fetched (query tx (q/V (elem/id-of created)) q/next!)]
        (is (= (elem/id-of created) (elem/id-of fetched)))
        ;; TODO find shortcut for this to work again
        #_(is (= (q/property-map created) (q/property-map fetched))))))

  (testing "Creating and immediately accessing a node with properties."
    (tg/with-transaction [tx *graph*]
      (let [created (tv/create! tx {:mykey "value"})
            fetched (query tx (q/V (elem/id-of created)) q/next!)]
        (is (= (elem/id-of created) (elem/id-of fetched)))
        ;; TODO find shortcut for this to work again
        #_(is (= (q/property-map created) (q/property-map fetched))))))

  ;; (testing "Upsert!"
  ;;   (tg/with-transaction [tx *graph*]
  ;;     (let [v1-a (tv/upsert! tx :first-name
  ;;                            {:first-name "Zack" :last-name "Maril" :age 21})
  ;;           v1-b (tv/upsert! tx :first-name
  ;;                            {:first-name "Zack" :last-name "Maril" :age 22})
  ;;           v2   (tv/upsert! tx :first-name
  ;;                            {:first-name "Brooke" :last-name "Maril" :age 19})]
  ;;       (is (= 22
  ;;              (q/properties (tv/refresh tx (first v1-a)) :age)
  ;;              (q/properties (tv/refresh tx (first v1-b)) :age)))
  ;;       (tv/upsert! tx :last-name {:last-name "Maril"
  ;;                                  :heritage "Some German Folks"})
  ;;       (is (= "Some German Folks"
  ;;              (q/properties (tv/refresh tx (first v1-a)) :heritage)
  ;;              (q/properties (tv/refresh tx (first v1-b)) :heritage)
  ;;              (q/properties (tv/refresh tx (first v2)) :heritage))))))

  ;; (testing "Unique upsert!"
  ;;   (tg/with-transaction [tx *graph*]
  ;;    (let [v1-a (tv/unique-upsert! tx :first-name
  ;;                                  {:first-name "Zack" :last-name "Maril" :age 21})
  ;;          v1-b (tv/unique-upsert! tx :first-name
  ;;                                  {:first-name "Zack" :last-name "Maril" :age 22})
  ;;          v2   (tv/unique-upsert! tx :first-name
  ;;                                  {:first-name "Brooke" :last-name "Maril" :age 19})]
  ;;      (is (= 22
  ;;             (q/properties (tv/refresh tx v1-a) :age)
  ;;             (q/properties (tv/refresh tx v1-b) :age)))
  ;;      (is (thrown-with-msg? Throwable #"There were 2 vertices returned."
  ;;                            (tv/unique-upsert! tx :last-name {:last-name "Maril"}))))))

  (testing "Add vertex with label"
    (ts/with-management-system [mgmt *graph*]
      (ts/make-vertex-label mgmt "Foo"))
    (tg/with-transaction [tx *graph*]
      (let [v1 (tv/create-with-label! tx "Foo")]
        (is (.id v1))
        (is (= "Foo" (.label v1))))
      (let [v2 (tv/create-with-label! tx "Foo" {:first-name "Zack"})]
        (is (.id v2))
        (is (= "Foo" (.label v2)))
        (is (= "Zack" (.value (.property v2 "first-name"))))))))


