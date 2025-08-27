package xyz.srunners.aiembedded.openai.service

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor
import org.springframework.ai.chat.memory.ChatMemoryRepository
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.image.ImageGeneration
import org.springframework.ai.image.ImagePrompt
import org.springframework.ai.image.ImageResponse
import org.springframework.ai.openai.*
import org.springframework.ai.openai.api.OpenAiAudioApi
import org.springframework.ai.openai.api.OpenAiAudioApi.TranscriptResponseFormat
import org.springframework.ai.openai.audio.speech.SpeechPrompt
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.pgvector.PgVectorStore
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import xyz.srunners.aiembedded.openai.dto.CityResponseDTO
import xyz.srunners.aiembedded.openai.entity.ChatEntity
import xyz.srunners.aiembedded.openai.repository.ChatRepository
import xyz.srunners.aiembedded.openai.template.AIPromptTemplate
import xyz.srunners.aiembedded.openai.tools.ChatTools
import java.util.function.Function


@Service
class OpenAIService(
    private val openAiChatModel: OpenAiChatModel,
    private val openAiEmbeddingModel: OpenAiEmbeddingModel,
    private val openAiImageModel: OpenAiImageModel,
    private val openAiAudioSpeechModel: OpenAiAudioSpeechModel,
    private val openAiAudioTranscriptionModel: OpenAiAudioTranscriptionModel,
    private val chatMemoryRepository: ChatMemoryRepository,
    private val chatRepository: ChatRepository,
    private val vectorStore: PgVectorStore
) {

    // chatmodel: response
    fun generate(text: String): String?{

        //유저&페이지별 ChatMemory를 관리하기 위한 key (POC 명시적으로)
        val userId = "disp" + "_" + "1"

        // 먼저 RAG 검색을 수행하여 메타데이터 정보 확보
        val ragDocuments = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(text)
                .similarityThreshold(0.3)
                .topK(6)
                .build()
        )

        // 메타데이터 정보를 포함한 사용자 메시지 생성
        val sourcesInfo = ragDocuments.mapIndexed { index, doc ->
            "참고문서${index + 1}: ${doc.metadata["title"]} (${doc.metadata["section"]})"
        }.joinToString(", ")

        val enhancedText = if (ragDocuments.isNotEmpty()) {
            "$text\n\n[출처: $sourcesInfo]"
        } else {
            text
        }


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

        // 로깅
        val loggerAdvisor = SimpleLoggerAdvisor(
            { request: ChatClientRequest? -> "Custom request: " + request!!.prompt().getUserMessage() },
            { response: ChatResponse? -> "Custom response: " + response!!.getResult() },
            0
        )

        // 챗메모리
        val chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
            .conversationId(userId)
            .build()
        // RAG
        // text -> 임베딩
        // 임베딩 -> DB 에서 조회 n 추출
        // 문서를 프롬프트에 붙여서

        val ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder().similarityThreshold(0.3).topK(6).build())
            .promptTemplate(AIPromptTemplate.QUESTION_ANSWER.createPromptTemplate())
            .build()

        val chatClient = ChatClient.builder(openAiChatModel)
            .defaultAdvisors(chatMemoryAdvisor, ragAdvisor, loggerAdvisor)
            .build()

        chatMemoryRepository.saveAll(userId, chatMemory.get(userId))

        val chatResponse = chatClient.prompt()
            .tools(ChatTools())
            .user(enhancedText)
            .call()
            .chatResponse()

        val content = chatResponse?.result?.output?.let { it.text!! }
        // 전체 대화 저장용
        val chatAssistantEntity = ChatEntity(
            userId = userId,
            content = content,
            type = MessageType.ASSISTANT
        )
        chatRepository.saveAll(listOf(chatUserEntity, chatAssistantEntity))

        return content
    }

    // chatmodel: response stream 멀티턴&히스토리
    fun generateStream(text: String): Flux<String?> {

        //유저&페이지별 ChatMemory를 관리하기 위한 key (POC 명시적으로)
        val userId = "disp" + "_" + "1"

        // 먼저 RAG 검색을 수행하여 메타데이터 정보 확보
        val ragDocuments = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(text)
                .similarityThreshold(0.3)
                .topK(6)
                .build()
        )

        // 메타데이터 정보를 포함한 사용자 메시지 생성
        val sourcesInfo = ragDocuments.mapIndexed { index, doc ->
            "참고문서${index + 1}: ${doc.metadata["docId"]} (${doc.metadata["page_number"]})"
        }.joinToString(", ")

        val enhancedText = if (ragDocuments.isNotEmpty()) {
            "$text\n\n[출처: $sourcesInfo]"
        } else {
            text
        }

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
//        chatMemory.add(userId, UserMessage(text))

        // 로깅
        val loggerAdvisor = SimpleLoggerAdvisor(
            { request: ChatClientRequest? -> "Custom request: " + request!!.prompt().getUserMessage() },
            { response: ChatResponse? -> "Custom response: " + response!!.getResult() },
            0
        )

        // 챗메모리
        val chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
            .conversationId(userId)
            .build()
        // RAG
        // text -> 임베딩
        // 임베딩 -> DB 에서 조회 n 추출
        // 문서를 프롬프트에 붙여서

        val ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder().similarityThreshold(0.3).topK(6).build())
            .promptTemplate(AIPromptTemplate.QUESTION_ANSWER.createPromptTemplate())
            .build()

        val chatClient = ChatClient.builder(openAiChatModel)
            .defaultAdvisors(chatMemoryAdvisor, ragAdvisor, loggerAdvisor)
            .build()

        // 응답 메시지를 저장할 임시 버퍼
        val responseBuffer = StringBuilder()

        return chatClient.prompt()
            .tools(ChatTools())
            .user(enhancedText)
            .stream()
            .content()
            .mapNotNull {
                responseBuffer.append(it)
                it
            }
            .doOnComplete {
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

    // chatmodel: response stream 멀티턴&히스토리
    fun generateStreamWithChatModel(text: String): Flux<String?> {

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
                if (!token.isNullOrEmpty()) {
                    responseBuffer.append(token)
                    token
                } else {
                    null
                }

            }
            .doOnComplete {
                // responseBuffer의 내용을 정리 (혹시 남아있을 수 있는 null 문자열 제거)
                val cleanContent = responseBuffer.toString()
                    .replace("null", "") // "null" 문자열 제거
                    .trim() // 앞뒤 공백 제거

                chatMemory.add(userId, AssistantMessage(cleanContent))
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
    fun generateStreamBase(text: String): Flux<String> {

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