# NETCONF Proxy

A NETCONF proxy/adapter with transformation capabilities that sits between NETCONF clients and target NETCONF devices. It can intercept, log, and transform NETCONF RPCs and responses on the fly.

## Features

- **SSH-based NETCONF Server**: Accepts NETCONF connections from clients over SSH (northbound)
- **NETCONF Client**: Connects to target NETCONF devices (southbound)
- **RPC Transformation**: Pattern-based RPC and response transformation using XSLT
- **Custom Hello Messages**: Override device capabilities with custom hello XML
- **Static Responses**: Return predefined responses for specific RPC patterns
- **Message Logging**: Log all NETCONF messages for debugging and analysis
- **Connection Pooling**: Shared device connections across multiple client sessions

## Requirements

- Java 11 or higher
- Maven 3.6+ (for building)

## Building

Build the project using the provided script:

```bash
./build.sh
```

Or manually with Maven:

```bash
mvn clean package
```

This creates an executable JAR: `target/netconf-proxy-1.0.0-SNAPSHOT.jar`

## Configuration

The proxy uses [Typesafe Config](https://github.com/lightbend/config) (HOCON format) for configuration.

### Configuration File Structure

```hocon
proxy {
  server {
    host = "0.0.0.0"           # Proxy server bind address
    port = 17833               # Proxy server port
    username = "admin"         # SSH authentication username
    password = "admin"         # SSH authentication password
    helloXmlFile = "./samples/hello-openconfig-200.xml"  # Optional custom hello
  }

  device {
    host = "device-hostname"   # Target NETCONF device
    port = 830                 # Target device port
    username = "username"      # Device authentication
    password = "password"
    connectionTimeoutMs = 180000
    keepAliveIntervalMs = 50000
  }

  transformation {
    enabled = true
    rpcPatternsFile = "./samples/rpc-patterns.xml"
    xsltFilesPath = "./xslt-files"
    # transformerClass = "com.example.CustomTransformer"  # Optional custom transformer
  }

  logging {
    logMessages = true         # Log all NETCONF messages
    logDir = "./logs"
  }
}
```

### Sample Configuration

A sample configuration is provided in `samples/proxy.conf`.

## Running

### Using the run script

```bash
./run.sh [config-file]
```

### Using Java directly

With default configuration (uses `src/main/resources/application.conf`):

```bash
java -jar target/netconf-proxy-1.0.0-SNAPSHOT.jar
```

With custom configuration:

```bash
java -Dconfig.file=/path/to/proxy.conf -jar target/netconf-proxy-1.0.0-SNAPSHOT.jar
```

Or pass the config file as an argument:

```bash
java -jar target/netconf-proxy-1.0.0-SNAPSHOT.jar /path/to/proxy.conf
```

## Connecting to the Proxy

Once running, NETCONF clients can connect using SSH:

```bash
ssh admin@localhost -p 17833 -s netconf
```

The proxy will:
1. Accept the client connection
2. Connect to the target device (if not already connected)
3. Forward RPCs between client and device
4. Apply any configured transformations
5. Log messages if enabled

## RPC Pattern Matching and Transformation

The proxy can match specific RPC patterns and apply transformations or return static responses.

### Pattern Configuration

Patterns are defined in an XML file (e.g., `samples/rpc-patterns.xml`):

```xml
<rpc-patterns>
  <pattern>
    <!-- RPC pattern to match -->
    <nc:rpc xmlns:nc="urn:ietf:params:xml:ns:netconf:base:1.0">
      <nc:get>
        <nc:filter nc:type="subtree">
          <netconf-state xmlns="urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring">
            <schemas/>
          </netconf-state>
        </nc:filter>
      </nc:get>
    </nc:rpc>

    <!-- Return a static response file -->
    <response-file>./netconf-monitoring-schemas-response.xml</response-file>
  </pattern>

  <pattern>
    <!-- Another pattern with XSLT transformation -->
    <nc:rpc xmlns:nc="urn:ietf:params:xml:ns:netconf:base:1.0">
      <nc:get>
        <nc:filter nc:type="subtree">
          <system xmlns="http://openconfig.net/yang/system"/>
        </nc:filter>
      </nc:get>
    </nc:rpc>

    <!-- Transform the RPC before sending to device -->
    <rpc-transformer>
      <xslt-file>transform-request.xslt</xslt-file>
    </rpc-transformer>

    <!-- Transform the response before sending to client -->
    <reply-transformer>
      <xslt-file>transform-response.xslt</xslt-file>
    </reply-transformer>
  </pattern>
</rpc-patterns>
```

### Pattern Matching

- Patterns are matched in order (first match wins)
- Wildcards are supported using empty elements
- Namespace-aware matching
- Attribute matching supported

### Transformation Options

1. **Static Response**: Return a predefined XML response from a file
2. **XSLT Transformation**: Transform RPC requests and/or responses using XSLT
3. **Custom Transformer**: Implement the `RpcTransformer` interface for complex logic

## Custom Hello Messages

Override device capabilities by providing a custom hello XML file:

```xml
<hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
  <capabilities>
    <capability>urn:ietf:params:netconf:base:1.0</capability>
    <capability>urn:ietf:params:netconf:base:1.1</capability>
    <capability>http://openconfig.net/yang/system?module=openconfig-system&amp;revision=2022-06-30</capability>
    <!-- Add more capabilities as needed -->
  </capabilities>
</hello>
```

This is useful for:
- Testing client behavior with different capability sets
- Hiding device-specific capabilities
- Adding capabilities not supported by the device

## Logging

When `logMessages = true`, all NETCONF messages are logged to the configured `logDir`:

- `rpc-<timestamp>.xml` - RPC requests from clients
- `reply-<timestamp>.xml` - Responses from devices
- `notification-<timestamp>.xml` - Notifications from devices

## Architecture

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│   NETCONF   │  SSH    │  NETCONF Proxy   │  SSH    │   Target    │
│   Client    ├────────>│                  ├────────>│   Device    │
│             │         │  - Transform     │         │             │
│             │<────────┤  - Log           │<────────┤             │
└─────────────┘         │  - Pattern Match │         └─────────────┘
                        └──────────────────┘
```

### Key Components

- **NetconfSshServer**: SSH server accepting client connections (Apache MINA SSHD)
- **NetconfSubsystemCommand**: Handles NETCONF subsystem for each client session
- **SharedDeviceClientManager**: Manages pooled connections to target device
- **JnccNetconfDeviceClient**: NETCONF client connecting to target device (JSch)
- **RpcPatternMatcher**: Matches RPCs against configured patterns
- **XsltTransformer**: Applies XSLT transformations
- **RpcTransformer**: Interface for custom transformation logic

## Use Cases

1. **Protocol Adaptation**: Translate between different NETCONF dialects or YANG models
2. **Testing**: Mock device responses for client testing
3. **Debugging**: Log and inspect NETCONF traffic
4. **Capability Filtering**: Hide or add capabilities
5. **Performance Testing**: Simulate device behavior without actual hardware
6. **Schema Translation**: Convert between OpenConfig and vendor-specific models

## SSH Host Key

The proxy automatically generates an SSH host key (`hostkey.ser`) on first run. This file is persisted and reused across restarts to maintain consistent host key fingerprints.

## Dependencies

- **Apache MINA SSHD** (2.17.1): SSH server implementation
- **JSch** (0.2.21): SSH client for NETCONF connections
- **Typesafe Config** (1.4.3): Configuration management
- **DOM4J** (2.2.0): XML processing
- **SLF4J/Logback**: Logging

## License

Eclipse Public License v1.0

## Copyright

Copyright © 2026 1FINITY Inc., and others. All rights reserved.
