package xyz.srunners.aiembedded.entity

import jakarta.persistence.*
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor
import java.time.LocalDateTime

@Entity
@Table(
    name = "mb_wishlist_p",
    uniqueConstraints = [
        UniqueConstraint(
            name = "mb_wishlist_p_unique",
            columnNames = ["mbr_no", "disp_mall_no", "wishlist_detail_no1"]
        )
    ]
)
@Data
@NoArgsConstructor
@AllArgsConstructor
data class WishEntity(
    
    @Id
    @Column(name = "wishlist_no", length = 15, nullable = false)
    val wishlistNo: String,
    
    @Column(name = "disp_mall_no", length = 15)
    val dispMallNo: String? = null,
    
    @Column(name = "mbr_no", length = 15)
    val mbrNo: String? = null,
    
    @Column(name = "occur_ts")
    val occurTs: LocalDateTime? = null,
    
    @Column(name = "wishlist_dcode", length = 6)
    val wishlistDcode: String? = null,
    
    @Column(name = "wishlist_detail_no1", length = 15)
    val wishlistDetailNo1: String? = null,
    
    @Column(name = "wishlist_detail_no2", length = 15)
    val wishlistDetailNo2: String? = null,
    
    @Column(name = "wishlist_detail_no3", length = 15)
    val wishlistDetailNo3: String? = null,
    
    @Column(name = "searchword", length = 200)
    val searchword: String? = null,
    
    @Column(name = "srch_param", columnDefinition = "TEXT")
    val srchParam: String? = null,
    
    @Column(name = "sale_shop_dcode", length = 6)
    val saleShopDcode: String? = null,
    
    @Column(name = "sale_shop_no", length = 15)
    val saleShopNo: String? = null,
    
    @Column(name = "sale_region_no", length = 15)
    val saleRegionNo: String? = null,
    
    @Column(name = "contents_identify_no", length = 15)
    val contentsIdentifyNo: String? = null,
    
    @Column(name = "wishlist_category_no", length = 15)
    val wishlistCategoryNo: String? = null,
    
    @Column(name = "reg_ts", nullable = false)
    val regTs: LocalDateTime,
    
    @Column(name = "regpsn_id", length = 50, nullable = false)
    val regpsnId: String,
    
    @Column(name = "mod_ts", nullable = false)
    val modTs: LocalDateTime,
    
    @Column(name = "modpsn_id", length = 50, nullable = false)
    val modpsnId: String
)
