# PATTERN USED


The full typical set of items for a Spring Boot API endpoint with persistence:

```
┌────────────┬─────────────────────────────────────────────────────────────────────┬─────────────────────────────────┐
│   Layer    │                                Role                                 │          Your example           │
├────────────┼─────────────────────────────────────────────────────────────────────┼─────────────────────────────────┤
│ Controller │ HTTP routing — maps requests to service calls, DTOs in/out          │ LinkController                  │
├────────────┼─────────────────────────────────────────────────────────────────────┼─────────────────────────────────┤
│ DTOs       │ Shape of data in/out over the wire (request + response)             │ CreateLinkRequest, LinkResponse │
├────────────┼─────────────────────────────────────────────────────────────────────┼─────────────────────────────────┤
│ Service    │ Business logic, orchestrates repository calls, transaction boundary │ LinkService                     │
├────────────┼─────────────────────────────────────────────────────────────────────┼─────────────────────────────────┤
│ Repository │ Data access (JPA queries)                                           │ LinkRepository                  │
├────────────┼─────────────────────────────────────────────────────────────────────┼─────────────────────────────────┤
│ Entity     │ Maps to the DB table                                                │ Link                            │
└────────────┴─────────────────────────────────────────────────────────────────────┴─────────────────────────────────┘
```

## Request flow:

&emsp;_Controller_

&emsp;&emsp;→ receives a _DTO_

&emsp;&emsp;→ calls _Service_

&emsp;&emsp;→ Service uses _Repository_ to load/save the _Entity_

&emsp;&emsp;→ Service/Controller maps the Entity back into a response DTO.

Not every endpoint needs all five from day one (e.g., a stateless computation endpoint has no entity/repository), but for anything reading or writing data — like your Link feature — this is the standard shape, and it's exactly what you already have in com.example.urlshortener.link.
