(defproject my-track "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.cerner/clara-rules "0.20.0"] ;; rule engine
                 [wxpush "0.1.0-SNAPSHOT"]
                 [reaver/reaver "0.1.3"]              ;html parser
                 [clojurewerkz/quartzite "2.1.0"] ;; cron
                 [ntestoc3/common "2.1.6-SNAPSHOT"]
                 [camel-snake-kebab/camel-snake-kebab "0.4.1"] ;; name convert
                 [compojure "1.6.1"]
                 [http-kit "2.3.0"]
                 [cheshire "5.10.0"]
                 ]
  :main ^:skip-aot my-track.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
