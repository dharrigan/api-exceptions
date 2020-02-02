(ns api.core
  (:require
   [aero.core :refer [read-config]]
   [clojure.java.io :as io]
   [clojure.tools.cli :refer [parse-opts]]
   [juxt.clip.core :as clip])
  (:gen-class))

(def system nil)

(defn config
  [{:keys [filename] :as opts}]
  (-> (io/resource (or filename "config/config.edn"))
      (read-config opts)))

(def cli-options
  [["-p" "--profile PROFILE" "Profile to use (as a Clojure keyword)"
    :default :default
    :parse-fn #(keyword %)]])

(defn -main
  [& args]
  (let [{{:keys [profile]} :options} (parse-opts args cli-options)
        system-config (config {:profile profile})
        system (clip/start system-config)]
    (alter-var-root #'system (constantly system))
    (.addShutdownHook
     (Runtime/getRuntime)
     (new Thread #(clip/stop system-config system))))
  ;; do work here if required!
  @(promise))

(comment

 ;; paste into the repl
 (require
  '[api.core :as core]
  '[juxt.clip.repl :refer [start stop set-init! system]])
 (def system-config (core/config {:profile :local}))
 (set-init! #(core/config {:profile :local}))
 (start)

 #_+)
