(ns daoutil.core
  (:require [clojure.set :as set]
            [clojure.java.jdbc :as jdbc]
            [daoutil.id :as id])
  (:import [com.mchange.v2.c3p0 ComboPooledDataSource]))

(defn init-database
  [db df]
  (jdbc/with-connection db
    (id/init-idset db)
    (dorun (map
            (fn [[table-name table-def]]
              (try (jdbc/drop-table table-name)
                   (catch Exception e))
              (apply jdbc/create-table (cons table-name table-def)))
            df))))

(defn convert-condition
  "把以vector或map方式输入的查询条件转换成clojure标准的格式"
  [c & [select]]
  (let [c (if (or (list? c) (vector? c) (map? c)) c (if c {:id c}))
        sql (apply str (when select (if c (str select " where ") select))
                   (->> c
                        (map (fn [[n v]] (str (name n) "=?")))
                        (interpose " and ")))]
    (vec (cons sql (map second c)))))

(defn insert-data! [db table data]
  (let [data (if (:id data) data (assoc data :id (id/id-generator db table)))
        columns (vec (keys data))
        values (map #(data %) columns)]
    (jdbc/with-connection db (jdbc/insert-values table columns values)) data))

(defn update-data! [db table key data]
  (let [key (if (map? key) key {:id key})]
    (jdbc/with-connection db
      (jdbc/update-values table (convert-condition key) data))))

(defn delete-data! [db table & [condition]]
  (jdbc/with-connection db
    (jdbc/delete-rows table (convert-condition condition)) nil))

(defn get-datas
  [db table & [condition]]
  (let [paras (convert-condition condition (str "select * from " (name table)))]
    (jdbc/with-connection db
      (jdbc/with-query-results rs paras
        (vec rs)))))

(defn get-data [db table condition] (first (get-datas db table condition)))

(defn count-datas
  [db table & condition]
  (let [paras (convert-condition condition (str "select count (*) from " (name table)))]
    (jdbc/with-connection db
      (jdbc/with-query-results rs paras
        (val (first (first rs)))))))

(defprotocol DataTranslator
  (after-read [this data])
  (before-save [this data]))

(deftype PlainDataTranslator [] DataTranslator
  (after-read [_ data] data)
  (before-save [_ data] data))

(def plain-data-translator (PlainDataTranslator.))

(defn data-provide [id ops]
  (let [db (:db ops)
        table (:table ops)
        translator (ops :translator plain-data-translator)]
    (->> (get-data db table id)
         (after-read translator))))

(defn data-recover
  [id old-data new-data ops]
  (let [db (:db ops)
        table (:table ops)
        translator (ops :translator plain-data-translator)
        new-data (before-save translator new-data)
        old-data (before-save translator old-data)]
    (when (not= new-data old-data)
      (cond
       (and new-data old-data) (update-data! db table id new-data)
       (and new-data (nil? old-data))
       (->> (insert-data! db table (merge new-data (if (map? id) id {:id id})))
            (after-read translator))
       :else (delete-data! db table id)))))
