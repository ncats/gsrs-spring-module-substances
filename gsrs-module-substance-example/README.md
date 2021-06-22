# Example GSRS Substance microservice

## How to run with Load file

If you have a .gsrs file you can load it on start up like this:

```
../mvnw clean spring-boot:run -Dspring-boot.run.jvmArguments="-Dix.ginas.load.file=/path/to/data.gsrs"
```
## PostGreSQL requires custom 'dialect' setting
Background: one of the goals of the GSRS project is to support a variety of relational database systems.  Sometimes, an RDBMS requires a specific setting to run correctly.
<br/>
One such example is PostGreSQL with GSRS 3.0.
<br/>

In order for GSRS 3.0 to run correctly, a small configuration setting is required.  Within application.conf, add this line
```
	spring.jpa.database-platform = ix.ginas.models.GSRSPostgreSQLDialectCustom
```
<br/>
This directs the Spring Boot framework to use the GSRSPostgreSQLDialectCustom class our team  created to map large text and binary fields correctly to the database.
<br/>
The position of the line within the file is pretty much up to you. (As long as it follows any 'include' directives and does not break up any multi-line structures.)
