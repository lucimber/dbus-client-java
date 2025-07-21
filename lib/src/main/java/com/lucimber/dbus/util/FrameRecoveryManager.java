/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Frame recovery manager for handling corrupted D-Bus messages and frame synchronization.
 * 
 * <p>This utility provides sophisticated frame recovery mechanisms including:</p>
 * <ul>
 *   <li>Frame corruption detection and diagnostics</li>
 *   <li>Frame boundary synchronization after corruption</li>
 *   <li>Partial frame recovery when possible</li>
 *   <li>Detailed error context for debugging</li>
 *   <li>Statistics collection for monitoring frame health</li>
 * </ul>
 * 
 * <p>The frame recovery manager is designed to be stateless and thread-safe,
 * making it suitable for use across multiple connections and decoder instances.</p>
 */
public final class FrameRecoveryManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FrameRecoveryManager.class);
    
    // D-Bus protocol constants
    private static final byte LITTLE_ENDIAN_FLAG = 'l';
    private static final byte BIG_ENDIAN_FLAG = 'B';
    private static final int HEADER_LENGTH_OFFSET = 4;
    private static final int BODY_LENGTH_OFFSET = 8;
    private static final int HEADER_FIELD_LENGTH_OFFSET = 12;
    private static final int MIN_HEADER_LENGTH = 16;
    private static final int MAX_MESSAGE_SIZE = 128 * 1024 * 1024; // 128MB
    
    // Statistics tracking
    private static final AtomicLong totalFramesProcessed = new AtomicLong(0);
    private static final AtomicLong corruptedFramesDetected = new AtomicLong(0);
    private static final AtomicLong framesRecovered = new AtomicLong(0);
    private static final AtomicLong framesSkipped = new AtomicLong(0);
    
    private FrameRecoveryManager() {
        // Utility class - no instantiation
    }
    
    /**
     * Analyzes a potentially corrupted frame and provides recovery information.
     * 
     * @param buffer the buffer containing the potentially corrupted frame
     * @return frame analysis result with recovery recommendations
     */
    public static FrameAnalysis analyzeFrame(ByteBuffer buffer) {
        totalFramesProcessed.incrementAndGet();
        
        if (buffer == null || !buffer.hasRemaining()) {
            return FrameAnalysis.createInvalid("Buffer is null or empty", CorruptionType.BUFFER_UNDERRUN);
        }
        
        int originalPosition = buffer.position();
        
        try {
            // Check minimum header length
            if (buffer.remaining() < MIN_HEADER_LENGTH) {
                return FrameAnalysis.createInvalid(
                    String.format("Buffer too small for header: %d bytes (need %d)", 
                        buffer.remaining(), MIN_HEADER_LENGTH),
                    CorruptionType.BUFFER_UNDERRUN
                );
            }
            
            // Check endianness flag
            byte endianFlag = buffer.get(0);
            ByteOrder byteOrder;
            if (endianFlag == LITTLE_ENDIAN_FLAG) {
                byteOrder = ByteOrder.LITTLE_ENDIAN;
            } else if (endianFlag == BIG_ENDIAN_FLAG) {
                byteOrder = ByteOrder.BIG_ENDIAN;
            } else {
                return FrameAnalysis.createInvalid(
                    String.format("Invalid endianness flag: 0x%02x (expected 'l' or 'B')", endianFlag),
                    CorruptionType.INVALID_ENDIAN_FLAG
                );
            }
            
            buffer.order(byteOrder);
            
            // Validate message type
            byte messageType = buffer.get(1);
            if (messageType < 1 || messageType > 4) {
                return FrameAnalysis.createInvalid(
                    String.format("Invalid message type: %d (expected 1-4)", messageType),
                    CorruptionType.INVALID_MESSAGE_TYPE
                );
            }
            
            // Check flags
            byte flags = buffer.get(2);
            // Flags validation is less strict as undefined bits should be ignored
            
            // Check protocol version
            byte protocolVersion = buffer.get(3);
            if (protocolVersion != 1) {
                return FrameAnalysis.createInvalid(
                    String.format("Unsupported protocol version: %d (expected 1)", protocolVersion),
                    CorruptionType.INVALID_PROTOCOL_VERSION
                );
            }
            
            // Validate body length
            int bodyLength = buffer.getInt(BODY_LENGTH_OFFSET);
            if (bodyLength < 0 || bodyLength > MAX_MESSAGE_SIZE) {
                return FrameAnalysis.createInvalid(
                    String.format("Invalid body length: %d (max %d)", bodyLength, MAX_MESSAGE_SIZE),
                    CorruptionType.INVALID_LENGTH
                );
            }
            
            // Validate header field length
            int headerFieldLength = buffer.getInt(HEADER_FIELD_LENGTH_OFFSET);
            if (headerFieldLength < 0 || headerFieldLength > MAX_MESSAGE_SIZE) {
                return FrameAnalysis.createInvalid(
                    String.format("Invalid header field length: %d (max %d)", headerFieldLength, MAX_MESSAGE_SIZE),
                    CorruptionType.INVALID_LENGTH
                );
            }
            
            // Calculate total frame size
            int headerPadding = (8 - (headerFieldLength % 8)) % 8;
            int totalFrameSize = MIN_HEADER_LENGTH + headerFieldLength + headerPadding + bodyLength;
            
            if (totalFrameSize > MAX_MESSAGE_SIZE) {
                return FrameAnalysis.createInvalid(
                    String.format("Frame too large: %d bytes (max %d)", totalFrameSize, MAX_MESSAGE_SIZE),
                    CorruptionType.FRAME_TOO_LARGE
                );
            }
            
            // Check if we have enough data for the complete frame
            if (buffer.remaining() < totalFrameSize) {
                return FrameAnalysis.createPartial(
                    String.format("Incomplete frame: have %d bytes, need %d", buffer.remaining(), totalFrameSize),
                    totalFrameSize - buffer.remaining()
                );
            }
            
            // Frame appears valid
            return FrameAnalysis.createValid(totalFrameSize, byteOrder, messageType, bodyLength, headerFieldLength);
            
        } catch (Exception e) {
            return FrameAnalysis.createInvalid(
                String.format("Exception during frame analysis: %s", e.getMessage()),
                CorruptionType.PARSE_EXCEPTION
            );
        } finally {
            buffer.position(originalPosition);
        }
    }
    
    /**
     * Attempts to find the next valid frame boundary in a corrupted buffer.
     * 
     * @param buffer the buffer to scan
     * @param maxScanBytes maximum number of bytes to scan
     * @return position of next potential frame start, or -1 if not found
     */
    public static int findNextFrameBoundary(ByteBuffer buffer, int maxScanBytes) {
        if (buffer == null || !buffer.hasRemaining()) {
            return -1;
        }
        
        int startPosition = buffer.position();
        int endPosition = Math.min(startPosition + maxScanBytes, buffer.limit());
        
        for (int pos = startPosition; pos <= endPosition - MIN_HEADER_LENGTH; pos++) {
            buffer.position(pos);
            
            // Check for valid endianness flag
            byte endianFlag = buffer.get(pos);
            if (endianFlag != LITTLE_ENDIAN_FLAG && endianFlag != BIG_ENDIAN_FLAG) {
                continue;
            }
            
            // Quick validation of the potential frame
            FrameAnalysis analysis = analyzeFrame(buffer);
            if (analysis.isValid() || analysis.isPartialFrame()) {
                buffer.position(startPosition); // Reset position
                return pos;
            }
        }
        
        buffer.position(startPosition); // Reset position
        return -1;
    }
    
    /**
     * Creates a detailed diagnostic report for a corrupted frame.
     * 
     * @param buffer the corrupted frame buffer
     * @param maxBytes maximum bytes to include in the hex dump
     * @return diagnostic report
     */
    public static FrameDiagnostic createDiagnostic(ByteBuffer buffer, int maxBytes) {
        if (buffer == null) {
            return new FrameDiagnostic("Buffer is null", "", FrameAnalysis.createInvalid("Null buffer", CorruptionType.BUFFER_UNDERRUN));
        }
        
        int dumpBytes = Math.min(maxBytes, buffer.remaining());
        StringBuilder hexDump = new StringBuilder();
        StringBuilder asciiDump = new StringBuilder();
        
        int originalPosition = buffer.position();
        
        for (int i = 0; i < dumpBytes; i++) {
            if (i % 16 == 0) {
                hexDump.append(String.format("%04x: ", i));
            }
            
            byte b = buffer.get(originalPosition + i);
            hexDump.append(String.format("%02x ", b));
            asciiDump.append(isPrintableAscii(b) ? (char) b : '.');
            
            if ((i + 1) % 16 == 0 || i == dumpBytes - 1) {
                // Pad hex dump to align ascii
                while ((i + 1) % 16 != 0 && i != dumpBytes - 1) {
                    hexDump.append("   ");
                    i++;
                }
                hexDump.append(" |").append(asciiDump).append("|\n");
                asciiDump.setLength(0);
            }
        }
        
        FrameAnalysis analysis = analyzeFrame(buffer);
        String summary = String.format(
            "Frame diagnostic: %d bytes available, corruption type: %s",
            buffer.remaining(),
            analysis.isValid() ? "NONE" : analysis.getCorruptionType()
        );
        
        return new FrameDiagnostic(summary, hexDump.toString(), analysis);
    }
    
    /**
     * Records a frame corruption incident for monitoring.
     * 
     * @param corruptionType the type of corruption detected
     * @param recovered whether the frame was successfully recovered
     */
    public static void recordCorruption(CorruptionType corruptionType, boolean recovered) {
        corruptedFramesDetected.incrementAndGet();
        if (recovered) {
            framesRecovered.incrementAndGet();
        } else {
            framesSkipped.incrementAndGet();
        }
        
        LOGGER.debug("Frame corruption recorded: type={}, recovered={}", corruptionType, recovered);
    }
    
    /**
     * Gets frame recovery statistics.
     * 
     * @return current statistics
     */
    public static FrameStatistics getStatistics() {
        return new FrameStatistics(
            totalFramesProcessed.get(),
            corruptedFramesDetected.get(),
            framesRecovered.get(),
            framesSkipped.get()
        );
    }
    
    /**
     * Resets frame recovery statistics.
     */
    public static void resetStatistics() {
        totalFramesProcessed.set(0);
        corruptedFramesDetected.set(0);
        framesRecovered.set(0);
        framesSkipped.set(0);
    }
    
    private static boolean isPrintableAscii(byte b) {
        return b >= 32 && b <= 126;
    }
    
    /**
     * Result of frame analysis.
     */
    public static class FrameAnalysis {
        private final boolean valid;
        private final boolean partialFrame;
        private final String errorMessage;
        private final CorruptionType corruptionType;
        private final int frameSize;
        private final int missingBytes;
        private final ByteOrder byteOrder;
        private final int messageType;
        private final int bodyLength;
        private final int headerFieldLength;
        
        private FrameAnalysis(boolean valid, boolean partialFrame, String errorMessage, 
                             CorruptionType corruptionType, int frameSize, int missingBytes,
                             ByteOrder byteOrder, int messageType, int bodyLength, int headerFieldLength) {
            this.valid = valid;
            this.partialFrame = partialFrame;
            this.errorMessage = errorMessage;
            this.corruptionType = corruptionType;
            this.frameSize = frameSize;
            this.missingBytes = missingBytes;
            this.byteOrder = byteOrder;
            this.messageType = messageType;
            this.bodyLength = bodyLength;
            this.headerFieldLength = headerFieldLength;
        }
        
        public static FrameAnalysis createValid(int frameSize, ByteOrder byteOrder, int messageType, 
                                              int bodyLength, int headerFieldLength) {
            return new FrameAnalysis(true, false, null, null, frameSize, 0, 
                                   byteOrder, messageType, bodyLength, headerFieldLength);
        }
        
        public static FrameAnalysis createInvalid(String errorMessage, CorruptionType corruptionType) {
            return new FrameAnalysis(false, false, errorMessage, corruptionType, -1, 0, 
                                   null, -1, -1, -1);
        }
        
        public static FrameAnalysis createPartial(String errorMessage, int missingBytes) {
            return new FrameAnalysis(false, true, errorMessage, CorruptionType.INCOMPLETE_FRAME, -1, missingBytes,
                                   null, -1, -1, -1);
        }
        
        public boolean isValid() { return valid; }
        public boolean isPartialFrame() { return partialFrame; }
        public String getErrorMessage() { return errorMessage; }
        public CorruptionType getCorruptionType() { return corruptionType; }
        public int getFrameSize() { return frameSize; }
        public int getMissingBytes() { return missingBytes; }
        public ByteOrder getByteOrder() { return byteOrder; }
        public int getMessageType() { return messageType; }
        public int getBodyLength() { return bodyLength; }
        public int getHeaderFieldLength() { return headerFieldLength; }
    }
    
    /**
     * Types of frame corruption.
     */
    public enum CorruptionType {
        BUFFER_UNDERRUN,
        INVALID_ENDIAN_FLAG,
        INVALID_MESSAGE_TYPE,
        INVALID_PROTOCOL_VERSION,
        INVALID_LENGTH,
        FRAME_TOO_LARGE,
        INCOMPLETE_FRAME,
        PARSE_EXCEPTION
    }
    
    /**
     * Frame diagnostic information.
     */
    public static class FrameDiagnostic {
        private final String summary;
        private final String hexDump;
        private final FrameAnalysis analysis;
        
        public FrameDiagnostic(String summary, String hexDump, FrameAnalysis analysis) {
            this.summary = summary;
            this.hexDump = hexDump;
            this.analysis = analysis;
        }
        
        public String getSummary() { return summary; }
        public String getHexDump() { return hexDump; }
        public FrameAnalysis getAnalysis() { return analysis; }
        
        @Override
        public String toString() {
            return summary + "\n" + hexDump;
        }
    }
    
    /**
     * Frame recovery statistics.
     */
    public static class FrameStatistics {
        private final long totalFrames;
        private final long corruptedFrames;
        private final long recoveredFrames;
        private final long skippedFrames;
        
        public FrameStatistics(long totalFrames, long corruptedFrames, long recoveredFrames, long skippedFrames) {
            this.totalFrames = totalFrames;
            this.corruptedFrames = corruptedFrames;
            this.recoveredFrames = recoveredFrames;
            this.skippedFrames = skippedFrames;
        }
        
        public long getTotalFrames() { return totalFrames; }
        public long getCorruptedFrames() { return corruptedFrames; }
        public long getRecoveredFrames() { return recoveredFrames; }
        public long getSkippedFrames() { return skippedFrames; }
        
        public double getCorruptionRate() {
            return totalFrames > 0 ? (double) corruptedFrames / totalFrames : 0.0;
        }
        
        public double getRecoveryRate() {
            return corruptedFrames > 0 ? (double) recoveredFrames / corruptedFrames : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "FrameStats{total=%d, corrupted=%d, recovered=%d, skipped=%d, " +
                "corruption_rate=%.2f%%, recovery_rate=%.2f%%}",
                totalFrames, corruptedFrames, recoveredFrames, skippedFrames,
                getCorruptionRate() * 100, getRecoveryRate() * 100
            );
        }
    }
}