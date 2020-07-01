(ns my-track.core
  (:require [cheshire.core :as json]
            [camel-snake-kebab.core :refer :all]
            [common.config :as config]
            [common.wrap :as wrap]
            [clojure.java.io :as io]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojure.repl]
            [wxpush.core :as wxpush]
            [diehard.core :as dh]
            [common.log-ext :as log-ext]
            [my-track.api :as api]
            [my-track.rule :as rule]
            [my-track.notify :as notify]
            [my-track.cron :as cron]
            [clara.rules :as cr]
            [taoensso.timbre :as log])
  (:use [compojure.route :only [files not-found]]
        [compojure.core :only [defroutes GET POST DELETE ANY context]]
        org.httpkit.server)
  (:gen-class))

(defn add-new-user [req]
  (let [body (-> (:body req)
                 io/reader
                 (json/parse-stream ->kebab-case-keyword))]
    (if-let [user (get-in body [:data :uid])]
      (do
        (log/info :add-new-user user)
        (config/update-config! :users conj user)
        (config/save-config!)
        "ok")
      "failed")))

(defn get-qr-code
  [req]
  (when-let [new-url (-> (config/get-config :wxpush-token)
                         (wxpush/make-qrcode)
                         (get-in [:data :url]))]
    {:status 302
     :headers {"Location" new-url}
     }))

(defroutes all-routes
  (GET "/qrcode" [] get-qr-code)
  (POST "/user/add" [] add-new-user)
  (not-found "<p>Page not found.</p>")) ;; all other, return 404

(defn check-funds
  [funds-code]
  (log/info :check-funds funds-code)
  (let [dapan (map rule/map->DapanInfo (api/get-dapan))
        get-fund-info (comp rule/map->FundInfo api/get-guzhi)
        funds (map get-fund-info funds-code)]
    (-> (cr/mk-session 'my-track.rule)
        (cr/insert-all dapan)
        (cr/insert-all funds)
        (cr/fire-rules))))

(def running (atom false))
(defjob CheckFundJob
  [ctx]
  (if @running
    (log/info :check-fund-job "running, exit!")
    (do (reset! running true)
        (log/info :check-fund-job :run)
        (dh/with-retry {:retry-on Exception
                        :max-retries 10
                        :backoff-ms [500 5000]
                        ;; 如果重试失败，则返回nil
                        :fallback (fn [r e]
                                    (log/error :check-fund-job
                                               "falied! result:" r
                                               "exception:" e))
                        ;; 出现异常，则执行
                        :on-failed-attempt (fn [r e]
                                             (log/warn :check-fund-job
                                                       "result:" r
                                                       "error:" e ", retring..."))}
          (when (api/trading-day?)
            (-> (config/get-config :funds-code)
                check-funds)))
        (log/info :check-fund-job :done)
        (reset! running false))))

(defn shutdown [_]
  (log/warn :system-shutdown)
  (config/save-config!)
  (System/exit 0))


(defn -main
  [& args]
  (config/init-config {:cli-args args})

  (log-ext/log-time-format!)
  (log-ext/log-to-file! (or (config/get-config :log-file)
                            "logs.log"))

  (run-server #'all-routes {:port (or (config/get-config :server-port)
                                      8088)})
  (log/info :http-server-ok)
  (doseq [cron-config (config/get-config :cron-configs)]
    (cron/make-cron CheckFundJob cron-config))
  (clojure.repl/set-break-handler! shutdown)
  (notify/start-message-service)
  (log/info :crontabs-ok)
  )


(comment

  (-main)

  :cron-configs [{:job-key "lixian.job.1"
                  :trigger-key "lixian.trigger.1"
                  :schedule "0 0/30 5-21 * * ?"}
                 {:job-key "lixian.morning.job.1"
                  :trigger-key "lixian.morning.trigger.1"
                  :schedule "0 0/3 6-8 * * ?"}
                 ]

  )
