# hawkbit-update-server

Want to chat with the team behind hawkBit? [![Join the chat at https://gitter.im/eclipse/hawkbit](https://badges.gitter.im/eclipse/hawkbit.svg)](https://gitter.im/eclipse/hawkbit?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[hawkBit](https://projects.eclipse.org/projects/iot.hawkbit) is an domain independent back end solution for rolling out software updates to constrained edge devices as well as more powerful controllers and gateways connected to IP based networking infrastructure.


# Compile
```
mvn install
```

## Try out examples
#### Standalone Test Application Server
[Example Application](examples/hawkbit-example-app)
#### Device Simulator using the DMF AMQP API
[Device Simulator](examples/hawkbit-device-simulator)

# Modules
`hawkbit-core` : core elements.  
`hawkbit-security-core` : core security elements.  
`hawkbit-security-integration` : security integration elements to integrate security into hawkbit.  
`hawkbit-artifact-repository-mongo` : artifact repository implementation to mongoDB.    
`hawkbit-autoconfigure` : spring-boot auto-configuration.  
`hawkbit-dmf-api` : API for the Device Management Integration.  
`hawkbit-dmf-amqp` : AMQP endpoint implementation for the DMF API.  
`hawkbit-repository` : repository implemenation based on SQL for all meta-data.    
`hawkbit-http-security` : implementation for security filters for HTTP.    
`hawkbit-rest-api` : API classes for the REST Management API.  
`hawkbit-rest-resource` : HTTP REST endpoints for the Management and the Direct Device API.  
`hawkbit-ui` : Vaadin UI.


# Device Integration
There are two device integration APIs provided by the hawkbit update server.
* [Direct Device Integration API (HTTP)](DDIA.md)
* [Device Management Federation API (AMQP)](DMFA.md)
