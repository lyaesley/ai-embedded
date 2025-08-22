package xyz.srunners.aiembedded.openai.dto

data class UserResponseDTO(
    val name: String,
    val age: Long,
    val address: String,
    val phoneNumber: String,
    val zipCode: String
) {
}