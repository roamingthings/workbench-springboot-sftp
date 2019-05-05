package de.roamingthings.workbench.sftp

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import de.roamingthings.workbench.sftp.configuration.SftpConfiguration
import org.springframework.stereotype.Service
import org.springframework.util.ResourceUtils
import java.io.FileNotFoundException
import java.util.*

@Service
class SimpleSftpService(val sftpConfiguration: SftpConfiguration) {

    fun retrieveRemoteFolderList(): Vector<*> {

        try {

            val session = createSession()
            session.connect()

            val channel = openSftpChannel(session)

            val ls = channel.ls("/share")

            channel.disconnect()
            session.disconnect()

            return ls
        } catch (e: Exception) {
            throw IllegalStateException("Could not load", e)
        }
    }

    @Throws(JSchException::class)
    private fun openSftpChannel(session: Session): ChannelSftp {
        val channel = session.openChannel("sftp")
        channel.connect()
        return channel as ChannelSftp
    }

    @Throws(JSchException::class, FileNotFoundException::class)
    private fun createSession(): Session {
        val jsch = JSch()

        jsch.setKnownHosts(knowHostsAbsolutePath())

        jsch.addIdentity(
                localPrivateKeyAbsolutePath(),
                sftpConfiguration.userPrivateKeyPassphrase)

        val session = jsch.getSession(
                sftpConfiguration.username,
                sftpConfiguration.remoteHost,
                sftpConfiguration.remotePort
        )

        session.setConfig(sessionConfiguration())
        return session
    }

    @Throws(FileNotFoundException::class)
    private fun knowHostsAbsolutePath(): String {
        return ResourceUtils.getFile(sftpConfiguration.keysFolder + "/known_hosts").absolutePath
    }

    @Throws(FileNotFoundException::class)
    private fun localPrivateKeyAbsolutePath(): String {
        return ResourceUtils.getFile(sftpConfiguration.keysFolder + sftpConfiguration.userPrivateKey).absolutePath
    }

    private fun sessionConfiguration(): Properties {
        val prop = Properties()
        prop["StrictHostKeyChecking"] = sftpConfiguration.strictHostKeyChecking
        return prop
    }
}
