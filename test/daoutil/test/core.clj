(ns daoutil.test.core
  (:use [daoutil.core]
        [midje.sweet])
  (:require [clojure.java.jdbc :as jdbc]))

(fact
  (convert-condition {:id 7 :num "01" :name "skz"}) => ["name=? and num=? and id=?" "skz" "01" 7])

(def database-df
     {:teacher
      [[:id :bigint "PRIMARY KEY"]
       [:num "varchar(30)"]
       [:name "varchar(30)"]]})

(with-default-connection (init-database database-df))

(defn insert-data! [table data]
  (let [columns (vec (keys data))
        values (map #(data %) columns)]
    (jdbc/insert-values table columns values) data))

(with-default-connection
  (doseq [i (range 10)]
    (let [data {:id i :num (str "num" i) :name (str "name" i)}]
      (insert-data! :teacher data))))

(fact
  (with-default-connection
    (get-data :teacher ["id=?" 2]) => (just {:id 2 :num "num2" :name "name2"})
    (count (get-datas :teacher ["1=1"])) => 10
    (count-datas :teacher ["1=1"]) => 10))
