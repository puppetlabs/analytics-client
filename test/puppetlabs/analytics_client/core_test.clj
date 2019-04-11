(ns puppetlabs.analytics-client.core-test
  (:require [clojure.test :refer :all]
            [puppetlabs.analytics-client.core :as core]
            [puppetlabs.trapperkeeper.app :as tk-app]
            [puppetlabs.trapperkeeper.services.webserver.jetty9-service :refer :all]
            [puppetlabs.trapperkeeper.testutils.bootstrap :refer [with-app-with-config]]
            [puppetlabs.trapperkeeper.testutils.logging :refer [with-test-logging]]
            [cheshire.core :as json]))

(def test-resources-path "./dev-resources/puppetlabs/analytics/test")
(def ssl-cert-path (str test-resources-path "/localhost_cert.pem"))
(def ssl-key-path (str test-resources-path "/localhost_key.pem"))
(def ssl-ca-cert-path (str test-resources-path "/ca.pem"))

(def ssl-opts
  {:ssl-cert ssl-cert-path
   :ssl-key ssl-key-path
   :ssl-ca-cert ssl-ca-cert-path})

(def jetty-config
  {:webserver {:port 8080
               :ssl-host "0.0.0.0"
               :ssl-port 4433
               :ssl-cert ssl-cert-path
               :ssl-key ssl-key-path
               :ssl-ca-cert ssl-ca-cert-path}})

(def entrypoint-payload
  {:commands {"event" {:name "event"
                       :rel "https://api.puppetlabs.com/analytics/v1/commands/event"
                       :id "https://localhost:4433/analytics/v1/api/commands/event"
                       :params {"namespace" {"datatype" "string"}
                                "event" {"datatype" "string"}
                                "metadata" {"datatype" "object"
                                            "optional" "true"}}}
              "snapshot" {:name "snapshot"
                          :rel "https://api.puppetlabs.com/analytics/v1/commands/snapshot"
                          :id "https://localhost:4433/analytics/v1/api/commands/snapshot"
                          :params {"namespace" {"datatype" "string"}
                                   "fields" {"datatype" "object"}}}}
   :version {:server "1.0.0"}})

(def event-payload
  {:status "success"})

(def snapshot-payload
  {:status "success"})

(defn add-analytics-service
  "Adds a handler for the analytics service."
  [app handler-extension a-promise]
  (add-ring-handler
   (tk-app/get-service app :WebserverService)
   (fn [req]
     (if (and (= (:uri req) "/analytics/v1/api") (= (:request-method req) :get))
       ;; GET Entrypoint
       (let [params (json/parse-string (slurp (:body req)))
             return-message entrypoint-payload
             body (json/encode return-message)]
         {:status 200 :body body})
       (if (and (= (:uri req) "/analytics/v1/api/commands/event") (= (:request-method req) :post))
         ;; POST Event
         (let [params (json/parse-string (slurp (:body req)))
               return-message event-payload
               body (json/encode return-message)]
           (when a-promise
             (deliver a-promise params))
           {:status 200 :body body})
         (if (and (= (:uri req) "/analytics/v1/api/commands/snapshot") (= (:request-method req) :post))
           ;; POST Snapshot
           (let [params (json/parse-string (slurp (:body req)))
                 return-message snapshot-payload
                 body (json/encode return-message)]
             (when a-promise
               (deliver a-promise params))
             {:status 200 :body body})
           {:status 404 :body (json/encode "Can't handle that request")}))))
   handler-extension)
  (str "https://localhost:4433" handler-extension))

(defn check-promise
  ([promise]
   (check-promise promise nil))
  ([promise result]
   (if (realized? promise)
     @promise
     (if (nil? result)
       (throw (RuntimeException. "Promise not realized before returning"))
       (throw (RuntimeException. (str "Promise not realized before returning; error was " result)))))))

(deftest test-entrypoint
  (testing "Returns entrypoint data"
    (with-app-with-config app
      [jetty9-service]
      jetty-config
      (let [url (add-analytics-service app "/analytics/v1/api" nil)
            entrypoint (core/entrypoint url ssl-opts)]
        (is (= (keys entrypoint) ["commands", "version"]))
        (is (= "https://localhost:4433/analytics/v1/api/commands/event"
               (-> entrypoint
                   (get "commands")
                   (get "event")
                   (get "id"))))))))

(deftest test-store-snapshot
  (with-app-with-config
   app
   [jetty9-service]
   jetty-config
    (testing "Sends snapshot data to proper URL"
      (let [promise (promise)
            url (add-analytics-service app "/analytics/v1/api" promise)
            config {:analytics {:url url
                                :ssl-opts ssl-opts}}
            analytics {:fields {:abc.def.ghi 123
                                :foo [{"bar" "baz"}]}}
            result (core/store-snapshot config analytics)
            params (check-promise promise result)]
        (is (= (get result "status") "success"))
        (is (= params {"fields" {"abc.def.ghi" 123
                                 "foo" [{"bar" "baz"}]}}))))

   (testing "Fails when provided an incorrect URL"
     (with-test-logging
      (let [config {:analytics {:url "https://some.invalid.url"
                               :ssl-opts ssl-opts}}
            analytics {:fields {:abc.def.ghi 123}}
           _ (core/store-snapshot config analytics)]
       (is (logged? #"Cannot locate analytics service; not reporting" :info)))))))

(deftest test-store-event
  (testing "Sends event data to proper URL"
    (with-app-with-config
     app
     [jetty9-service]
     jetty-config
     (let [promise (promise)
           url (add-analytics-service app "/analytics/v1/api" promise)
           config {:analytics {:url url
                               :ssl-opts ssl-opts}}
           analytics {:event "abc.def.ghi"
                      :metadata {:something "serious"}}
           result (core/store-event config analytics)
           params (check-promise promise result)]
       (is (= (get result "status") "success"))
       (is (= params {"event" "abc.def.ghi"
                      "metadata" {"something" "serious"}})))))

  (testing "Fails when provided an incorrect URL"
    (with-test-logging
     (let [config {:analytics {:url "https://some.invalid.url"
                               :ssl-opts ssl-opts}}
           analytics {:event "abc.def.ghi"
                      :metadata {:something "serious"}}
           _ (core/store-event config analytics)]
       (is (logged? #"Cannot locate analytics service; not reporting" :info))))))
