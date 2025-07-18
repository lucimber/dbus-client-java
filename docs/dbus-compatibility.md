# D-Bus Specification Compatibility

This document tracks the D-Bus specification versions and our implementation compatibility.

## Current Status

- **D-Bus Protocol Version**: 1 (stable since 2006)
- **Latest D-Bus Specification**: 0.43 (October 2024)
- **Our Implementation Target**: 0.41+ (with selective newer features)

## Specification Version History

| Version | Date | Key Changes | Implementation Status |
|---------|------|-------------|----------------------|
| 0.43 | 2024-10-29 | System services loading, Stats interface, Verbose interface | ❌ Not implemented |
| 0.42 | 2024-xx-xx | Various updates | ❌ Not implemented |
| 0.41 | 2023-xx-xx | Various updates | ✅ **Target version** |
| 0.26 | ~2015 | `BecomeMonitor` method | ❌ Not implemented |
| 0.21 | ~2012 | Unicode noncharacters allowed | ✅ Implemented |
| 0.18 | ~2011 | `eavesdrop` match key | ❌ Not implemented |
| 0.17 | ~2010 | `ObjectManager` interface | ❌ Not implemented |
| 0.16 | ~2009 | `path_namespace`, `arg0namespace` match keys | ❌ Not implemented |
| 0.12 | ~2007 | `arg[N]path` match keys | ❌ Not implemented |
| 1.0 | 2006-11-08 | **Protocol frozen** | ✅ Fully implemented |

## Implementation Details

### ✅ Fully Implemented
- **Core Protocol**: Message format, type system, wire protocol
- **Authentication**: EXTERNAL and DBUS_COOKIE_SHA1 SASL mechanisms
- **Transport**: Unix domain sockets and TCP/IP connections
- **Message Types**: Method calls, returns, signals, errors
- **Type System**: All basic and container types
- **Unicode Support**: Including noncharacters (spec 0.21+)

### ❌ Not Implemented
- **Advanced Match Rules**: `path_namespace`, `arg0namespace`, `arg[N]path`
- **Monitoring**: `BecomeMonitor` method
- **Object Manager**: `org.freedesktop.DBus.ObjectManager` interface
- **Stats Interface**: Bus daemon statistics
- **Verbose Interface**: Enhanced debugging

### 🔄 Partially Implemented
- **Message Bus**: Basic bus functionality, but not all advanced features
- **Introspection**: Basic support, but missing some newer features

## Compatibility Notes

1. **Wire Protocol**: Our implementation is fully compatible with all D-Bus implementations using protocol version 1.

2. **Specification Features**: We target specification version 0.41 but selectively implement useful features from newer versions.

3. **Interoperability**: Tested with standard D-Bus daemon and systemd's sd-bus.

4. **Future Work**: We may add support for newer specification features in future releases.

## Testing Coverage

Our integration tests verify compatibility with:
- Standard D-Bus daemon (dbus-daemon)
- SASL authentication mechanisms
- Both TCP and Unix socket transports
- Basic message bus operations

## References

- [D-Bus Specification](https://dbus.freedesktop.org/doc/dbus-specification.html)
- [D-Bus Protocol](https://dbus.freedesktop.org/doc/dbus-specification.html#message-protocol)
- [Freedesktop.org D-Bus Project](https://www.freedesktop.org/wiki/Software/dbus/)