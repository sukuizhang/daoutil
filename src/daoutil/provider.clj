(ns daoutil.provider)

(defprotocol DataTranslator
  (after-read [this data])
  (before-save [this data]))

(deftype PlainDataTranslator [] DataTranslator
  (after-read [_ data] data)
  (before-save [_ data] data))

(def plain-data-translator (PlainDataTranslator.))

(defn data-provide [id _ ops]
  (let [db (:db ops)
        table (:table ops)
        translator (ops :translator plain-data-translator)]
    (->> (get-data db table id)
         (after-read translator))))

(defn data-recover
  [id _ old-data new-data ops]
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
