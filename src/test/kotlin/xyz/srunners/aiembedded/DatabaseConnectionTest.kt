package xyz.srunners.aiembedded

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import javax.sql.DataSource

@SpringBootTest
@TestPropertySource(properties = [
    "logging.level.org.springframework.jdbc=DEBUG"
])
class DatabaseConnectionTest {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `데이터베이스 연결 테스트`() {
        // 데이터소스 연결 확인
        dataSource.connection.use { connection ->
            println("데이터베이스 연결 성공: ${connection.metaData.url}")
            println("데이터베이스 제품: ${connection.metaData.databaseProductName}")
            println("드라이버 버전: ${connection.metaData.driverVersion}")
        }
    }

    @Test
    fun `PGvector 확장 확인 테스트`() {
        val result = jdbcTemplate.queryForList(
            "SELECT extname, extversion FROM pg_extension WHERE extname = 'vector'"
        )

        if (result.isNotEmpty()) {
            println("PGvector 확장이 설치되어 있습니다:")
            result.forEach { row ->
                println("확장명: ${row["extname"]}, 버전: ${row["extversion"]}")
            }
        } else {
            println("PGvector 확장이 설치되어 있지 않습니다.")
        }
    }

    @Test
    fun `vector_store 테이블 확인 테스트`() {
        val tableExists = jdbcTemplate.queryForObject(
            """
            SELECT EXISTS (
                SELECT FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name = 'vector_store'
            )
            """.trimIndent(),
            Boolean::class.java
        )

        if (tableExists == true) {
            println("vector_store 테이블이 존재합니다.")

            // 테이블 구조 확인
            val columns = jdbcTemplate.queryForList(
                """
                SELECT column_name, data_type, is_nullable
                FROM information_schema.columns
                WHERE table_name = 'vector_store'
                ORDER BY ordinal_position
                """.trimIndent()
            )

            println("테이블 구조:")
            columns.forEach { column ->
                println("- ${column["column_name"]}: ${column["data_type"]} (nullable: ${column["is_nullable"]})")
            }
        } else {
            println("vector_store 테이블이 존재하지 않습니다.")
        }
    }
}