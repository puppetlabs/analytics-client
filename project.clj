(defn deploy-info
  [url]
  {:url url
   :username :env/nexus_jenkins_username
   :password :env/nexus_jenkins_password
   :sign-releases false})

(defproject puppetlabs/analytics-client "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [prismatic/schema]
                 [puppetlabs/http-client]
                 [cheshire "5.5.0"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[puppetlabs/trapperkeeper nil :classifier "test" :scope "test"]
                                  [puppetlabs/trapperkeeper nil]
                                  [puppetlabs/kitchensink nil :classifier "test" :scope "test"]
                                  [puppetlabs/trapperkeeper-webserver-jetty9]
                                  [ring-mock "0.1.5"]]} }
  :parent-project {:coords [puppetlabs/clj-parent "0.8.0"]
                   :inherit [:managed-dependencies]}
  :plugins [[lein-parent "0.3.1"]]

  :repositories [["releases" "http://nexus.delivery.puppetlabs.net/content/repositories/releases/"]
                 ["snapshots"  "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/"]]

  :deploy-repositories [["releases" ~(deploy-info "http://nexus.delivery.puppetlabs.net/content/repositories/releases/")]
                        ["snapshots" ~(deploy-info "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/")]])
