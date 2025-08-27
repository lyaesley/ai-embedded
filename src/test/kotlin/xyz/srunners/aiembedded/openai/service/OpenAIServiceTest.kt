package xyz.srunners.aiembedded.openai.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile

@SpringBootTest
@Profile("dev")
class OpenAIServiceTest {

    @Autowired
    private lateinit var openAIService: OpenAIService

    @Test
    fun 챗봇_동기호출() {
        val generate = openAIService.generate("이벤트응모 방법 알려줘")
        println(generate)
    }
}