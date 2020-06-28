(ns my-track.notify
  (:require
   [wxpush.core :as wxpush]
   [common.config :as config]
   [taoensso.timbre :as log]))


(defn notify-all-user
  [msg]
  (log/info :notify msg)
  #_(wxpush/send-message (config/get-config :wxpush-token)
                       {:uids (config/get-config :users)}))
