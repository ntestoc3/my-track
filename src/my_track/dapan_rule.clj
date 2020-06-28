(ns my-track.dapan-rule
  (:require [clara.rules :refer :all]
            [taoensso.timbre :as log]))

(defrecord NotifyRule [op target value])

(defrule gzl-jump
  "估值增长率上升"
  [FundInfo (= ?gszzl gszzl)]
  [NotifyRule (= op ">") (= target "ggzzl") (> ?gszzl value)]
  =>
  (log/info :guzhilv-jump " to: " ?gszzl)
  )

(defrule gzl-sink
  "估值增长率下降"
  [FundInfo (= ?gszzl gszzl)]
  [NotifyRule (= op "<") (= target "ggzzl") (< ?gszzl value) ]
  =>
  (log/info :guzhilv-sink " to: " ?gszzl)
  )

(defrule gz-jump
  "估值增长"
  [FundInfo (= ?gsz gsz)]
  [NotifyRule (= op ">") (= target "gsz") (> ?gsz value)]
  =>
  (log/info :guzhi-jump" to: " ?gsz)
 
  )

(defrule gz-sink
  "估值降低"
  [FundInfo (= ?gsz gsz)]
  [NotifyRule (= op "<") (= target "gsz") (< ?gsz value)]
  =>
  (log/info :guzhi-sink " to: " ?gsz)
  )

(comment

  (require '[my-track.api :as api])

  (def gz (api/get-guzhi "470009"))

  (require '[clara.tools.tracing :refer :all])

  (def g2 (api/get-guzhi "470008"))

  (time (-> (mk-session)
            (insert (->NotifyRule ">" "ggzzl" -1))
            (insert (map->FundInfo gz))
            (insert (map->FundInfo g2))
            (fire-rules)
            ))

  (time (if (> (:gszzl gz)
               -1 )
          (log/info :test-if (:gszzl gz))
          (log/info :test-if "sink")))

  )
