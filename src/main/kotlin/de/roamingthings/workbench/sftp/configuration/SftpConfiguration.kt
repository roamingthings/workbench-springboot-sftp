package de.roamingthings.workbench.sftp.configuration

import com.jcraft.jsch.ChannelSftp.LsEntry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.InboundChannelAdapter
import org.springframework.integration.annotation.Poller
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.annotation.Transformer
import org.springframework.integration.file.remote.session.CachingSessionFactory
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.jdbc.lock.DefaultLockRepository
import org.springframework.integration.jdbc.lock.JdbcLockRegistry
import org.springframework.integration.jdbc.lock.LockRepository
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter
import org.springframework.integration.sftp.inbound.SftpStreamingMessageSource
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate
import org.springframework.integration.support.leader.LockRegistryLeaderInitiator
import org.springframework.integration.support.locks.LockRegistry
import org.springframework.integration.transformer.StreamTransformer
import org.springframework.messaging.MessageHandler
import javax.sql.DataSource


@Configuration
class SftpConfiguration(private val sftpProperties: SftpProperties) {

    @Bean
    fun lockRepository(dataSource: DataSource): DefaultLockRepository {
        return DefaultLockRepository(dataSource)
    }

    @Bean
    fun lockRegistry(lockRepository: LockRepository): JdbcLockRegistry {
        return JdbcLockRegistry(lockRepository)
    }

    @Bean
    fun leaderInitiator(lockRegistry: LockRegistry): LockRegistryLeaderInitiator {
        return LockRegistryLeaderInitiator(lockRegistry)
    }

    @Bean
    @Transformer(inputChannel = "sftpChannel", outputChannel = "data")
    fun transformer(): org.springframework.integration.transformer.Transformer {
        return StreamTransformer("UTF-8")
    }

    @Bean
    @ServiceActivator(inputChannel = "data")
    fun handlerContent(store: JdbcMetadataStore): MessageHandler {
        return MessageHandler { message ->
            val remoteDirectory = message.headers["file_remoteDirectory"] as String
            println(remoteDirectory)
            val remoteFile = message.headers["file_remoteFile"] as String
            println(remoteFile)
            println(message.payload.toString())
//            if (payload is File) {
//                println(payload.absolutePath)
//                println(payload.readText())
//                payload.delete()
//            }
        }
    }

    @Bean
    fun metadataStore(dataSource: DataSource): JdbcMetadataStore {
        return JdbcMetadataStore(dataSource)
    }

    @Bean
    @InboundChannelAdapter(
            autoStartup = "true",
            channel = "sftpChannel",
            poller = [Poller(fixedRate = "1000", maxMessagesPerPoll = "1")]
    )
    fun sftpMessageSource(metadataStore: JdbcMetadataStore): SftpStreamingMessageSource {

        val messageSource = SftpStreamingMessageSource(
                SftpRemoteFileTemplate(sftpSessionFactory()),
                Comparator.comparing(LsEntry::getFilename)
        )
        messageSource.setRemoteDirectory("share/")
        messageSource.setFilter(SftpRegexPatternFileListFilter("^.*\\.(txt|TXT)\$"))
        messageSource.maxFetchSize = 1
        messageSource.isCountsEnabled = true
        return messageSource
    }

    /*
        @Bean
        @InboundChannelAdapter(
                autoStartup = "true",
                channel = "sftpChannel",
                poller = [Poller(fixedRate = "1000", maxMessagesPerPoll = "1")]
        )
        fun sftpMessageSource(metadataStore: JdbcMetadataStore): SftpInboundFileSynchronizingMessageSource {
            val source = SftpInboundFileSynchronizingMessageSource(sftpInboundFileSynchronizer())
            source.setLocalDirectory(File("ftp-inbound"))
            source.setAutoCreateLocalDirectory(true)
            val local = FileSystemPersistentAcceptOnceFileListFilter(metadataStore, "test-prefix")
            source.setLocalFilter(local)
            source.isCountsEnabled = true
            return source
        }

    private fun sftpInboundFileSynchronizer(): AbstractInboundFileSynchronizer<LsEntry>? {
        val cachingSessionFactory = CachingSessionFactory(sftpSessionFactory())

        // synchronizer config
        val sftpSynchronizer = SftpInboundFileSynchronizer(cachingSessionFactory)
        sftpSynchronizer.setRemoteDirectory(sftpProperties.remoteDirectory)
        // remote filter
        sftpSynchronizer.setFilter(SftpRegexPatternFileListFilter("^.*\\.(txt|TXT)\$"))
        sftpSynchronizer.setPreserveTimestamp(true)
        sftpSynchronizer.setDeleteRemoteFiles(false)
        return sftpSynchronizer
    }
    */

    @Bean
    fun sftpSessionFactory(): SessionFactory<LsEntry> {
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
        return CachingSessionFactory(factory)
    }
}
