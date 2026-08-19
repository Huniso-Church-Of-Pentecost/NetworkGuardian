package com.networkguardian.network.discovery

import android.content.Context
import com.networkguardian.domain.models.CapabilityStatus
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Result of a single discovery pass. This reflects only devices that have recently
 * communicated on the local link — it is not authoritative client enumeration, and
 * NetworkGuardian must never present it as such.
 */
data class DiscoveredHost(
    val ipAddress: String,
    val macAddress: String?,
    val reachable: Boolean
)

/** A local IPv4 interface NetworkGuardian can probe for other devices on the same subnet. */
data class LocalSubnet(
    val interfaceName: String,
    val localAddress: String,
    val prefixLength: Int
)

/**
 * Legitimate, non-intrusive local network discovery available to ordinary Android apps:
 *  1. Enumerate the device's own network interfaces directly via java.net.NetworkInterface
 *     (a standard, non-privileged API) to find the subnet actually worth probing.
 *  2. For addresses in that subnet, use InetAddress.isReachable (ICMP/TCP echo-style probe on a
 *     small, capped range) — a standard reachability check, not a port/vulnerability scan.
 *  3. Cross-reference /proc/net/arp (world-readable neighbor table) when accessible, to recover
 *     MAC addresses for hosts already known to the kernel. Not always accessible/populated
 *     depending on OEM/Android version; when it isn't, MAC addresses are simply omitted.
 *
 * IMPORTANT: this deliberately does NOT rely on ConnectivityManager.activeNetwork to find the
 * subnet to probe. When this device is hosting a Wi-Fi hotspot, its "active network" (the one
 * providing internet) is typically mobile data — a completely different subnet from the
 * hotspot's own local AP subnet where connected clients actually sit. Reading interfaces
 * directly finds the real local subnet regardless of which network is providing internet.
 *
 * This deliberately avoids: raw sockets, packet crafting, ARP spoofing, broadcast floods, or any
 * technique intended to manipulate rather than merely observe the network.
 */
class NetworkDiscoveryEngine(private val context: Context) {

    /** Caps the sweep to keep this battery- and network-friendly. Typical home/hotspot subnets are /24. */
    private val maxHostsToProbe = 254
    private val reachabilityTimeoutMs = 400

    /**
     * Finds a local IPv4 subnet worth probing by reading the device's own network interfaces
     * directly. Prefers Wi-Fi/AP-style interfaces and private address ranges; skips loopback
     * and interfaces that are down. Returns null if nothing usable is found — never guesses.
     */
    fun findLocalSubnet(): LocalSubnet? {
        val interfaces = try {
            NetworkInterface.getNetworkInterfaces()?.toList()
        } catch (_: Exception) {
            null
        } ?: return null

        val candidates = interfaces
            .filter { iface ->
                try {
                    iface.isUp && !iface.isLoopback && !iface.isVirtual
                } catch (_: Exception) {
                    false
                }
            }
            .flatMap { iface ->
                iface.interfaceAddresses.mapNotNull { ifaceAddr ->
                    val addr = ifaceAddr.address
                    if (addr is Inet4Address && isPrivateIPv4(addr)) {
                        LocalSubnet(
                            interfaceName = iface.name,
                            localAddress = addr.hostAddress ?: return@mapNotNull null,
                            prefixLength = ifaceAddr.networkPrefixLength.toInt()
                        )
                    } else {
                        null
                    }
                }
            }

        // Prefer interfaces that look like Wi-Fi/hotspot AP interfaces (common Android naming:
        // "wlan0", "ap0", "swlan0") over anything else (e.g. USB tethering interfaces), but fall
        // back to any private-range interface found rather than reporting nothing.
        return candidates.firstOrNull { it.interfaceName.startsWith("ap") || it.interfaceName.startsWith("wlan") }
            ?: candidates.firstOrNull()
    }

    private fun isPrivateIPv4(addr: Inet4Address): Boolean {
        val bytes = addr.address
        val first = bytes[0].toInt() and 0xFF
        val second = bytes[1].toInt() and 0xFF
        return when {
            first == 10 -> true
            first == 172 && second in 16..31 -> true
            first == 192 && second == 168 -> true
            else -> false
        }
    }

    /**
     * Performs one discovery pass. Returns an empty list (never fabricated hosts) if the
     * device has no usable local subnet to probe.
     */
    fun discoverHosts(): List<DiscoveredHost> {
        val subnet = findLocalSubnet() ?: return emptyList()
        val prefix = subnet.prefixLength

        val candidateIps = generateSubnetAddresses(subnet.localAddress, prefix).take(maxHostsToProbe)
        val arpTable = readArpTable()

        val results = mutableListOf<DiscoveredHost>()
        for (ip in candidateIps) {
            if (ip == subnet.localAddress) continue // skip the device's own address
            val reachable = try {
                InetAddress.getByName(ip).isReachable(reachabilityTimeoutMs)
            } catch (_: Exception) {
                false
            }
            val mac = arpTable[ip]
            if (reachable || mac != null) {
                results += DiscoveredHost(ipAddress = ip, macAddress = mac, reachable = reachable)
            }
        }
        return results
    }

    /** Reports whether discovery is meaningfully possible right now (has a subnet to probe). */
    fun discoveryStatus(): CapabilityStatus =
        if (findLocalSubnet() != null) CapabilityStatus.LIMITED else CapabilityStatus.NOT_AVAILABLE

    private fun generateSubnetAddresses(baseIp: String, prefixLength: Int): List<String> {
        if (prefixLength < 16 || prefixLength > 30) {
            // Outside typical home/hotspot subnet sizes — avoid probing huge or degenerate ranges.
            return emptyList()
        }
        val parts = baseIp.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return emptyList()

        val hostBits = 32 - prefixLength
        val hostCount = (1 shl hostBits) - 2 // exclude network/broadcast
        if (hostCount <= 0) return emptyList()

        val base = (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
        val mask = if (prefixLength == 0) 0 else (-1 shl hostBits)
        val network = base and mask

        val addresses = mutableListOf<String>()
        for (i in 1..hostCount) {
            val addr = network + i
            addresses += intToIp(addr)
        }
        return addresses
    }

    private fun intToIp(addr: Int): String {
        return "${(addr shr 24) and 0xFF}.${(addr shr 16) and 0xFF}.${(addr shr 8) and 0xFF}.${addr and 0xFF}"
    }

    /** Reads the kernel neighbor table if available. Returns an empty map otherwise — never guesses. */
    private fun readArpTable(): Map<String, String> {
        val file = File("/proc/net/arp")
        if (!file.exists() || !file.canRead()) return emptyMap()

        return try {
            BufferedReader(FileReader(file)).use { reader ->
                reader.readLine() // header
                val map = mutableMapOf<String, String>()
                reader.forEachLine { line ->
                    val cols = line.trim().split(Regex("\\s+"))
                    // Format: IP address / HW type / Flags / HW address / Mask / Device
                    if (cols.size >= 4) {
                        val ip = cols[0]
                        val mac = cols[3]
                        if (mac != "00:00:00:00:00:00") {
                            map[ip] = mac
                        }
                    }
                }
                map
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
