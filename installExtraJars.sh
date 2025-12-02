
./mvnw install:install-file -Dfile=extraJars/gsrs-spring-module-drug-applications-3.1.3-3x-SNAPSHOT-pom.xml -DpomFile=extraJars/gsrs-spring-module-drug-applications-3.1.3-3x-SNAPSHOT-pom.xml
./mvnw install:install-file -Dfile=extraJars/gsrs-spring-module-clinical-trials-3.1.3-3x-SNAPSHOT-pom.xml -DpomFile=extraJars/gsrs-spring-module-clinical-trials-3.1.3-3x-SNAPSHOT-pom.xml
./mvnw install:install-file -Dfile=extraJars/gsrs-spring-module-drug-products-3.1.3-3x-SNAPSHOT-pom.xml -DpomFile=extraJars/gsrs-spring-module-drug-products-3.1.3-3x-SNAPSHOT-pom.xml 

./mvnw install:install-file -Dfile=extraJars/applications-api-3.1.3-3x-SNAPSHOT.jar -DgroupId=gov.nih.ncats -DartifactId=applications-api -Dversion=3.1.3-3x-SNAPSHOT -Dpackaging=jar
./mvnw install:install-file -Dfile=extraJars/clinical-trials-api-3.1.3-3x-SNAPSHOT.jar -DgroupId=gov.nih.ncats -DartifactId=clinical-trials-api -Dversion=3.1.3-3x-SNAPSHOT -Dpackaging=jar
./mvnw install:install-file -Dfile=extraJars/products-api-3.1.3-3x-SNAPSHOT.jar -DgroupId=gov.nih.ncats -DartifactId=products-api -Dversion=3.1.3-3x-SNAPSHOT -Dpackaging=jar

./mvnw install:install-file -Dfile=extraJars/Featureize-Nitrosamines-0.0.4-SNAPSHOT.jar
