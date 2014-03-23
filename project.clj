(defproject mese-test "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://mese-test.herokuapp.com"
  :license {:name "FIXME: choose"
            :url "http://example.com/FIXME"}
  :dependencies [[org.clojure/clojure "1.5.0"]
                 [compojure "1.1.1"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [ring/ring-devel "1.1.0"]
                 [ring-basic-authentication "1.0.1"]
                 [environ "0.2.1"]
                 [com.cemerick/drawbridge "0.0.6"]
                 [clj-time "0.5.1"]

                 [com.ashafa/clutch "0.4.0-RC1"]

                 ;;Client-deps
                 [seesaw "1.4.4"]
                 [http-kit "2.1.17"]
                 [fontselector "1.0.0"]]
  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.2.1"]]
  :hooks [environ.leiningen.hooks]
  :profiles {:production {:env {:production true}}})
