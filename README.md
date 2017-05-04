# Reactive access of dockerized mongo instance from Play Framework 2.5
This showcases reactive CRUD operations from a play application to a mongodb instance running as a docker container.
On the application run, the play run time hook(/project/DockeredMongo.scala) read the docker file(/infrastructure/Dockerfile)
and, installs and run a mongodb container instance in a ubuntu docker image. On application exit, the containers are removed
but the images are not deleted.

# To Run
On the first run, the docker images are downloaded. These images are not deleted when the application exits. Remove docker images manually when needed.

Make sure docker service is running
```
sbt run
```
# Operations

UserController uses guice injected modules of userDB service to do single and bulk CRUD operations as well as a clockService module to handle time.

## Single operations

These CRUD operations takes a single user_id url parameter to manipulate the corresponding user.

## Add a single user
```
Use PUT method with json body of the format 
{
  "name" : "First Last", 
  "high_score" : 100, 
  "last_login" : "2016-09-02T15:26:49Z"
}
Here last_login is in ISO UTC format
```
Examples:
```
curl -H "Content-Type: application/json" -X PUT -d '{"name":"First1 Last1","high_score": 103, "last_login":"2017-05-01T02:26:49Z"}' http://localhost:9000/users/user_id=1
```
```
curl -H "Content-Type: application/json" -X PUT -d '{"name":"First2 Last2","high_score": 100, "last_login":"2017-05-03T02:26:49Z"}' http://localhost:9000/users/user_id=2
```

## Retrieve a single user
Example:
```
curl -X GET http://localhost:9000/user_id=1
```

## Update a single user
```
Use PUT method with json body of the format
{
    "name" : "UpdateFirst Last",
    "high_score" : 101,
    "last_login" : "2017-05-02T15:26:49Z"
}
```
Example:
```
curl -H "Content-Type: application/json" -X PUT -d '{"name":"UpdatedFirst1 Last1","high_score": 102, "last_login":"2017-05-01T02:26:49Z"}' http://localhost:9000/users/user_id=1
```

## Delete a single user
Example:
```
curl -X DELETE http://localhost:9000/user_id=1
```

## Bulk Operations

## Add/Update multiple users
   Here the user id appears in the json body as below. For users that appears in the json array with the same id,
   insertion/updation happens with the one with the latest last_login value

```
Use PUT method with json body of the format

[
    {"id": 11, "user": { "name":"name11","high_score":101,"last_login":"2017-05-01T16:26:49-0700"}},
    {"id": 12, "user": { "name":"name12","high_score":102,"last_login":"2017-05-02T16:26:49-0700"}},
    {"id": 11, "user": { "name":"recent_name11","high_score":101,"last_login":"2017-05-02T16:26:49-0700"}}
]
```
Example:
```
curl -H "Content-Type: application/json" -X PUT -d
 '[
      {"id": 11, "user": { "name":"name11","high_score":101,"last_login":"2017-05-01T16:26:49-0700"}},
      {"id": 12, "user": { "name":"name12","high_score":102,"last_login":"2017-05-02T16:26:49-0700"}},
      {"id": 13, "user": { "name": "name13","high_score":106,"last_login":"2017-05-03T16:26:49-0700"}},
      {"id": 21, "user": { "name":"name21","high_score":107,"last_login":"2017-05-01T16:26:49-0700"}},
      {"id": 22, "user": { "name":"name22","high_score":108,"last_login":"2017-05-01T16:26:49-0700"}},
      {"id": 23, "user": { "name": "name23","high_score":101,"last_login":"2017-05-02T16:26:49-0700"}},
      {"id": 31, "user": { "name":"name31","high_score":102,"last_login":"2017-05-03T16:26:49-0700"}},
      {"id": 32, "user": { "name":"name32","high_score":103,"last_login":"2017-05-01T16:26:49-0700"}},
      {"id": 33, "user": { "name": "name33","high_score":104,"last_login":"2017-05-01T16:26:49-0700"}},
      {"id": 11, "user": { "name":"recent_name11","high_score":101,"last_login":"2017-05-02T16:26:49-0700"}}
  ]' http://localhost:9000/users
```

## Get all the users
Example:
```
curl -X GET http://localhost:9000/users
```

## Delete all/specified users

Bulk deleteion expects an json body that can be empty or of the format as below

Use DELETE method with json body of the format

  a) Delete all users between an inclusive id range: {"fromId": 13, "toId": 23}
  Example:
  ```
     curl -H "Content-Type: application/json" -X DELETE -d '{"fromId": 13, "toId": 22}' http://localhost:9000/users
  ```
 b) Delete all users >= to a specied id: {"fromId": 22 }
 Example:
 ```
    curl -H "Content-Type: application/json" -X DELETE -d '{"fromId": 12}' http://localhost:9000/users
 ```
 c) Delete all users <= to a specied id: {"toId": 22 }
 Example:
 ```
    curl -H "Content-Type: application/json" -X DELETE -d '{"toId": 22}' http://localhost:9000/users
 ```
 d) Delete all the users: { }
 Example:
 ```
    curl -H "Content-Type: application/json" -X DELETE -d '{}' http://localhost:9000/users
 ```

## Tests
   The UserController use a guice injected clockService for date and time manipulations. By default the controller use a service module based on the system clock.
   To test the date and time calculations a fixed time clock service module can be injected to the application(/test/ClockServiceSpec)

   To run the tests
   ```
   sbt testAll
   ```
