;; Copyright (c) 2013-2014 Michael S. Klishin, Alex Petrov, Zack Maril, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.titanium.elements
  (:refer-clojure :exclude [assoc! dissoc!])
  (:require [clojure.walk :as w]
            [clojurewerkz.ogre.util :as u])
  (:import [org.apache.tinkerpop.gremlin.structure Element Vertex]
           [com.thinkaurelius.titan.core TitanElement]))


;;
;; API
;;

(defn new?
  "Returns true if entity has been newly created, false otherwise"
  [^TitanElement e]
  (.isNew e))

(defn loaded?
  "Returns true if entity has been loaded and not yet modified in the current transaction,
   false otherwise"
  [^TitanElement e]
  (.isLoaded e))

(defn modified?
  "Returns true if entity has been loaded and modified in the current transaction,
   false otherwise"
  [^TitanElement e]
  (.isModified e))

(defn removed?
  "Returns true if entity has been deleted in the current transaction,
   false otherwise"
  [^TitanElement e]
  (.isRemoved e))

(defn id-of
  "Returns the id of the entity."
  [^TitanElement e]
  (.id e))

(defn label-of
  "Returns the label of the entity."
  [^TitanElement e]
  (keyword (.label e)))
                                        ;
(defn set-properties!
  "Adds properties with the specified keys and values to an element."
  ([^Element elem properties]
   (when-not (empty? properties)
     (apply set-properties! elem (apply concat properties)))
   elem)
  ([^Element elem key val]
   (.property elem (name key) val)
   elem)
  ([^Element elem key val & kvs]
   (set-properties! elem key val)
   (doseq [kv (partition 2 kvs)]
     (.property elem (name (first kv)) (last kv)))
   elem))

(defn remove-properties!
  "Removes properties with the specified keys from an element."
  [^Element elem & keys]
  (doseq [key keys]
    (doall (map #(.remove % ) (iterator-seq (.properties elem (u/keywords-to-str-array [key]))))))
  elem)

(defn remove-all-properties!
  "Removes all properties from an element."
  [^Element elem]
  (apply remove-properties! (cons elem (.keys elem))))

(defn value
  "Gets the value(s) of the property with the supplied key."
  [element property-key]
  (let [^java.util.Iterator prop-iter (-> element (.properties (u/keywords-to-str-array [property-key])))
        prop (if (.hasNext prop-iter) (map #(.value %) (iterator-seq prop-iter)) nil)]
    (if (= (count prop) 1) (first prop) prop)))
