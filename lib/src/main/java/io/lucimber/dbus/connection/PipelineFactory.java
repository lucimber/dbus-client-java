package io.lucimber.dbus.connection;

public interface PipelineFactory {
    Pipeline create(Connection connection);
}
