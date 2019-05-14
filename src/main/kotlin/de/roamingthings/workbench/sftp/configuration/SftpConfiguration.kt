package de.roamingthings.workbench.sftp.configuration

import com.jcraft.jsch.ChannelSftp.LsEntry
import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.integration.annotation.InboundChannelAdapter
import org.springframework.integration.annotation.Poller
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.annotation.Transformer
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.file.filters.CompositeFileListFilter
import org.springframework.integration.file.filters.FileListFilter
import org.springframework.integration.file.remote.RemoteFileTemplate
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter
import org.springframework.integration.sftp.inbound.SftpStreamingMessageSource
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate
import org.springframework.integration.transaction.DefaultTransactionSynchronizationFactory
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor
import org.springframework.integration.transaction.PseudoTransactionManager
import org.springframework.integration.transaction.TransactionSynchronizationFactory
import org.springframework.integration.transformer.StreamTransformer
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.support.ErrorMessage
import java.util.regex.Pattern
import javax.sql.DataSource


val log = getLogger("MessageHandler")

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
        messageSource.setRemoteDirectory("share/foos")
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
    fun template(): RemoteFileTemplate<LsEntry>? {
        return SftpRemoteFileTemplate(cachedSessionFactory);
    }

    @Bean
    fun acceptOnceFilter(): SftpPersistentAcceptOnceFileListFilter {
        return SftpPersistentAcceptOnceFileListFilter(
                metadataStore(),
                "streaming-")
    }

    @Bean
    fun metadataStore(): JdbcMetadataStore {
        return JdbcMetadataStore(dataSource)
    }

    @Bean
    fun sftpChannel() = DirectChannel()

    @Bean
    fun dataChannel() = DirectChannel()

    @Bean
    @Transformer(inputChannel = "sftpChannel", outputChannel = "dataChannel")
    fun transformer(): org.springframework.integration.transformer.Transformer {
        return StreamTransformer("UTF-8")
    }

    @Bean
    fun transactionManager() = PseudoTransactionManager()

    @Bean
    fun transactionSynchronizationFactory(applicationContext: ApplicationContext): TransactionSynchronizationFactory {
        val parser = SpelExpressionParser()

        val syncProcessor = ExpressionEvaluatingTransactionSynchronizationProcessor()
        syncProcessor.setBeanFactory(applicationContext.autowireCapableBeanFactory);
//    syncProcessor.setAfterCommitExpression(parser.parseExpression(
//            "payload.renameTo(new java.io.File('test/archive' " +
//                    " + T(java.io.File).separator + 'ARCHIVE-' + payload.name))"));
        syncProcessor.setAfterRollbackExpression(parser.parseExpression(
                "payload.delete()"));
        return DefaultTransactionSynchronizationFactory(syncProcessor);
    }

    @Bean
    @ServiceActivator(inputChannel = "dataChannel", adviceChain = ["afterAdvice"])
    fun handlerContent(store: JdbcMetadataStore): MessageHandler {
        return MessageHandler { message ->
            val remoteDirectory = message.headers["file_remoteDirectory"] as String
            val remoteFile = message.headers["file_remoteFile"] as String
            log.info("\nProcessing <$remoteDirectory/$remoteFile>\n${message.payload}")

            if (message.payload.toString().contains("exception")) {
                throw IllegalStateException("Boooom")
            }
        }
    }

    @Bean
    fun afterAdvice(): ExpressionEvaluatingRequestHandlerAdvice {
        val advice = ExpressionEvaluatingRequestHandlerAdvice()
        advice.setSuccessChannelName("successChannel")
        advice.setOnSuccessExpressionString(
                "@template.remove(headers['file_remoteDirectory'] + '/' + headers['file_remoteFile'])")

        advice.setFailureChannelName("failureChannel")
//        advice.setOnFailureExpressionString(
//                "@template.rename(headers['file_remoteDirectory'] + '/' + headers['file_remoteFile'], headers['file_remoteDirectory'] + '/' + headers['file_remoteFile'] + '.failed')")

        advice.setPropagateEvaluationFailures(true)
        // Exception will not be logged
//        advice.setTrapException(true)
        return advice
    }

    // https://github.com/spring-projects/spring-integration/blob/master/src/reference/asciidoc/handler-advice.adoc#expression-advice
    @Bean
    @ServiceActivator(inputChannel = "successChannel")
    fun handlerSuccess(): MessageHandler {
        return MessageHandler { message ->
            log.info("Success!")
        }
    }

    @Bean
    @ServiceActivator(inputChannel = "failureChannel")
    fun handlerFailure(store: JdbcMetadataStore, sftpSessionFactory: DefaultSftpSessionFactory): MessageHandler {
        return MessageHandler { message ->
            log.error("Failure!")
            if (message is ErrorMessage) {
                val adviceException = message.payload as ExpressionEvaluatingRequestHandlerAdvice.MessageHandlingExpressionEvaluatingAdviceException
                val failedMessage = adviceException.failedMessage
                val remoteDirectory = failedMessage?.headers?.get("file_remoteDirectory") as String?
                val remoteFile = failedMessage?.headers?.get("file_remoteFile") as String?
                if (remoteFile != null && remoteDirectory != null) {
                    sftpSessionFactory.session.rename("$remoteDirectory/$remoteFile", "$remoteDirectory/$remoteFile.failed")
                    store.remove("streaming-$remoteFile")
                }
            }
        }
    }
}
