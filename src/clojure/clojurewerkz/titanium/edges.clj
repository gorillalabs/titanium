;; Copyright (c) 2013-2014 Michael S. Klishin, Alex Petrov, Zack Maril, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.titanium.edges
  (:require [clojurewerkz.titanium.elements :as elem]
            [clojurewerkz.titanium.vertices :as tv]
            [clojurewerkz.ogre.util :as t])
  (:import [org.apache.tinkerpop.gremlin.structure Vertex Edge Graph]))

(defn find-by-id
  "Retrieves edges by id from the graph."
  [^Graph g & ids]
  (let [result (.E (.traversal g) (into-array ids))]
    (if (= 1 (count ids))
      (if (.hasNext result) (.next result) nil)
      (t/into-vec! result))))


(defn refresh
  "Goes and grabs the edge from the graph again. Useful for \"refreshing\" stale edges."
  [^Graph g ^Edge edge]
  (find-by-id g (.id edge)))


(defn remove!
  "Removes an edge."
  [^Edge edge]
  (.remove edge))

(defn ^Vertex head-vertex
  "Get the head vertex of the edge."
  [^Edge e]
  (.inVertex e))

(defn ^Vertex tail-vertex
  "Get the tail vertex of the edge."
  [^Edge e]
  (.outVertex e))

(defn edges-between
  "Returns a set of the edges between two vertices, optionally with the given label."
  ([^Vertex v1 ^Vertex v2]
    (edges-between v1 nil v2))
  ([^Vertex v1 label ^Vertex v2]
    ;; Source for these edge queries:
    ;; https://groups.google.com/forum/?fromgroups=#!topic/gremlin-users/R2RJxJc1BHI
    (let [^Edge edges (set (if label (tv/outgoing-edges-of v1 label) (tv/outgoing-edges-of v1)))
          v2-id (.id v2)
          edge-set (set (filter #(= v2-id (.id ^Vertex (head-vertex %))) edges))]
      (when (not (empty? edge-set))
        edge-set))))

(defn connect!
  "Connects two vertices with the given label, optionally with the given properties."
  ([^Vertex v1 label ^Vertex v2]
    (connect! v1 label v2 {}))
  ([^Vertex v1 label ^Vertex v2 properties]
   (let [kw-or-string #(if (keyword? %) (name %) %)
         flat-properties (reduce (fn [r v] (conj r (kw-or-string (first v)) (second v))) [] properties)]
     (.addEdge v1 ^String (name label) v2 (to-array flat-properties)))))

(defn upconnect!
  "Takes all the edges between the given vertices with the given label
   and, if the data is provided, merges the data with the current
   properties of the edge. If no such edge exists, then an edge is
   created with the given data."
  ([^Vertex v1 label ^Vertex v2]
    (upconnect! v1 label v2 {}))
  ([^Vertex v1 label ^Vertex v2 data]
    (if-let [^Edge edges (edges-between v1 label v2)]
      (do
        (doseq [^Edge edge edges] (elem/set-properties! edge data)) edges)
       #{(connect! v1 label v2 data)})))

(defn unique-upconnect!
  "Like upconnect!, but throws an error when more than element is returned."
  [& args]
  (let [upconnected (apply upconnect! args)]
    (if (= 1 (count upconnected))
      (first upconnected)
      (throw (Throwable.
               (str
                 "Don't call unique-upconnect! when there is more than one element returned.\n"
                 "There were " (count upconnected) " edges returned.\n"
                 "The arguments were: " args "\n"))))))

(defn connected?
  "Returns whether or not two vertices are connected with an optional third
   argument specifying the label of the edge."
  ([^Vertex v1 ^Vertex v2]
    (connected? v1 nil v2))
  ([^Vertex v1 label ^Vertex v2]
   (not (empty? (edges-between v1 label v2)))))
