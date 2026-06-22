# Test Failure Report - Full Test Suite Run

**Date:** May 22, 2026  
**Test Profile:** -Pfull-test-suite  
**Status:** FAILED  

## Summary
The full test suite execution identified **3 distinct failing test cases** resulting from **2 different root causes**.

---

## Failing Tests

### 1. BulkSearchTests (2 failures)

#### Test: `example.substance.search.bulk.BulkSearchTests.testBulkSearchWithNonIdentifiers`
- **Status:** FAILURE (Assertion Error)
- **Expected:** 0 errors
- **Actual:** 9 errors  
- **Error Type:** `java.lang.AssertionError: expected:<0> but was:<9>`
- **Location:** `BulkSearchTests.java:131`
- **Root Cause:** Root cause appears to be upstream error in test setup (SiteContainer identifier generation)

#### Test: `example.substance.search.bulk.BulkSearchTests.testBulkSearchWithIdentifiers`
- **Status:** FAILURE (Assertion Error)
- **Expected:** 0 errors
- **Actual:** 3 errors
- **Error Type:** `java.lang.AssertionError: expected:<0> but was:<3>`
- **Location:** `BulkSearchTests.java:168`
- **Root Cause:** Root cause appears to be upstream error in test setup (SiteContainer identifier generation)

**Setup Error Details (affecting both tests):**
- Error in `@BeforeEach` method `clearIndexers()` at `BulkSearchTests.java:114`
- **Exception:** `org.hibernate.id.IdentifierGenerationException`
- **Message:** `Identifier of entity 'ix.ginas.models.v1.SiteContainer' must be manually assigned before calling 'persist()'`
- **Stack Trace:** 
  - Chain through cascading saves
  - `TestSubstanceEntityServiceImpl.create()` → `AbstractGsrsEntityService.transactionalPersist()`
  - Entity persistence fail during cascade operations

---

### 2. SequenceSearchFullStackTest (1 failure)

#### Test: `example.substance.SequenceSearchFullStackTest.addProteinSequenceAndThenEditSearchShouldNotHonorOldSearch`
- **Status:** FAILURE (Multiple LazyInitializationExceptions)
- **Error Type:** `org.hibernate.LazyInitializationException` (Hibernate proxy initialization failures)
- **Location:** `SequenceSearchFullStackTest.java:208`
- **Setup Method:** `assertUpdatedAPI()` at line 156

**Root Causes (Multiple Chained Exceptions):**

1. **Primary Exception Chain:**
   ```
   com.fasterxml.jackson.databind.JsonMappingException: 
   failed to lazily initialize a collection of role: 
   ix.ginas.models.v1.Substance.names: could not initialize proxy - no Session
   ```
   - Through reference chain: `ix.ginas.models.v1.ProteinSubstance["names"]`

2. **Secondary Issues (encountered during indexing):**
   - `ix.ginas.models.v1.Substance.notes` - LazyInitializationException
   - `ix.ginas.models.v1.Substance.references` - LazyInitializationException  
   - `ix.ginas.models.v1.Name.nameOrgs` - LazyInitializationException

3. **Affected Stack:**
   - Text indexing fails during entity update
   - Called from `TextIndexerEntityListener.updateEntity()` (async operation)
   - Attempt to access lazy-loaded collections outside of session context
   - Methods affected:
     - `Substance.getBestName()` → accesses `names` collection
     - `Substance.getDisplayName()` → accesses `names`
     - `Substance.getNameCount()` → accesses `names`
     - `Substance.getReferenceCount()` → accesses `references`
     - `Substance.getValidation()` → accesses `notes`

---

## Root Cause Analysis

### Issue 1: SiteContainer ID Generation (affects BulkSearchTests)
**Problem:** During entity cascade persistence, the `SiteContainer` entity doesn't have a manually assigned ID before `persist()` is called.

**Location:** 
- `TestSubstanceEntityServiceImpl.create()` line 15
- During cascading persist in `AbstractGsrsEntityService.transactionalPersist()` line 360

**Implications:** 
- Both bulk search tests fail in their `@BeforeEach` setup
- The test data cannot be properly inserted

### Issue 2: LazyInitializationException in Async Operations (affects SequenceSearchFullStackTest)
**Problem:** The `TextIndexerEntityListener` executes asynchronously and attempts to access lazy-loaded Substance collections outside of an active Hibernate session.

**Location:**
- `TextIndexerEntityListener.updateEntity()` (async method)
- Jackson JSON serialization triggers lazy collection access
- Called from: `SequenceSearchFullStackTest.Configuration.flush()` line 111

**Implications:**
- The test cannot complete entity update and indexing
- Collections like `names`, `notes`, `references` cannot be accessed

---

## Approach Recommendations

### For BulkSearchTests (Priority: HIGH)
1. **Investigation Needed:**
   - Check `SiteContainer` entity ID generation strategy
   - Verify if ID needs to be manually assigned before persist
   - Check if cascade settings need adjustment

2. **Potential Fix Areas:**
   - `TestSubstanceEntityServiceImpl.create()` - may need to pre-generate IDs
   - Entity `@GeneratedValue` configuration
   - Cascade strategy in relationship definitions

### For SequenceSearchFullStackTest (Priority: HIGH)
1. **Investigation Needed:**
   - Determine why `TextIndexerEntityListener` is executing outside session context
   - Check if async execution is causing session issues
   - Verify Hibernate configuration for lazy loading strategy

2. **Potential Fix Areas:**
   - Session management in async listeners
   - Eager loading vs lazy loading configuration
   - OpenSessionInView pattern or Transaction boundaries
   - Force collection initialization before async operations

---

## Test Execution Details
- **Test Framework:** JUnit 5 (Jupiter)
- **Surefire Version:** Maven Surefire Plugin
- **Spring Boot Version:** 3.5.7
- **Hibernate Version:** Latest (from Spring Boot dependency)
- **Tests Run:** 2 in BulkSearchTests, 1 in SequenceSearchFullStackTest
- **Failures:** 3 (2 assertion failures + 1 initialization exception)
- **Time Elapsed (BulkSearchTests):** ~7.189 seconds
- **Time Elapsed (SequenceSearchFullStackTest):** Still running/hanging with lazy initialization issues

---

## Next Steps

1. **Immediate:** Examine the SiteContainer entity mappings and ID generation
2. **Secondary:** Review Hibernate configuration and session management  
3. **Tertiary:** Consider lazy loading strategy and async listener design
4. **Validation:** Once fixes are applied, rerun the full test suite to confirm resolution

