package de.roamingthings.workbench.sftp

import de.roamingthings.workbench.sftp.configuration.SftpProperties
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory
import org.apache.sshd.common.keyprovider.ClassLoadableResourceKeyPairProvider
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Profile
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import org.springframework.stereotype.Service
import org.springframework.util.Base64Utils.decode
import org.springframework.util.StreamUtils.copyToString
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteBuffer.wrap
import java.nio.charset.Charset.defaultCharset
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec

const val PORT_AUTO = 0

@Service
@Profile("embeddedSftp")
class EmbeddedSftpServer(
        private val sftpProperties: SftpProperties,
        private val sftpSessionFactory: DefaultSftpSessionFactory) : InitializingBean, SmartLifecycle {

    private val server = SshServer.setUpDefaultServer()

    @Volatile
    private var port: Int = PORT_AUTO

    @Volatile
    private var running: Boolean = false


    fun setPort(port: Int) {
        this.port = port
    }

    override fun afterPropertiesSet() {
        val allowedKey = decodePublicKey()
        server.setPublickeyAuthenticator { username, key, session -> key == allowedKey }
        server.port = this.port
        server.keyPairProvider = ClassLoadableResourceKeyPairProvider("META-INF/keys/ssh_host_rsa_key")
        server.subsystemFactories = listOf(SftpSubsystemFactory())
        val pathname = System.getProperty("java.io.tmpdir") + File.separator + "sftptest" + File.separator
        File(pathname).mkdirs()
        server.fileSystemFactory = VirtualFileSystemFactory(Paths.get(pathname))
    }

    private fun decodePublicKey(): PublicKey {
        val publicKeyInputStream = sftpProperties.userPublicKey?.inputStream
                ?: throw IllegalStateException("Public user key not set")
        val keyText = copyToString(publicKeyInputStream, defaultCharset()).trim()

        val keyBytes = encodedKeyFrom(keyText)

        val buffer = wrap(decode(keyBytes))
        val len = buffer.int
        val type = ByteArray(len)
        buffer.get(type)
        if ("ssh-rsa" == String(type)) {
            val e = decodeBigInt(buffer)
            val m = decodeBigInt(buffer)
            val spec = RSAPublicKeySpec(m, e)
            return KeyFactory.getInstance("RSA").generatePublic(spec)
        } else {
            throw IllegalArgumentException("Only supports RSA")
        }
    }

    private fun encodedKeyFrom(keyText: String) =
            if (keyText.startsWith("ssh-rsa")) keyText.split(" ")[1].toByteArray() else keyText.toByteArray()

    private fun decodeBigInt(bb: ByteBuffer): BigInteger {
        val len = bb.int
        val bytes = ByteArray(len)
        bb.get(bytes)
        return BigInteger(bytes)
    }

    override fun isAutoStartup(): Boolean {
        return this.port == PORT_AUTO
    }

    override fun getPhase(): Int {
        return Integer.MAX_VALUE
    }

    override fun start() {
        try {
            this.server.start()
            val serverPort = this.server.port
            this.sftpSessionFactory.setPort(serverPort)
            this.running = true
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }

    }

    override fun stop(callback: Runnable) {
        stop()
        callback.run()
    }

    override fun stop() {
        if (this.running) {
            try {
                server.stop(true)
            } catch (e: Exception) {
                throw IllegalStateException(e)
            } finally {
                this.running = false
            }
        }
    }

    override fun isRunning(): Boolean {
        return this.running
    }

}
