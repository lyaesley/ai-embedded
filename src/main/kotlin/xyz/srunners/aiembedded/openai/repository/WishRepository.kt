package xyz.srunners.aiembedded.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import xyz.srunners.aiembedded.entity.WishEntity
import java.time.LocalDateTime

@Repository
interface WishRepository : JpaRepository<WishEntity, String> {
    
    /**
     * 회원별 위시리스트 조회 (등록일시 내림차순)
     */
    fun findByMbrNoOrderByRegTsDesc(mbrNo: String): List<WishEntity>
    
    /**
     * 회원별 위시리스트 페이징 조회
     */
    fun findByMbrNo(mbrNo: String, pageable: Pageable): Page<WishEntity>
    
    /**
     * 회원 + 전시몰별 위시리스트 조회
     */
    fun findByMbrNoAndDispMallNoOrderByRegTsDesc(
        mbrNo: String, 
        dispMallNo: String
    ): List<WishEntity>
    
    /**
     * 위시리스트 구분코드별 조회
     */
    fun findByWishlistDcodeOrderByRegTsDesc(wishlistDcode: String): List<WishEntity>
    
    /**
     * 회원 + 위시리스트 구분코드별 조회
     */
    fun findByMbrNoAndWishlistDcodeOrderByRegTsDesc(
        mbrNo: String, 
        wishlistDcode: String
    ): List<WishEntity>
    
    /**
     * 특정 기간 내 등록된 위시리스트 조회
     */
    fun findByRegTsBetweenOrderByRegTsDesc(
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): List<WishEntity>
    
    /**
     * 회원 + 기간별 위시리스트 조회
     */
    fun findByMbrNoAndRegTsBetweenOrderByRegTsDesc(
        mbrNo: String,
        startDate: LocalDateTime, 
        endDate: LocalDateTime
    ): List<WishEntity>
    
    /**
     * 검색어로 위시리스트 조회
     */
    fun findBySearchwordContainingIgnoreCaseOrderByRegTsDesc(searchword: String): List<WishEntity>
    
    /**
     * 회원 + 검색어로 위시리스트 조회
     */
    fun findByMbrNoAndSearchwordContainingIgnoreCaseOrderByRegTsDesc(
        mbrNo: String, 
        searchword: String
    ): List<WishEntity>
    
    /**
     * 위시리스트 카테고리별 조회
     */
    fun findByWishlistCategoryNoOrderByRegTsDesc(wishlistCategoryNo: String): List<WishEntity>
    
    /**
     * 회원 + 카테고리별 위시리스트 조회
     */
    fun findByMbrNoAndWishlistCategoryNoOrderByRegTsDesc(
        mbrNo: String, 
        wishlistCategoryNo: String
    ): List<WishEntity>
    
    /**
     * 특정 상품에 대한 위시리스트 존재 여부 확인
     */
    fun existsByMbrNoAndDispMallNoAndWishlistDetailNo1(
        mbrNo: String, 
        dispMallNo: String, 
        wishlistDetailNo1: String
    ): Boolean
    
    /**
     * 회원의 위시리스트 개수 조회
     */
    fun countByMbrNo(mbrNo: String): Long
    
    /**
     * 회원 + 구분코드별 위시리스트 개수 조회
     */
    fun countByMbrNoAndWishlistDcode(mbrNo: String, wishlistDcode: String): Long
    
    /**
     * 인기 위시리스트 상품 조회 (동일한 상품을 위시리스트에 담은 횟수 기준)
     */
    @Query("""
        SELECT w.dispMallNo, w.wishlistDetailNo1, COUNT(*) as wishCount
        FROM WishEntity w
        WHERE w.dispMallNo IS NOT NULL AND w.wishlistDetailNo1 IS NOT NULL
        GROUP BY w.dispMallNo, w.wishlistDetailNo1
        ORDER BY COUNT(*) DESC
    """)
    fun findPopularWishlistItems(pageable: Pageable): List<Array<Any>>
    
    /**
     * 특정 기간 내 가장 많이 담긴 위시리스트 키워드
     */
    @Query("""
        SELECT w.searchword, COUNT(*) as searchCount
        FROM WishEntity w
        WHERE w.searchword IS NOT NULL 
        AND w.searchword != ''
        AND w.regTs BETWEEN :startDate AND :endDate
        GROUP BY w.searchword
        ORDER BY COUNT(*) DESC
    """)
    fun findPopularSearchwords(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): List<Array<Any>>
    
    /**
     * 회원의 최근 위시리스트 활동 조회
     */
    @Query("""
        SELECT w FROM WishEntity w
        WHERE w.mbrNo = :mbrNo
        AND w.occurTs >= :recentDate
        ORDER BY w.occurTs DESC
    """)
    fun findRecentWishlistActivity(
        @Param("mbrNo") mbrNo: String,
        @Param("recentDate") recentDate: LocalDateTime
    ): List<WishEntity>
    
    /**
     * 매장별 위시리스트 통계 조회
     */
    @Query("""
        SELECT w.saleShopNo, w.saleShopDcode, COUNT(*) as wishCount
        FROM WishEntity w
        WHERE w.saleShopNo IS NOT NULL
        GROUP BY w.saleShopNo, w.saleShopDcode
        ORDER BY COUNT(*) DESC
    """)
    fun findWishlistStatsByShop(pageable: Pageable): List<Array<Any>>
    
    /**
     * 등록자 ID로 위시리스트 조회 (등록일시 내림차순)
     */
    fun findByRegpsnIdOrderByRegTsDesc(regpsnId: String): List<WishEntity>

    /**
     * 등록자 ID로 위시리스트 페이징 조회
     */
    fun findByRegpsnId(regpsnId: String, pageable: Pageable): Page<WishEntity>

    /**
     * 등록자 ID + 회원번호로 위시리스트 조회
     */
    fun findByRegpsnIdAndMbrNoOrderByRegTsDesc(
        regpsnId: String,
        mbrNo: String
    ): List<WishEntity>

    /**
     * 등록자 ID + 전시몰번호로 위시리스트 조회
     */
    fun findByRegpsnIdAndDispMallNoOrderByRegTsDesc(
        regpsnId: String,
        dispMallNo: String
    ): List<WishEntity>

    /**
     * 등록자 ID + 기간별 위시리스트 조회
     */
    fun findByRegpsnIdAndRegTsBetweenOrderByRegTsDesc(
        regpsnId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<WishEntity>

    /**
     * 등록자 ID별 위시리스트 개수 조회
     */
    fun countByRegpsnId(regpsnId: String): Long

    /**
     * 등록자 ID + 위시리스트 구분코드별 개수 조회
     */
    fun countByRegpsnIdAndWishlistDcode(regpsnId: String, wishlistDcode: String): Long

    /**
     * 등록자별 위시리스트 통계 조회 (관리자용)
     */
    @Query("""
        SELECT w.regpsnId, COUNT(*) as wishCount, 
               MIN(w.regTs) as firstRegTs, MAX(w.regTs) as lastRegTs
        FROM WishEntity w
        WHERE w.regpsnId = :regpsnId
        GROUP BY w.regpsnId
    """)
    fun findWishlistStatsByRegpsnId(@Param("regpsnId") regpsnId: String): List<Array<Any>>

    /**
     * 특정 등록자가 특정 기간 동안 등록한 위시리스트 조회
     */
    @Query("""
        SELECT w FROM WishEntity w
        WHERE w.regpsnId = :regpsnId
        AND w.regTs BETWEEN :startDate AND :endDate
        ORDER BY w.regTs DESC
    """)
    fun findByRegpsnIdInPeriod(
        @Param("regpsnId") regpsnId: String,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<WishEntity>

// ... existing code ...
}
