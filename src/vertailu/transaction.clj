(ns vertailu.transaction
  (:require [datahike.api :as d]
            [datomic.api :as c]))

(def schema2 [{:db/ident       :client/name
              :db/valueType   :db.type/string
              :db/unique      :db.unique/identity
              :db/index       true
              :db/cardinality :db.cardinality/one
              :db/doc         "a client's name"}
             {:db/ident       :client/email
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/many
              :db/doc         "a client's email"}])

(def schema [{:db/ident       :client/name
              :db/valueType   :db.type/string
              :db/unique      :db.unique/identity
              :db/cardinality :db.cardinality/one
              :db/doc         "a client's name"}
             {:db/ident       :client/email
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/many
              :db/doc         "a client's email"}])

(def example-data [{:client/name "alice" :client/email "alice@exam.ple"}
                   {:client/name "john" :client/email "john@exam.ple"}])


(def query0 '[:find ?e
              :where [?e :client/email]])


(def query1 '[:find ?email
              :where [?e :client/email ?email]
                     [?e :client/name "alice"]])

(def query2 '[:find [?email ?name]
              :where [?e :client/email ?email]
                     [?e :client/name  ?name]])

(def query3 '[:find ?email .
              :where [?e :client/email ?email]])


;; Preparation
(def dconn (let [uri "datahike:mem://datahike-vs-datomic"]
             (d/delete-database uri)
             (d/create-database uri)
             (d/connect uri)))
(def cconn (let [uri "datomic:mem://datahike-vs-datomic"]
             (c/delete-database uri)
             (c/create-database uri)
             (c/connect uri)))

(def ddb (d/db dconn))
(def cdb (c/db cconn))

;; Transacting data before schema
(d/transact dconn example-data)                             ;; Execution error: No schema found in db
(c/transact cconn example-data)
;; #object[datomic.promise$settable_future$reify__4751 0x3d588077 {:status :failed, :val #error{:cause ":db.error/not-an-entity Unable to resolve entity: :contributor/name", :data {:entity :contributor/name, :db/error :db.error/not-an-entity}, :via [{:type java.util.concurrent.ExecutionException, :message "java.lang.IllegalArgumentException: :db.error/not-an-entity Unable to resolve entity: :contributor/name", :at [datomic.promise$throw_executionexception_if_throwable invokeStatic "promise.clj" 10]} {:type datomic.impl.Exceptions$IllegalArgumentExceptionInfo, :message ":db.error/not-an-entity Unable to resolve entity: :contributor/name", :data {:entity :contributor/name, :db/error :db.error/not-an-entity}, :at [datomic.error$arg invokeStatic "error.clj" 57]}], :trace [<stacktrace>]}}]

;; Transacting schema data
(d/transact dconn schema)
;; #datahike.db.TxReport {:db-before #datahike/DB{:schema #:db{:ident #:db{:unique :db.unique/identity}}, :datoms []}, :db-after  #datahike/DB{:schema  {:db/ident #:db{:unique :db.unique/identity}, <schema>, :datoms [<datoms>]}, :tx-data [<datoms>], :tempids #:db{:current-tx 536870913}, :tx-meta nil}
(c/transact cconn schema)
;; #object [datomic.promise$settable_future$reify__4751 0x66fc0a3c {:status :ready, :val {:db-before datomic.db.Db, @9addbb1 :db-after, datomic.db.Db @e5eb210f, :tx-data [<datoms>], :tempids {-9223301668109598143 63, -9223301668109598142 64, -9223301668109598141 65}}}] (pretty printed


;; Transacting data
(d/transact dconn example-data)                             ;; see schema transaction
(c/transact cconn example-data)                             ;; see schema transaction


;; Querying the data
(d/q query0 @dconn)                                         ;; #{["alice@exam.ple"]}
(c/q '[:find ?e :where [?e :client/name]] cdb)
(c/q query0 cdb)
(c/q query1 cdb)
(c/q query2 cdb)
(c/q query3 cdb)


(d/pull @dconn '[*] [:client/name "alice"])

(c/pull cdb '[*] [:client/name "alice"])


(d/datoms ddb :eavt)                                        ;; nil
(c/datoms cdb :eavt)                                        ;; #object[datomic.db$datoms$reify__1584 0x53aeeeca "datomic.db$datoms$reify__1584@53aeeeca"]

(d/entity ddb 4)
(c/entity cdb 1)