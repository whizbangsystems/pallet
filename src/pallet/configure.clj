(ns pallet.configure
  "Pallet configuration using ~/.pallet/config.clj"
  (:require
   [pallet.environment :as environment]
   [pallet.utils :as utils]
   [clojure.java.io :as java-io]
   [clojure.walk :as walk]
   [clojure.contrib.logging :as logging]))

(def ^{:private true} config nil)

(defn- unquote-vals [args]
  (walk/walk
   (fn [item]
     (cond (and (seq? item) (= `unquote (first item))) (second item)
           ;; needed if we want fn literals to be usable by eval-in-project
           (and (seq? item) (= 'fn (first item))) (list 'quote item)
           (symbol? item) (list 'quote item)
           :else (unquote-vals item)))
   identity
   args))

(defmacro defpallet
  [& {:keys [provider identity credential providers admin-user]
      :as config-options}]
  `(let [m# (zipmap
             ~(cons 'list (keys config-options))
             ~(cons 'list (unquote-vals (vals config-options))))]
    (alter-var-root
     #'config
     (fn [_#] m#))))

(defn- read-config
  [file]
  (try
    (use '[pallet.configure :only [defpallet]])
    (load-file file)
    config
    (catch java.io.FileNotFoundException _)))

(defn- home-dir
  "Returns full path to Pallet home dir ($PALLET_HOME or $HOME/.pallet)"
  []
  (.getAbsolutePath
   (doto (if-let [pallet-home (System/getenv "PALLET_HOME")]
           (java.io.File. pallet-home)
           (java.io.File. (System/getProperty "user.home") ".pallet"))
     .mkdirs)))

(defn pallet-config
  "Read pallet configuration file."
  []
  (read-config (.getAbsolutePath (java-io/file (home-dir) "config.clj"))))

(defn compute-service-properties
  "Helper to read compute service properties"
  [config profiles]
  (when config
    (when (:providers config)
      (logging/warn
       "DEPRECATED: use of :providers key in ~/.pallet/config.clj
           is deprecated. Please change to use :services."))
    (let [service (first profiles)
          default-service (map config [:provider :identity :credential])
          services (:services config (:providers config))
          environment (when-let [env (:environment config)]
                        (environment/eval-environment env))]
      (cond
       (every? identity default-service) (select-keys
                                          config
                                          [:provider :identity :credential
                                           :blobstore :endpoint :environment])
       (map? services) (->
                        (or
                         (and service (or
                                       (services (keyword service))
                                       (services service)))
                         (and (not service) ; use default if no profile
                                        ; requested
                              (first services)
                              (-> services first val)))
                        (utils/maybe-update-in
                         [:environment]
                         #(environment/merge-environments
                           environment (environment/eval-environment %))))
       :else nil))))
