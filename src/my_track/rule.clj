(ns my-track.rule
  (:require [clara.rules :refer :all]
            [my-track.notify :refer [notify-all-user]]
            [taoensso.timbre :as log]
            [common.config :as config]
            [my-track.api :as api]
            [java-time :as time]))

(defrecord DapanInfo [name today-new change-num change-percent])
(defrecord FundInfo [name fundcode gztime gsz gszzl dwjz])

(defn jump-style
  [s]
  (format "<p style='color:red'>%s</p>" s))

(defn sink-style
  [s]
  (format "<p style='color:green'>%s</p>" s))

(defrule shangzheng-up
  [DapanInfo
   (= name "上证指数")
   (> today-new (config/get-config :shangzheng-up))
   (= ?name name)
   (= ?today-new today-new)]
  =>
  (-> (format "[%s] %s 突破%s,达到%.2f点！"
              (time/format "yyyy-MM-dd'T'H:m" (time/local-date-time))
              ?name
              (config/get-config :shangzheng-up)
              ?today-new)
      jump-style
      notify-all-user))

(defrule shangzheng-down
  [DapanInfo
   (= name "上证指数")
   (< today-new (config/get-config :shangzheng-down))
   (= ?name name)
   (= ?today-new today-new)]
  =>
  (-> (format "[%s] %s 跌破%s,达到%.2f点！"
              (time/format "yyyy-MM-dd'T'H:m" (time/local-date-time))
              ?name
              (config/get-config :shangzheng-down)
              ?today-new)
      sink-style
      notify-all-user))


(defrule guzhi-jump
  "估值增长率上升"
  [FundInfo (> gszzl (config/get-config :guzhi-up))
   (= ?gszzl gszzl)
   (= ?gztime gztime)
   (= ?name name)]
  =>
  (-> (format "[%s] %s 估值增长 %.2f%% !" ?gztime ?name ?gszzl)
      jump-style
      notify-all-user))

(defrule guzhi-sink
  "估值增长率下降"
  [FundInfo (< gszzl (- (config/get-config :guzhi-down)))
   (= ?gszzl gszzl)
   (= ?gztime gztime)
   (= ?name name)]
  =>
  (-> (format "[%s] %s 估值下降 %.2f%% !" ?gztime ?name (.abs ?gszzl))
      sink-style
      notify-all-user))

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

  (def gzz (->> (config/get-config :funds-code)
                (map api/get-guzhi)))

  (-> (mk-session)
      (insert-all (map map->DapanInfo dps))
      (insert-all (map map->FundInfo gzz))
      (fire-rules)
      )

  )
