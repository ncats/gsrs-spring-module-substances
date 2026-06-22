# Quick Reference: Test Exclusion Patterns

## Overview

Tests in `gsrs-module-substance-example` are categorized into two suites:

### 1. Quick Test Suite (Default)
```bash
.\mvnw.cmd -pl gsrs-module-substance-example test
```
- **Duration**: ~4-5 minutes
- **Scope**: Unit tests only
- **Purpose**: Fast feedback during development

### 2. Full Test Suite
```bash
.\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite test
```
- **Duration**: Longer (~10+ minutes)
- **Scope**: Includes integration and full-stack tests
- **Purpose**: Comprehensive testing before commit/deployment

---

## Test Exclusion Rules

### What Gets Excluded from Default Run?

| Pattern | Type | Example | When to Use Full Suite |
|---------|------|---------|------------------------|
| `**/*IT.java` | Integration tests | `UserIT.java` | Always (excluded from full suite too) |
| `**/*ITTest.java` | Integration test variant | `StructureSearchITTest.java` | Need full app context |
| `**/*FullStackTest.java` | Full-stack tests | `ExportSupportingITTest.java` | Need database + services |
| `**/*BulkLoadTest.java` | Bulk load tests | `LargeBulkLoadTest.java` | Performance testing |
| `@Tag("fullstack")` | JUnit 5 tagged tests | `@Tag("fullstack")` | Any full-stack test |

### What Gets Included in Default Run?

- `**/*Test.java` (standard JUnit tests)
- `**/*Tests.java` (standard JUnit tests)
- Anything NOT matching above patterns
- Tests WITHOUT `@Tag("fullstack")`

---

## Categorizing Your Test

### Use Quick Test Suite If:
- ✓ Test runs in < 1 second
- ✓ Doesn't need full Spring Boot context
- ✓ No database required
- ✓ No external services needed
- ✓ Standard unit test

**Example**:
```java
@Test
public void testCalculationLogic() {
    assertEquals(2, 1 + 1);
}
```

### Use Full Test Suite If:
- ✗ Needs `@SpringBootTest`
- ✗ Uses `@Autowired` for services
- ✗ Requires database/transactions
- ✗ Takes > 1 second
- ✗ Tests integration between multiple services

**Example - Option A (Use @Tag)**:
```java
@Tag("fullstack")
@SpringBootTest
@DirtiesContext
public class MyServiceIT {
    // ...
}
```

**Example - Option B (Use Naming Convention)**:
```java
@SpringBootTest
@DirtiesContext
public class MyServiceFullStackTest {  // ← Naming convention
    // ...
}
```

or

```java
@SpringBootTest
@DirtiesContext  
public class MyServiceITTest {  // ← Naming convention
    // ...
}
```

---

## Common Scenarios

### Scenario 1: I created a new test - which suite?

1. **Is it testing a single class/method in isolation?**
   - YES → Use default suite (name it `*Test.java`)
   - NO → Go to step 2

2. **Does it need Spring Boot application context?**
   - NO → Use default suite (unit test)
   - YES → Use full suite (add `@Tag("fullstack")` and name it `*ITTest.java`)

3. **Does it access database or external services?**
   - NO → Use default suite
   - YES → Use full suite (full-stack integration test)

### Scenario 2: A test is failing only in full suite

1. Check if it's using shared database state
2. Add `@DirtiesContext` to clean context between tests
3. Add explicit transaction management with `TransactionTemplate`

### Scenario 3: A test is too slow for quick development

**Move from quick to full suite**:
```java
@Tag("fullstack")  // ← Add this
@SpringBootTest(classes = GsrsModuleSubstanceApplication.class)
public class MySlowTest {
    // ...
}
```

---

## Running Specific Tests

### Run specific test class (quick suite):
```bash
.\mvnw.cmd -pl gsrs-module-substance-example -Dtest=MyTest test
```

### Run specific test class (full suite):
```bash
.\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite -Dtest=MyTest test
```

### Run one test method:
```bash
.\mvnw.cmd -pl gsrs-module-substance-example -Dtest=MyTest#testMethod test
```

### Run all tests matching pattern:
```bash
.\mvnw.cmd -pl gsrs-module-substance-example -Dtest=MyTest* test
```

### Run full suite with higher test JVM heap (if full suite hits OOM):
```bash
.\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite -Dtest.jvm.full.argLine="-Xms2g -Xmx6g -XX:+UseG1GC" test
```

### Debug-only variant (not for production runtime defaults):
```bash
.\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite -Dtest.jvm.full.argLine="-Xms2g -Xmx6g -XX:+UseG1GC -Dakka.jvm-exit-on-fatal-error=false" test
```

---

## POM Configuration

**File**: `gsrs-module-substance-example/pom.xml`

**Default Excludes** (lines 67-72):
```xml
<excludes>
    <exclude>**/*IT.java</exclude>
    <exclude>**/*ITTest.java</exclude>
    <exclude>**/*FullStackTest.java</exclude>
    <exclude>**/*BulkLoadTest.java</exclude>
</excludes>
<excludedGroups>${surefire.excludedGroups}</excludedGroups>
```

**Default Excluded Groups** (line 49):
```xml
<surefire.excludedGroups>fullstack</surefire.excludedGroups>
```

**Full-Test-Suite Profile** (lines 146-175):
``` xml
<profile>
    <id>full-test-suite</id>
    <properties>
        <surefire.excludedGroups></surefire.excludedGroups>
    </properties>
    <!-- Includes: **/*ITTest.java, **/*FullStackTest.java -->
    <!-- Excludes: **/*IT.java -->
</profile>
```

---

## CI/CD Pipeline Recommendation

```yaml
# Phase 1: Quick feedback (< 5 min)
test-quick:
  script: .\\mvnw.cmd -pl gsrs-module-substance-example test
  
# Phase 2: Full validation (runs in parallel)
test-full:
  script: .\\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite -Dtest.jvm.full.argLine="-Xms2g -Xmx6g -XX:+UseG1GC" test
```

---

## Troubleshooting

### Q: My test runs in quick suite but shouldn't
**A**: Rename or add `@Tag("fullstack")`

### Q: My full-stack test isn't running
**A**: Check:
1. Does it have `@SpringBootTest`? ✓
2. Is it named `*ITTest.java` or `*FullStackTest.java`? ✓
3. Or does it have `@Tag("fullstack")`? ✓
4. Run with: `.\mvnw.cmd -pl gsrs-module-substance-example -Pfull-test-suite test`

### Q: StructureSearchITTest still running by default
**A**: Verify `@Tag("fullstack")` is present at class level:
```java
@Tag("fullstack")  // ← Must be here
@SpringBootTest
public class StructureSearchITTest {
```

---

## Last Updated
**Date**: May 21, 2026  
**Related Files**:
- `gsrs-module-substance-example/pom.xml`
- `STRUCTURE_SEARCH_TEST_REFACTORING.md`
- `TEST_EXCLUSION_ANALYSIS.md`

