# Hibernate 6 Migration Fixes - Status Report

**Date:** May 27, 2026  
**Focus:** UUID/ID Assignment for Assigned-ID Entities

## Changes Implemented

### 1. TestPersistUuidSupport.ensureAssignedIdentifier()
**File:** `gsrs-module-substances-tests/src/main/java/gsrs/substances/tests/TestPersistUuidSupport.java`

**Change:** Removed the check that was skipping fields with `@GeneratedValue` annotation.

**Rationale:** In Hibernate 6, custom @GeneratedValue generators like `NullUUIDGenerator` still require IDs to be pre-assigned before persist() is called. The previous logic was:
- Skip any field with @GeneratedValue (assuming generator would handle it)
- Only assign UUIDs to fields without @GeneratedValue

The new logic:
- Check if the field already has a non-null value (skip if it does)
- Assign a UUID for null id/uuid fields, regardless of @GeneratedValue annotation

### 2. GinasChemicalStructure  copy() Method
**File:** `gsrs-module-substances-core/src/main/java/ix/ginas/models/v1/GinasChemicalStructure.java`

**Change:** Added code to reset the `version` field to null when creating a copy.

**Rationale:** When copying an entity and setting id=null, Hibernate needs to also reset the version field to null so it treats the entity as new, not detached.

## Test Results

Running `MixtureValidationTest.setup()` still produces:
```
org.hibernate.id.IdentifierGenerationException: Identifier of entity 'ix.ginas.models.v1.Substance' must be manually assigned before calling 'persist()'
```

This indicates that the Substance entity's UUID field is still null after `TestPersistUuidSupport.ensurePersistableIds()` is called.

## Root Cause Analysis

The issue appears to be in how the TestPersistUuidSupport helper is trying to assign UUIDs:

1. **Problem 1:** The helper looks for fields named "uuid" or "id" using reflection
2. **Problem 2:** These fields are defined in `GinasCommonData`, an external class
3. **Problem 3:** The field might not be public or accessible via simple field reflection
4. **Problem 4:** The call to `getOrGenerateUUID()` at line 81 might not actually be modifying the object's field

The helper is structured as:
```java
if (current instanceof GinasCommonData data) {
    data.getOrGenerateUUID();  // <- Returns UUID but doesn't assign it to field
}
ensureAssignedIdentifier(current);  // <- Tries to find and assign via reflection
```

##  Needed Investigation

To properly fix this, we need to determine:

1. **What is the actual field name** in `GinasCommonData` that holds the entity ID?
   - Is it `uuid`, `id`, or something else?
   - Is it accessible via public reflection?

2. **How does `getOrGenerateUUID()` work?**
   - Does it modify the object's field in place?
   - Does it just return a UUID without side effects?
   - If it's the latter, does the returned UUID need to be used to set the field?

3. **Can we access GinasCommonData source code?**
   - It appears to be in an external JAR (ix.ginas.models package)
   - We may need to inspect the actual class definition

## Recommendations

### Short-term Fix (if source code unavailable):
1. Enhance the ensureAssignedIdentifier() method to handle the case where `getOrGenerateUUID()` returns a value but doesn't set it
2. Store the result from `getOrGenerateUUID()` and explicitly set it back to the object if the field is still null

### Long-term Fix (if source code available):
1. Review GinasCommonData ID generation strategy
2. Ensure the custom @GeneratedValue generator works correctly with Hibernate 6
3. Consider if getOrGenerateUUID() should accept a parameter to force UUID assignment

## Files Modified

- `gsrs-module-substances-tests/src/main/java/gsrs/substances/tests/TestPersistUuidSupport.java`
- `gsrs-module-substances-core/src/main/java/ix/ginas/models/v1/GinasChemicalStructure.java`

## Next Steps

1. Verify whether GinasCommonData source is accessible
2. Trace getOrGenerateUUID() implementation
3. Check if the UUID returned needs to be explicitly set to a field
4. Consider alternative approaches such as:
   - Using Hibernate validators/callbacks to assign UUIDs
   - Modifying the generator itself rather than pre-assignment
   - Using JPA @PrePersist hooks instead of field-level reflection

