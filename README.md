# daoutil
   daoutil是一个工具箱，把一些经过实践的可复用的api放在一起。
   另一方面，基于datacontext，它提供了一种有效的隔离数据库的开发方式。

## 使用
   在project.clj中加入依赖:

```clojure
:dependencies [[daoutil "0.1.0"]]
```
 
## api介绍

```clojure    
(daoutil.core/init-database db df)
```

根据df中表结构的定义建表，df的结构举例如下:

```clojure
   (def database-df
     {:active_info
      [[:id :bigint "PRIMARY KEY"] ;活动id
       [:start_time :timestamp] ;开始时间
       [:end_time :timestamp] ;开始时间
       [:gametypes "varchar(30)"] ;支持的游戏类型
       [:min_exp :int] ;要求的最小经验值 
       [:max_wx :int] ;要求最大悟性
       [:award_wx :int]]   ;奖励的悟性

      :join_user
      [[:id :bigint "PRIMARY KEY"]
       [:user_id :bigint] ;用户id
       [:active_id :int]       ;活动id
       [:status "varchar(10)"] ;状态
       [:join_time :timestamp] ;开始时间
       [:award_time :timestamp]];获奖时间
      })
```



```clojure     
(daoutil.core/insert-data! db table data)
```
   
插入一条数据，data是一个map，insert-data!能够在需要的时候，自动给数据分配id，并在返回值中送回来。


```clojure
(daoutil.core/update-data! db table key data)
```
修改一条数据。

key:  查找数据的条件，通常是一个map，存放一个组合条件，但在{:id id}的情况下，也可以直接让id作为key送进来。

data: 新值。


```clojure
(daoutil.core/delete-data! db table condition)
```

根据条件删除数据，condition可以是以下几种形式:

{:age 20 :school_id 27}

[[:age 20] [:school_id 27]]

{:id 100} 可以直接写成 100


```clojure
(daoutil.core/get-datas db table condition)
```

查询数据，condition同上。


```clojure
(daoutil.core/get-data db table condition)
```
查询一条数据，同上。


## 和datacontext结合。
   daoutil提供了一种简单的dao数据上下文:
   daoutil.core/data-provide 和 daoutil.core/data-recover，下面是使用的简单例子。

### 建数据库
```clojure
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
```

### 编写对student的DataTranslator
```clojure
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
```
DataTranslator是用来在数据库数据和逻辑处理数据之间做一个转换，有时候可
以用这种方式使数据更适合逻辑处理，这里简单演示，把学生数据中的
   {:maths_score maths_score :chinese_score chinese_score} 
换成
   {:score {:maths maths :chinese chinese} 

### 绑定数据上下文
```clojure
(def-context :teacher 'data-provide 'data-recover {:db memory-db :table :teacher})
(def-context :student 'data-provide 'data-recover {:db memory-db :table :student :translator translator})
```
### 绑定之后就可以用比较简单的方式写数据逻辑，像下面这样。
```clojure
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
```
### 生成接口函数
```clojure
(wrap-pure-ns)
```

创建一个教师记录。

```clojure
(new-teacher "01" "xyy" 22)
```

修改id为3的教师的信息。

```clojure
(update-teacher 3 "001" 22)
```

修改编号为"02"的教师的信息。

```clojure
(update-teacher {:num "02"} "002" 17)
```

比较编号为"01"的学生和id为13的学生的成绩。

```clojure
(better {:num "01"} 13)
```

判断编号为"002"的教师是否姓名为"zrr"的学生的老师。

```clojure
(teacherof {:num "002"} {:name "zrr"})
```

