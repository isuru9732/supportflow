# common

Shared library used by every SupportFlow backend service — DTOs and response
envelopes that implement the API conventions from `docs/05-API-Design.md`.

## What's in here

- `com.supportflow.common.dto.RequestEnvelope<T>` — generic `{"data": {...}}` request wrapper
- `com.supportflow.common.response.ApiResponse<T>` — generic `{"data", "meta", "pagination"}` response wrapper
- `com.supportflow.common.response.PaginationMeta` — pagination block for list endpoints
- `com.supportflow.common.response.ApiError` — standard error shape

## Setup

1. Place this folder at `services/common/` in the monorepo (alongside `identity/`, `organization/`, etc.)
2. Install it to your local Maven repository so other services can depend on it:
   ```bash
   cd services/common
   mvn clean install
   ```
3. In any service's `pom.xml`, add:
   ```xml
   <dependency>
       <groupId>com.supportflow</groupId>
       <artifactId>common</artifactId>
       <version>0.0.1-SNAPSHOT</version>
   </dependency>
   ```

## Usage example

```java
@PostMapping("/register")
public ResponseEntity<ApiResponse<RegisterResponse>> register(
        @Valid @RequestBody RequestEnvelope<RegisterRequest> envelope) {
    RegisterResponse response = authService.register(envelope.getData());
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
}
```

## Note for CI/CD (Doc 09)

Local `mvn install` is sufficient for solo development. Before this ships through
GitHub Actions / Docker builds, the pipeline needs an explicit step to build and
install this module before building any dependent service — currently tracked as
an open task, not yet implemented. See Doc 09 §4 and Doc 10 Epic 0/8.
