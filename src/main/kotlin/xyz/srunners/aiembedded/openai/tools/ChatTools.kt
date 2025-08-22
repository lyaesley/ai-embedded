package xyz.srunners.aiembedded.openai.tools

import org.springframework.ai.tool.annotation.Tool
import xyz.srunners.aiembedded.openai.dto.UserResponseDTO

class ChatTools {

    @Tool(description = "User personal information : name, age, address, phone, etc")
    fun getUserInfoTool() = UserResponseDTO("DISP", 29, "123 Main St", "123-456-7890", "12345")
}