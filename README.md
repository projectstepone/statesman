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

Note: Ensure Java version is 11 and JAVA_HOME environment variable is set.

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

To start testing, follow below mentioned steps:
```
0. Run swagger on localhost:8080/swagger/
1. Insert workflow template from configs/workflow/workflow_template.json in "Create Workflow Template". Copy the workflow template id in the response. Note: This would be different from the id in request. Save this template id for future reference.
2. Insert actions from configs/actions/actions.json in "Create Action Template". All actions need to be added individually. Alternately you can run the insert_actions.py script
3. Insert transitions from configs/transitions/transitions.json in "Create State Transitions". Use the workflow template id obtained above
4. Initiate a new workflow using "Trigger new workflow for given WorkflowTemplateId". Use the workflow template id obtained above. Copy the workflow id in the response. This is the actual workflow instance
5. Trigger the workflow with expected data from "Trigger workflow" flow. The request body is available using "$.update" in transitions and using "dataObject.data" in actions. Workflow data is accessible using "$.data" in transitions and using "dataObject.data" in actions
```

