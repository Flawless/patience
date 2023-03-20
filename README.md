> In the art of medicine, patience is not just a virtue - it is a necessity.
>
> `--- GPT-3.5)`

# TL;DR
## Run repl in dev mode
```
clojure -A:env/dev:env/test:cljs
```
## Launch
inside repl call
```
(require '[patience.core :as patience])
(patience/start) ; => :started
```
## Test
```
clojure -X:test :dirs '["src/test"]'
```
## Build
```
clojure -T:build uber
```
## Launch
```
java -jar target/patience.jar
```
## Build docker
```
docker build -t flawlesslt/patience docker
```
## k8s
see readme in k8s directory

# Structure
## Brief description
This is one of my playgrouds and in the same time the demo app. Desite it wasn't designed for a real usage, the app
contains all necessery parts to simulate whole process of development an real world production ready application: it can
store data, inside various backends: postgresql for production persistent storage and inmemory store for development and
testing, it exposes an UI to interact with user, the main UI written with pure html (rendered via hiccup), but app also
has expirimental UI on nice but a bit too raw shadow-grove library and API for it. The app may be built to uberjar and
packed into docker image, this process is automated with github actions, also there is an all-in-one (except config
generation) template for deploing on k8s. Let me show you it in more details.

## Back-end
Back-end was build on top of the integrant, to allow flexible component management both in development and
production. Jetty with ring is used to serve hiccup UI and API, also jetty is able to use to serve static. Reitit with
muuntaja an malli is used to route, coerce and validate received requests and transmitted responses. Connection to
postgresql managed by next.jdbc and SQL requests render with honeysql. There is also ability to easy switching of
storage backends (see `patience.db`) for example to connect inmemory store in development or tests.

## Front-end+
Despite main UI views is built on just hiccup and shadow-css and is preparing on server side avoiding any usage of js,
there is also experimental frontend written with experimental shadow-grove (frontend library replacing react, reagent
and re-frame stack). There is still a lot of work to make it functional, but I'm realy exited about pure clojure
replacment of react, so I tried to use it in thi project as far as it's nature is very experimental for me too.

## Integration and Deployment
The project contains github action to test, build, pack the project inside docker container and push to Dockerhub. There
is also template for k8s to deploy the project on a k8s cluster.

# Configuration
The app allow configuration through `/etc/patience.edn` file. You can find an example of such config in
`k8s/patience.edn` file.  To configure administrator should have at least basic knowledge about edn structures. The file
contains a map where each nested map represent single config section:
## :db/patiens
Represents config of patients db, two keys under this config section is allowed `:type` - `:sql` of `:dummy`, defines
type of backend - SQL db or Inmemory store, and `:config` key - when SQL backend selected this key consists of jdbc.next
datasource config, e.g.:
```clj
{:dbtype "postgres"
 :dbname "patience"
 :user "postgres"
 :password "postgres"
 :host "postgres"}
```
## :ring/handler
Contains handler config, should contains `:patients` key with special value `#ig/ref :db/patients` - an integrant link
to patients db and optional boolean key `:serve-static?` defines is static served by embedded web server or not,
default true.
## :jetty/server
Defines jetty server config, contains special key `:ring-handler` that should contain integrant ref to ring/handler
(`#ig/ref :ring/handler`), an `:host` reprsented by string and iteger `:port` in range from 1 to 65535 (why I'm
writting here such well-known things? (: )

As it was said above, you can find an example of config in k8s directory.

# Future development
There is some thing to improve in a future. I tryied to add FIXME comments in such places but here is short summary:
- the first of all the coercion should be imporved to prevent receiving empty strings from received forms and
especcialy to deal with timezones, currently there is a problems as far as date of birth coerced to insts as dates with
local tz (instead of just dates).
- the second big thing I want to do is a frontend on shadow-grove, there is some not 100% usable version of it and it
should be made usefull (maybe after shadow-grove will be a more well documented (: )
- the third it's good to see here some integration or maybe even e2e tests, it should be through out, but as far as
it's just a demo app, I believe it's ok not to have such heavy constructions here.
