# clb-back-end

## This service is designed to be used to handle crawl data from multiple site with massive request

## Technologies
- Spring Webflux
- Postgres, Redis
- Spock Test
- Gitlab Cicd, GCP

### Why Webflux
- WebFlux is designed to work with reactive streams and is optimized for non-blocking I/O.
- This makes it a good choice for applications that need to handle high traffic loads or work with real-time data.
- WebFlux allows you to write code that is more responsive and efficient, with fewer threads and less memory usage.

### Why Spock
- Easy to read
- Shorter and clearer than Junit
- Support everything: BDD,DB,...
- Work perfectly with Junit,Mockito
- Support Groovy,Java
## Project Structure
```
📦modules
 ┣ 📂clb-api
 ┃ ┣ 📂 controler
 ┃ ┣ 📂 scheduler
 ┃ ┣ 📂 socket
 ┃ ┣ 📂 config
 ┣ 📂clb-base
 ┣ 📂clb-service

```