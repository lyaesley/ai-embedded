package xyz.srunners.aiembedded.openai.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import xyz.srunners.aiembedded.openai.service.VectorStoreService

data class AddDocumentRequest(
    val content: String,
    val metadata: Map<String, Any> = emptyMap()
)

data class SearchRequest(
    val query: String,
    val topK: Int = 5,
    val threshold: Double = 0.7
)

data class SearchResponse(
    val query: String,
    val results: List<DocumentResult>
)

data class DocumentResult(
    val content: String,
    val metadata: Map<String, Any>,
    val score: Double? = null
)

@RestController
@RequestMapping("/api/vector-store")
class VectorStoreController(
    private val vectorStoreService: VectorStoreService
) {

    /**
     * 문서 추가 API
     */
    @PostMapping("/documents")
    fun addDocument(@RequestBody request: AddDocumentRequest): ResponseEntity<String> {
        return try {
            vectorStoreService.addDocument(request.content, request.metadata)
            ResponseEntity.ok("문서가 성공적으로 추가되었습니다.")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("문서 추가 실패: ${e.message}")
        }
    }

    /**
     * 유사도 검색 API
     */
    @PostMapping("/search")
    fun search(@RequestBody request: SearchRequest): ResponseEntity<SearchResponse> {
        return try {
            val results = vectorStoreService.searchSimilar(
                request.query,
                request.topK,
                request.threshold
            )

            val response = SearchResponse(
                query = request.query,
                results = results.map { doc ->
                    DocumentResult(
                        content = doc.text!!,
                        metadata = doc.metadata
                    )
                }
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                SearchResponse(
                    query = request.query,
                    results = emptyList()
                )
            )
        }
    }

    /**
     * 간단한 검색 API (GET 방식)
     */
    @GetMapping("/search")
    fun simpleSearch(
        @RequestParam query: String,
        @RequestParam(defaultValue = "5") topK: Int,
        @RequestParam(defaultValue = "0.7") threshold: Double
    ): ResponseEntity<SearchResponse> {
        return search(SearchRequest(query, topK, threshold))
    }

    /**
     * 임베딩 테스트 API
     */
    @PostMapping("/test-embedding")
    fun testEmbedding(@RequestBody request: Map<String, String>): ResponseEntity<Map<String, Any>> {
        val text = request["text"] ?: return ResponseEntity.badRequest().body(
            mapOf("error" to "text 필드가 필요합니다.")
        )

        return try {
            val embedding = vectorStoreService.testEmbedding(text)
            ResponseEntity.ok(mapOf(
                "text" to text,
                "dimensions" to embedding.size,
                "embedding_preview" to embedding.take(10)
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf("error" to "임베딩 생성 실패: ${e.message}")
            )
        }
    }

    /**
     * 헬스 체크 API
     */
    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "service" to "Vector Store API",
            "timestamp" to System.currentTimeMillis().toString()
        ))
    }
}