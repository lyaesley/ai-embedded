package xyz.srunners.aiembedded.openai.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.srunners.aiembedded.openai.entity.ChatEntity
import xyz.srunners.aiembedded.openai.repository.ChatRepository

@Service
class ChatService(
    private val chatRepository: ChatRepository
) {

    @Transactional(readOnly = true)
    fun readAllChats(userId: String): List<ChatEntity> = chatRepository.findByUserIdOrderByCreatedAtAsc(userId)
}