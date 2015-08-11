# pangu
A simple JSON REST API server for money transfer among test user accounts

Data integraity is no. 1 priority in financial data systems.
This demo shows how to use relational database transaction and SQL query techniques to ensure data integrity, avoid dead lock and concurrent update conflict with little impact on performance.

## How to build
You need:
* Latest stable Oracle JDK 8
* Latest stable Apache Maven

## Api endpoints
It runs on http://localhost:9997/
* "GET /api/balance/{userid}": get user account balance; return a json object for balance
* "GET /api/transaction/{transactionid}": get individual transaction information; return a json object of transaction info(time, form_user, to_user, amount)
* "GET /api/transactions/{userid}?startdate={startdate}&enddate={enddate}": query user's transactions; return a json array of transaction info
* "POST /api/transfer": request body should be a json object(from_user, to_user, amount); return a json object for transaction id

error codes:
* 404: requested resource not found
* 400: bad request
* 500: internal server error

response body may include a json object showing detailed error message 

## Test data
By default 3 users are created:
* user 1 has balance 100
* user 2 has balance 200
* user 3 has balance 300
