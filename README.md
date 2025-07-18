# A D-Bus Framework for Java based Applications

[![Build Status](https://github.com/lucimber/dbus-client-java/workflows/Java%20CI%20with%20Gradle/badge.svg)](https://github.com/lucimber/dbus-client-java/actions)
[![CodeQL](https://github.com/lucimber/dbus-client-java/workflows/CodeQL/badge.svg)](https://github.com/lucimber/dbus-client-java/actions)
[![codecov](https://codecov.io/gh/lucimber/dbus-client-java/branch/main/graph/badge.svg)](https://codecov.io/gh/lucimber/dbus-client-java)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

This software is a framework that enables a Java based
application to interact with a D-Bus instance in the role
of a client. The D-Bus instance can be a system or
session bus. The framework supports multiple transport types
including Unix domain sockets and TCP/IP connections through
a pluggable strategy pattern. The type system, the wire
format and the message protocol are fully implemented as
of D-Bus Specification 0.41.

## What is D-Bus?
D-Bus is a bus system commonly found on the Linux desktop.
It enables applications to communicate with each other
indirectly via a bus. Applications can use this bus to signal state
changes to or exchange information with other applications.
Since D-Bus defines a marshalling format for its type system,
an application written in Java can talk to an application
written in C via D-Bus. For in depth information about D-Bus,
please visit https://www.freedesktop.org/wiki/Software/dbus/.

## Distinction
This D-Bus framework is written from scratch and doesn't
suffer the same architectural decisions as the reference
implementation. Furthermore, it can handle empty responses,
and both byte orders (little and big endian)
on the wire format of D-Bus.

## Architecture
This framework is based on a non-blocking I/O framework
called [Netty](https://netty.io). Therefore, it's an asynchronous
event-driven network application framework.

The framework features a sophisticated dual-pipeline architecture with strict thread isolation.
**Handlers can safely perform blocking operations** (database calls, REST APIs, file I/O) as they
run on dedicated thread pools, not the Netty event loop. The RealityCheckpoint bridge ensures
proper message routing between the transport and application layers.

The type system of D-Bus is implemented by introducing
wrappers to the data types of Java.
This choice makes the framework a bit more robust and
the use of this framework approachable.

## 🚀 Getting Started

**New to the library?** Start with the [Developer Guide](docs/developer-guide.md) for a structured learning path.

## Dependencies
* Java Runtime 17 or higher
* D-Bus 1.12 or higher

## Build
This project relies on Gradle's toolchain support. The wrapper will download
JDK 17 automatically when running the build. Simply execute `./gradlew test`
to compile and test the code.

## Testing

### Unit Tests
```bash
# Basic unit tests (memory-intensive tests skipped)
./gradlew test

# Include memory-intensive tests (requires more memory)
./gradlew test -PwithMemoryIntensiveTests
```

### Integration Tests
For reliable cross-platform D-Bus integration testing (single entry point):
```bash
# Main integration test command (works on any platform)
./gradlew integrationTest

# With verbose output to see detailed test results
./gradlew integrationTest -PshowOutput

# With debug logs
./gradlew integrationTest -Pdebug
```

**Note:** Integration tests always run in a Linux container to ensure consistent SASL authentication across all platforms. The `integrationTest` task automatically:
- Sets up a Linux container with D-Bus daemon
- Copies source code and compiles it inside the container
- Runs tests with proper SASL authentication
- Copies test results back to your local project


See [docs/testing-guide.md](docs/testing-guide.md) for detailed testing documentation.

## Examples

### Bootstrap a connection
```
Connection connection = NettyConnection.newSystemBusConnection();
Pipeline pipeline = connection.getPipeline();
pipeline.addLast("EXAMPLE_HANDLER", new ExampleHandler());
CompletionStage<Void> connectStage = connection.connect();
```

### Make a simple method call
This method is intended for simple request-response interactions where no additional
pipeline-based processing is needed.
```
DBusUInt32 serial = connection.getNextSerial();
OutboundMethodCall msg = OutboundMethodCall.Builder
    .create()
    .withSerial(serial)
    .withPath(DBusObjectPath.valueOf("/"))
    .withMember(DBusString.valueOf("GetManagedObjects"))
    .withDestination(DBusString.valueOf("org.bluez"))
    .withInterface(DBusString.valueOf("org.freedesktop.DBus.ObjectManager"))
    .withReplyExpected(true)
    .build();
CompletionStage<InboundMessage> reply = connection.sendRequest(msg);
```

### Example Handler (D-Bus Peer) for complex messaging
In this example, the handler implements the `org.freedesktop.DBus.Peer` interface,
in order to respond to a ping request and/or to a request to return the machine-id.
This method is intended for scenarios where custom or advanced processing of responses is needed,
while keeping message transmission efficient.
```java
public final class DbusPeerHandler extends AbstractInboundHandler implements InboundHandler {
  private static final DBusString INTERFACE = DBusString.valueOf("org.freedesktop.DBus.Peer");
  private final UUID machineId;

  public DbusPeerHandler(UUID machineId) {
    this.machineId = Objects.requireNonNull(machineId);
  }

  private static void respondToPing(Context ctx, InboundMethodCall methodCall) {
    OutboundMethodReturn methodReturn = OutboundMethodReturn.Builder
            .create()
            .withSerial(ctx.getConnection().getNextSerial())
            .withReplySerial(methodCall.getSerial())
            .withDestination(methodCall.getSender())
            .build();
    
    CompletableFuture<Void> writeFuture = new CompletableFuture<>();
    writeFuture.exceptionally(t -> {
      System.err.println("Couldn't respond to ping: " + t);
      return null;
    });
    
    ctx.propagateOutboundMessage(methodReturn, writeFuture);
  }

  private void handleInboundMethodCall(Context ctx, InboundMethodCall methodCall) {
    if (methodCall.getInterfaceName().orElse(DBusString.valueOf("")).equals(INTERFACE)) {
      DBusString methodName = methodCall.getMember();
      if (methodName.equals(DBusString.valueOf("Ping"))) {
        respondToPing(ctx, methodCall);
      } else if (methodName.equals(DBusString.valueOf("GetMachineId"))) {
        respondWithMachineId(ctx, methodCall);
      } else {
        ctx.propagateInboundMessage(methodCall);
      }
    } else {
      ctx.propagateInboundMessage(methodCall);
    }
  }

  private void respondWithMachineId(Context ctx, InboundMethodCall methodCall) {
    DBusSignature sig = DBusSignature.valueOf("s");
    List<DBusType> payload = new ArrayList<>();
    payload.add(DBusString.valueOf(machineId.toString()));
    
    OutboundMethodReturn methodReturn = OutboundMethodReturn.Builder
            .create()
            .withSerial(ctx.getConnection().getNextSerial())
            .withReplySerial(methodCall.getSerial())
            .withDestination(methodCall.getSender())
            .withBody(sig, payload)
            .build();

    CompletableFuture<Void> writeFuture = new CompletableFuture<>();
    writeFuture.exceptionally(t -> {
      System.err.println("Couldn't respond with machine ID: " + t);
      return null;
    });
    
    ctx.propagateOutboundMessage(methodReturn, writeFuture);
  }

  @Override
  public void handleInboundMessage(Context ctx, InboundMessage msg) {
    if (msg instanceof InboundMethodCall) {
      handleInboundMethodCall(ctx, (InboundMethodCall) msg);
    } else {
      ctx.propagateInboundMessage(msg);
    }
  }
}
```

## Participation
Participation is welcome and endorsed by the chosen license
and a simplified contributor agreement.

### Contributor Agreement
As the chosen open source license implicitly serves
as both the inbound (from contributors) and
outbound (to other contributors and users) license,
there's no need for an additional contributor agreement.

But to be super safe, this project requires developers
to state that each commit they make is authorized.
A Developer Certificate of Origin requirement is how many
projects achieve this.

> By making a contribution to this project, I certify that:
> 
> a. The contribution was created in whole or in part by me and I have the right to submit it under the open source license indicated in the file; or
>
> b. The contribution is based upon previous work that, to the best of my knowledge, is covered under an appropriate open source license and I have the right under that license to submit that work with modifications, whether created in whole or in part by me, under the same open source license (unless I am permitted to submit under a different license), as indicated in the file; or
>
> c. The contribution was provided directly to me by some other person who certified (a), (b) or (c) and I have not modified it.
>
> d. I understand and agree that this project and the contribution are public and that a record of the contribution (including all personal information I submit with it, including my sign-off) is maintained indefinitely and may be redistributed consistent with this project or the open source license(s) involved.

Therefore the contributors to this project sign-off that
they adhere to these requirements by adding
a Signed-off-by line to commit messages.

    This is an example commit message.
    
    Signed-off-by: Peter Peterson <pp@example.org>

## License
Copyright 2021-2025 Lucimber UG

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
