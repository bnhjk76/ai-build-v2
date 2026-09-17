package com.ticketwallet.domain.invoice;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.ticketwallet.common.error.ApiException;
import com.ticketwallet.common.error.ErrorCode;
import com.ticketwallet.common.security.SecurityConfig.AppPrincipal;
import com.ticketwallet.common.web.MaskingUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 发票服务（api-design §4.2）：
 * 创建=11 字段校验+totalAmount 服务端重算（不信前端）；列表=五维筛选（AND 组合/同维 OR）+掩码；
 * 详情=loadOwned 谓词（越权一律 404 INV_001，architecture §4.3 纪律）。
 */
@Service
public class InvoiceService {

    /** 业务口径时区（api-design §2：自然月/年、≤今天固定东八区）。 */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern NUMBER = Pattern.compile("^\\d{8,20}$");
    private static final Pattern CODE = Pattern.compile("^[0-9A-Za-z]{1,20}$");
    private static final Pattern MONEY = Pattern.compile("^\\d{1,10}\\.\\d{2}$");
    private static final Pattern MONTH = Pattern.compile("^\\d{4}-\\d{2}$");

    private final InvoiceMapper mapper;

    public InvoiceService(InvoiceMapper mapper) {
        this.mapper = mapper;
    }

    public record ListQuery(String month, String title, String amountMin, String amountMax,
                            List<String> category, List<String> medium, List<String> status,
                            int page, int pageSize, boolean showSensitive) {}

    public record CreateInput(String invoiceCode, String invoiceNumber, String issuedDate, String title,
                              String amount, String taxAmount, InvoiceCategory category, InvoiceMedium medium,
                              InvoiceStatus status, String remark) {}

    @Transactional
    public Invoice create(AppPrincipal principal, CreateInput in) {
        BigDecimal amount = parseMoney(in.amount());
        BigDecimal taxAmount = parseMoney(in.taxAmount());
        if (amount == null || amount.signum() <= 0) {
            throw field("amount", "金额必须大于 0");
        }
        if (taxAmount == null || taxAmount.signum() < 0) {
            throw field("taxAmount", "税额不能为负数");
        }
        if (in.invoiceNumber() == null || !NUMBER.matcher(in.invoiceNumber()).matches()) {
            throw field("invoiceNumber", "发票号码需为 8–20 位数字");
        }
        if (in.invoiceCode() != null && !in.invoiceCode().isBlank()
                && !CODE.matcher(in.invoiceCode()).matches()) {
            throw field("invoiceCode", "发票代码需为 ≤20 位数字/字母");
        }
        if (in.title() == null || in.title().isBlank() || in.title().length() > 100) {
            throw field("title", "抬头需为 1–100 个字符");
        }
        if (in.remark() != null && in.remark().length() > 200) {
            throw field("remark", "备注不能超过 200 字");
        }
        LocalDate issuedDate;
        try {
            issuedDate = LocalDate.parse(in.issuedDate());
        } catch (Exception e) {
            throw field("issuedDate", "开票日期格式应为 YYYY-MM-DD");
        }
        if (issuedDate.isAfter(LocalDate.now(BIZ_ZONE))) {
            throw field("issuedDate", "开票日期不能晚于今天");
        }

        Invoice inv = new Invoice();
        inv.setId(java.util.UUID.randomUUID().toString());
        inv.setUserId(principal.userId());
        inv.setInvoiceCode(blankToNull(in.invoiceCode()));
        inv.setInvoiceNumber(in.invoiceNumber());
        inv.setIssuedDate(issuedDate);
        inv.setTitle(in.title().trim());
        inv.setAmount(amount);
        inv.setTaxAmount(taxAmount);
        inv.setTotalAmount(amount.add(taxAmount));   // 服务端重算（ER 要点 1）
        inv.setCategory(in.category());
        inv.setMedium(in.medium());
        inv.setStatus(in.status() == null ? InvoiceStatus.NORMAL : in.status());
        inv.setRemark(blankToNull(in.remark()));
        OffsetDateTime now = OffsetDateTime.now();
        inv.setCreatedAt(now);
        inv.setUpdatedAt(now);
        mapper.insert(inv);
        return inv;
    }

    /** 列表分页（F07/F08）：五维筛选 + 开票日期倒序。 */
    public Page<Invoice> page(AppPrincipal principal, ListQuery q) {
        QueryWrapper qw = buildFilterWrapper(principal, q);
        qw.orderBy("issued_date", false).orderBy("created_at", false);
        return mapper.paginate(q.page(), q.pageSize(), qw);
    }

    /** 五维筛选谓词组装（列表与导出共用，api-design §4.2：export query 同列表参数）。 */
    public QueryWrapper buildFilterWrapper(AppPrincipal principal, ListQuery q) {
        QueryWrapper qw = QueryWrapper.create()
                .where("user_id = ?", principal.userId())
                .and("deleted_at IS NULL");

        if (q.month() != null) {
            if (!MONTH.matcher(q.month()).matches()) {
                throw inv003("month", "月份格式应为 YYYY-MM");
            }
            YearMonth ym;
            try {
                ym = YearMonth.parse(q.month());
            } catch (Exception e) {
                throw inv003("month", "月份无效");
            }
            qw.and("issued_date >= ?", ym.atDay(1))
              .and("issued_date < ?", ym.plusMonths(1).atDay(1));
        }
        if (q.title() != null && !q.title().isBlank()) {
            // spike 结论：抬头检索走原生 ILIKE（Flex like 大小写敏感）
            qw.and("title ILIKE ?", "%" + q.title().trim() + "%");
        }
        BigDecimal min = null, max = null;
        if (q.amountMin() != null && !q.amountMin().isBlank()) min = parseMoney(q.amountMin());
        if (q.amountMax() != null && !q.amountMax().isBlank()) max = parseMoney(q.amountMax());
        if ((q.amountMin() != null && !q.amountMin().isBlank() && min == null)
                || (q.amountMax() != null && !q.amountMax().isBlank() && max == null)) {
            throw inv003("amountMin/amountMax", "金额需为两位小数数字");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw inv003("amountMin", "最小金额不能大于最大金额");
        }
        if (min != null) qw.and("total_amount >= ?", min);
        if (max != null) qw.and("total_amount <= ?", max);

        qw = appendIn(qw, "category", q.category());
        qw = appendIn(qw, "medium", q.medium());
        qw = appendIn(qw, "status", q.status());
        return qw;
    }

    /** 掩码（R8：默认 ****5678，show_sensitive=1 全号）。 */
    public String displayNumber(Invoice inv, boolean showSensitive) {
        return showSensitive ? inv.getInvoiceNumber() : MaskingUtils.maskInvoiceNumber(inv.getInvoiceNumber());
    }

    /** 详情：loadOwned 谓词——非本人/不存在一律 404（不暴露存在性）。 */
    public Invoice loadOwned(AppPrincipal principal, String id) {
        Invoice inv = mapper.selectOneByQuery(QueryWrapper.create()
                .where("id = ?", id)
                .and("user_id = ?", principal.userId())
                .and("deleted_at IS NULL"));
        if (inv == null) {
            throw new ApiException(ErrorCode.INV_001);
        }
        return inv;
    }

    /** 编辑（F05，M2）：部分字段合并，保留 createdAt、刷新 updatedAt，校验/重算同创建。 */
    @Transactional
    public Invoice patch(AppPrincipal principal, String id, CreateInput patch) {
        Invoice inv = loadOwned(principal, id);
        if (patch.invoiceNumber() != null) {
            if (!NUMBER.matcher(patch.invoiceNumber()).matches()) {
                throw field("invoiceNumber", "发票号码需为 8–20 位数字");
            }
            inv.setInvoiceNumber(patch.invoiceNumber());
        }
        if (patch.invoiceCode() != null) inv.setInvoiceCode(blankToNull(patch.invoiceCode()));
        if (patch.issuedDate() != null) {
            LocalDate d;
            try { d = LocalDate.parse(patch.issuedDate()); }
            catch (Exception e) { throw field("issuedDate", "开票日期格式应为 YYYY-MM-DD"); }
            if (d.isAfter(LocalDate.now(BIZ_ZONE))) throw field("issuedDate", "开票日期不能晚于今天");
            inv.setIssuedDate(d);
        }
        if (patch.title() != null) {
            if (patch.title().isBlank() || patch.title().length() > 100) {
                throw field("title", "抬头需为 1–100 个字符");
            }
            inv.setTitle(patch.title().trim());
        }
        if (patch.remark() != null) inv.setRemark(blankToNull(patch.remark()));
        if (patch.category() != null) inv.setCategory(patch.category());
        if (patch.medium() != null) inv.setMedium(patch.medium());
        if (patch.status() != null) inv.setStatus(patch.status());
        if (patch.amount() != null || patch.taxAmount() != null) {
            BigDecimal amount = patch.amount() != null ? parseMoney(patch.amount()) : inv.getAmount();
            BigDecimal tax = patch.taxAmount() != null ? parseMoney(patch.taxAmount()) : inv.getTaxAmount();
            if (amount == null || amount.signum() <= 0) throw field("amount", "金额必须大于 0");
            if (tax == null || tax.signum() < 0) throw field("taxAmount", "税额不能为负数");
            inv.setAmount(amount);
            inv.setTaxAmount(tax);
            inv.setTotalAmount(amount.add(tax));   // 服务端重算
        }
        inv.setUpdatedAt(OffsetDateTime.now());
        mapper.update(inv);
        return inv;
    }

    private QueryWrapper appendIn(QueryWrapper qw, String column, List<String> values) {
        if (values == null || values.isEmpty()) return qw;
        List<String> upper = values.stream().map(String::toUpperCase).toList();
        qw.and(String.format("%s IN (%s)", column,
                String.join(",", upper.stream().map(v -> "'" + v.replace("'", "''") + "'").toList())));
        return qw;
    }

    private BigDecimal parseMoney(String s) {
        if (s == null || !MONEY.matcher(s).matches()) return null;
        return new BigDecimal(s);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private ApiException field(String field, String message) {
        return new ApiException(ErrorCode.INV_002, ErrorCode.INV_002.message(),
                List.of(new ApiException.FieldError(field, message)));
    }

    private ApiException inv003(String field, String message) {
        return new ApiException(ErrorCode.INV_003, ErrorCode.INV_003.message(),
                List.of(new ApiException.FieldError(field, message)));
    }
}
