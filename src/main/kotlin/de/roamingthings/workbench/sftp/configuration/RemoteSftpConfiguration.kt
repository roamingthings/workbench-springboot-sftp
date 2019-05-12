package de.roamingthings.workbench.sftp.configuration;

import com.jcraft.jsch.ChannelSftp.LsEntry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.file.remote.session.CachingSessionFactory
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory

@Configuration
class RemoteSftpConfiguration(private val sftpProperties: SftpProperties) {
    @Bean
    fun sftpSessionFactory(): DefaultSftpSessionFactory {
        val factory = DefaultSftpSessionFactory(true)
        factory.setHost(sftpProperties.host)
        factory.setPort(sftpProperties.port)
        factory.setUser(sftpProperties.user)

        if (sftpProperties.userPrivateKey != null) {
            factory.setPrivateKey(sftpProperties.userPrivateKey)
            factory.setPrivateKeyPassphrase(sftpProperties.userPrivateKeyPassphrase)
        } else {
            factory.setPassword(sftpProperties.password)
        }
        sftpProperties.knownHosts?.let {
            val absolutePath = it.file.absolutePath
            factory.setKnownHosts(absolutePath)
            factory.setAllowUnknownKeys(false)
        } ?: run {
            factory.setAllowUnknownKeys(true)
        }
        return factory
    }

    @Bean
    fun cachedSessionFactory(): SessionFactory<LsEntry> {
        return CachingSessionFactory(sftpSessionFactory())
    }
}
