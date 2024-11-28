# Network Load Testing Tool

A Java-based network load testing utility designed for stress testing web servers and analyzing their performance under heavy load conditions.

## Features

- Multi-threaded design for maximum concurrent connections
- TLS/SSL support with TLSv1.2
- Configurable number of threads based on system CPU cores
- Dynamic thread spawning for progressive load increase
- Ultra-short timeouts for aggressive testing
- Keep-alive connections for sustained load

## Requirements

- Java 8 or higher
- Maven or similar build tool

## Building

```bash
mvn clean package
```

## Usage

Run the application with default settings:

```bash
java -jar loadtester.jar
```

Or specify custom thread count:

```bash
java -jar loadtester.jar 1000
```

### Configuration

The following parameters can be modified in the code:

- `SOCKET_TIMEOUT`: Socket connection timeout (default: 500ms)
- Thread multiplier: Default is CPU cores * 500
- Connection keep-alive settings
- Target host and port

## Implementation Details

The tool utilizes:
- SSL/TLS connections via `javax.net.ssl`
- Non-blocking I/O operations
- Cached thread pool for efficient thread management
- Aggressive timeout settings
- TCP no-delay for maximum throughput

## Warning

This tool is designed for controlled testing environments only. Improper use could potentially:
- Overwhelm network resources
- Cause service disruption
- Impact system stability

Always obtain proper authorization before conducting load tests.

## Contributing

Contributions are welcome. Please fork the repository and submit a pull request with your changes.

## License

This project is licensed under the MIT License - see the LICENSE file for details.