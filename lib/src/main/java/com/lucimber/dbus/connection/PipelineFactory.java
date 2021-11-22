package com.lucimber.dbus.connection;

public interface PipelineFactory {
    Pipeline create(Connection connection);
}
