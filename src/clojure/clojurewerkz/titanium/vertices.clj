;; Copyright (c) 2013-2014 Michael S. Klishin, Alex Petrov, Zack Maril, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.titanium.vertices
  (:require [clojurewerkz.ogre.util :as u]
            [clojurewerkz.titanium.elements :as elem])
  (:import [com.thinkaurelius.titan.core TitanGraph]
           [org.apache.tinkerpop.gremlin.structure Vertex Element Direction]))

(defn create-with-label!
  "Create a vertex with the specified label and optional property map."
  ([g label]
   (create-with-label! g label {}))
  ([^TitanGraph g ^String label m]
   (let [^Vertex new-vertex (.addVertex g (name label))]
     (elem/set-properties! new-vertex m))))

(defn create!
  "Create a vertex with the specified properties"
  [^TitanGraph g properties]
  (let [^Vertex new-vertex (.addVertex g (into-array []))]
    (elem/set-properties! new-vertex properties)))

(defn refresh
  "Gets a vertex back from the database and refreshes it to be usable again."
  [^TitanGraph g ^Vertex vertex]
  (.next (.vertices g (to-array [(.id vertex)]))))

(defn remove!
  "Removes a vertex from the graph."
  [^Vertex vertex]
  (.remove vertex))

(defprotocol EdgeDirectionConversion
  (to-edge-direction [input] "Converts input to a Gremlin structure edge direction"))

(extend-protocol EdgeDirectionConversion
  clojure.lang.Named
  (to-edge-direction [input]
    (to-edge-direction (name input)))

  String
  (to-edge-direction [input]
    (case (.toLowerCase input)
      "in"    Direction/IN
      "out"   Direction/OUT
      "both"  Direction/BOTH))

  Direction
  (to-edge-direction [input]
    input))


(defn outgoing-edges-of
  "Returns outgoing (outbound) edges that this vertex is part of, with given labels."
  [^Vertex v & labels]
  (iterator-seq (.edges v Direction/OUT (u/keywords-to-str-array labels))))

(defn incoming-edges-of
  "Returns incoming (inbound) edges that this vertex is part of, with given labels."
  [^Vertex v & labels]
  (.edges v Direction/IN (u/keywords-to-str-array labels)))

(defn edges-of
  "Returns edges that this vertex is part of with direction and with given labels."
  [^Vertex v direction & labels]
  (.edges v (to-edge-direction direction) (u/keywords-to-str-array labels)))

(defn all-edges-of
  "Returns edges that this vertex is part of, with given labels."
  [^Vertex v & labels]
  (.edges v Direction/BOTH (u/keywords-to-str-array labels)))








