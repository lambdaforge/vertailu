# vertailu

This project compares our Datahike API with the Datomic Clojure API.

## Comparing Function Signatures

| Datahike | Datomic | Status |
|----------|---------|--------|
| (connect config) | (connect client arg-map) | :warning: |
| (database-exists? config) | - | :x: |
| (create-database config) | (create-database client arg-map) | :warning: |
| (delete-database config) | (delete-database client arg-map) | :warning: |
| (transact conn tx-data) | (transact conn arg-map) | :warning: (rename arg?)|
| (transact! conn tx-data tx-meta) | - | :x: |
| (load-entities conn tx-data) | - | :x: |
| (release conn) | - | :x: |
| (pull db selector eid) | (pull db selector eid) | :heavy_check_mark: |
| (pull db selector eid) | (pull db arg-map) | :warning: |
| (pull-many db selector eids) | - | :x: |
| (q query & inputs) | (q query & args) | :warning: (rename arg?) |
| (q query-map) | (q arg-map) | :warning: (rename arg?) |
| (datoms db index & components | (datoms db arg-map) | :warning: |
| (seek-datoms db index & components) | - | :x: |
| (tempid part x) | - | :x: |
| (entity db eid) | - | :x: |
| (entity-db entity) | - | :x: |
| (is-filtered x) | - | :x: |
| (filter db pred) | - | :x: |
| (with db tx-data tx-meta) | (with db arg-map) | :warning: (rename arg?) |
| (db-with db tx-data) | (with-db conn) | :warning: (rename function and args?) |
| (db conn) | (db conn) | :heavy_check_mark: (ILookup Interface support?) |
| (history db) | (history db) | :heavy_check_mark: |
| (as-of db timepoint) | (as-of db time-point) | :heavy_check_mark: |
| (since db timepoint) | (since db t) | :heavy_check_mark: |
| - | (administer-system client arg-map) | :warning: |
| - | (client arg-map) | :warning: |
| - | (db-stats db) | :warning: |
| - | (index-pull db arg-map) | :warning: |
| - | (index-range db arg-map) | :warning: |
| - | (list-databases client arg-map) | :warning: |
| - | (qseq arg-map) (qseq query & args) | :warning: |
| - | (sync conn t) | :warning: |
| - | (tx-range conn arg-map) | :warning: |

- :heavy_check_mark: means the signature is already comparable
- :warning: means the signature should probably be matched
- :x: means the function is only available in Datahike and needs no adjustment

## License

Copyright © 2019 lambdaforge UG

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
