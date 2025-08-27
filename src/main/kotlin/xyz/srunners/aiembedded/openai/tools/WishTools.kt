package xyz.srunners.aiembedded.openai.tools

import org.springframework.ai.tool.annotation.Tool
import xyz.srunners.aiembedded.entity.WishEntity
import xyz.srunners.aiembedded.repository.WishRepository


class WishTools(
    private val wishRepository: WishRepository
) {

    @Tool(description = """
        사용자가 찜 정보 조회 문의 했을때 참고
        List<WishEntity> 에서 dispMallNo, wishlistDetailNo1(상품번호) 컬럼만 사용자에게 제공
    """)
    fun getWishByUserId(userId: String): List<WishEntity> {
        val entities = wishRepository.findByRegpsnIdOrderByRegTsDesc(userId)
        return entities.take(5);
    }
}