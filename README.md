# Money Transfer

### Start

sbt run

### Test

sbt test

## Endpoints

Host: http://localhost:8005

### Open Account

POST /accounts

Response Code 202

Location Header

Example curl

```
curl -X POST \
  http://localhost:8005/accounts \
  -H 'cache-control: no-cache' \
   -H 'content-type: application/json'
```

### Get Account

GET /accounts/{id}

Response 200

Example curl

```
curl -X GET \
  http://localhost:8005/accounts/{id} \
  -H 'cache-control: no-cache'
```

### Deposit Account (needed to be able to make a transfer)

POST /accounts/{id}/deposits

Body:
``` json
{
  "amount": 50000
}
```

Response Code 202

Example curl

```
curl -X POST \
  http://localhost:8005/accounts/{id}/deposits \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -d '{
	"amount": 500
}'
```

### Transfer

POST /accounts/{id}/transfers

Body:
``` json
{
  "to": "abc-def"
  "amount": 50000
}
```

Response Code 202

Example curl

```
curl -X POST \
  http://localhost:8005/accounts/{id}/transfers \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -d '{
	"to": "{toId}",
	"amount": 500

}'
```