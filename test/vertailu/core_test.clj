(ns vertailu.core-test
  (:require [clojure.test :refer :all]
            [datahike.api :as d]
            [datomic.api :as c]
            [clojure.string :as s]))


(def datahike-uri "datahike:mem://datahike-vs-datomic")
(def datomic-uri "datomic:mem://datahike-vs-datomic")

(def dconn (let [uri datahike-uri]
             (d/delete-database uri)
             (d/create-database uri)
             (d/connect uri)))
(def cconn (let [uri datomic-uri]
             (c/delete-database uri)
             (c/create-database uri)
             (c/connect uri)))

(def schema [{:db/ident       :client/name
              :db/valueType   :db.type/string
              :db/unique      :db.unique/identity
              :db/index       true
              :db/cardinality :db.cardinality/one
              :db/doc         "a client's name"}
             {:db/ident       :client/email
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/many
              :db/doc         "a client's email"}])


(def example-data [{:client/name "alice" :client/email "alice@exam.ple"}
                   {:client/name "john" :client/email "john@exam.ple"}])


(defn error-info [f]
  (try (f) (catch Exception e [(.getCanonicalName (.getClass e)) (.getMessage e)])))

(defn same-error? [func1 func2]
  (let [[class1 _] (error-info func1)
        [class2 _] (error-info func2)]
    (= class1 class2)))

(defn instance-of-non-clojure-class? [x]
  (not (s/starts-with? (last (s/split (type x) #" ")) "clojure")))

(defn last-class-name-part [x]
  (last (s/split (type x) #".")))

(defn instance-of-db-class? [x]
  (or (s/ends-with? (last-class-name-part x) "DB")
      (= (last-class-name-part x) "Db")))

(defn instance-of-entity-class? [x]
  (s/includes? (last-class-name-part x) "Entity"))

(defn instance-of-datom-class? [x]
  (or (= (last-class-name-part x) "Datum")
      (= (last-class-name-part x) "Datom")))

(deftest test-connection
  (testing "Deleting a non-existing database"
    (is (= (d/delete-database datahike-uri)                 ;; {}
           (c/delete-database datomic-uri))))               ;; false

  (testing "Creating a non-existing database"
    (is (= (d/create-database datahike-uri)                 ;; nil
           (c/create-database datomic-uri))))               ;; true

  (testing "Creating an existing database"
    (is (= (d/create-database datahike-uri)                 ;; nil
           (c/create-database datomic-uri))))               ;; false

  (testing "Connecting an existing database"
    (is (= (d/connect datahike-uri)                         ;; #object[clojure.lang.Atom 0x22226112 {:status :ready, :val #datahike/DB {:schema #:db{:ident #:db{:unique :db.unique/identity}}, :datoms []}}]
           (c/connect datomic-uri))))                       ;; #object[datomic.peer.LocalConnection 0x33c5977d datomic.peer.LocalConnection@33c5977d]

  (testing "Database objects"
    (is (= (d/db (d/connect datahike-uri))                  ;; #datahike/DB{:schema #:db{:ident #:db{:unique :db.unique/identity}}, :datoms []}
           (c/db (c/connect datomic-uri)))))                ;; datomic.db.Db@40f12152))

  (testing "Deletion an existing database"
    (is (= (d/delete-database datahike-uri)                 ;; {}
           (c/delete-database datomic-uri))))               ;; true

  (testing "Connection of a non-existing database"
    (is (same-error? #(d/connect datahike-uri)                        ;; Execution error: Backend does not exist
                     #(c/connect datomic-uri)))))                     ;; Execution error: :db.error/db-not-found Could not find datahike-vs-datomic in catalog



(deftest test-transaction-and-query
  (testing "Transacting data before schema"
    (is (same-error? #(d/transact dconn example-data)                ;; Execution error: No schema found in db
                     #(deref (c/transact cconn example-data)))))     ;; Execution error (Exceptions$IllegalArgumentExceptionInfo) :db.error/not-an-entity Unable to resolve entity: :client/name

  (testing "Transacting schema data"
    (is (= (keys (d/transact dconn schema))                          ;; (:db-before :db-after :tx-data :tempids :tx-meta)
           (keys @(c/transact cconn schema)))))                      ;; (:db-before :db-after :tx-data :tempids)

  (testing "Transacting data"
    (is (= (keys (d/transact dconn example-data))                    ;; (:db-before :db-after :tx-data :tempids :tx-meta)
           (keys @(c/transact cconn example-data)))))                ;; (:db-before :db-after :tx-data :tempids)

  (testing "Querying data"
    (let [query '[:find ?email
                  :where [?e :client/email ?email]
                  [?e :client/name "alice"]]]
      (is (= (d/q query (d/db dconn))                                ;; #{["alice@exam.ple"]}
             (c/q query (c/db cconn)))))                             ;; #{["alice@exam.ple"]}

    (let [dp (d/pull (d/db dconn) '[*] [:client/name "alice"])       ;;  {:db/id 3, :client/name "alice", :client/email ["alice@exam.ple"]}
          cp (c/pull (c/db cconn) '[*] [:client/name "alice"])]      ;;  {:db/id 17592186045418, :client/name "alice", :client/email ["alice@exam.ple"]}
      (is (= (keys dp) (keys cp)))
      (is (= (:client/email dp) (:client/email cp)))))

  (testing "Database datom listing"
    (let [ddatoms (d/datoms (d/db dconn) :eavt)                      ;; (<datoms>)
          cdatoms (c/datoms (c/db cconn) :eavt)]                     ;; #object[datomic.db$datoms$reify__1584 0x59776c53 "datomic.db$datoms$reify__1584@59776c53"]

      (is (= (instance-of-datom-class? (first (vec ddatoms)))        ;; datahike.datom.Datom
             (instance-of-datom-class? (first (vec cdatoms)))))))    ;; datomic.db.Datum


  (testing "Seeking datoms"
    (let [ddatoms (d/seek-datoms (d/db dconn) :eavt)                 ;; (<datoms>)
          cdatoms (c/seek-datoms (c/db cconn) :eavt)]                ;; #object[datomic.db$seek-datoms$reify__1555 0x3b5653dd "datomic.db$seek_datoms$reify__1555@3b5653dd"]

      (is (= (instance-of-datom-class? (first (vec ddatoms)))        ;; datahike.datom.Datom
             (instance-of-datom-class? (first (vec cdatoms)))))))    ;; datomic.db.Datum


  (testing "Entity identification"
    (let [dentity (d/entity (d/db dconn) :client/email)              ;; #:db{:id 2}
          centity (c/entity (c/db cconn) :client/email)]             ;; #:db{:id 64}

      (is (= (instance-of-entity-class? dentity)                     ;; datahike.impl.entity.Entity
             (instance-of-entity-class? centity)))))                 ;; datomic.query.EntityMap

  (testing "TempIDs"
    (let [did (d/tempid :db.part/tx)                                 ;; :db/current-tx
          cid (d/tempid :db.part/tx)]                                ;; #db/id[:db.part/tx -1000005]

      (is (= (instance-of-non-clojure-class? did)              ;; clojure.lang.Keyword
             (instance-of-non-clojure-class? cid)))))          ;; datomic.db.DbId

  (testing "History"
    (is (= (instance-of-db-class? (d/history (d/db dconn)))    ;; #datahike.db.HistoricalDB
           (instance-of-db-class? (c/history (c/db cconn)))))) ;; datomic.db.Db@a8299901

  (testing "Database of a given entity"
    (is (= (instance-of-db-class? (d/entity-db (d/entity (d/db dconn) :client/email)))     ;; datahike.db.HistoricalDB
           (instance-of-db-class? (c/entity-db (c/entity (c/db cconn) :client/email))))))  ;; datomic.db.Db

  (testing " Hypothetical database"
    (let [new-data [{:client/name "bill" :client/email "bill@exam.ple"}]]
      (is (= (keys (d/with (d/db dconn) new-data))       ;; (:db-before :db-after :tx-data :tempids :tx-meta)
             (keys (c/with (c/db cconn) new-data)))))))  ;; (:db-before :db-after :tx-data :tempids)


(deftest test-filter-functions
  (let [filter (fn [_ datom] (not= 10 (.a datom)))
        dfiltered (d/filter (d/db dconn) filter)
        cfiltered (c/filter (c/db cconn) filter)]

    (testing "Filtered check on unfiltered DB"
      (is (= (d/is-filtered (d/db dconn))                   ;; false
             (c/is-filtered (c/db cconn)))))                ;; false

    (testing "Using custom filter"
      (is (= (instance-of-db-class? dfiltered)              ;; #datahike/DB{:schema {:db/ident #:db{:unique :db.unique/identity}, :client/name #:db{:ident :client/name, :valueType :db.type/string, :unique :db.unique/identity, :index true, :cardinality :db.cardinality/one, :doc "a client's name"}, 1 :client/name, :client/email #:db{:ident :client/email, :valueType :db.type/string, :unique :db.unique/identity, :index true, :cardinality :db.cardinality/many, :doc "a client's email"}, 2 :client/email}, :datoms [<first 10 datoms>}
             (instance-of-db-class? cfiltered))))           ;; datomic.db.Db@e7b5b5eb

    (testing "Filtered check on filtered DB"
      (is (= (d/is-filtered dfiltered)                      ;; true
             (c/is-filtered cfiltered))))                   ;; true

    (testing "As-of filtering with date class"
      (is (= (instance-of-db-class? (d/as-of (d/db dconn) (Date.)))    ;; Error printing return value (ClassCastException) at clojure.core/key (core.clj:1567).
             (instance-of-db-class? (c/as-of (c/db cconn) (Date.)))))) ;; datomic.db.Db@20310ffa

    (testing "As-of filtering with date integer"
      (is (= (instance-of-db-class? (d/as-of (d/db dconn) 536870913))         ;; #datahike.db.AsOfDB{}
             (instance-of-db-class? (c/as-of (c/db cconn) 17592186045419))))) ;; datomic.db.Db@20310ff8

    (testing "Since filtering with date class"
      (is (= (instance-of-db-class? (d/since (d/db dconn) (Date.)))    ;; #datahike.db.SinceDB{}
             (instance-of-db-class? (c/since (c/db cconn) (Date.)))))) ;; datomic.db.Db@632b6c24

    (testing "Since filtering with date integer"
      (is (= (instance-of-db-class? (d/since (d/db dconn) 536870913))           ;; Error printing return value (ClassCastException) at clojure.core/key (core.clj:1567).
             (instance-of-db-class? (c/since (c/db cconn) 17592186045419))))))) ;; datomic.db.Db@632b6c23
