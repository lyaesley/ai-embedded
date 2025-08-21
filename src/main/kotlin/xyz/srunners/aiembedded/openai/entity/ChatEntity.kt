package xyz.srunners.aiembedded.openai.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.springframework.ai.chat.messages.MessageType
import java.time.LocalDateTime


@Entity
@Table(name = "spring_ai_chat_history")
data class ChatEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id")
    var userId: String? = null,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @Enumerated(EnumType.STRING)
    var type: MessageType? = null,

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    var createdAt: LocalDateTime? = null
)