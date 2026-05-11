# Seems like in 3x, we have to install api Jar's parent pom.xml if we don't install the actual module first.
./mvnw install:install-file -Dfile=extraJars/gsrs-spring-module-drug-applications-3.2.5-SNAPSHOT-pom.xml -DpomFile=extraJars/gsrs-spring-module-drug-applications-3.2.5-SNAPSHOT-pom.xml
./mvnw install:install-file -Dfile=extraJars/gsrs-spring-module-clinical-trials-3.2.5-SNAPSHOT-pom.xml -DpomFile=extraJars/gsrs-spring-module-clinical-trials-3.2.5-SNAPSHOT-pom.xml
./mvnw install:install-file -Dfile=extraJars/gsrs-spring-module-drug-products-3.2.5-SNAPSHOT-pom.xml -DpomFile=extraJars/gsrs-spring-module-drug-products-3.2.5-SNAPSHOT-pom.xml

./mvnw install:install-file -Dfile=extraJars/applications-api-3.2.5-SNAPSHOT.jar 
./mvnw install:install-file -Dfile=extraJars/products-api-3.2.5-SNAPSHOT.jar
./mvnw install:install-file -Dfile=extraJars/clinical-trials-api-3.2.5-SNAPSHOT.jar
./mvnw install:install-file -Dfile=extraJars/Featureize-Nitrosamines-0.0.4-SNAPSHOT.jar
