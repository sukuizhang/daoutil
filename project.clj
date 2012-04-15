(defproject daoutil "0.1.0"
  :description "dao的一些"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/java.jdbc "0.1.3"]
                 [hsqldb/hsqldb "1.8.0.10"]
                 [postgresql/postgresql "8.1-404.jdbc3"]
                 [org.clojure/tools.logging "0.2.3"]
                 [c3p0 "0.9.1.2"]]
  :dev-dependencies [[midje "1.3-alpha5"]
                     [datacontext "0.2.0"]])
