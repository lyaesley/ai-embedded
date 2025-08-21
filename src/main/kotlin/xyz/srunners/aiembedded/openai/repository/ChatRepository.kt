package xyz.srunners.aiembedded.openai.repository

import org.springframework.data.jpa.repository.JpaRepository
import xyz.srunners.aiembedded.openai.entity.ChatEntity


interface ChatRepository : JpaRepository<ChatEntity, Long> {
    fun findByUserIdOrderByCreatedAtAsc(userId: String): List<ChatEntity>
}