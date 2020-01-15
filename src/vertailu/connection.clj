(ns vertailu.connection
  (:require [datahike.api :as d]
            [datomic.api :as c]))

;; URI scheme
(def datahike-uri "datahike:mem://datahike-vs-datomic")
(def datomic-uri "datomic:mem://datahike-vs-datomic")

;; Deletion of non-existing database
(d/delete-database datahike-uri)                            ;; {}
(c/delete-database datomic-uri)                             ;; false

;; Creation of non-existing database
(d/create-database datahike-uri)                            ;; nil
(c/create-database datomic-uri)                             ;; true

;; Creation of existing database
(d/create-database datahike-uri)                            ;; nil
(c/create-database datomic-uri)                             ;; false

;; Existence check of existing database
(d/database-exists? datahike-uri)                           ;; true
(c/database-exists? datomic-uri)                            ;; Syntax error: No such var

;; Deletion of existing database
(d/delete-database datahike-uri)                            ;; {}
(c/delete-database datomic-uri)                             ;; true

;; Existence check of non-existing database
(d/database-exists? datahike-uri)                           ;; false
(c/database-exists? datomic-uri)                            ;; Syntax error: No such var

;; Connection of non-existing database
(d/connect datahike-uri)                                    ;; Execution error: Backend does not exist
(c/connect datomic-uri)                                     ;; Execution error: :db.error/db-not-found Could not find datahike-vs-datomic in catalog

;; Preparation
(d/create-database datahike-uri)
(c/create-database datomic-uri)

;; Connection of existing database
(d/connect datahike-uri)
;; #object[clojure.lang.Atom 0x22226112 {:status :ready, :val #datahike/DB {:schema #:db{:ident #:db{:unique :db.unique/identity}}, :datoms []}}]
(c/connect datomic-uri)
;; #object[datomic.peer.LocalConnection 0x33c5977d datomic.peer.LocalConnection@33c5977d]



;; Listing databases
(d/get-database-names "datahike:mem://*")                   ;; Syntax error: No such var
(c/get-database-names "datomic:mem://*")                    ;; ("datahike-vs-datomic")

;; Renaming databases
(d/rename-database datahike-uri "renamed")                  ;; Syntax error: No such var
(c/rename-database datomic-uri "renamed")                   ;; true
(c/rename-database "datomic:mem://renamed" "datahike-vs-datomic")

;; Database from connection
(d/db (d/connect datahike-uri))                                                ;; #datahike/DB{:schema #:db{:ident #:db{:unique :db.unique/identity}}, :datoms []}
(c/db (c/connect datomic-uri))                                                ;; datomic.db.Db@40f12152
