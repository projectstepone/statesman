# Statesman
Statesman is a Generic Workflow Service. 

# Local setup
```
git clone <statesman>
cd statesman
CODE=$(pwd)
mvn clean package 
cd statesman-server/
```

Change the line on Dockerfile (in the path $CODE/statesman-server/Dockerfile): 

ADD config/docker.yml docker.yml

To the following for local:

ADD config/local.yml docker.yml
```
docker-compose build
docker-compose up 
```

Incase you have ports conflicting (8080, 8081, 8090, 34407, 35508 will be used by this app), ports are mentioned in the following files:
```
$CODE/statesman-server/Dockerfile 
$CODE/statesman-server/config/local.yml
$CODE/statesman-server/docker-compose.yml
```

To debug the statesman service running via Docker in IntelliJ, follow below mentioned steps:
```shell
# Open Run Configurations in IntelliJ
1. Add Remote JVM Debug config
2. Add host: localhost 
3. Add port: 8090
4. Select statesman-server module for classpath selection 
5. Save the configuration
6. Bring up Docker Container via CLI
7. Click on Debug with newly created config
```