package com.ticketwallet.domain.stats;

import com.mybatisflex.core.query.QueryWrapper;
import com.ticketwallet.common.security.SecurityConfig.AppPrincipal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * 汇总服务（api-design §4.4）——口径 A3 的唯一实现点（tech-stack：所有统计一律引用本模块）：
 *   有效金额 validAmount = Σ(正常 totalAmount) − Σ(红冲 totalAmount)（可为负）
 *   有效张数 validCount  = 正常 + 红冲
 *   作废 voidedCount    只计张数不计金额
 * 范围：东八区当前自然月/自然年，deleted_at IS NULL。
 */
@Service
public class StatsService {

    private static final ZoneId BIZ = ZoneId.of("Asia/Shanghai");

    public record PeriodStat(long totalCount, long validCount, String validAmount,
                             long voidedCount, long reversedCount) {}
    public record Distribution(String category, String medium, long count, String amount) {}
    public record Summary(PeriodStat month, PeriodStat year, List<Distribution> distribution) {}

    @Mapper
    public interface StatsMapper {
        @Select("""
                SELECT
                  COUNT(*) AS total,
                  COUNT(*) FILTER (WHERE status = 'NORMAL')  AS normal_cnt,
                  COUNT(*) FILTER (WHERE status = 'REVERSED') AS reversed_cnt,
                  COUNT(*) FILTER (WHERE status = 'VOIDED')   AS voided_cnt,
                  COALESCE(SUM(total_amount) FILTER (WHERE status = 'NORMAL'), 0)
                    - COALESCE(SUM(total_amount) FILTER (WHERE status = 'REVERSED'), 0) AS valid_amount
                FROM invoices
                WHERE user_id = #{userId} AND deleted_at IS NULL
                  AND issued_date >= #{from} AND issued_date < #{to}
                """)
        Map<String, Object> aggregate(String userId, LocalDate from, LocalDate to);

        @Select("""
                SELECT category, medium, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS amount
                FROM invoices
                WHERE user_id = #{userId} AND deleted_at IS NULL AND status != 'VOIDED'
                  AND issued_date >= #{from} AND issued_date < #{to}
                GROUP BY category, medium
                ORDER BY cnt DESC
                """)
        List<Map<String, Object>> distribution(String userId, LocalDate from, LocalDate to);
    }

    private final StatsMapper statsMapper;

    public StatsService(StatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    public Summary summary(AppPrincipal principal) {
        LocalDate today = LocalDate.now(BIZ);
        LocalDate yearStart = today.withDayOfYear(1);
        return new Summary(
                period(principal.userId(), YearMonth.from(today).atDay(1), today.plusDays(1)),
                period(principal.userId(), yearStart, today.plusDays(1)),
                dist(principal.userId(), yearStart, today.plusDays(1)));
    }

    private PeriodStat period(String userId, LocalDate from, LocalDate to) {
        Map<String, Object> r = statsMapper.aggregate(userId, from, to);
        long normal = num(r.get("normal_cnt"));
        long reversed = num(r.get("reversed_cnt"));
        return new PeriodStat(num(r.get("total")), normal + reversed,
                new BigDecimal(String.valueOf(r.get("valid_amount"))).toPlainString(),
                num(r.get("voided_cnt")), reversed);
    }

    private List<Distribution> dist(String userId, LocalDate from, LocalDate to) {
        return statsMapper.distribution(userId, from, to).stream()
                .map(r -> new Distribution(String.valueOf(r.get("category")).toLowerCase(),
                        String.valueOf(r.get("medium")).toLowerCase(),
                        num(r.get("cnt")),
                        new BigDecimal(String.valueOf(r.get("amount"))).toPlainString()))
                .toList();
    }

    private long num(Object v) {
        return v == null ? 0 : Long.parseLong(String.valueOf(v));
    }
}
