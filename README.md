# gsrs-spring-module-substances
GSRS Spring Boot Module for Substances

This is the GSRS 3 module for working with IDMP Substances as specified by the ISO 11238 Substance Model.

## To install With FDA Extension modules
The FDA extension and example modules in this Repository are not yet in Maven Central and so rather than
having to make people track down those repositories, we provide sample jars that can be installed into your local maven repository.

###To install the extra jars in Unix
```
./installExtraJars.sh 
```

###To install the extra jars in Windows

```
./installExtraJars.cmd 
```

## Additional documentation

- Test refactoring and stabilization status: [`TEST_REFACTORING_STATUS.md`](./TEST_REFACTORING_STATUS.md)

## Bulk import name-lookup indexing

Case-insensitive duplicate-name validation uses `UPPER(name)`. A plain index on
`name` does not generally accelerate that query, so validation can get slower as
the names table grows. Before a bulk import, the loader prepares an indexed,
database-generated `name_upper_prefix` column on `ix_ginas_name`. Searches use its
first 128 uppercase characters to narrow candidates, then compare the full name
with the original `UPPER(name)` predicate. Long names and prefix collisions still
receive a full comparison; duplicate checking is not disabled.

`ix.ginas.batch.indexNameLookups=true` is the default. The first import requires
`ALTER TABLE` and `CREATE INDEX` privileges and may pause while the database builds
the column/index for existing rows. Preparation happens before worker transactions
start. Later imports reuse the schema, including after restarting the application.
Generated columns remain current when names are inserted or edited.

For DBA-managed schemas, provision the column using the appropriate definition:

| Database | Column definition in `ALTER TABLE ix_ginas_name ADD ...` |
| --- | --- |
| PostgreSQL 12+ | `COLUMN name_upper_prefix VARCHAR(128) GENERATED ALWAYS AS (SUBSTRING(UPPER(name), 1, 128)) STORED` |
| Oracle | `(name_upper_prefix GENERATED ALWAYS AS (SUBSTR(UPPER(name), 1, 128)) VIRTUAL)` |
| MySQL 5.7+ / MariaDB 10.2+ | `COLUMN name_upper_prefix VARCHAR(128) GENERATED ALWAYS AS (SUBSTRING(UPPER(name), 1, 128)) STORED` |
| SQL Server | `name_upper_prefix AS (CAST(SUBSTRING(UPPER(name), 1, 128) AS NVARCHAR(128))) PERSISTED` |
| H2 | `COLUMN name_upper_prefix VARCHAR(128) GENERATED ALWAYS AS (SUBSTRING(UPPER(name), 1, 128))` |

Then create `ix_ginas_name_upper_prefix` on `ix_ginas_name(name_upper_prefix)`.
Use a maintenance window when preparing large production tables. Schema errors
stop submission explicitly instead of silently falling back to a slow query.

To retain the original lookup, set `ix.ginas.batch.indexNameLookups=false` and
restart. This does not remove the generated column or index. Loading-thread,
validation, audit, and indexing-deferral settings are unchanged.

## Bulk upload database limits

The legacy bulk-import endpoint, shared payload-upload endpoint, and legacy
`/upload` aliases check `ix.ginas.batch.maxUploadSize` (default
`100MB`) before reading the uploaded file into a byte array or saving the payload.
Rejected files receive HTTP **413** with a `message` explaining the limiting
setting. The Spring multipart file/request limits and any reverse-proxy limits
still apply independently and may reject the request before this endpoint runs.

For database-backed payloads, MariaDB/MySQL preflight reads both session and
global `max_allowed_packet` on a pooled connection. It uses the smaller value,
allows for JDBC binary escaping (up to twice the file size), and reserves 64 KiB
for SQL/metadata. This is intentionally conservative. Settings are read for each
upload, not cached; the application never changes database-global settings.
File-backed payloads skip the database packet check but retain the upload cap.

| Database | Administrator configuration |
| --- | --- |
| MariaDB / MySQL | For the default 100 MiB upload cap, use `max_allowed_packet=256M` under `[mysqld]`. A DBA can apply `SET GLOBAL max_allowed_packet = 268435456` immediately; persist the setting separately. Restart GSRS to refresh pooled session limits. Confirm both values using `SELECT @@GLOBAL.max_allowed_packet, @@SESSION.max_allowed_packet` on a new connection. Also ensure `ix_core_filedata.data` is `LONGBLOB`, not a smaller BLOB type. |
| PostgreSQL | There is no `max_allowed_packet` setting. Database-backed `bytea` values have an approximately 1 GiB limit, which preflight caps with headroom. Keep the default upload cap well below that and review memory/storage capacity. |
| Oracle | There is no equivalent packet setting. Ensure the file data column uses `BLOB` and review tablespace/LOB storage and driver capacity. |
| SQL Server | There is no equivalent packet setting. Use `varbinary(max)` for file data; its 2 GiB limit exceeds the default upload cap. Do not raise network packet size to address this MariaDB/MySQL error. |
| H2 | Use `BLOB` for file data and keep uploads within the configured cap and available memory/disk. |

This preflight prevents known size-limit failures, not every possible storage
failure. Database errors while checking limits are surfaced rather than treated
as permission to upload. After a DBA change, retry the upload; rejected requests
do not create payloads or processing jobs. Split large import files if server
limits cannot be raised. Raising only Spring's multipart limits does not increase
database capacity.

## Running tests (team standard)

Always run tests from the repository root and use the Maven wrapper so all contributors use the same Maven version.

### Full test suite for `gsrs-module-substance-example`

Note: this suite can take a while and produces very verbose chemistry/FHIR logs. Let it finish, then use the quick summary commands below to confirm totals.

Windows PowerShell:

```powershell
cd C:\Users\kassahungb\IdeaProjects\gsrs-spring-module-substances
.\mvnw.cmd -pl gsrs-module-substance-example -am -Pfull-test-suite clean test
```

Quick post-run summary (Windows PowerShell):

```powershell
$r = "C:\Users\kassahungb\IdeaProjects\gsrs-spring-module-substances\gsrs-module-substance-example\target\surefire-reports"
$xmls = Get-ChildItem $r -Filter "TEST-*.xml"
$tot = @{tests=0; failures=0; errors=0; skipped=0}
foreach($f in $xmls){ [xml]$x = Get-Content $f.FullName; $s = $x.testsuite; $tot.tests += [int]$s.tests; $tot.failures += [int]$s.failures; $tot.errors += [int]$s.errors; $tot.skipped += [int]$s.skipped }
"TOTAL tests=$($tot.tests) failures=$($tot.failures) errors=$($tot.errors) skipped=$($tot.skipped)"
```

Unix/macOS:

```bash
cd /path/to/gsrs-spring-module-substances
./mvnw -pl gsrs-module-substance-example -am -Pfull-test-suite clean test
```

### Temporary Hibernate 6 test quarantine (opt-in)

Use profile `quarantine-hibernate-id` only as a temporary unblocker when the known Hibernate assigned-id test regressions are failing.

- It is test-only (does not change runtime artifacts)
- It intentionally skips a known failing subset, so do not treat it as full release confidence

Windows PowerShell:

```powershell
cd C:\Users\kassahungb\IdeaProjects\gsrs-spring-module-substances
.\mvnw.cmd -pl gsrs-module-substance-example -Pquarantine-hibernate-id test
```

With full suite profile:

```powershell
cd C:\Users\kassahungb\IdeaProjects\gsrs-spring-module-substances
.\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite,quarantine-hibernate-id test
```

Unix/macOS:

```bash
cd /path/to/gsrs-spring-module-substances
./mvnw -pl gsrs-module-substance-example -Pquarantine-hibernate-id test
```

Quick decision table:

| Goal | Command profile(s) | Use when | Avoid when |
| --- | --- | --- | --- |
| Normal validation (preferred) | none (default) | You want full confidence and can address failing tests | Known Hibernate assigned-id regression tests are currently blocking the team |
| Temporary unblocker | `quarantine-hibernate-id` | You need progress while the known Hibernate assigned-id tests are being fixed | Final release validation / sign-off |
| Broad coverage + temporary unblocker | `full-test-suite,quarantine-hibernate-id` | You need wider suite signal while still bypassing the known failing subset | Final release validation / sign-off |

Quick post-run summary (Unix/macOS):

```bash
grep -h '<testsuite ' gsrs-module-substance-example/target/surefire-reports/TEST-*.xml \
| awk -F'"' '{for(i=1;i<=NF;i++){if($i=="tests")t+=$(i+1); if($i=="failures")f+=$(i+1); if($i=="errors")e+=$(i+1); if($i=="skipped")s+=$(i+1)}} END{printf("TOTAL tests=%d failures=%d errors=%d skipped=%d\n",t,f,e,s)}'
```

### Quick diagnostics if output shows `Tests run: 0`

Windows PowerShell:

```powershell
cd C:\Users\kassahungb\IdeaProjects\gsrs-spring-module-substances
.\mvnw.cmd -v
.\mvnw.cmd -q help:active-profiles
.\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite -X test 2>&1 | Select-String -Pattern "Using.*Provider|surefire|junit|testng|skipTests|maven.test.skip"
```
