(ns khie.core
  (:require [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.string :as string]
            [com.rpl.specter :as s]))

(create-ns 'khie.config)
(alias 'config 'khie.config)

(defn subst-home [home-path config]
  (s/transform (s/walker string?)
               #(string/replace % "$HOME" home-path)
               config))

(defn create-marks [mark-path mappings]
  (doseq [[mark path] mappings]
    (shell/sh "bash" "-c" (format "ln -s %s %s/%s"
                                  path mark-path (name mark)))))

(defn format-shortcut [aformat prefix [k v]]
  (format aformat (str prefix (name k)) v))

(defn format-shortcuts [aformat prefix mappings]
  (string/join \newline
               (map #(format-shortcut aformat prefix %) mappings)))

(defn run [config-p]
  (let [config (->> config-p 
                    slurp 
                    edn/read-string 
                    (subst-home (System/getenv "HOME")))]
    (create-marks (::config/mark-path config) (::config/dir-mappings config))
    (doseq [{:keys [::config/path ::config/format]} (::config/emit-specs config)]
      (spit path 
            (str (format-shortcuts format "F" (::config/file-mappings config))
                 \newline
                 (format-shortcuts format "O" (::config/dir-mappings config))
                 \newline)))))

(comment

  (run (str (System/getenv "HOME") "/.config/Khie/config.edn"))

  )

(defn -main [[config-p]]
  (run config-p))
