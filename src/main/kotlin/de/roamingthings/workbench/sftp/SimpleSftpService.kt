package de.roamingthings.workbench.sftp

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import de.roamingthings.workbench.sftp.configuration.SftpProperties
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.util.*

@Service
class SimpleSftpService(val sftpProperties: SftpProperties) {

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
                sftpProperties.userPrivateKeyPassphrase)

        val session = jsch.getSession(
                sftpProperties.user,
                sftpProperties.host,
                sftpProperties.port
        )

        session.setConfig(sessionConfiguration())
        return session
    }

    @Throws(FileNotFoundException::class)
    private fun knowHostsAbsolutePath(): String {
        return sftpProperties.knownHosts?.file?.absolutePath ?: throw FileNotFoundException()
    }

    @Throws(FileNotFoundException::class)
    private fun localPrivateKeyAbsolutePath(): String {
        return sftpProperties.userPrivateKey?.file?.absolutePath ?: throw FileNotFoundException()
    }

    private fun sessionConfiguration(): Properties {
        val prop = Properties()
        prop["StrictHostKeyChecking"] = sftpProperties.strictHostKeyChecking
        return prop
    }
}
