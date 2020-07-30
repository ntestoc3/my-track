(ns my-track.notify
  (:require
   [wxpush.core :as wxpush]
   [common.config :as config]
   [taoensso.timbre :as log]
   [clojure.core.async :as async]
   [diehard.core :as dh]
   [clojure.string :as str]))

(defn send-messages
  [msgs]
  (log/info :send-messages)
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
                         (str/join "<br/>" msgs)
                         {:content-type :html})))

;; 超过10条消息就会阻塞
(def msg-queue (async/chan 10))

(def kill (async/chan))

(defn start-message-service
  []
  (log/info :message-service-start)
  (async/go-loop []
    ;; 3秒执行1次消息发送
    (<! (async/timeout 3000))
    (let [msgs (->> (repeatedly #(async/poll! msg-queue))
                    (take-while identity))]
      (when (seq msgs)
        (send-messages msgs)))
    (if (async/poll! kill)
      (log/info "message service down!")
      (recur))))

(defn stop-message-service
  []
  (async/>!! kill true))

(defn notify-all-user
  [msg]
  (log/info :notify msg)
  (async/>!! msg-queue msg))

(comment



  )
