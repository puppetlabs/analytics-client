(defn deploy-info
  [url]
  {:url url
   :username :env/clojars_jenkins_username
   :password :env/clojars_jenkins_password
   :sign-releases false})

(defproject puppetlabs/analytics-client "1.1.1-SNAPSHOT"
  :description "A library for submitting metrics to the trapperkeeper analytics service."
  :url "https://github.com/puppetlabs/analytics-client"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure]
                 [prismatic/schema]
                 [puppetlabs/http-client]
                 [cheshire]]
  :profiles {:defaults {:source-paths ["dev"]
                        :dependencies [[puppetlabs/trapperkeeper :classifier "test" :scope "test"]
                                       [puppetlabs/trapperkeeper]
                                       [puppetlabs/kitchensink :classifier "test" :scope "test"]
                                       [puppetlabs/trapperkeeper-webserver-jetty9]
                                       [ring-mock "0.1.5"]]}
             :dev [:defaults {:dependencies [[org.bouncycastle/bcpkix-jdk15on]]}]
             :fips [:defaults {:dependencies [[org.bouncycastle/bctls-fips]
                                              [org.bouncycastle/bcpkix-fips]
                                              [org.bouncycastle/bc-fips]]
                               :jvm-opts ~(let [version (System/getProperty "java.version")
                                                [major minor _] (clojure.string/split version #"\.")
                                                unsupported-ex (ex-info "Unsupported major Java version. Expects 8 or 11."
                                                                 {:major major
                                                                  :minor minor})]
                                            (condp = (java.lang.Integer/parseInt major)
                                              1 (if (= 8 (java.lang.Integer/parseInt minor))
                                                  ["-Djava.security.properties==./dev-resources/java.security.jdk8-fips"]
                                                  (throw unsupported-ex))
                                              11 ["-Djava.security.properties==./dev-resources/java.security.jdk11-fips"]
                                              (throw unsupported-ex)))}]}
  :parent-project {:coords [puppetlabs/clj-parent "4.9.4"]
                   :inherit [:managed-dependencies]}
  :plugins [[lein-parent "0.3.8"]]

  :repositories [["releases" "https://artifactory.delivery.puppetlabs.net/artifactory/clojure-releases__local/"]
                 ["snapshots" "https://artifactory.delivery.puppetlabs.net/artifactory/clojure-snapshots__local/"]]
  :deploy-repositories [["releases" ~(deploy-info "https://clojars.org/repo")]
                        ["snapshots" "https://artifactory.delivery.puppetlabs.net/artifactory/clojure-snapshots__local/"]])
