package xyz.srunners.aiembedded.openai.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import xyz.srunners.aiembedded.openai.service.ChatService
import xyz.srunners.aiembedded.openai.service.OpenAIService

@Controller
class ChatController(
    private val openAiService: OpenAIService,
    private val chatService: ChatService
) {

    // 채팅 페이지 렌더링
    @GetMapping("/chat")
    fun chatPage(): String {
        return "chat"
    }

    @ResponseBody
    @PostMapping("/chat")
    fun chat(@RequestBody body: Map<String, String>) = openAiService.generate(body.getValue("text"))

    @ResponseBody
    @PostMapping("/chat/stream")
    fun chatStream(@RequestBody body: Map<String, String>) = openAiService.generateStream(body.getValue("text"))

    @ResponseBody
    @PostMapping("/chat/history/{userId}")
    fun getChatHistory(@PathVariable userId: String) = chatService.readAllChats(userId)
}