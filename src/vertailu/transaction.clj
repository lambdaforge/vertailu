(ns vertailu.transaction
  (:require [datahike.api :as d]
            [datomic.api :as c])
  (:import  [java.util Date]))

(def schema [{:db/ident       :client/name
              :db/valueType   :db.type/string
              :db/unique      :db.unique/identity
              :db/index       true
              :db/cardinality :db.cardinality/one
              :db/doc         "a client's name"}
             {:db/ident       :client/email
              :db/valueType   :db.type/string
              :db/unique      :db.unique/identity
              :db/index       true
              :db/cardinality :db.cardinality/many
              :db/doc         "a client's email"}])

(def example-data [{:client/name "alice" :client/email "alice@exam.ple"}
                   {:client/name "john" :client/email "john@exam.ple"}])

(def example-data2 [{:client/name "bill" :client/email "bill@exam.ple"}])

(def query '[:find ?email
              :where [?e :client/email ?email]
                     [?e :client/name "alice"]])


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

;; Transacting data before schema
(d/transact dconn example-data)                             ;; Execution error: No schema found in db
(deref (c/transact cconn example-data))                     ;; Execution error (Exceptions$IllegalArgumentExceptionInfo) :db.error/not-an-entity Unable to resolve entity: :client/name

;; Transacting schema data
(d/transact dconn schema)
;; #datahike.db.TxReport {:db-before #datahike/DB{:schema #:db{:ident #:db{:unique :db.unique/identity}}}, :db-after  #datahike/DB{:schema  {:db/ident #:db{:unique :db.unique/identity}, <schema>}, :tx-data [<datoms>], :tempids #:db{:current-tx 536870913}, :tx-meta nil}
(deref (c/transact cconn schema))
;; {:db-before datomic.db.Db, @6f444793 :db-after, datomic.db.Db @74243af5, :tx-data [#datom[13194139534312 50 #inst"2019-12-18T18:13:47.977-00:00" 13194139534312 true] #datom[63 10 :client/name 13194139534312 true] #datom[63 40 23 13194139534312 true] #datom[63 42 38 13194139534312 true] #datom[63 41 35 13194139534312 true]           #datom[63 62 "a client's name" 13194139534312 true]           #datom[64 10 :client/email 13194139534312 true]           #datom[64 40 23 13194139534312 true]           #datom[64 41 36 13194139534312 true]           #datom[64 62 "a client's email" 13194139534312 true]           #datom[0 13 64 13194139534312 true]           #datom[0 13 63 13194139534312 true]], :tempids {-9223301668109598142 63, -9223301668109598141 64}}

;; Transacting data
(d/transact dconn example-data)                             ;; see schema transaction
(deref (c/transact cconn example-data))                     ;; see schema transaction


;; Querying the data
(d/q query (d/db dconn))                                    ;; #{["alice@exam.ple"]}
(c/q query (c/db cconn))                                    ;; #{["alice@exam.ple"]}

(d/pull (d/db dconn) '[*] [:client/name "alice"])           ;; {:db/id 3, :client/name "alice", :client/email ["alice@exam.ple"]}
(c/pull (c/db cconn) '[*] [:client/name "alice"])           ;; {:db/id 17592186045418, :client/name "alice", :client/email ["alice@exam.ple"]}

(d/pull-many (d/db dconn) '[*] [1 2])                       ;; [#:db{:id 1,:cardinality :db.cardinality/one,:doc "a client's name",:ident :client/name,:index true,:unique :db.unique/identity,:valueType :db.type/string}]
(c/pull-many (c/db cconn) '[*] [1 2])                       ;; [#:db{:id 1, :ident :db/add, :doc "Primitive assertion. All transactions eventually reduce to a collection of primitive assertions and retractions of facts, e.g. [:db/add fred :age 42]."}]


;; Get hypothetical database after transaction
(d/with (d/db dconn) example-data2)
;; #datahike.db.TxReport {:db-before #datahike/DB{:schema #:db{:ident #:db{:unique :db.unique/identity}}}, :db-after  #datahike/DB{:schema  {:db/ident #:db{:unique :db.unique/identity}, <schema>}, :tx-data [<datoms>], :tempids #:db{:current-tx 536870913}, :tx-meta nil}
(c/with (c/db cconn) example-data2)
;; {:db-before datomic.db.Db, @1a31deb2 :db-after, datomic.db.Db @e14ef0bd, :tx-data [#datom[13194139534318 50 #inst"2020-01-15T16:37:31.178-00:00" 13194139534318 true] #datom[17592186045423 63 "bill" 13194139534318 true]  #datom[17592186045423 64 "bill@exam.ple" 13194139534318 true]], :tempids {-9223301668109598132 17592186045423}}

(type (d/with (d/db dconn) example-data2))
(type (c/with (c/db cconn) example-data2))

;; Get the database of a given entity
(d/entity-db (d/entity (d/db dconn) :client/email))
;; #datahike/DB{:schema {:db/ident #:db{:unique :db.unique/identity}, :client/name #:db{:ident :client/name, :valueType :db.type/string, :unique :db.unique/identity, :index true, :cardinality :db.cardinality/one, :doc "a client's name"}, 1 :client/name, :client/email #:db{:ident :client/email, :valueType :db.type/string, :unique :db.unique/identity, :index true, :cardinality :db.cardinality/many, :doc "a client's email"}, 2 :client/email},:datoms <datoms>}
(c/entity-db (c/entity (c/db cconn) :client/email))
;; datomic.db.Db@1a31deb2

(str (type (d/entity-db (d/entity (d/db dconn) :client/email))))     ;; datahike.db.DB
(str (type (c/entity-db (c/entity (c/db cconn) :client/email))))     ;; datomic.db.Db

;; Get the historical database
(d/history ddb)                                                      ;; #datahike.db.HistoricalDB{}
(d/history (d/db dconn))                                             ;; Error printing return value (ClassCastException) at clojure.core/key (core.clj:1567).
(c/history (c/db cconn))                                             ;; datomic.db.Db@a8299901


;; Filtering

(d/is-filtered (d/db dconn))                                         ;; false
(c/is-filtered (c/db cconn))                                         ;; false

(def filter (fn [_ datom] (not= 10 (.a datom))))
(d/filter (d/db dconn) filter)                                       ;; #datahike/DB{:schema {:db/ident #:db{:unique :db.unique/identity}, :client/name #:db{:ident :client/name, :valueType :db.type/string, :unique :db.unique/identity, :index true, :cardinality :db.cardinality/one, :doc "a client's name"}, 1 :client/name, :client/email #:db{:ident :client/email, :valueType :db.type/string, :unique :db.unique/identity, :index true, :cardinality :db.cardinality/many, :doc "a client's email"}, 2 :client/email}, :datoms [<first 10 datoms>}
(c/filter (c/db cconn) filter)                                       ;; datomic.db.Db@e7b5b5eb

(d/is-filtered (d/filter (d/db dconn) filter))                       ;; true
(c/is-filtered (c/filter (c/db cconn) filter))                       ;; true

(d/as-of ddb (Date.))                                                ;; #datahike.db.AsOfDB{}
(d/as-of (d/db dconn) (Date.))                                       ;; Error printing return value (ClassCastException) at clojure.core/key (core.clj:1567).
(c/as-of (c/db cconn) (Date.))                                       ;; datomic.db.Db@20310ffa

(d/since (d/db dconn) (Date.))                                       ;; #datahike.db.SinceDB{}
(c/since (c/db cconn) (Date.))                                       ;; datomic.db.Db@632b6c24

(str (type (d/since (d/db dconn) (Date.))))                          ;; datahike.db.SinceDB
(str (type (c/since (c/db cconn) (Date.))))                          ;; datomic.db.Db

(d/as-of (d/db dconn) 536870913)                                     ;; #datahike.db.AsOfDB{}
(c/as-of (c/db cconn) 17592186045419)                                ;; datomic.db.Db@20310ff8

(d/since ddb 536870913)                                              ;; #datahike.db.SinceDB{}
(d/since (d/db dconn) 536870913)                                     ;; Error printing return value (ClassCastException) at clojure.core/key (core.clj:1567).
(c/since (c/db cconn) 17592186045419)                                ;; datomic.db.Db@632b6c23


;; Datoms
(d/datoms (d/db dconn) :eavt)                               ;; (<datoms>)
(c/datoms (c/db cconn) :eavt)                               ;; #object[datomic.db$datoms$reify__1584 0x59776c53 "datomic.db$datoms$reify__1584@59776c53"]
(vec (c/datoms (c/db cconn) :eavt))                         ;; [<datoms>]

(str (type (first (vec (d/datoms (d/db dconn) :eavt)))))    ;; datahike.datom.Datom
(str (type (first (vec (c/datoms (c/db cconn) :eavt)))))    ;; datomic.db.Datum

(str (type (d/datoms (d/db dconn) :eavt)))                  ;; me.tonsky.persistent_sorted_set.Seq
(str (type (c/datoms (c/db cconn) :eavt)))                  ;; datomic.db$datoms$reify__1584
(str (type (vec (c/datoms (c/db cconn) :eavt))))            ;; clojure.lang.PersistentVector

(count (d/datoms (d/db dconn) :eavt))                       ;; 18
(count (vec (c/datoms (c/db cconn) :eavt)))                 ;; 247

(d/seek-datoms (d/db dconn) :eavt)                          ;; (<ddatoms>)
(c/seek-datoms (c/db cconn) :eavt)                          ;; #object[datomic.db$seek_datoms$reify__1555 0x23c90e19 "datomic.db$seek_datoms$reify__1555@23c90e19"]


;; Entity representation
(d/entity (d/db dconn) :client/email)                       ;; #:db{:id 2}
(c/entity (c/db cconn) :client/email)                       ;; #:db{:id 64}

(str (type (d/entity (d/db dconn) :client/email)))          ;; datahike.impl.entity.Entity
(str (type (c/entity (c/db cconn) :client/email)))          ;; datomic.query.EntityMap


;; IDs
(d/tempid :db.part/tx)                                      ;; :db/current-tx
(c/tempid :db.part/tx)                                      ;; #db/id[:db.part/tx -1000005]

(str (type (d/tempid :db.part/tx)))                         ;; clojure.lang.Keyword
(str (type (c/tempid :db.part/tx)))                         ;; datomic.db.DbId