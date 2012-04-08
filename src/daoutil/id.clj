(ns daoutil.id
  (:require [clojure.set :as set]
            [clojure.java.jdbc :as jdbc]))

(defn init-idset [db]
  (jdbc/with-connection db
    (try
      (jdbc/drop-table :idset)
      (catch Exception e))
    (jdbc/create-table :idset
                       [:table_name "varchar(40)" "PRIMARY KEY"]
                       [:id :int])))

(def fetch-size 10000)
(defn fetch-ids
  [[start end] db table-name]
  (let [start (or start 0)
        end (or end 0)]
    (or (and (> end start) [(inc start) end])
        (jdbc/with-connection db
          (let [last-used (jdbc/with-query-results rs
                            ["select id from idset where table_name = ?"
                             (name table-name)]
                            (:id (first rs)))
                [start end] (if (= end last-used)
                              [start end]
                              [(or last-used 0) (or last-used 0)])]
            (if last-used
              (jdbc/update-values :idset
                                  ["table_name=?" (name table-name)]
                                  {:id (+ last-used fetch-size)})
              (jdbc/insert-values :idset
                                  [:id :table_name] [fetch-size (name table-name)]))
            [(inc start) (+ end fetch-size)])))))

(def idset (atom {}))

(defn id-generator
  [db table-name]
  (let [table-name (or (and (keyword? table-name) table-name)
                 (keyword table-name))]
    (first (table-name (swap! idset update-in [table-name] fetch-ids db table-name)))))
