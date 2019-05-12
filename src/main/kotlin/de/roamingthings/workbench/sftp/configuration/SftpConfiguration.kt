package de.roamingthings.workbench.sftp.configuration

import com.jcraft.jsch.ChannelSftp.LsEntry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.InboundChannelAdapter
import org.springframework.integration.annotation.Poller
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.annotation.Transformer
import org.springframework.integration.file.filters.CompositeFileListFilter
import org.springframework.integration.file.filters.FileListFilter
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore
import org.springframework.integration.metadata.ConcurrentMetadataStore
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter
import org.springframework.integration.sftp.inbound.SftpStreamingMessageSource
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate
import org.springframework.integration.transformer.StreamTransformer
import org.springframework.messaging.MessageHandler
import java.util.regex.Pattern
import javax.sql.DataSource


@Configuration
class SftpConfiguration(
        private val dataSource: DataSource,
        private val cachedSessionFactory: SessionFactory<LsEntry>
) {

    @Bean
    @InboundChannelAdapter(
            autoStartup = "true",
            channel = "sftpChannel",
            poller = [Poller(fixedRate = "1000", maxMessagesPerPoll = "1")]
    )
    fun sftpMessageSource(): SftpStreamingMessageSource {

        val messageSource = SftpStreamingMessageSource(
                template(),
                Comparator.comparing(LsEntry::getFilename)
        )
        messageSource.setRemoteDirectory("foos")
        messageSource.setFilter(filter())
        messageSource.maxFetchSize = 1
        messageSource.isCountsEnabled = true
        return messageSource
    }

    fun filter(): FileListFilter<LsEntry> {
        val filter = CompositeFileListFilter<LsEntry>()
        val pattern = Pattern.compile("(?i)^.*\\.txt$")
        filter.addFilter(SftpRegexPatternFileListFilter(pattern))
        filter.addFilter(acceptOnceFilter())
        return filter
    }

    @Bean
    fun template(): SftpRemoteFileTemplate {
        return SftpRemoteFileTemplate(cachedSessionFactory);
    }

    @Bean
    fun acceptOnceFilter(): SftpPersistentAcceptOnceFileListFilter {
        return SftpPersistentAcceptOnceFileListFilter(
                metadataStore(),
                "streaming-")
    }

    @Bean
    fun metadataStore(): ConcurrentMetadataStore {
        return JdbcMetadataStore(dataSource)
    }


    @Bean
    @Transformer(inputChannel = "sftpChannel", outputChannel = "data")
    fun transformer(): org.springframework.integration.transformer.Transformer {
        return StreamTransformer("UTF-8")
    }

    @Bean
    @ServiceActivator(inputChannel = "data")
    fun handlerContent(store: ConcurrentMetadataStore): MessageHandler {
        return MessageHandler { message ->
            val remoteDirectory = message.headers["file_remoteDirectory"] as String
            val remoteFile = message.headers["file_remoteFile"] as String
            println("Processing <$remoteDirectory/$remoteFile>")
            println(message.payload.toString())
        }
    }
}
