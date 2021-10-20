package io.lucimber.dbus.util;

import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.function.Supplier;

/**
 * Provides common methods necessary for logging.
 */
public final class LoggerUtils {

    public static final String MARKER_CONNECTION_INBOUND = "DBUS_CONNECTION_INBOUND";
    public static final String MARKER_CONNECTION_OUTBOUND = "DBUS_CONNECTION_OUTBOUND";
    public static final String MARKER_DATA_MARSHALLING = "DBUS_DATA_MARSHALLING";
    public static final String MARKER_DATA_UNMARSHALLING = "DBUS_DATA_UNMARSHALLING";
    public static final String MARKER_SASL_INBOUND = "DBUS_SASL_INBOUND";
    public static final String MARKER_SASL_OUTBOUND = "DBUS_SASL_OUTBOUND";

    private LoggerUtils() {
        // Utility class
    }

    /**
     * Logs a message at the DEBUG level.
     *
     * @param logger   The instance of the logger.
     * @param supplier The supplier that provides the message.
     */
    public static void debug(final Logger logger, final Supplier<String> supplier) {
        if (logger.isDebugEnabled()) {
            logger.debug(supplier.get());
        }
    }

    /**
     * Logs a message at the DEBUG level.
     *
     * @param logger   The instance of the logger.
     * @param supplier The supplier that provides the message.
     * @param cause    The throwable to log.
     */
    public static void debug(final Logger logger, final Supplier<String> supplier, final Throwable cause) {
        if (logger.isDebugEnabled()) {
            logger.debug(supplier.get(), cause);
        }
    }

    /**
     * Logs a message at the DEBUG level.
     *
     * @param logger   The instance of the logger.
     * @param marker   The marker related to the message.
     * @param supplier The supplier that provides the message.
     */
    public static void debug(final Logger logger, final Marker marker, final Supplier<String> supplier) {
        if (logger.isDebugEnabled()) {
            logger.debug(marker, supplier.get());
        }
    }

    /**
     * Logs a message at the DEBUG level.
     *
     * @param logger   The instance of the logger.
     * @param marker   The marker related to the message.
     * @param supplier The supplier that provides the message.
     * @param cause    The cause of the failure.
     */
    public static void debug(final Logger logger, final Marker marker, final Supplier<String> supplier,
                             final Throwable cause) {
        if (logger.isDebugEnabled()) {
            logger.debug(marker, supplier.get(), cause);
        }
    }

    /**
     * Logs a message at the INFO level.
     *
     * @param logger   The instance of the logger.
     * @param supplier The supplier that provides the message.
     */
    public static void info(final Logger logger, final Supplier<String> supplier) {
        if (logger.isInfoEnabled()) {
            logger.info(supplier.get());
        }
    }

    /**
     * Logs a message at the INFO level.
     *
     * @param logger   The instance of the logger.
     * @param marker   The marker related to the message.
     * @param supplier The supplier that provides the message.
     */
    public static void info(final Logger logger, final Marker marker, final Supplier<String> supplier) {
        if (logger.isInfoEnabled()) {
            logger.info(marker, supplier.get());
        }
    }

    /**
     * Logs a message at the TRACE level.
     *
     * @param logger   The instance of the logger.
     * @param supplier The supplier that provides the message.
     */
    public static void trace(final Logger logger, final Supplier<String> supplier) {
        if (logger.isTraceEnabled()) {
            logger.trace(supplier.get());
        }
    }

    /**
     * Logs a message at the TRACE level.
     *
     * @param logger   The instance of the logger.
     * @param marker   The marker related to the message.
     * @param supplier The supplier that provides the message.
     */
    public static void trace(final Logger logger, final Marker marker, final Supplier<String> supplier) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, supplier.get());
        }
    }

    /**
     * Logs a message at the WARN level.
     *
     * @param logger   The instance of the logger.
     * @param supplier The supplier that provides the message.
     */
    public static void warn(final Logger logger, final Supplier<String> supplier) {
        if (logger.isWarnEnabled()) {
            logger.warn(supplier.get());
        }
    }

    /**
     * Logs a message at the WARN level.
     *
     * @param logger   The instance of the logger.
     * @param marker   The marker related to the message.
     * @param supplier The supplier that provides the message.
     */
    public static void warn(final Logger logger, final Marker marker, final Supplier<String> supplier) {
        if (logger.isWarnEnabled()) {
            logger.warn(marker, supplier.get());
        }
    }

    /**
     * Logs a message at the ERROR level.
     *
     * @param logger   The instance of the logger.
     * @param supplier The supplier that provides the message.
     */
    public static void error(final Logger logger, final Supplier<String> supplier) {
        if (logger.isErrorEnabled()) {
            logger.error(supplier.get());
        }
    }

    /**
     * Logs a message at the ERROR level.
     *
     * @param logger   The instance of the logger.
     * @param marker   The marker related to the message.
     * @param supplier The supplier that provides the message.
     */
    public static void error(final Logger logger, final Marker marker, final Supplier<String> supplier) {
        if (logger.isErrorEnabled()) {
            logger.error(marker, supplier.get());
        }
    }

    /**
     * Logs a message at the ERROR level.
     *
     * @param logger    The instance of the logger.
     * @param supplier  The supplier that provides the message.
     * @param throwable The throwable to log.
     */
    public static void error(final Logger logger, final Supplier<String> supplier,
                             final Throwable throwable) {
        if (logger.isErrorEnabled()) {
            logger.error(supplier.get(), throwable);
        }
    }

    /**
     * Logs a message at the ERROR level.
     *
     * @param logger    The instance of the logger.
     * @param marker    The marker related to the message.
     * @param supplier  The supplier that provides the message.
     * @param throwable The throwable to log.
     */
    public static void error(final Logger logger, final Marker marker, final Supplier<String> supplier,
                             final Throwable throwable) {
        if (logger.isErrorEnabled()) {
            logger.error(marker, supplier.get(), throwable);
        }
    }
}
