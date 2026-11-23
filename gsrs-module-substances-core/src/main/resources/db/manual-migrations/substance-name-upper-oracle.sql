-- DBA-run migration for Oracle. Run in the schema containing ix_ginas_name.
-- Review existing indexes and obtain approval before running this script.
CREATE INDEX ix_ginas_name_upper ON ix_ginas_name (UPPER(name));
