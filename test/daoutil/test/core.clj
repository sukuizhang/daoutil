(ns daoutil.test.core
  (:use [daoutil.core]
        [daoutil.db]
        [datacontext.core]
        [midje.sweet]))

(fact
  (convert-condition [[:id 7] [:num "01"] [:name "skz"]]) => ["id=? and num=? and name=?" 7 "01" "skz"]
  (convert-condition '([:id 7] [:num "01"] [:name "skz"])) => ["id=? and num=? and name=?" 7 "01" "skz"]
  (convert-condition {:id 7 :num "01" :name "skz"}) => ["name=? and num=? and id=?" "skz" "01" 7]
  (convert-condition 13) => ["id=?" 13]
  (convert-condition 13 "select * from table") => ["select * from table where id=?" 13]
  (convert-condition nil "select * from table") => ["select * from table"])

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

(init-database memory-db database-df)

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

(init-database memory-db database-df)

(deftype StudentTranslator [] DataTranslator
  (after-read [_ data] (when data (-> data
                                      (assoc :score {:maths (:maths_score data)
                                                     :chinese (:chinese_score data)})
                                    (dissoc :maths_score :chinese_score))))
  (before-save [_ data]  (when data (-> data
                                      (assoc :maths_score (get-in data [:score :maths]))
                                      (assoc :chinese_score (get-in data [:score :chinese]))
                                      (dissoc :score)))))

(def translator (StudentTranslator.))
(def-context :teacher 'data-provide 'data-recover {:db memory-db :table :teacher})
(def-context :student 'data-provide 'data-recover {:db memory-db :table :student :translator translator})

(defn ^{:wrapcontext true :save :teacher}
  new-teacher0 [num name age]
  {:num num :name name :age age})

(defn ^{:wrapcontext true :save :teacher}
  update-teacher0 [teacher num age]
  (-> teacher
      (assoc :num num)
      (assoc :age age)))

(defn ^{:wrapcontext true :save :student}
  new-student0 [num name t-name score]
  {:num num :name name :teacher t-name :score score})

(defn ^{:wrapcontext true :save :student}
  update-student0 [student score]
  (assoc student :score score))

(defn ^:wrapcontext better0 [student1 student2]
  (let [score1 (apply + (vals (:score student1)))
        score2 (apply + (vals (:score student2)))]
    (if (>= score1 score2) student1 student2)))

(defn ^:wrapcontext teacherof0
  ([teacher student1]
     (= (:name teacher) (:teacher student1)))
  ([teacher student1 student2]
      (and (= (:name teacher) (:teacher student1))
           (= (:name teacher) (:teacher student2)))))

(wrap-pure-ns)

(fact
  (let [t1 (new-teacher "01" "xyy" 22)
        t2 (new-teacher "02" "zyy" 17)
        s1 (new-student "01" "yjj" "zyy" {:maths 100 :chinese 100})
        s2 (new-student "02" "zrr" "zyy" {:maths 101 :chinese 100})
        s3 (new-student "03" "lcc" "xyy" {:maths 99 :chinese 100})]
    (get-data memory-db :teacher  (:id t1)) => (just {:id (:id t1) :num "01" :name "xyy" :age 22})
    (get-data memory-db :teacher (:id t2)) => (just t2)
    (update-teacher (:id t1) "001" 22)
    (get-data memory-db :teacher (:id t1)) => (just (assoc t1 :num "001"))
    (update-teacher {:num "02"} "002" 17)
    (get-data memory-db :teacher {:num "002"}) => (just (assoc t2 :num "002"))
    (update-teacher {:num "002" :name "zyy"} "002" 18)
    (get-data memory-db :teacher {:num "002" :age 18}) => (just (merge t2 {:num "002" :age 18}))
    (better (:id s1) (:id s2)) => (just s2)
    (teacherof {:num "001"} {:name "lcc"}) => true
    (teacherof {:num "001"} {:name "yjj"}) => false
    (teacherof {:num "002"} {:name "zrr"} {:name "yjj"}) => true
    (teacherof {:num "002"} {:name "zrr"} {:name "lcc"}) => false))
