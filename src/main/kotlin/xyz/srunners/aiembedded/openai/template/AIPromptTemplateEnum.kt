package xyz.srunners.aiembedded.openai.template

import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate

/**
 * PromptTemplate 객체를 제공하는 enum 클래스
 * Spring AI의 PromptTemplate을 활용합니다.
 */
enum class AIPromptTemplate(
    val templateName: String,
    val description: String,
    val requiredParameters: List<String>
) {

    /**
     * 텍스트 요약용 PromptTemplate
     */
    TEXT_SUMMARY(
        templateName = "텍스트 요약",
        description = "긴 텍스트를 지정된 길이로 요약하는 템플릿",
        requiredParameters = listOf("text", "maxLength")
    ) {
        override fun createPromptTemplate(): PromptTemplate {
            return PromptTemplate("""
                다음 텍스트의 핵심 내용을 {maxLength}자 이내로 요약해주세요.
                
                요약 시 다음 사항을 고려해주세요:
                - 가장 중요한 정보와 핵심 메시지를 포함
                - 불필요한 세부사항은 제외
                - 명확하고 간결한 문체로 작성
                - 원문의 의미와 맥락을 유지
                
                원본 텍스트:
                {text}
                
                요약 결과:
            """.trimIndent())
        }
    },

    /**
     * 질의응답용 PromptTemplate
     */
    QUESTION_ANSWER(
        templateName = "질의응답",
        description = "컨텍스트 기반 질의응답을 위한 템플릿",
        requiredParameters = listOf("context", "question")
    ) {
        override fun createPromptTemplate(): PromptTemplate {
            return PromptTemplate("""
                👋 안녕하세요! 저는 우리 회사의 **전담 정보 도우미**입니다.

                    **🔍 귀하의 질문**
                    > {query}
                    
                    **📋 검색된 회사 자료**
                    --------------------------
                    {question_answer_context}
                    --------------------------
                                        
                    **💡 답변 가이드라인**
                    - ✨ **신뢰성**: 제공된 회사 자료만을 근거로 답변
                    - 🎯 **정확성**: 불확실한 내용은 추측하지 않음  
                    - 🤝 **친절함**: 이해하기 쉽고 도움이 되는 설명
                    - 📞 **연결성**: 필요시 적절한 담당자/부서 안내
                    
                    ** 답변 형식은은 마크다운 형식으로 작성**
                    
                    **📄 답변 결과**
                    
                    답변이 가능한 경우
                                        
                    📝 **상세 설명**:
                    [구조화된 답변 내용]
                    
                    질문에 [출처: ...] 해당 부분을 출처에 참고해. 
                    답변 내에 출처를 언급하지 말고 마지막에 따로 분리해서 표기.
                    
                    ---
                    
                    답변이 불가능한 경우
                    ❗ **안내사항**: 
                    죄송합니다. 현재 제공된 회사 자료에서는 정확한 답변을 찾을 수 없습니다.
                    
                    🔄 **제안드리는 방법**:
                    - 질문을 더 구체적으로 다시 작성해 주세요
                    - 또는 관련 부서(개발팀, 기획팀 등)에 직접 문의해 주세요
                    
                    궁금한 점이 더 있으시면 언제든 말씀해 주세요! 😊
            """.trimIndent())
        }
    },

    /**
     * 대화용 PromptTemplate
     */
    CONVERSATION(
        templateName = "대화",
        description = "이전 대화 맥락을 고려한 자연스러운 대화를 위한 템플릿",
        requiredParameters = listOf("conversationHistory", "userMessage")
    ) {
        override fun createPromptTemplate(): PromptTemplate {
            return PromptTemplate("""
                당신은 친근하고 도움이 되는 AI 어시스턴트입니다.
                이전 대화 내용을 참고하여 사용자와 자연스럽고 일관성 있는 대화를 이어가세요.
                
                대화 시 다음 사항을 유지해주세요:
                - 이전 대화의 맥락과 톤을 고려
                - 사용자의 감정이나 상황에 공감적으로 반응
                - 구체적이고 도움이 되는 응답 제공
                - 필요시 추가 질문으로 더 나은 도움 제안
                
                이전 대화 기록:
                {conversationHistory}
                
                사용자의 새로운 메시지: {userMessage}
                
                응답:
            """.trimIndent())
        }
    };

    /**
     * 기본 PromptTemplate 생성 (추상 메소드)
     */
    abstract fun createPromptTemplate(): PromptTemplate

    /**
     * 파라미터를 사용해 Prompt 생성
     */
    fun createPrompt(parameters: Map<String, Any>): Prompt {
        validateParameters(parameters)
        return createPromptTemplate().create(parameters)
    }

    /**
     * 템플릿에 필요한 모든 파라미터가 제공되었는지 검증합니다.
     */
    fun validateParameters(parameters: Map<String, Any>) {
        val missingParams = getMissingParameters(parameters)
        if (missingParams.isNotEmpty()) {
            throw IllegalArgumentException(
                "다음 필수 파라미터가 누락되었습니다: ${missingParams.joinToString(", ")}"
            )
        }
    }

    /**
     * 누락된 파라미터 목록을 반환합니다.
     */
    fun getMissingParameters(parameters: Map<String, Any>): List<String> {
        return requiredParameters.filter { paramName ->
            !parameters.containsKey(paramName) || parameters[paramName] == null
        }
    }
}