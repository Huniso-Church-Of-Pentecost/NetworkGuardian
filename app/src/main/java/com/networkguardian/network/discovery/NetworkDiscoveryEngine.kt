package com.networkguardian.network.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.LinkProperties
import android.net.Network
import android.system.OsConstants
import com.networkguardian.domain.models.CapabilityStatus
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Result of a single discovery pass. `limited` is always true because this technique
 * (subnet enumeration + reachability + the kernel ARP/neighbor table) reflects only devices
 * that have recently communicated on the local link — it is not authoritative client
 * enumeration, and NetworkGuardian must never present it as such.
 */
data class DiscoveredHost(
    val ipAddress: String,
    val macAddress: String?,
    val reachable: Boolean
)

/**
 * Legitimate, non-intrusive local network discovery available to ordinary Android apps:
 *  1. Read the device's own link properties (subnet, prefix) via ConnectivityManager/LinkProperties.
 *  2. For addresses in that subnet, use InetAddress.isReachable (ICMP/TCP echo-style probe on a
 *     small, capped range) — a standard reachability check, not a port/vulnerability scan.
 *  3. Cross-reference /proc/net/arp (world-readable neighbor table) when accessible, to recover
 *     MAC addresses for hosts already known to the kernel. This file is NOT always accessible or
 *     populated depending on OEM/Android version; when it isn't, MAC addresses are simply omitted.
 *
 * This deliberately avoids: raw sockets, packet crafting, ARP spoofing, broadcast floods, or any
 * technique intended to manipulate rather than merely observe the network.
 */
class NetworkDiscoveryEngine(private val context: Context) {

    /** Caps the sweep to keep this battery- and network-friendly. Typical home subnets are /24. */
    private val maxHostsToProbe = 254
    private val reachabilityTimeoutMs = 400

    fun currentSubnetCidr(): LinkAddress? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val network: Network = cm.activeNetwork ?: return null
        val props: LinkProperties = cm.getLinkProperties(network) ?: return null
        return props.linkAddresses.firstOrNull { it.address is java.net.Inet4Address }
    }

    /**
     * Performs one discovery pass. Returns an empty list (never fabricated hosts) if the
     * device has no usable subnet information.
     */
    fun discoverHosts(): List<DiscoveredHost> {
        val link = currentSubnetCidr() ?: return emptyList()
        val base = link.address.hostAddress ?: return emptyList()
        val prefix = link.prefixLength

        val candidateIps = generateSubnetAddresses(base, prefix).take(maxHostsToProbe)
        val arpTable = readArpTable()

        val results = mutableListOf<DiscoveredHost>()
        for (ip in candidateIps) {
            val reachable = try {
                InetAddress.getByName(ip).isReachable(reachabilityTimeoutMs)
            } catch (_ : Exception) {
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
        if (currentSubnetCidr() != null) CapabilityStatus.LIMITED else CapabilityStatus.NOT_AVAILABLE

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
