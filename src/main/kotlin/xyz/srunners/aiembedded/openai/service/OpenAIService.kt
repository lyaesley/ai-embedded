package xyz.srunners.aiembedded.openai.service

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt
import org.springframework.ai.chat.memory.ChatMemoryRepository
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.image.ImageGeneration
import org.springframework.ai.image.ImagePrompt
import org.springframework.ai.image.ImageResponse
import org.springframework.ai.openai.*
import org.springframework.ai.openai.api.OpenAiAudioApi
import org.springframework.ai.openai.api.OpenAiAudioApi.TranscriptResponseFormat
import org.springframework.ai.openai.audio.speech.SpeechPrompt
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import xyz.srunners.aiembedded.openai.entity.ChatEntity
import xyz.srunners.aiembedded.openai.repository.ChatRepository


@Service
class OpenAIService(
    private val openAiChatModel: OpenAiChatModel,
    private val openAiEmbeddingModel: OpenAiEmbeddingModel,
    private val openAiImageModel: OpenAiImageModel,
    private val openAiAudioSpeechModel: OpenAiAudioSpeechModel,
    private val openAiAudioTranscriptionModel: OpenAiAudioTranscriptionModel,
    private val chatMemoryRepository: ChatMemoryRepository,
    private val chatRepository: ChatRepository
) {

    // chatmodel: response
    fun generate(text: String): String? {

        //메시지
        val systemMessage = SystemMessage("")
        val userMessage = UserMessage(text)
        val assistantMessage = AssistantMessage("")

        val options = OpenAiChatOptions.builder()
            .model("gpt-4o-mini")
            .temperature(0.7)
            .build()

        // 프롬프트
        val prompt = Prompt(listOf(systemMessage, userMessage, assistantMessage), options)

        // 요청 및 응답
        val response = openAiChatModel.call(prompt)
        return response.result.output.text
    }

    // chatmodel: response stream
    fun generateStream(text: String): Flux<String?> {

        //유저&페이지별 ChatMemory를 관리하기 위한 key (POC 명시적으로)
        val userId = "disp" + "_" + "1"

        val chatUserEntity = ChatEntity(
            userId = userId,
            content = text,
            type = MessageType.USER
        )

        //ChatMemory 로드 메시지
        val chatMemory = MessageWindowChatMemory.builder()
            .maxMessages(6)
            .chatMemoryRepository(chatMemoryRepository)
            .build()

        // 신규 메시지 추가
        chatMemory.add(userId, UserMessage(text))

        val options = OpenAiChatOptions.builder()
            .model("gpt-4o-mini")
            .temperature(0.7)
            .build()

        // 프롬프트
        val prompt = Prompt(chatMemory.get(userId), options)

        // 응답 메시지를 저장할 임시 버퍼
        val responseBuffer = StringBuilder()

        // 요청 및 응답
        return openAiChatModel.stream(prompt)
            .mapNotNull {
                val token = it.result.output.text
                responseBuffer.append(token)
                token
            }
            .doOnComplete {
                chatMemory.add(userId, AssistantMessage(responseBuffer.toString()))
                chatMemoryRepository.saveAll(userId, chatMemory.get(userId))

                // 전체 대화 저장용
                val chatAssistantEntity = ChatEntity(
                    userId = userId,
                    content = responseBuffer.toString(),
                    type = MessageType.ASSISTANT
                )
                chatRepository.saveAll(listOf(chatUserEntity, chatAssistantEntity))
            }

    }

    // chatmodel: response stream
    fun generateStreamBackup(text: String): Flux<String> {

        val systemMessage = SystemMessage("")
        val userMessage = UserMessage(text)
        val assistantMessage = AssistantMessage("")

        val options = OpenAiChatOptions.builder()
            .model("gpt-4o-mini")
            .temperature(0.7)
            .build()

        // 프롬프트
        val prompt = Prompt(listOf(systemMessage, userMessage, assistantMessage), options)

        return openAiChatModel.stream(prompt).mapNotNull { it.result.output.text }
    }

    // Embedding api 호출 메서드
    fun generateEmbedding(texts: List<String>, model: String): List<FloatArray?> {

        // 옵션
        val embeddingOptions = OpenAiEmbeddingOptions.builder()
            .model(model)
            .build()

        // 프롬프트
        val request = EmbeddingRequest(texts, embeddingOptions)

        // 요청 및 응답
        val response = openAiEmbeddingModel.call(request)
        return response.results.stream()
            .map { it.output }
            .toList()
    }

    // 이미지 모델 api 호출 메서드
    fun generateImages(text: String?, count: Int, height: Int, width: Int): MutableList<String?> {

        // 옵션
        val imageOptions = OpenAiImageOptions.builder()
            .quality("hd")
            .N(count)
            .height(height)
            .width(width)
            .build()

        // 프롬프트
        val prompt = ImagePrompt(text, imageOptions)

        // 요청 및 응답
        val response: ImageResponse = openAiImageModel.call(prompt)
        return response.results.stream()
            .map { image: ImageGeneration? -> image!!.output.url }
            .toList()
    }

    // TTS
    fun tts(text: String?): ByteArray? {
        // 옵션
        val speechOptions = OpenAiAudioSpeechOptions.builder()
            .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
            .speed(1.0f)
            .model(OpenAiAudioApi.TtsModel.TTS_1.value)
            .build()

        // 프롬프트
        val prompt = SpeechPrompt(text, speechOptions)

        // 요청 및 응답
        val response = openAiAudioSpeechModel.call(prompt)
        return response.result.output
    }

    // STT
    fun stt(audioFile: Resource?): String {
        // 옵션
        val responseFormat = TranscriptResponseFormat.VTT
        val transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
            .language("ko") // 인식할 언어
            .prompt("Ask not this, but ask that") // 음성 인식 전 참고할 텍스트 프롬프트
            .temperature(0f)
            .model(OpenAiAudioApi.TtsModel.TTS_1.value)
            .responseFormat(responseFormat) // 결과 타입 지정 VTT 자막형식
            .build()

        // 프롬프트
        val prompt = AudioTranscriptionPrompt(audioFile, transcriptionOptions)

        // 요청 및 응답
        val response = openAiAudioTranscriptionModel.call(prompt)
        return response.result.output
    }
}