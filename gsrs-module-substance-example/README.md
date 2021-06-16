# Example GSRS Substance microservice

## How to run with Load file

If you have a .gsrs file you can load it on start up like this:

```
../mvnw clean spring-boot:run -Dspring-boot.run.jvmArguments="-Dix.ginas.load.file=/path/to/data.gsrs"
```
