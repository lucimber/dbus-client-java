package io.lucimber.dbus.connection;

import java.util.concurrent.CompletionStage;

/**
 * A factory that creates implementation specific instances of a {@link Connection}.
 */
public interface ConnectionFactory {
    /**
     * Creates a new connection that can be used to invoke methods or to subscribe to signals.
     *
     * @param initializer will be used to configure the connection pipeline
     * @return A {@link CompletionStage} with a new instance of a {@link Connection}.
     * @throws ConnectionException if the connection could not be established
     */
    CompletionStage<Connection> create(PipelineInitializer initializer) throws ConnectionException;
}
