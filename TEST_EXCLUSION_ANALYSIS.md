# Test Exclusion and StructureSearchITTest Analysis

## Summary of POM Exclude Blocks

### Default Test Configuration (gsrs-module-substance-example/pom.xml)

Lines 67-72 define the **default** Surefire plugin excludes:

```xml
<excludes>
    <exclude>**/*IT.java</exclude>           <!-- Exclude pure integration tests -->
    <exclude>**/*ITTest.java</exclude>       <!-- Exclude *ITTest classes like StructureSearchITTest -->
    <exclude>**/*FullStackTest.java</exclude> <!-- Exclude full-stack tests -->
    <exclude>**/*BulkLoadTest.java</exclude> <!-- Exclude bulk load tests -->
</excludes>
```

### Full-Test-Suite Profile Configuration (lines 146-175)

When running with `.\\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite test`, the configuration changes to:

```xml
<includes>
    <include>**/*Test.java</include>
    <include>**/*Tests.java</include>
    <include>**/*ITTest.java</include>        <!-- INCLUDE ITTest classes -->
    <include>**/*FullStackTest.java</include> <!-- INCLUDE FullStackTest classes -->
</includes>
<excludes>
    <exclude>**/*IT.java</exclude>           <!-- Still exclude pure IT tests -->
</excludes>
```

### Tagged Test Exclusions (Both Configurations)

Line 49 defines excluded JUnit 5 groups:
```xml
<surefire.excludedGroups>fullstack</surefire.excludedGroups>
```

Any test with `@Tag("fullstack")` is excluded by default, but included with the full-test-suite profile.

---

## Where StructureSearchITTest is Excluded

**File**: `gsrs-module-substance-example/src/test/java/example/structureSearch/StructureSearchITTest.java`

**Why excluded in default run**: 
- Filename ends with `ITTest.java` → matches pattern `**/*ITTest.java` → **EXCLUDED**

**Extends**: `AbstractSubstanceJpaFullStackEntityTest` (full-stack test base class)

**Nature**: Integration test requiring:
- Full Spring Boot application context
- Database access
- Structure indexer service
- Transaction management

---

## Other Excluded Tests (Summary)

### By Filename Pattern (DEFAULT EXCLUDED):
1. `**/*IT.java` - Pure integration tests (e.g., `SomeIT.java`)
2. `**/*ITTest.java` - Integration test variants (e.g., `StructureSearchITTest.java`)
3. `**/*FullStackTest.java` - Explicitly named full-stack tests
4. `**/*BulkLoadTest.java` - Bulk load/performance tests

### By JUnit 5 Tag (DEFAULT EXCLUDED):
Any test with `@Tag("fullstack")`, including:
- `UpdateChemicalWithPersistedMoietyAmountTest` 
- `AbstractExportSupportingGsrsEntityControllerTest`
- (and others marked during stabilization)

---

## StructureSearchITTest Status

### Test Execution Result
✅ **PASSES with full-test-suite profile** (6 of 6 tests pass)

### Current Status
✅ **No current failures**

The SMARTS expectation in `ensureSubstructureSearchHasBasicSmartsSupport` was corrected to assert only the structure that matches the specific bicyclic SMARTS pattern. The test now passes consistently in the full suite profile.

---

## Recommendations

### Option 1: Add @Tag("fullstack") to StructureSearchITTest ✅ RECOMMENDED

This properly categorizes the test and keeps quick runs green:

```java
@Tag("fullstack")
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class StructureSearchITTest extends AbstractSubstanceJpaFullStackEntityTest {
    // ...
}
```

**Benefits**:
- Keeps it excluded from default runs (maintains green quick suite)
- Can be run with `mvn test` (excludes fullstack) vs `mvn test -Pfull-test-suite` (includes fullstack)
- Properly categorizes it as a full-stack test

### Option 2: Rename to use `*FullStackTest` suffix

Change: `StructureSearchITTest.java` → `StructureSearchFullStackTest.java`

Less ideal because it changes the test naming convention.

---

## SMARTS Expectation Update

The previous mismatch in `ensureSubstructureSearchHasBasicSmartsSupport` was resolved in test code by correcting the expected UUID set for the selected SMARTS query. No production matcher changes were required for this fix.

---

## Running the Tests

### Run only quick tests (excludes integration, fullstack, bulk load):
```bash
.\mvnw.cmd -pl gsrs-module-substance-example test
```

### Run full test suite including integration tests:
```bash
.\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite test
```

### Run only StructureSearchITTest:
```bash
.\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite -Dtest=StructureSearchITTest test
```

### Run full suite with higher test JVM heap (if full suite hits OOM):
```bash
.\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite -Dtest.jvm.full.argLine="-Xms2g -Xmx6g -XX:+UseG1GC" test
```

### Debug-only variant (not production runtime default):
```bash
.\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite -Dtest.jvm.full.argLine="-Xms2g -Xmx6g -XX:+UseG1GC -Dakka.jvm-exit-on-fatal-error=false" test
```

---

## Summary Table

| Category | Exclusion Method | Reason | Solution |
|----------|-----------------|--------|----------|
| `StructureSearchITTest` | Filename pattern `*ITTest.java` | Integration test needs full context | Add `@Tag("fullstack")` |
| `*Bulk*.java` | Filename pattern `*BulkLoadTest.java` | Performance/load tests | Keep excluded or run separately |
| `*Full*.java` | Filename pattern `*FullStackTest.java` | Full stack tests | Keep excluded or run separately |
| `*IT.java` | Filename pattern `*IT.java` | Integration tests | Keep excluded or run separately |
| `@Tag("fullstack")` | JUnit 5 tag group | Custom categorization | Keep excluded or run separately |

