(ns my-track.core
  (:require [cheshire.core :as json]
            [camel-snake-kebab.core :refer :all]
            [common.config :as config]
            [common.wrap :as wrap]
            [clojure.java.io :as io]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojure.repl]
            [wxpush.core :as wxpush]
            [common.log-ext :as log-ext]
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
      (let [users (or (config/get-config :users)
                      #{})]
        (log/info :add-new-user user)
        (config/set-config! :users (conj users user))
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

(def running (atom false))
(defjob CheckLiXianJob
  [ctx]
  (if @running
    (log/info :check-lixian-job "running, exit!")
    (do (reset! running true)
        (log/info :check-lixian-job :run)
        (again/with-retries
          {::again/callback log-ext/log-attempt
           ::again/strategy (again/constant-strategy 30000)
           ::again/user-context (atom {})}
          (lixian/check-lixian))
        (log/info :check-lixian-job :done)
        (reset! running false))))

(defn shutdown [_]
  (log/warn :system-shutdown)
  (config/save-config!)
  (System/exit 0))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (log-ext/log-to-file! (config/get-config :log-file "logs.log"))

  (run-server #'all-routes {:port 8088})
  (log/info :http-server-ok)
  (doseq [cron-config (config/get-config :cron-configs [{:job-key "lixian.job.1"
                                                         :trigger-key "lixian.trigger.1"
                                                         :schedule "0 0/30 5-21 * * ?"}])]
    (cron/make-cron CheckLiXianJob cron-config))
  (clojure.repl/set-break-handler! shutdown)
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
