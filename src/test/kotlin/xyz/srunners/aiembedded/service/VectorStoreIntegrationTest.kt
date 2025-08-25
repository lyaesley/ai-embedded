package xyz.srunners.aiembedded.service

import org.junit.jupiter.api.*
import org.springframework.ai.document.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
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

    @Test
    @Order(8)
    fun `08_파일_업로드_및_벡터_저장_테스트`() {
        // 텍스트 파일 테스트
        val textContent = """
            Spring AI 파일 업로드 테스트
            
            이 문서는 파일 업로드 기능을 테스트하기 위한 문서입니다.
            Spring AI의 Document Reader를 통해 파일 내용이 벡터 스토어에 저장됩니다.
            
            주요 기능:
            - 파일 타입 자동 인식
            - 문서 분할 및 청킹
            - 메타데이터 자동 생성
            - 벡터 임베딩 및 저장
        """.trimIndent()
        
        val mockFile = MockMultipartFile(
            "file",
            "test-upload.txt",
            "text/plain",
            textContent.toByteArray()
        )

        // 파일 업로드 및 처리
        val result = vectorStoreService.processAndStoreFile(mockFile, null)
        
        // 결과 검증
        Assertions.assertEquals("success", result["status"])
        Assertions.assertEquals("test-upload.txt", result["fileName"])
        Assertions.assertEquals(textContent.toByteArray().size.toLong(), result["fileSize"])
        Assertions.assertTrue((result["splitDocuments"] as Int) > 0, "분할된 문서가 없습니다")
        
        println("파일 업로드 결과:")
        println("- 파일명: ${result["fileName"]}")
        println("- 파일 크기: ${result["fileSize"]} bytes")
        println("- 원본 문서 수: ${result["originalDocuments"]}")
        println("- 분할된 문서 수: ${result["splitDocuments"]}")
        println("- 업로드 시간: ${result["uploadTime"]}")

        // 잠시 대기 (인덱싱 완료를 위해)
        Thread.sleep(2000)

        // 업로드된 파일 내용 검색 테스트
        val searchResults = vectorStoreService.searchSimilar("파일 업로드 테스트", topK = 3, threshold = 0.5)
        
        val foundUploadedContent = searchResults.any { doc ->
            doc.text!!.contains("Spring AI 파일 업로드 테스트") || 
            doc.metadata["filename"] == "test-upload.txt"
        }
        
        println("\n검색 결과:")
        searchResults.forEach { doc ->
            println("- ${doc.text?.take(100)}...")
            println("  메타데이터: ${doc.metadata}")
        }
        
        Assertions.assertTrue(foundUploadedContent, "업로드된 파일 내용을 검색할 수 없습니다")
        
        println("\n✅ 파일 업로드 및 벡터 저장 테스트 성공")
    }

    @Test
    @Order(9)
    fun `09_다양한_파일_타입_업로드_테스트`() {
        // PDF 파일 시뮬레이션 (실제로는 텍스트 내용)
        val pdfContent = "PDF 문서 내용 시뮬레이션 - Spring AI PDF Reader 테스트"
        val mockPdfFile = MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            pdfContent.toByteArray()
        )

        // 마크다운 파일
        val markdownContent = """
            # Markdown 테스트 문서
            
            ## Spring AI 기능
            - 문서 처리
            - 벡터 검색
            - RAG 구현
            
            **중요**: 이 문서는 마크다운 형식입니다.
        """.trimIndent()
        
        val mockMarkdownFile = MockMultipartFile(
            "file",
            "test.md",
            "text/markdown",
            markdownContent.toByteArray()
        )

        // PDF 파일 업로드 테스트 (실제로는 텍스트로 처리됨)
        val pdfResult = vectorStoreService.processAndStoreFile(mockPdfFile)
        Assertions.assertEquals("success", pdfResult["status"])
        println("PDF 파일 처리 결과: ${pdfResult["fileName"]}, 분할 문서 수: ${pdfResult["splitDocuments"]}")

        // 마크다운 파일 업로드 테스트
        val mdResult = vectorStoreService.processAndStoreFile(mockMarkdownFile)
        Assertions.assertEquals("success", mdResult["status"])
        println("마크다운 파일 처리 결과: ${mdResult["fileName"]}, 분할 문서 수: ${mdResult["splitDocuments"]}")

        // 잠시 대기
        Thread.sleep(1500)

        // 업로드된 파일들 검색
        val searchResults = vectorStoreService.searchSimilar("Spring AI", topK = 5, threshold = 0.3)
        
        val foundPdfContent = searchResults.any { doc -> doc.metadata["filename"] == "test.pdf" }
        val foundMarkdownContent = searchResults.any { doc -> doc.metadata["filename"] == "test.md" }
        
        println("\n다양한 파일 타입 검색 결과:")
        searchResults.forEach { doc ->
            println("- 파일: ${doc.metadata["filename"]}, 내용: ${doc.text?.take(50)}...")
        }

        Assertions.assertTrue(foundPdfContent, "PDF 파일 내용을 찾을 수 없습니다")
        Assertions.assertTrue(foundMarkdownContent, "마크다운 파일 내용을 찾을 수 없습니다")

        println("\n✅ 다양한 파일 타입 업로드 테스트 성공")
    }

    @Test
    @Order(10)
    fun `10_문서_버전_관리_테스트`() {
        val docId = "TEST-DOC-001"
        
        // 첫 번째 버전 업로드
        val version1Content = """
            문서 버전 관리 테스트 - 버전 1.0
            
            이 문서는 버전 관리 기능을 테스트하기 위한 첫 번째 버전입니다.
            초기 내용입니다.
        """.trimIndent()
        
        val mockFileV1 = MockMultipartFile(
            "file",
            "test-versioning-v1.txt",
            "text/plain",
            version1Content.toByteArray()
        )

        val additionalMetadata = mapOf(
            "version" to "1.0",
            "category" to "테스트"
        )

        // 첫 번째 버전 업로드
        val resultV1 = vectorStoreService.processAndStoreFileWithVersioning(mockFileV1, docId, additionalMetadata)
        
        Assertions.assertEquals("success", resultV1["status"])
        Assertions.assertEquals(docId, resultV1["docId"])
        println("첫 번째 버전 업로드 완료: ${resultV1["fileName"]}")

        // 잠시 대기
        Thread.sleep(2000)

        // 첫 번째 버전 검색 확인
        val searchV1 = vectorStoreService.searchWithMetadataFilter("버전 관리", "docId", docId, 10)
        Assertions.assertTrue(searchV1.isNotEmpty(), "첫 번째 버전을 찾을 수 없습니다")
        println("첫 번째 버전 검색 결과: ${searchV1.size}개 문서")

        // 두 번째 버전 업로드
        val version2Content = """
            문서 버전 관리 테스트 - 버전 2.0
            
            이 문서는 버전 관리 기능을 테스트하기 위한 두 번째 버전입니다.
            업데이트된 내용입니다.
            새로운 기능이 추가되었습니다.
        """.trimIndent()
        
        val mockFileV2 = MockMultipartFile(
            "file",
            "test-versioning-v2.txt",
            "text/plain",
            version2Content.toByteArray()
        )

        val additionalMetadataV2 = mapOf(
            "version" to "2.0",
            "category" to "테스트"
        )

        // 두 번째 버전 업로드 (기존 문서 교체)
        val resultV2 = vectorStoreService.processAndStoreFileWithVersioning(mockFileV2, docId, additionalMetadataV2)
        
        Assertions.assertEquals("success", resultV2["status"])
        Assertions.assertEquals(docId, resultV2["docId"])
        println("두 번째 버전 업로드 완료: ${resultV2["fileName"]}")

        // 잠시 대기
        Thread.sleep(2000)

        // 최종 검색 - 두 번째 버전만 존재해야 함
        val searchFinal = vectorStoreService.searchWithMetadataFilter("버전 관리", "docId", docId, 10)
        
        println("\n최종 검색 결과:")
        searchFinal.forEach { doc ->
            println("- 내용: ${doc.text?.take(50)}...")
            println("  메타데이터: ${doc.metadata}")
        }
        
        // 두 번째 버전 내용이 포함되어 있는지 확인
        val hasV2Content = searchFinal.any { doc ->
            doc.text?.contains("버전 2.0") == true || doc.text?.contains("업데이트된 내용") == true
        }
        
        // 첫 번째 버전 내용이 제거되었는지 확인
        val hasV1Content = searchFinal.any { doc ->
            doc.text?.contains("버전 1.0") == true && !doc.text?.contains("버전 2.0")!!
        }
        
        Assertions.assertTrue(hasV2Content, "두 번째 버전 내용을 찾을 수 없습니다")
        Assertions.assertFalse(hasV1Content, "첫 번째 버전 내용이 여전히 존재합니다")
        
        // 모든 결과의 docId가 동일한지 확인
        val allSameDocId = searchFinal.all { doc ->
            doc.metadata["docId"] == docId
        }
        Assertions.assertTrue(allSameDocId, "검색된 문서들의 docId가 일치하지 않습니다")

        println("\n✅ 문서 버전 관리 테스트 성공")
        println("- 기존 버전 삭제: 성공")
        println("- 새 버전 저장: 성공")
        println("- 버전 교체 검증: 성공")
    }
}