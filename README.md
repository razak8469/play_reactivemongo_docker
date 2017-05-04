# Reactive access of dockerized mongo instance from Play 2.5
This showcases reactive CRUD operations from a play application to a mongodb instance running as a docker container. On the application run, the play run time hook(./project/DockeredMongo.scala) read the docker file(./infrastructure/Dockerfile) and, installs and run a mongodb container instance in a ubuntu docker image.


# MongoDB configuration

Edit in application.conf if needed override the default host and collection
```
mongodb.uri = "mongodb://localhost/users"
```

# To Run
```
Make sure docker service is running
sbt run
```
# Operations

UserController uses guice injected modules of userDB service to do single and bulk CRUD operations as well as a clockService module to handle time. 

## Single Operations
