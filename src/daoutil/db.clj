(ns daoutil.db
  (:import [com.mchange.v2.c3p0 ComboPooledDataSource]))

(def memory-db {:classname "org.hsqldb.jdbcDriver"
         :subprotocol "hsqldb:mem:idb"
         :subname ""         
         :user "sa"         
         :password ""})

(defn ds-db
  "创建一个只包含:datasource的db，传入一个创建datasource的函数"
  [ds-factory spec]
  (let [db-delay (delay {:datasource (ds-factory spec)})]
    (fn [] @db-delay)))

(defn c3p0-pooled-datasource
  "创建c3p0 datasource。"
  [spec]
  (doto (ComboPooledDataSource.)
    (.setDriverClass (:classname spec)) 
    (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
    (.setUser (:user spec))
    (.setPassword (:password spec))
    (.setMaxIdleTimeExcessConnections (* 30 60))
    (.setMaxIdleTime (* 3 60 60))
    (.setMaxPoolSize 20)))
