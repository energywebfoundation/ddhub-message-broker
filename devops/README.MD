# Quick guide on DDHub-messagebroker deployment introduction

  

This document will briefly explain the deployment implementation of [DDHub-messagebroker-helm](https://github.com/energywebfoundation/ddhub-messagebroker-helm)

### Prerequisites 
DDHub Messagebroker depends on blow services to be available. they are configured via environment variables (will be covered later).
 - Nats Jetstream
- Azure Blob storage
- MongoDB / Cosmos DB
  
### Environment Variables
  The DDHub Messagebroker environment variables are located [here](https://github.com/energywebfoundation/ddhub-message-broker/blob/dev/src/main/resources/application.properties) 
  
 - Non-sensitive ones are reflected in the helm chart config portion via `configMap`,  [default values](https://github.com/energywebfoundation/ddhub-messagebroker-helm#values) including the `natsClusterUrl` for Nats jetstream
 - DB connection string, access keys are passed in through secret. (next section will explain a bit more in details)

### Secret contents
- Azure Blob Storage access credentials. 
	 1. `BLOB_STORAGE_ACCOUNT_NAME`  Storage account name 
	 2. `BLOB_CONTAINER_NAME` Container name under the above storage account
	 3. `BLOB_STORAGE_ACCESS_KEY` Access key for accessing the storage account
	 
- MongoDB/CosmosDB connection credentials. 
	Due to a requirement, document DB connection was break down to pieces, which would enable the user password can be rotated and managed separately.  (for instance, connection string may looks like `mongodb://mongodb_user_name:mongodb_user_pwd@aemo-cosmos-db-demo.mongo.cosmos.azure.com:10255/?ssl=true&replicaSet=globaldb&retrywrites=false&maxIdleTimeMS=120000&appName=@aemo-cosmos-db-demo@`) the pieces will be 
	1. `MONGODB_USER_NAME` the username 'mongodb_user_name'
	2. `MONGODB_USER_PWD` the user's password 'mongodb_user_pwd'
	3. `MONGODB_QUERY` the document DB server info, everything after the '@' symbol behind user password, it would be 'aemo-cosmos-db-demo.mongo.cosmos.azure.com:10255/?s****-cosmos-db-demo@'
	4. `MONGODB_DB_NAME`the db name you gave, which will be created/used
	5. you can see how the connection string is formed [here](https://github.com/energywebfoundation/ddhub-message-broker/blob/dev/src/main/resources/application.properties#L29)

### Authentication
DDHub-messagebroker access are role-base controlled, only authenticated users are allowed. itself does not have authentication token issuing ability. usually a seperate DID-Auth component is deployed to safeguard the DDHub-messagebroker layer.  
Please refer to [DID-auth-proxy](https://github.com/energywebfoundation/did-auth-proxy) for more details and implementation. 

### Example implementation 
The [DDHub-messagebroker shared-dev instance](https://github.com/energywebfoundation/ddhub-message-broker/tree/dev/devops/helms/ddhub-messagebroker-dev-shared)
- DID-auth component is enabled [here](https://github.com/energywebfoundation/ddhub-message-broker/blob/dev/devops/helms/ddhub-messagebroker-dev-shared/values-auth.yaml#L6) in the 'values-auth.yaml' file
- The DDHub-messagebroker sealed secret contains the required [sensitive environment variables](https://github.com/energywebfoundation/ddhub-message-broker/blob/dev/devops/helms/ddhub-messagebroker-dev-shared/values.yaml#L33-L40)  
