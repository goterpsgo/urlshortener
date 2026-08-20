# URL redirection

## How to use

### Starting the app

#### Backend only

`$ mvn spring-boot:run [-Dspring-boot.run.profiles=dev/test/prod]`

- `-Dspring-boot.run.profiles` is optional

#### Frontend only

```
$ cd frontend
$ npm install
$ npm run dev
```

#### Both (via Hivemind)

Runs the backend and frontend together using the `Procfile` at the repo root.

```
$ brew install hivemind   # one-time setup
$ hivemind
```

### [DEV] Viewing the application

- Backend: <http://localhost:8080/>
- Frontend: <http://localhost:5173/>

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
