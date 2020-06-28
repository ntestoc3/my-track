(ns my-track.rule
  (:require [clara.rules :refer :all]
            [my-track.notify :refer [notify-all-user]]
            [taoensso.timbre :as log]
            [common.config :as config]))

(defrecord DapanInfo [name today-new change-num change-percent])
(defrecord FundInfo [name fundcode gztime gsz gszzl dwjz])

(defrule shangzheng-up-3000
  [DapanInfo
   (= name "上证指数")
   (> today-new (config/get-config :shangzheng-up))
   (= ?name name)
   (= ?today-new today-new)]
  =>
  (notify-all-user (format "%s 突破%s,达到%.2f点！"
                           ?name
                           (config/get-config :shangzheng-up)
                           ?today-new)))

(defrule guzhi-jump
  "估值增长率上升"
  [FundInfo (> gszzl (config/get-config :guzhi-up))
   (= ?gszzl gszzl)
   (= ?name name)]
  =>
  (notify-all-user (format "%s 估值%.2f%% !" ?name ?gszzl)))

(defrule guzhi-sink
  "估值增长率下降"
  [FundInfo (< gszzl (- (config/get-config :guzhi-down)))
   (= ?gszzl gszzl)
   (= ?name name)]
  =>
  (notify-all-user (format "%s 估值%.2f%% !" ?name ?gszzl)))

(comment

  (require '[my-track.api :as api])

  (def gz (api/get-guzhi "470009"))

  (require '[clara.tools.tracing :refer :all])

  (def g2 (api/get-guzhi "470006"))

  (time (-> (mk-session)
            (insert (map->FundInfo gz))
            (fire-rules)
            ))

  (def dps (api/get-dapan))
  (def gz (assoc gz :gszzl 1.2))

  (-> (mk-session)
      (insert-all (map map->DapanInfo dps))
      (insert (map->FundInfo g2))
      (insert (map->FundInfo gz))
      (fire-rules)
      )

  )
