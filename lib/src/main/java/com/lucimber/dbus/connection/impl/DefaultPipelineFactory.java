package com.lucimber.dbus.connection.impl;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.connection.PipelineFactory;

public final class DefaultPipelineFactory implements PipelineFactory {
    @Override
    public Pipeline create(final Connection connection) {
        return new DefaultPipeline(connection);
    }
}
