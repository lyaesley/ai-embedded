package xyz.srunners.aiembedded.openai.config

import org.springframework.ai.chat.memory.ChatMemoryRepository
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class AIConfig {

    @Bean
    fun chatMemoryRepository(jdbcTemplate: JdbcTemplate, transactionManager: PlatformTransactionManager): ChatMemoryRepository {
        return JdbcChatMemoryRepository.builder()
            .jdbcTemplate(jdbcTemplate)
            .transactionManager(transactionManager)
            .build();
    }
}