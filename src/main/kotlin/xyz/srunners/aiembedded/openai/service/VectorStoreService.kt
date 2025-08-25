package xyz.srunners.aiembedded.openai.service

import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.reader.ExtractedTextFormatter
import org.springframework.ai.reader.pdf.PagePdfDocumentReader
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig
import org.springframework.ai.reader.markdown.MarkdownDocumentReader
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig
import org.springframework.ai.reader.TextReader
import org.springframework.ai.reader.tika.TikaDocumentReader
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

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

    /**
     * 파일 업로드 및 벡터 스토어 저장 (문서 버전 관리)
     */
    fun processAndStoreFileWithVersioning(file: MultipartFile, docId: String, additionalMetadata: Map<String, Any>? = null): Map<String, Any> {
        val fileName = file.originalFilename ?: "unknown"
        val fileExtension = fileName.substringAfterLast('.', "").lowercase()
        val uploadTime = LocalDateTime.now().toString()
        
        try {
            // 버전 정보 생성 (추가 메타데이터에서 버전 정보 추출 또는 기본값 설정)
            val version = additionalMetadata?.get("version")?.toString() ?: "1.0"
            val currentTime = System.currentTimeMillis()
            
            // 기본 메타데이터 생성 (docId 포함)
            val baseMetadata = mutableMapOf<String, Any>(
                "docId" to docId,
                "version" to version,
                "filename" to fileName,
                "fileSize" to file.size,
                "contentType" to (file.contentType ?: "unknown"),
                "uploadTime" to uploadTime,
                "lastUpdated" to currentTime
            )
            
            // 추가 메타데이터가 있으면 포함
            additionalMetadata?.let {
                baseMetadata.putAll(it)
            }

            // 기존 버전 삭제 (동일한 docId를 가진 모든 문서들)
            try {
                println("문서 ID '$docId'의 기존 버전들을 삭제합니다...")
                
                // 필터 표현식을 사용하여 기존 문서 삭제
                val filterExpression = "docId == '$docId'"
                vectorStore.delete(filterExpression)
                
                println("기존 버전 삭제 완료: docId = $docId")
            } catch (e: Exception) {
                println("기존 문서 삭제 중 오류: ${e.message}")
                // 삭제가 실패해도 새 버전은 저장하도록 계속 진행
                println("삭제 실패했지만 새 버전 저장을 계속 진행합니다.")
            }

            // ETL 파이프라인 패턴에 따른 DocumentReader 사용
            val documents = when (fileExtension) {
                "pdf" -> {
                    val resource = InputStreamResource(file.inputStream)
                    val pdfReader = PagePdfDocumentReader(
                        resource,
                        PdfDocumentReaderConfig.builder()
                            .withPageTopMargin(0)
                            .withPageExtractedTextFormatter(
                                ExtractedTextFormatter.builder()
                                    .withNumberOfTopTextLinesToDelete(0)
                                    .build()
                            )
                            .withPagesPerDocument(1)
                            .build()
                    )
                    pdfReader.read()
                }
                "md", "markdown" -> {
                    val resource = InputStreamResource(file.inputStream)
                    val config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(true)
                        .withIncludeBlockquote(true)
                        .withAdditionalMetadata("filename", fileName)
                        .build()
                    val markdownReader = MarkdownDocumentReader(resource, config)
                    markdownReader.get()
                }
                "txt", "text" -> {
                    val resource = InputStreamResource(file.inputStream)
                    val textReader = TextReader(resource)
                    // TextReader는 getCustomMetadata() 메소드 사용
                    textReader.customMetadata.putAll(baseMetadata)
                    textReader.get()
                }
                "doc", "docx", "ppt", "pptx", "xls", "xlsx", "rtf", "odt", "ods", "odp" -> {
                    val resource = InputStreamResource(file.inputStream)
                    val tikaReader = TikaDocumentReader(resource)
                    tikaReader.get()
                }
                else -> {
                    // 기타 파일의 경우 TikaDocumentReader로 처리
                    val resource = InputStreamResource(file.inputStream)
                    val tikaReader = TikaDocumentReader(resource)
                    tikaReader.get()
                }
            }

            // 메타데이터를 각 문서에 추가 (null-safe 처리)
            val documentsWithMetadata = documents.map { document ->
                val cleanMetadata = document.metadata.filterValues { it != null }.toMutableMap()
                cleanMetadata.putAll(baseMetadata)
                Document(document.text ?: "", cleanMetadata)
            }
            
            // ETL 파이프라인 패턴 적용: DocumentTransformer + DocumentWriter
            val textSplitter = TokenTextSplitter()
            
            // ETL 패턴: Transform -> Write
            val splitDocuments = textSplitter.apply(documentsWithMetadata)
            vectorStore.accept(splitDocuments)

            println("새 버전 저장 완료: $fileName (docId: $docId, version: $version)")
            println("원본 문서 수: ${documentsWithMetadata.size}")
            println("분할된 문서 수: ${splitDocuments.size}")

            return mapOf(
                "status" to "success",
                "docId" to docId,
                "version" to version,
                "fileName" to fileName,
                "fileSize" to file.size,
                "originalDocuments" to documentsWithMetadata.size,
                "splitDocuments" to splitDocuments.size,
                "uploadTime" to uploadTime,
                "message" to "문서 버전이 성공적으로 업데이트되었습니다."
            )
        } catch (e: Exception) {
            println("파일 처리 중 오류 발생: ${e.message}")
            return mapOf(
                "status" to "error",
                "docId" to docId,
                "fileName" to fileName,
                "message" to "파일 처리 실패: ${e.message}"
            )
        }
    }

    /**
     * 파일 업로드 및 벡터 스토어 저장 (기존 메소드 유지)
     */
    fun processAndStoreFile(file: MultipartFile, additionalMetadata: Map<String, Any>? = null): Map<String, Any> {
        val fileName = file.originalFilename ?: "unknown"
        val fileExtension = fileName.substringAfterLast('.', "").lowercase()
        val uploadTime = LocalDateTime.now().toString()
        
        try {
            // 기본 메타데이터 생성
            val baseMetadata = mutableMapOf<String, Any>(
                "filename" to fileName,
                "fileSize" to file.size,
                "contentType" to (file.contentType ?: "unknown"),
                "uploadTime" to uploadTime
            )
            
            // 추가 메타데이터가 있으면 포함
            additionalMetadata?.let {
                baseMetadata.putAll(it)
            }

            // ETL 파이프라인 패턴에 따른 DocumentReader 사용
            val documents = when (fileExtension) {
                "pdf" -> {
                    val resource = InputStreamResource(file.inputStream)
                    val pdfReader = PagePdfDocumentReader(
                        resource,
                        PdfDocumentReaderConfig.builder()
                            .withPageTopMargin(0)
                            .withPageExtractedTextFormatter(
                                ExtractedTextFormatter.builder()
                                    .withNumberOfTopTextLinesToDelete(0)
                                    .build()
                            )
                            .withPagesPerDocument(1)
                            .build()
                    )
                    pdfReader.read()
                }
                "md", "markdown" -> {
                    val resource = InputStreamResource(file.inputStream)
                    val config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(true)
                        .withIncludeBlockquote(true)
                        .withAdditionalMetadata("filename", fileName)
                        .build()
                    val markdownReader = MarkdownDocumentReader(resource, config)
                    markdownReader.get()
                }
                "txt", "text" -> {
                    val resource = InputStreamResource(file.inputStream)
                    val textReader = TextReader(resource)
                    // TextReader는 getCustomMetadata() 메소드 사용
                    textReader.customMetadata.putAll(baseMetadata)
                    textReader.get()
                }
                "doc", "docx", "ppt", "pptx", "xls", "xlsx", "rtf", "odt", "ods", "odp" -> {
                    val resource = InputStreamResource(file.inputStream)
                    val tikaReader = TikaDocumentReader(resource)
                    tikaReader.get()
                }
                else -> {
                    // 기타 파일의 경우 TikaDocumentReader로 처리
                    val resource = InputStreamResource(file.inputStream)
                    val tikaReader = TikaDocumentReader(resource)
                    tikaReader.get()
                }
            }

            // 메타데이터를 각 문서에 추가 (null-safe 처리)
            val documentsWithMetadata = documents.map { document ->
                val cleanMetadata = document.metadata.filterValues { it != null }.toMutableMap()
                cleanMetadata.putAll(baseMetadata)
                Document(document.text ?: "", cleanMetadata)
            }


            // ETL 파이프라인 패턴 적용: DocumentTransformer + DocumentWriter
            val textSplitter = TokenTextSplitter()

            // ETL 패턴: Transform -> Write
            // vectorStore.accept(tokenTextSplitter.apply(documents)) 패턴 적용
            val splitDocuments = textSplitter.apply(documentsWithMetadata)
            vectorStore.accept(splitDocuments)

            println("파일 처리 완료: $fileName")
            println("원본 문서 수: ${documents.size}")
            println("분할된 문서 수: ${splitDocuments.size}")

            return mapOf(
                "status" to "success",
                "fileName" to fileName,
                "fileSize" to file.size,
                "originalDocuments" to documents.size,
                "splitDocuments" to splitDocuments.size,
                "uploadTime" to uploadTime
            )
        } catch (e: Exception) {
            println("파일 처리 중 오류 발생: ${e.message}")
            throw RuntimeException("파일 처리 실패: ${e.message}", e)
        }
    }
}