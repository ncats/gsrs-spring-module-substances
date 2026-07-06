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

