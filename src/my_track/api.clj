(ns my-track.api
  (:require [clj-http.client :as http]
            [taoensso.timbre :as log]
            [cheshire.core :as json]
            [java-time]
            [reaver :as html]
            [clojure.set :as set]))

(defn get-guzhi
  "获取估值"
  [code]
  (some-> (http/get (format "http://fundgz.1234567.com.cn/js/%s.js" code))
          :body
          (->> (re-find #"jsonpgz\((.*)\);"))
          second
          (json/decode keyword)
          (update :gztime #(java-time/local-date-time "yyyy-M-d H:m" %1))
          (update :dwjz bigdec)
          (update :gsz bigdec)
          (update :gszzl bigdec)
          ))

(defn- parse-history
  "解析历史记录"
  [table]
  (when-let [doc (html/parse-fragment table)]
    (html/extract-from doc "tbody tr"
                       [:date :unit-value :total-value :day-growth]
                       "td:eq(0)" html/text
                       "td:eq(1)" html/text
                       "td:eq(2)" html/text
                       "td:eq(3)" html/text)))

(defn get-history
  "获取历史净值"
  [code]
  (some-> (http/get (format "http://fund.eastmoney.com/f10/F10DataApi.aspx?type=lsjz&code=%s" code))
          :body
          (->> (re-find #"content:\"(.*?)\","))
          second
          parse-history
          ))

(defn- fix-keyname
  [info]
  (set/rename-keys info {:f14 :name
                         :f12 :code
                         :f17 :today-start
                         :f15 :today-highest
                         :f16 :today-lowest
                         :f3 :change-percent
                         :f4 :change-num
                         :f24 :change-days-60
                         :f25 :change-year
                         :f2 :today-new
                         :f104 :change-jump-count
                         :f105 :change-sink-count
                         :f106 :change-unchanged-count
                         }))

(defn- get-dapan
  "获取大盘状态"
  []
  (some-> (http/get "http://push2.eastmoney.com/api/qt/ulist.np/get?fltt=2&secids=1.000001,0.399001"
                    {:as :json})
          (get-in [:body :data :diff])
          (->> (map fix-keyname))))

(defn get-all-funds
  "获取所有基金代码"
  []
  (some-> (http/get "http://fund.eastmoney.com/js/fundcode_search.js")
          :body
          (->> (re-find #"var r = (.*);"))
          second
          json/decode
          (->> (map (partial zipmap [:code :short :name :type :full])))))


(comment

  (get-guzhi "470009")

  (first (get-all-funds))

  )
