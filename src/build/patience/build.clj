(ns patience.build
  (:require
   [clojure.tools.build.api :as b]
   [shadow.css.build :as cb]
   [clojure.java.io :as io]))

(defn css-release [& _args]
  (let [build-state
        (-> (cb/start)
            (cb/index-path (io/file "src" "main") {})
            (cb/generate '{:ui {:include [patience.views]}})
            (cb/write-outputs-to (io/file "resources/public" "css")))]

    (doseq [mod (:outputs build-state)
            {:keys [warning-type] :as warning} (:warnings mod)]

      (prn [:CSS (name warning-type) (dissoc warning :warning-type)]))))

(def lib 'flawless/patience)
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s.jar" (name lib)))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (css-release)
  (b/copy-dir {:src-dirs ["src/main" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :java-cmd "java"
                  :src-dirs ["src/main"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'patience.main}))
