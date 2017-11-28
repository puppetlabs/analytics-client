# analytics-client

A Clojure library designed to send analytics to the analytics service.

## Usage

Analytics are broken up into two types:

- Snapshots
- Events

Snapshots describe the state of a system at a given time, while
events describe a discrete event. Events can be reported to the
analytics service immedately as they occcur, while snapshots are more
often reported periodically.

Two functions are supplied for sending these analytics are
`store-snapshot` and `store-event`.

If the analytics-client is being called from within a Trapperkeeper
application, the `config` argument can be derived from Trapperkeeper's
default service config via:

```
(select-keys config [:analytics])
```

### Sending Snapshots

To send a snapshot to the analytics service, use the `store-snapshot`
function. Its arguments are:

1. An object, keys:
-- `:ssl-opts`: An object with keys `:ssl-cert`, `:ssl-key`, and `:ssl-ca-cert`.
-- `:url`: A string representing the URL where the analytics service
           is running.
1. An object, keys:
-- `:fields`: An object with *unique* keyword keys representing the
              various metrics being collected. Each keyword should be
              namespaced accordingly, typically using periods between
              namespaces. The corresponding values for each key can be:
              - strings/numbers/booleans,
              - arrays of strings/numbers/booleans, or 
              - objects from keywords to strings/numbers/booleans.

A valid call to this function might look like this:

```
(let [config {:analytics {:url analytics-url
                          :ssl-opts ssl-opts}}
      analytics {:fields {:puppetserver.metric1 "value"
                          :puppetserver.metric2 1337
                          :puppetserver.metric3 ["123" "456"]
                          :puppetserver.metric4 {:something "extra"}}}]
  (analytics-client/store-snapshot config analytics))
```

### Sending Events

To send an event to the analytics service, use the `store-event`
function. Its arguments are:

1. An object, keys:
-- `:ssl-opts`: An object with keys `:ssl-cert`, `:ssl-key`, and `:ssl-ca-cert`.
-- `:url`: A string representing the URL where the analytics service
           is running.
1. An object, keys:
-- `:event`: A string representing the name of the event. Note that this
             should be namespaced with periods, and match what exists
             in the server's whitelist.
-- `:metadata`: An optional map representing any associated metadata
                to further define the nature of the event. Keys for this
                must be keywords and values can be:
                - strings/numbers/booleans,
                - arrays of strings/numbers/booleans, or 
                - objects from keywords to strings/numbers/booleans.

A valid call to this function might look like this:

```
(let [ssl-opts {:ssl-cert ssl-cert
                :ssl-key ssl-key
                :ssl-ca-cert ssl-ca-cert}
      config {:analytics {:url analytics-url
                          :ssl-opts ssl-opts}}
      analytics {:event "puppetserver.some-event"
                 :metadata {:property1 "value"
                            :property2 1337
                            :property3 ["123" "456"]
                            :property4 {:some "value"}}}
  (analytics-client/store-event config analytics))
```

### Exploration

To try out the analytics-client repo, perform the following:

1. Checkout the analytics repo and run `lein tk`.
1. Run `lein repl` in this repo.
1. Send commands to the service, e.g.: `(store-snapshot {:fields {:hello "world"}})`

## License

Copyright © 2017 Puppet

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
