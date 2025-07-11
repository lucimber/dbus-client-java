package com.lucimber.dbus.integration;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;
import java.net.InetSocketAddress;
import io.netty.channel.unix.DomainSocketAddress;

/**
 * Simple test to check Netty transport availability
 */
public class NettyTransportTest {
    public static void main(String[] args) {
        System.out.println("=== Netty Transport Availability ===");
        System.out.println("Epoll available: " + Epoll.isAvailable());
        if (!Epoll.isAvailable()) {
            System.out.println("Epoll unavailability reason: " + Epoll.unavailabilityCause());
        }
        System.out.println("KQueue available: " + KQueue.isAvailable());
        if (!KQueue.isAvailable()) {
            System.out.println("KQueue unavailability reason: " + KQueue.unavailabilityCause());
        }

        // Test socket address types
        System.out.println();
        System.out.println("=== Socket Address Types ===");
        System.out.println("InetSocketAddress: " + InetSocketAddress.class.getName());
        System.out.println("DomainSocketAddress: " + DomainSocketAddress.class.getName());
    }
}