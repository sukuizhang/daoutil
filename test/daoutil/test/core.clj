(ns daoutil.test.core
  (:use [daoutil.core]
        [midje.sweet]))

(fact
  (convert-condition
   {:id 7 :num "01" :name "skz"}) = ["name=? and num=? and id=?" "skz" "01" 7])

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
       [:chinese_score :int]]})

(with-default-connection
  (init-database database-df))

(fact
  (let [t1 (insert-data! memory-db :teacher {:num "01" :name "rr" :age 20})
        a 5 b 6]
    t1 => (get-data memory-db :teacher (:id t1))
    t1 => (get-data memory-db :teacher {:name "rr" :num "01"})
    (update-data! memory-db :teacher {:name "rr" :num "01"} {:age 21 :name "uvw"})
    (get-data memory-db :teacher (:id t1)) => (just (merge t1 {:age 21 :name "uvw"}))
    (doseq [i (range a) j (range b)]
      (insert-data! memory-db :student {:num (str "0" i) :name (str "name" j) :maths_score 120}))
    (doseq [i (range a)] b => (count (get-datas memory-db :student {:num (str "0" i)})))
    (doseq [j (range b)] a => (count (get-datas memory-db :student {:name (str "name" j) :maths_score 120})))
    (* a b) => (count-datas memory-db :student)
    (delete-data! memory-db :student {:name "name0" :maths_score 120})
    (* a (- b 1)) => (count-datas memory-db :student)
    (delete-data! memory-db :student (:id (first (get-datas memory-db :student))))
    (- (* a (- b 1)) 1) => (count-datas memory-db :student)))

