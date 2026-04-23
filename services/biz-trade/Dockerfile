FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o /binary .

FROM alpine:latest
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /binary /app/
USER appuser
CMD ["/app/binary"]
