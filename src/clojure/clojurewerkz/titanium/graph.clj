;; Copyright (c) 2013-2014 Michael S. Klishin, Alex Petrov, Zack Maril, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.titanium.graph
  (:import  [com.thinkaurelius.titan.core TitanFactory TitanGraph]
            [org.apache.tinkerpop.gremlin.structure Vertex Edge
             Graph Graph$Features$GraphFeatures]
            [org.apache.tinkerpop.gremlin.tinkergraph.structure TinkerGraph]
            [com.thinkaurelius.titan.core TitanTransaction]))

;;
;; API
;;

(defn close [graph]
  (.close graph))

(defn get-graph-features
  "Get a map of graph features for the given graph."
  ^Graph$Features$GraphFeatures
  [^Graph g]
  (-> g (.features) (.graph)))

(defn supports-transactions
  "Determines if the graph supports transactions."
  [^Graph g]
  (.supportsTransactions (get-graph-features g)))


(defn new-transaction
  "Creates a new transaction based on the given graph object."
  [^Graph g]
  (-> g (.tx) (.open)))

(defn commit
  "Commits all changes to the graph."
  [^Graph g]
  (-> g (.tx) (.commit)))

(defn close
  "Closes the graph."
  [^Graph g]
  (.close g))

(defn rollback
  "Stops the current transaction and rolls back any changes made."
  [^Graph g]
  (-> g (.tx) (.rollback)))

(defn with-transaction*
  [graph f & {:keys [threaded? rollback?]}]
  {:pre [(supports-transactions graph)]}
  (let [tx (if threaded? (new-transaction graph) graph)]
    (try
      (let [result (f tx)]
        (if rollback?
          (rollback tx)
          (commit tx))
        result)
      (catch Throwable t
        (try (rollback tx) (catch Exception _))
        (throw t)))))

;; This approach is copied from clojure.java.jdbc. The ^:once metadata and use of fn*
;; is explained by Christophe Grand in this blog post:
;; http://clj-me.cgrand.net/2013/09/11/macros-closures-and-unexpected-object-retention/
(defmacro with-transaction
  "Evaluates body in the context of a transaction on the specified graph, which must
   support transactions.  The binding provides the graph for the transaction and the
   name to which the transactional graph is bound for evaluation of the body.
   (with-transaction [tx graph]
     (vertex/create! tx)
     ...)
   If the graph supports threaded transactions, the binding may also specify that the
   body be executed in a threaded transaction.
   (with-transaction [tx graph :threaded? true]
      (vertex/create! tx)
      ...)
   Note that `commit` and `rollback` should not be called explicitly inside
   `with-transaction`. If you want to force a rollback, you must throw an
   exception or specify rollback in the `with-transaction` call:
   (with-transaction [tx graph :rollback? true]
      (vertex/create! tx)
      ...)"
  [binding & body]
  `(with-transaction*
     ~(second binding)
     (^{:once true} fn* [~(first binding)] ~@body)
     ~@(rest (rest binding))))


(defn convert-config-map
  [m]
  (let [conf (org.apache.commons.configuration.BaseConfiguration.)]
    (doseq [[k1 v1] m]
      (.setProperty conf (name k1) v1))
    conf))

(defprotocol TitaniumGraph
  (open [input] "Opens a new graph"))

(extend-protocol TitaniumGraph
  String
  (open [^String shortcut-or-file]
    (TitanFactory/open shortcut-or-file))

  java.io.File
  (open [^java.io.File f]
    (TitanFactory/open (.getPath f)))

  org.apache.commons.configuration.Configuration
  (open [^org.apache.commons.configuration.Configuration conf]
    (TitanFactory/open conf))

  java.util.Map
  (open [^java.util.Map m]
    (TitanFactory/open (convert-config-map m))))

;;
;; Automatic Indexing
;;

(defn index-vertices-by-key!
  [^TinkerGraph g ^String k]
  (.createIndex g k org.apache.tinkerpop.gremlin.structure.Vertex))

(defn deindex-vertices-by-key!
  [^TinkerGraph g ^String k]
  (.dropIndex g k org.apache.tinkerpop.gremlin.structure.Vertex))

(defn index-edges-by-key!
  [^TinkerGraph g ^String k]
  (.createIndex g k org.apache.tinkerpop.gremlin.structure.Edge))

(defn deindex-edges-by-key!
  [^TinkerGraph g ^String k]
  (.dropIndex g k org.apache.tinkerpop.gremlin.structure.Edge))

;;
;; Graph Variables
;;

(defn get-variable [^Graph g ^clojure.lang.Keyword key]
  (let [variable (.get (.variables g) (name key))]
    (if (.isPresent variable)
      (.get variable)
      nil)))

(defn set-variable [^Graph g ^clojure.lang.Keyword key value]
  (.set (.variables g) (name key) value))

(defn remove-variable [^Graph g ^clojure.lang.Keyword key]
  (.remove (.variables g) (name key)))
