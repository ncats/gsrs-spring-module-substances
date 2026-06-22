REM In 3.x we must install parent POMs for these API jars first.
mvnw.cmd install:install-file -Dfile=extraJars/gsrs-spring-module-drug-applications-3.2.5-SNAPSHOT-pom.xml -DpomFile=extraJars/gsrs-spring-module-drug-applications-3.2.5-SNAPSHOT-pom.xml
mvnw.cmd install:install-file -Dfile=extraJars/gsrs-spring-module-clinical-trials-3.2.5-SNAPSHOT-pom.xml -DpomFile=extraJars/gsrs-spring-module-clinical-trials-3.2.5-SNAPSHOT-pom.xml
mvnw.cmd install:install-file -Dfile=extraJars/gsrs-spring-module-drug-products-3.2.5-SNAPSHOT-pom.xml -DpomFile=extraJars/gsrs-spring-module-drug-products-3.2.5-SNAPSHOT-pom.xml

mvnw.cmd install:install-file -Dfile=extraJars/applications-api-3.2.5-SNAPSHOT.jar
mvnw.cmd install:install-file -Dfile=extraJars/products-api-3.2.5-SNAPSHOT.jar
mvnw.cmd install:install-file -Dfile=extraJars/clinical-trials-api-3.2.5-SNAPSHOT.jar
mvnw.cmd install:install-file -Dfile=extraJars/Featureize-Nitrosamines-0.0.4-SNAPSHOT.jar
