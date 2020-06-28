(ns my-track.notify
  (:require
   [wxpush.core :as wxpush]
   [common.config :as config]
   [taoensso.timbre :as log]
   [diehard.core :as dh]))


(defn notify-all-user
  [msg]
  (log/info :notify msg)
  (dh/with-retry {:retry-on Exception
                  :max-retries 5
                  :backoff-ms [500 5000]
                  ;; 如果重试失败，则返回nil
                  :fallback (fn [r e]
                              (log/error :wxpush-notify
                                         "falied! result:" r
                                         "exception:" e))
                  ;; 出现异常，则执行
                  :on-failed-attempt (fn [r e]
                                       (log/warn :wxpush-notify
                                                 "result:" r
                                                 "error:" e ", retring..."))}
    (wxpush/send-message (config/get-config :wxpush-token)
                         msg)))
