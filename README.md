# URL redirection

## How to use

### Starting the app

`$ mvn spring-boot:run [-Dspring-boot.run.profiles=dev/test/prod]`

- `-Dspring-boot.run.profiles` is optional

### [DEV] Viewing the application

<http://localhost:8080/>

### Testing

`$ mvn test`

### [DEV] Viewing the database

- <http://localhost:8080/h2-console/>
- JDBC URL: `jdbc:h2:mem:urlshortener`
- username:password: `sa:`

## Additional documentation

- [HISTORY](./docs/HISTORY.md)
- [PATTERN](./docs/PATTERN.md)
- [SECURITY](./docs/SECURITY.md)
