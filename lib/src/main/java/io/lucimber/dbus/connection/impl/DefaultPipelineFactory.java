package io.lucimber.dbus.connection.impl;

import io.lucimber.dbus.connection.Connection;
import io.lucimber.dbus.connection.Pipeline;
import io.lucimber.dbus.connection.PipelineFactory;

public final class DefaultPipelineFactory implements PipelineFactory {
    @Override
    public Pipeline create(final Connection connection) {
        return new DefaultPipeline(connection);
    }
}
