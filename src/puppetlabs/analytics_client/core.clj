(ns puppetlabs.analytics-client.core
  (:require [schema.core :as schema]
            [puppetlabs.http.client.sync :as http-client]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [puppetlabs.i18n.core :refer [trs]])
  (:import (com.puppetlabs.http.client HttpClientException)
           (java.io IOException)))

(def SSLOpts
  "A schema map of the expected keys for making an SSL request."
  {:ssl-cert schema/Str
   :ssl-key schema/Str
   :ssl-ca-cert schema/Str})

(def AnalyticsConfig
  {(schema/required-key :analytics) {:url schema/Str
                                     :ssl-opts SSLOpts}})

(def ScalarValue
  (schema/cond-pre schema/Str schema/Num schema/Bool))

(def FieldValue
  (schema/cond-pre
   [ScalarValue]  ; Should all be the same but not easy in schema
   {schema/Keyword ScalarValue}
   ScalarValue))

(def Metadata
  {schema/Keyword FieldValue})

(def Snapshot
  {(schema/required-key :fields) Metadata})

(def Event
  {(schema/required-key :event) schema/Str
   (schema/optional-key :metadata) Metadata})

(defn ^:private http-get-or-nil
  ([url ssl-opts]
   (http-get-or-nil url ssl-opts {}))
  ([url ssl-opts query-params]
   (try
     (http-client/get url (merge ssl-opts {:query-params query-params}))
     (catch IOException e
       (log/debug (trs "Failed to reach server {0}" url))
       (log/trace e (trs "Failed to reach server {0}" url)))
     (catch com.puppetlabs.http.client.HttpClientException e
       (log/debug (trs "Failed to reach server {0}" url))
       (log/trace e (trs "Failed to reach server {0}" url))))))

(schema/defn ^:always-validate entrypoint
  "Gets the entrypoint data from the analytics service"
  [url :- schema/Str
   ssl-opts :- SSLOpts]
  (let [payload (http-get-or-nil url ssl-opts)
        body (when payload
               (-> payload
                   :body
                   slurp
                   (json/parse-string false)))]
    body))

(schema/defn ^:always-validate run-command
  "Runs a given command against the server defined in
  the given config. It does this by first retrieving
  the entrypoint to determine the command's URL. It
  then sends a POST to the endpoint specified for
  the 'id' of that command."
  [config :- AnalyticsConfig
   command-name :- schema/Str
   payload :- {schema/Any schema/Any}]
  (let [ssl-opts (get-in config [:analytics :ssl-opts])
        url (get-in config [:analytics :url])
        entrypoint-data (entrypoint url ssl-opts)]
    (if entrypoint-data
      (let [command-url (get-in entrypoint-data ["commands" command-name "id"])
            result (http-client/post command-url (merge ssl-opts {:body (json/generate-string payload)
                                                                  :headers {"content-type" "application/json"}
                                                                  :as :text}))]
        (-> result
            :body
            (json/parse-string false)))
      (log/info "Cannot locate analytics service; not reporting"))))


(schema/defn ^:always-validate store-snapshot
  "Sends snapshot to analytics service"
  [config :- AnalyticsConfig
   analytics :- Snapshot]
  (run-command config "snapshot" analytics))

(schema/defn ^:always-validate store-event
  "Sends snapshot to analytics service"
  [config :- AnalyticsConfig
   analytics :- Event]
  (run-command config "event" analytics))
