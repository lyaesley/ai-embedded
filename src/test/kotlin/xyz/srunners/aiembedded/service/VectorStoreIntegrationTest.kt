package xyz.srunners.aiembedded.service

import org.junit.jupiter.api.*
import org.springframework.ai.document.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import xyz.srunners.aiembedded.openai.service.VectorStoreService

@SpringBootTest
@TestPropertySource(properties = [
    "logging.level.org.springframework.ai=DEBUG",
    "spring.ai.openai.api-key=\${OPENAI_API_KEY:test-key}"
])
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class VectorStoreIntegrationTest {

    @Autowired
    private lateinit var vectorStoreService: VectorStoreService

    companion object {
        // 테스트용 샘플 문서들
        val sampleDocuments = listOf(
            Document(
                "Spring Boot는 Java 기반의 웹 애플리케이션을 쉽게 개발할 수 있는 프레임워크입니다.",
                mapOf("category" to "spring", "type" to "framework", "language" to "java")
            ),
            Document(
                "Kotlin은 JetBrains에서 개발한 정적 타입 프로그래밍 언어입니다.",
                mapOf("category" to "language", "type" to "programming", "language" to "kotlin")
            ),
            Document(
                "PostgreSQL은 오픈소스 관계형 데이터베이스 관리 시스템입니다.",
                mapOf("category" to "database", "type" to "sql", "language" to "sql")
            ),
            Document(
                "Vector 데이터베이스는 AI와 머신러닝에서 유사도 검색을 위해 사용됩니다.",
                mapOf("category" to "ai", "type" to "database", "language" to "general")
            ),
            Document(
                "Spring AI는 Spring 생태계에서 AI 기능을 통합하는 프레임워크입니다.",
                mapOf("category" to "spring", "type" to "ai", "language" to "java")
            )
        )
    }

    @Test
    @Order(1)
    fun `01_임베딩 모델 테스트`() {
        val testText = "Spring Boot 테스트"
        val embedding = vectorStoreService.testEmbedding(testText)

        Assertions.assertNotNull(embedding)
        Assertions.assertTrue(embedding.isNotEmpty())
        Assertions.assertEquals(1536, embedding.size) // OpenAI text-embedding-3-small 차원

        println("✅ 임베딩 모델 테스트 성공")
    }

    @Test
    @Order(2)
    fun `02_문서 추가 테스트`() {
        // 기존 데이터 정리
        try {
            vectorStoreService.clearAll()
        } catch (e: Exception) {
            println("기존 데이터 정리 중 오류 (무시): ${e.message}")
        }

        // 샘플 문서들을 벡터 스토어에 추가
        vectorStoreService.addDocuments(sampleDocuments)

        println("✅ 문서 추가 테스트 성공")
    }

    @Test
    @Order(3)
    fun `03_유사도 검색 테스트`() {
        val query = "Spring 프레임워크에 대해 알려주세요"
        val results = vectorStoreService.searchSimilar(query, topK = 3)

        Assertions.assertTrue(results.isNotEmpty(), "검색 결과가 비어있습니다")
        Assertions.assertTrue(results.size <= 3, "검색 결과가 topK보다 많습니다")

        println("검색 결과:")
        results.forEachIndexed { index, doc ->
            println("${index + 1}. ${doc.text}")
            println("   메타데이터: ${doc.metadata}")
            println()
        }

        println("✅ 유사도 검색 테스트 성공")
    }

    @Test
    @Order(4)
    fun `04_메타데이터 필터 검색 테스트`() {
        val query = "프로그래밍"
        val results = vectorStoreService.searchWithMetadataFilter(
            query = query,
            metadataKey = "category",
            metadataValue = "language",
            topK = 2
        )

        println("필터링된 검색 결과:")
        results.forEach { doc ->
            println("- ${doc.text}")
            println("  메타데이터: ${doc.metadata}")

            // 메타데이터 필터가 제대로 작동하는지 확인
            Assertions.assertEquals("language", doc.metadata["category"])
        }

        println("✅ 메타데이터 필터 검색 테스트 성공")
    }

    @Test
    @Order(5)
    fun `05_다양한_쿼리_검색_테스트`() {
        val queries = listOf(
            "데이터베이스 관리",
            "AI 머신러닝",
            "Java 개발",
            "Kotlin 프로그래밍"
        )

        queries.forEach { query ->
            println("\n=== 쿼리: '$query' ===")
            val results = vectorStoreService.searchSimilar(query, topK = 2, threshold = 0.5)

            if (results.isNotEmpty()) {
                results.forEachIndexed { index, doc ->
                    println("${index + 1}. ${doc.text?.take(50)}...")
                    println("   카테고리: ${doc.metadata["category"]}")
                }
            } else {
                println("검색 결과가 없습니다.")
            }
        }

        println("\n✅ 다양한 쿼리 검색 테스트 성공")
    }

    @Test
    @Order(6)
    fun `06_단일_문서_추가_및_검색_테스트`() {
        val newContent = "IntelliJ IDEA는 JetBrains에서 개발한 통합 개발 환경입니다."
        val newMetadata = mapOf(
            "category" to "tool",
            "type" to "ide",
            "language" to "multi"
        )

        // 새 문서 추가
        vectorStoreService.addDocument(newContent, newMetadata)

        // 잠시 대기 (인덱싱 완료를 위해)
        Thread.sleep(1000)

        // 추가된 문서 검색
        val results = vectorStoreService.searchSimilar("개발 도구", topK = 5, threshold = 0.3)

        val foundNewDocument = results.any { doc ->
            doc.text!!.contains("IntelliJ IDEA")
        }

        println("검색된 문서들:")
        results.forEach { doc ->
            println("- ${doc.text}")
        }

        Assertions.assertTrue(foundNewDocument, "새로 추가된 문서를 찾을 수 없습니다")

        println("✅ 단일 문서 추가 및 검색 테스트 성공")
    }

    @Test
    @Order(7)
    fun `07_유사도_임계값_테스트`() {
        val query = "완전히 관련 없는 내용 - 요리 레시피 만들기"

        // 높은 임계값으로 검색 (관련성이 높은 문서만)
        val highThresholdResults = vectorStoreService.searchSimilar(
            query, topK = 5, threshold = 0.8
        )

        // 낮은 임계값으로 검색 (관련성이 낮아도 포함)
        val lowThresholdResults = vectorStoreService.searchSimilar(
            query, topK = 5, threshold = 0.3
        )

        println("높은 임계값(0.8) 결과: ${highThresholdResults.size}개")
        println("낮은 임계값(0.3) 결과: ${lowThresholdResults.size}개")

        // 높은 임계값일 때 더 적거나 같은 수의 결과가 나와야 함
        Assertions.assertTrue(
            highThresholdResults.size <= lowThresholdResults.size,
            "임계값 필터링이 제대로 작동하지 않습니다. 높은 임계값: ${highThresholdResults.size}, 낮은 임계값: ${lowThresholdResults.size}"
        )

        println("✅ 유사도 임계값 테스트 성공")
    }
}