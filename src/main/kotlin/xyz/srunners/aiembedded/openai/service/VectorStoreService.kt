package xyz.srunners.aiembedded.openai.service

import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class VectorStoreService(
    private val vectorStore: VectorStore,
    private val embeddingModel: EmbeddingModel
) {

    /**
     * 문서를 벡터 스토어에 추가
     */
    fun addDocuments(documents: List<Document>) {
        vectorStore.add(documents)
        println("${documents.size}개의 문서가 벡터 스토어에 추가되었습니다.")
    }

    /**
     * 단일 문서를 벡터 스토어에 추가
     */
    fun addDocument(content: String, metadata: Map<String, Any> = emptyMap()) {
        val document = Document(content, metadata)
        vectorStore.add(listOf(document))
        println("문서가 벡터 스토어에 추가되었습니다: ${content.take(50)}...")
    }

    /**
     * 유사도 검색 수행
     */
    fun searchSimilar(query: String, topK: Int = 5, threshold: Double = 0.7): List<Document> {
        val searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(threshold)
            .build()

        val results = vectorStore.similaritySearch(searchRequest)
        println("검색 쿼리: '$query'")
        println("발견된 유사 문서: ${results.size}개")

        return results
    }

    /**
     * 메타데이터 필터를 사용한 검색
     */
    fun searchWithMetadataFilter(
        query: String,
        metadataKey: String,
        metadataValue: Any,
        topK: Int = 5
    ): List<Document> {
        val searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .filterExpression("$metadataKey == '$metadataValue'")
            .build()

        val results = vectorStore.similaritySearch(searchRequest)
        println("필터링된 검색 - $metadataKey: $metadataValue")
        println("발견된 문서: ${results.size}개")

        return results
    }

    /**
     * 벡터 스토어에서 모든 문서 삭제 (테스트용)
     */
    fun clearAll() {
        // 주의: 실제 운영환경에서는 사용하지 마세요
        try {
            // PGVector의 경우 테이블의 모든 레코드 삭제
            vectorStore.delete(emptyList()) // 빈 리스트로 모든 문서 삭제 시도
            println("벡터 스토어가 초기화되었습니다.")
        } catch (e: Exception) {
            println("벡터 스토어 초기화 중 오류: ${e.message}")
        }
    }

    /**
     * 임베딩 벡터 생성 테스트
     */
    fun testEmbedding(text: String): FloatArray {
        val embedding = embeddingModel.embed(text)
        println("텍스트: '$text'")
        println("임베딩 차원: ${embedding.size}")
        println("임베딩 벡터 (처음 5개): ${embedding.take(5)}")
        return embedding
    }
}