(require '[puppetlabs.analytics-client.core :as client])

(def test-resources-path "./dev-resources/puppetlabs/analytics/test")
(def ssl-cert-path (str test-resources-path "/localhost_cert.pem"))
(def ssl-key-path (str test-resources-path "/localhost_key.pem"))
(def ssl-ca-cert-path (str test-resources-path "/ca.pem"))

(def ssl-opts
  {:ssl-cert ssl-cert-path
   :ssl-key ssl-key-path
   :ssl-ca-cert ssl-ca-cert-path})

(def config {:analytics {:url "https://localhost:4433/analytics" :ssl-opts ssl-opts}})

(defn store-snapshot
  [analytics]
  (client/store-snapshot config analytics))

(defn store-event
  [analytics]
  (client/store-event config analytics))
