(ns versioning
  (:require [clojure.java.shell :as shell]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(defn get-latest-tag []
      (-> (shell/sh "git" "describe" "--tags" "--abbrev=0")
          :out
          str/trim))

(defn get-commits [tag]
      (-> (shell/sh "git" "log" (str tag) "..HEAD" "--pretty=format:%s")
          :out
          str/split-lines))

(defn parse-commit [commit]
      (cond
        (str/starts-with? commit "feat:") {:type :feat}
        (str/starts-with? commit "fix:") {:type :fix}
        (str/starts-with? commit "BREAKING CHANGE:") {:type :breaking}
        :else {:type :other}))

(defn update-version [new-version]
  (let [project-file "project.clj"
        project-content (slurp project-file)
        updated-content (clojure.string/replace
                         project-content
                         #"\(defproject ([^\s]+) \"([^\"]+)\""
                         (str "(defproject $1 \"" new-version "\""))]
    (spit project-file updated-content)))

(defn determine-new-version [commits]
      (let [has-breaking-change (some #(= (:type (parse-commit %)) :breaking) commits)
            has-feature (some #(= (:type (parse-commit %)) :feat) commits)]
           (cond
             has-breaking-change (str "1.0.0") ; Example new version for a breaking change
             has-feature (str "0.1.0")          ; Example new version for a feature
             :else (str "0.0.2"))))             ; Example patch version

(defn main []
      (let [latest-tag (get-latest-tag)
            commits (get-commits latest-tag)
            new-version (determine-new-version commits)]
           (update-version new-version)))

(main)
