((clojure-mode
  (cider-jack-in-nrepl-middlewares . ("shadow.cljs.devtools.server.nrepl/middleware" ("refactor-nrepl.middleware/wrap-refactor" :predicate cljr--inject-middleware-p) "cider.nrepl/cider-middleware"))
  (cider-preferred-build-tool . clojure-cli)
  (cider-custom-cljs-repl-init-form . "(do (require '[shadow.cljs.devtools.api :as shadow]) (require '[shadow.cljs.devtools.server :as server]) (server/start!) (shadow/watch :app) (shadow/nrepl-select :app))")
  (cider-default-cljs-repl . custom)
  (cider-clojure-cli-global-options . "-A:env/dev:env/test:cljs")))
