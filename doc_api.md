# Backend API Documentation

The backend server is used for storing locally detected transactions to the cloud. Where user can query and analyse expenditure conveniently.

## Configurable Params

- URL: backend base url
- TOKEN: authentication token

## Adding Item

**Purpose:** Inserting a transaction record to cloud database.

Example request:
```shell
curl -X 'POST' \
  '${URL}/transaction_info' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer ${TOKEN}' \
  -H 'Content-Type: application/json' \
  -d '{
  "amount": 0.011,
  "merchant": "",
  "payment_method": "string",
  "occurred_at": "2024-03-01T10:15:30Z"
}'
```

Expected response code: 201
