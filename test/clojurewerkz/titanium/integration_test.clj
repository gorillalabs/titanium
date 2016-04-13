(ns clojurewerkz.titanium.integration-test
  (:require [clojurewerkz.titanium.graph    :as tg]
            [clojurewerkz.titanium.vertices :as tv]
            [clojurewerkz.titanium.edges    :as ted]
            [clojurewerkz.ogre.traversal :as t]
            [clojurewerkz.ogre.core :as g])
  (:use clojure.test
        [clojurewerkz.titanium.test.support :only (*graph* graph-fixture)]))

(use-fixtures :once graph-fixture)

;; The Graph of the Gods example from the Titan wiki
(deftest test-integration-example1
  (tg/with-transaction [tx *graph*]
    (let [saturn   (tv/create! tx {:name "Saturn"   :type "titan"})
          jupiter  (tv/create! tx {:name "Jupiter"  :type "god"})
          hercules (tv/create! tx {:name "Hercules" :type "demigod"})
          alcmene  (tv/create! tx {:name "Alcmene"  :type "human"})
          neptune  (tv/create! tx {:name "Neptune"  :type "god"})
          pluto    (tv/create! tx {:name "Pluto"    :type "god"})
          sea      (tv/create! tx {:name "Sea"      :type "location"})
          sky      (tv/create! tx {:name "Sky"      :type "location"})
          tartarus (tv/create! tx {:name "Tartarus" :type "location"})
          nemean   (tv/create! tx {:name "Nemean"   :type "monster"})
          hydra    (tv/create! tx {:name "Hydra"    :type "monster"})
          cerberus (tv/create! tx {:name "Cerberus" :type "monster"})]
      (ted/connect! neptune :lives sea)
      (ted/connect! jupiter :lives sky)
      (ted/connect! pluto :lives tartarus)
      (ted/connect! jupiter :father saturn)
      (ted/connect! hercules :father jupiter)
      (ted/connect! hercules :mother alcmene)
      (ted/connect! jupiter :brother pluto)
      (ted/connect! pluto :brother jupiter)
      (ted/connect! neptune :brother pluto)
      (ted/connect! pluto :brother neptune)
      (ted/connect! jupiter :brother neptune)
      (ted/connect! neptune :brother jupiter)
      (ted/connect! cerberus :lives tartarus)
      (ted/connect! pluto :pet cerberus)
      (ted/connect! hercules :battled nemean   {:times 1})
      (ted/connect! hercules :battled hydra    {:times 2})
      (ted/connect! hercules :battled cerberus {:times 12})

      (let [r1 (g/query (g/V saturn)
                        (g/<-- [:father])
                        (g/<-- [:father])
                        g/first-of!)
            r2 (g/query (g/V hercules)
                        (g/out :father :mother)
                        (g/properties :name)
                        g/into-set!)
            r3 (g/query (g/V hercules)
                        (g/-E> [:battled])
                        (g/has :times > 1)
                        (g/in-vertex)
                        (g/properties :name)
                        g/into-set!)
            c3 (g/query (g/V hercules)
                        (g/-E> [:battled])
                        (g/has :times > 1)
                        (g/in-vertex)
                        g/count!)
            r4 (g/query (g/V pluto)
                        (g/--> :lives)
                        (g/<-- [:lives])
;                        (g/except [pluto])
                        (g/properties :name)
                        g/into-set!)
            r9 (g/into-set! (t/values (t/in (t/out (t/V (.traversal (.graph pluto))) :lives) :lives) :name))
            r5 (g/query (g/V pluto)
                        (g/--> :brother)
                        (g/as  :god)
                        (g/--> :lives)
                        (g/as  :place)
                        (g/select-only :name)
                        g/all-into-maps!)]
        (is (= r1 hercules))
        (is (= r2 #{"Alcmene" "Jupiter" "Saturn"}))
        (is (= r3 #{"Cerberus" "Hydra"}))
        (is (= c3 2))
        (is (= r4 #{"Cerberus"}))
        (is (= r9 #{"Cerberus"}))
        ;; when https://github.com/tinkerpop/pipes/issues/75 is fixed,
        ;; we will be able to turn tables into vectors of maps, as they
        ;; should be represented (Neocons does it for Cypher responses). MK.

        
        ;; (is (= #{{:god "Neptune" :place "Sea"} {:god "Jupiter" :place "Sky"}}
        ;;        (set r5)))\
        ))))
