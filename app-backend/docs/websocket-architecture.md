# WebSocket Architecture

Initial WebSocket architecture for realtime features.

The use case is not fixed yet, so the current setup creates only the reusable foundation:

- API Gateway WebSocket API.
- WebSocket stage and access logs.
- CloudWatch log group for access logs.
- DynamoDB table for active connections.
- Output ARN for `execute-api:ManageConnections`.

## Current Infrastructure

```text
Client
  -> API Gateway WebSocket API
      -> future $connect integration
      -> future $disconnect integration
      -> future $default/custom route integration
  -> CloudWatch access logs
  -> DynamoDB connections table
```

The connections table stores active connections:

```text
connectionId
userId
expiresAt
```

`connectionId` is the primary key. `userId` has a GSI so the backend can find all open connections for a user.

## Recommended Runtime Later

Use Lambda first unless there is a strong reason to run a container.

Recommended handlers:

```text
$connect      -> verify token, store connection
$disconnect   -> delete connection
$default      -> handle unknown messages or reject them
custom route  -> handle specific actions later
```

Authorization should happen on `$connect`. For Cognito JWTs, use one of these patterns:

- Lambda authorizer on `$connect`.
- `$connect` Lambda validates the token and rejects unauthorized connections.

The API should validate Cognito-issued tokens, not Google or Microsoft tokens directly.

## Permissions Needed Later

The backend that sends messages to clients needs:

```text
execute-api:ManageConnections
dynamodb:GetItem
dynamodb:PutItem
dynamodb:DeleteItem
dynamodb:Query
```

Use the Terraform output:

```text
websocket_manage_connections_arn
```

for the `execute-api:ManageConnections` resource.

## Why DynamoDB

DynamoDB is a good default for connection tracking:

- No server to run.
- Pay-per-request billing.
- TTL cleans stale connections.
- GSI allows user-targeted sends.

## What Not To Add Yet

Do not add these until the use case is clear:

- Rooms/channels table.
- Broadcast fanout worker.
- EventBridge or queues.
- Container runtime.
- NAT Gateway.
- Complex custom routes.

Start with `$connect`, `$disconnect`, and a single message route when the first realtime feature is defined.
