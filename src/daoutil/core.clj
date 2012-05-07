(ns daoutil.core
  (:use [easyconf.core])
  (:require [clojure.java.jdbc :as jdbc])
  (:import [com.mchange.v2.c3p0 ComboPooledDataSource]))

(comment
  (def database-df
    {:teacher
     [[:id :bigint "PRIMARY KEY"]
      [:num "varchar(30)"]
      [:name "varchar(30)"]
      [:age :int]]
     
     :student
     [[:id :bigint "PRIMARY KEY"]
      [:num "varchar(30)"]
      [:name "varchar(30)"]
      [:teacher "varchar(30)"]
      [:maths_score :int]
      [:chinese_score :int]]}))

;;define database specifiy
(defconf db-spec {:classname "org.hsqldb.jdbcDriver"
                :subprotocol "hsqldb:mem:idb"
                :subname ""
                :user "sa"
                :password ""})

;;define max connection pool size
(defconf max-pool-size 10)

(defn pool
  "创建c3p0 datasource。"
  [spec pool-size]
  {:datasource (doto (ComboPooledDataSource.)
                 (.setDriverClass (:classname spec))
                 (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
                 (.setUser (:user spec))
                 (.setPassword (:password spec))
                 (.setMaxIdleTimeExcessConnections (* 30 60))
                 (.setMaxIdleTime (* 3 60 60))
                 (.setMaxPoolSize pool-size))})

(def delay-db (delay (pool db-spec max-pool-size)))

(defn get-default-db [] @delay-db)

(defmacro with-default-connection [& bodys]
  `(jdbc/with-connection (get-default-db)
     ~@bodys))

(defn init-database
  [df]
  (dorun (map (fn [[table-name table-def]]
                (try (jdbc/drop-table table-name)
                     (catch Exception e))
                (apply jdbc/create-table (cons table-name table-def)))
              df)))

(defn convert-condition
  "把map方式输入的查询条件转换成clojure jdbc 标准的格式"
  [m-condition]
  (let [sql (->> (keys m-condition)
                 (map #(str (name %) "=?"))
                 (interpose " and ")
                 (apply str))]
    (vec (cons sql (vals m-condition)))))

(defn get-datas
  [table & [condition]]
  (let [[where & params] condition]
    (jdbc/with-query-results rs
      (vec (cons (str "select * from " (name table) " where " where) params))
      (vec rs))))

(defn get-data [table & [condition]]
  (first (get-datas table condition)))

(defn count-datas
  [table & [condition]]
  (let [[where & params] condition]
    (jdbc/with-query-results rs
      (cons (str "select count(*) from " (name table) " where " where) params)
      (val (first (first rs))))))
