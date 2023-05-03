# A D-Bus Framework for Java based Applications
This software is a framework that enables a Java based
application to interact with a D-Bus instance in the role
of a client. The D-Bus instance can be a system or
session bus. As of now, only Unix sockets are supported
as transport by this framework. The type system, the wire
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
called Netty. Therefore its an asynchronous event-driven
network application framework.

The application needs to implement specific handlers
that will get called by this framework. Those handlers
must be programmed in a non-blocking fashion.
Using Future and CompletionStage is a good way to do so.

The type system of D-Bus is implemented by introducing
facades to the data types of Java (Facade Design Pattern).
This choice makes the framework a bit more robust and
the use of this framework approachable.

## Dependencies
* Java Runtime 8 or higher
* D-Bus 1.12 or higher

## Examples

### Bootstrap a connection
    String userId = "1000";
    String cookiePath = "~/.dbus-keyrings/";
    String socketPath = "/var/run/dbus/system_bus_socket";
    ConnectionFactory factory = new UnixSocketConnectionFactory(userId, cookiePath, socketPath);
    PipelineInitializer initializer = pipeline ->
        pipeline.addLast(EXAMPLE_HANDLER, new ExampleHandler());
    CompletionStage<Connection> connStage = factory.create(initializer);
### Make a method call
    UInt32 serial = pipeline.getConnection().getNextSerial();
    DBusString destination = DBusString.valueOf("org.bluez");
    ObjectPath path = ObjectPath.valueOf("/");
    DBusString name = DBusString.valueOf("GetManagedObjects");
    OutboundMethodCall msg = new OutboundMethodCall(serial, destination, path, name);
    DBusString iface = DBusString.valueOf("org.freedesktop.DBus.ObjectManager");
    msg.setInterfaceName(iface);
    pipeline.passOutboundMessage(msg);

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
This software is licensed under the Apache License 2.0.
A copy of this license is contained in the file LICENSE,
which is located in the root folder of this project.