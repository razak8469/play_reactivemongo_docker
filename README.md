# Reactive access of dockerized mongo instance from Play Framework 2.5
This showcases reactive CRUD operations from a play application to a mongodb instance running as a docker container.
On the application run, the play run time hook(/project/DockeredMongo.scala) read the docker file(/infrastructure/Dockerfile)
and, installs and run a mongodb container instance in a ubuntu docker image. On application exit, the containers are removed
but the images are not deleted.

# To Run
On the first run, the docker images are downloaded (and cached) and so it takes a few minutes longer than the successive runs. The docker commands in the runtime hook 
uses the cached images for the successive runs. These images are not deleted when the application exits. Remove docker images manually when needed.

Prerequisites:

  a)  Make sure Java 8 is installed and is the active version
  b)  Make sure docker service is running
```
sbt run
```
The play runtime hook has docker commands to build and run the mongodb container instance. These commands are run as seperate process from the application. The application 
is ready to use when these commands finishes. These commands finishes with a message similar to the following(the build id and the instance id varies in different runs)

```
Successfully built 2da7980dd2b5
3c8986f5338085222b416528b362eb06e0f9bfb991483e6155590946d9fc1035
```
The application is now ready to be operated on.

# Operations

UserController uses guice injected modules of userDB service to do single and bulk CRUD operations as well as a clockService module to handle time.

## Single operations

These CRUD operations takes a single user_id url parameter to manipulate the corresponding user.

## Add/Update(Upsert) a single user
If a user already exists with the specied user_id an update is performed. Otherwise an insert is performed
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
```
curl -H "Content-Type: application/json" -X PUT -d '{"name":"UpdatedFirst1 Last1","high_score": 102, "last_login":"2017-05-01T02:26:49Z"}' http://localhost:9000/users/user_id=1
```

## Retrieve a single user
Example:
```
curl -X GET http://localhost:9000/users/user_id=1
```

## Delete a single user
Example:
```
curl -X DELETE http://localhost:9000/users/user_id=1
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
 
## A few error calls:
Invalid json body in PUT request(high_score field not present)
```
  curl -H "Content-Type: application/json" -X PUT -d '{"name":"First1 Last1", "last_login":"2017-05-01T02:26:49Z"}' http://localhost:9000/users/user_id=1
```
Invalid json body in PUT request(highest_score is an invalid field)
```
curl -H "Content-Type: application/json" -X PUT -d '{"name":"First1 Last1","highest_score": 103, "last_login":"2017-05-01T02:26:49Z"}' http://localhost:9000/users/user_id=1
```
Invalid json body in bulk PUT request(id field absent for the second element of the json array)
```
curl -H "Content-Type: application/json" -X PUT -d
 '[
      {"id": 11, "user": { "name":"name11","high_score":101,"last_login":"2017-05-01T16:26:49-0700"}},
      {"user": { "name":"name12","high_score":102,"last_login":"2017-05-02T16:26:49-0700"}},
      {"id": 13, "user": { "name": "name13","high_score":106,"last_login":"2017-05-03T16:26:49-0700"}}
  ]' http://localhost:9000/users 
 ```
 Invalid json body in bulk PUT request(user field absent for the third element of the json array)
```
curl -H "Content-Type: application/json" -X PUT -d
 '[
      {"id": 11, "user": { "name":"name11","high_score":101,"last_login":"2017-05-01T16:26:49-0700"}},
      {"id": 12, "user": { "name":"name12","high_score":102,"last_login":"2017-05-02T16:26:49-0700"}},
      {"id": 13}
  ]' http://localhost:9000/users 
 ```
 No json body provided in the bulk DELETE request( a json body must be provided for bulk delete, it can be {} to delete all users)
 ```
 curl -X DELETE  http://localhost:9000/users
 ```

## Tests
   The UserController use a guice injected clockService for date and time manipulations. By default the controller use a service module based on the system clock.
   To test the date and time calculations a fixed time clock service module can be injected to the application(/test/ClockServiceSpec)

   To run the tests
   ```
   sbt test
   ```
