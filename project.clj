(defproject gorillalabs/titanium "1.0.2"
  :description "Titanium a powerful Clojure graph library build on top of Aurelius Titan"
  :url "http://titanium.clojurewerkz.org"
  :license {:name "Eclipse Public License"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [gorillalabs/titan-core "1.1.0g"]
                 [potemkin "0.4.3"]
                 [gorillalabs/archimedes "3.0.0.1"]
                 [gorillalabs/ogre "3.0.3.1"]]
  :source-paths  ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options     ["-target" "1.6" "-source" "1.6"]
  :profiles {:1.5    {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.7    {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :master {:dependencies [[org.clojure/clojure "1.8.0-master-SNAPSHOT"]]}
             :dev    {:dependencies [[gorillalabs/titan-cassandra "1.1.0g" :exclusions [org.slf4j/slf4j-log4j12 joda-time com.ning/compress-lzf]]
                                     [gorillalabs/titan-berkeleyje "1.1.0g"]
                                     [gorillalabs/titan-es "1.1.0g"]
                                     [clojurewerkz/support "1.1.0" :exclusions [com.google.guava/guava
                                                                                org.clojure/clojure]]

                                     [org.slf4j/slf4j-nop "1.7.5"]
                                     [commons-io/commons-io "2.4"]]
                      :plugins [[codox "0.8.10"]]
                      :codox   {:sources    ["src/clojure"]
                                :output-dir "doc/api"}}}
  :aliases {"all" ["with-profile" "dev,dev,1.5:dev,1.7:dev,master"]}
  :repositories {"sonatype"           {:url       "http://oss.sonatype.org/content/repositories/releases"
                                       :snapshots false
                                       :releases  {:checksum :fail}}
                 "sonatype-snapshots" {:url       "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases  {:checksum :fail :update :always}}
                 "clojars"
                 {:url       "http://clojars.org/repo"
                  :snapshots true
                  :releases  {:checksum :fail :update :always}}}
  ;;  :warn-on-reflection true
  )
