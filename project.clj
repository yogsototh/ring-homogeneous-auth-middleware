(defproject threatgrid/ring-homogeneous-auth-middleware "0.0.2-SNAPSHOT"
  :description "A simple middleware to deal with multiple auth middlewares"
  :url "http://github.com/threatgrid/ring-homogeneous-auth-middleware"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [metosin/ring-http-response "0.8.2"]
                 [metosin/compojure-api "1.1.9"]
                 [metosin/schema-tools "0.9.1"]
                 [prismatic/schema "1.1.3"]])
