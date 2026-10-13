package warlockfe.warlock3.app

import co.touchlab.kermit.Logger
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class LaunchRequest(
    val autoConnectName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val positionX: Int? = null,
    val positionY: Int? = null,
    val host: String? = null,
    val port: Int? = null,
    val key: String? = null,
    val salFile: String? = null,
    val exitOnDisconnect: Boolean = false,
) {
    fun toPayload(): String {
        val params = mutableListOf<String>()
        autoConnectName?.let { params.add("autoConnectName=" + URLEncoder.encode(it, "UTF-8")) }
        width?.let { params.add("width=$it") }
        height?.let { params.add("height=$it") }
        positionX?.let { params.add("positionX=$it") }
        positionY?.let { params.add("positionY=$it") }
        host?.let { params.add("host=" + URLEncoder.encode(it, "UTF-8")) }
        port?.let { params.add("port=$it") }
        key?.let { params.add("key=" + URLEncoder.encode(it, "UTF-8")) }
        salFile?.let { params.add("salFile=" + URLEncoder.encode(it, "UTF-8")) }
        if (exitOnDisconnect) params.add("exitOnDisconnect=true")
        return params.joinToString("&")
    }

    companion object {
        fun fromPayload(line: String): LaunchRequest {
            if (line.isBlank()) return LaunchRequest()
            val map = line.split('&').filter { it.contains('=') }.associate {
                val parts = it.split('=', limit = 2)
                parts[0] to URLDecoder.decode(parts[1], "UTF-8")
            }
            return LaunchRequest(
                autoConnectName = map["autoConnectName"],
                width = map["width"]?.toIntOrNull(),
                height = map["height"]?.toIntOrNull(),
                positionX = map["positionX"]?.toIntOrNull(),
                positionY = map["positionY"]?.toIntOrNull(),
                host = map["host"],
                port = map["port"]?.toIntOrNull(),
                key = map["key"],
                salFile = map["salFile"],
                exitOnDisconnect = map["exitOnDisconnect"] == "true",
            )
        }
    }
}

object SingleInstanceHelper {
    private const val CONNECT_TIMEOUT_MS = 2000

    /**
     * Attempts to delegate this launch request to an already running Warlock instance.
     * Returns true if successfully handled by the existing instance, or false if this
     * instance should become the master.
     */
    fun tryDelegateToExisting(portFile: File, request: LaunchRequest, logger: Logger): Boolean {
        if (!portFile.exists()) return false

        val port = try {
            portFile.readText().trim().toInt()
        } catch (_: Exception) {
            portFile.delete()
            return false
        }

        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), CONNECT_TIMEOUT_MS)
                socket.soTimeout = 3000
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))

                writer.write(request.toPayload())
                writer.newLine()
                writer.flush()

                val response = reader.readLine()
                if (response == "OK") {
                    logger.i { "Delegated window launch to existing Warlock instance on port $port" }
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            logger.d(e) { "Existing instance on port $port not responding; cleaning stale port file" }
            portFile.delete()
            false
        }
    }

    /**
     * Starts the single-instance IPC server to listen for new window launch requests.
     */
    fun startServer(
        portFile: File,
        logger: Logger,
        onLaunchReceived: (LaunchRequest) -> Unit,
    ): ServerSocket? {
        return try {
            val serverSocket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            val port = serverSocket.localPort

            portFile.parentFile?.mkdirs()
            portFile.writeText(port.toString())
            portFile.deleteOnExit()

            Runtime.getRuntime().addShutdownHook(Thread {
                try {
                    portFile.delete()
                    if (!serverSocket.isClosed) serverSocket.close()
                } catch (_: Exception) {}
            })

            Thread({
                try {
                    while (!serverSocket.isClosed) {
                        val client = serverSocket.accept()
                        Thread({
                            try {
                                client.use { sock ->
                                    sock.soTimeout = 3000
                                    val reader = BufferedReader(InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8))
                                    val writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8))

                                    val line = reader.readLine()
                                    if (line != null) {
                                        val request = LaunchRequest.fromPayload(line)
                                        logger.i { "Received remote window launch request: $request" }
                                        onLaunchReceived(request)
                                        writer.write("OK\n")
                                        writer.flush()
                                    }
                                }
                            } catch (e: Exception) {
                                logger.w(e) { "Error handling single-instance request" }
                            }
                        }, "Warlock-SingleInstance-Worker").start()
                    }
                } catch (e: Exception) {
                    if (!serverSocket.isClosed) {
                        logger.w(e) { "Single-instance server stopped" }
                    }
                }
            }, "Warlock-SingleInstance-Listener").apply { isDaemon = true }.start()

            logger.i { "Single-instance listener active on 127.0.0.1:$port (port file: ${portFile.absolutePath})" }
            serverSocket
        } catch (e: Exception) {
            logger.e(e) { "Failed to bind single-instance server" }
            null
        }
    }
}
