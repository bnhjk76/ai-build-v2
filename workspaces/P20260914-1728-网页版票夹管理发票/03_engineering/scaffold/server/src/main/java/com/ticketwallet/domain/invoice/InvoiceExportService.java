package com.ticketwallet.domain.invoice;

import com.ticketwallet.common.error.ApiException;
import com.ticketwallet.common.error.ErrorCode;
import com.ticketwallet.common.security.SecurityConfig.AppPrincipal;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CSV 导出（api-design §4.2 GET /invoices/export，F10/G3）：
 * 预检（>5000 → EXP_001，须在响应头前抛出）→ 流式写出，首字节 BOM，转义自实现。
 * 列序：开票日期/发票代码/发票号码/抬头/票种/介质/状态/金额/税额/价税合计/备注/录入时间；内容不脱敏。
 */
@Service
public class InvoiceExportService {

    private static final long EXPORT_LIMIT = 5000;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId BIZ = ZoneId.of("Asia/Shanghai");

    private final InvoiceMapper invoiceMapper;
    private final InvoiceService invoiceService;

    public InvoiceExportService(InvoiceMapper invoiceMapper, InvoiceService invoiceService) {
        this.invoiceMapper = invoiceMapper;
        this.invoiceService = invoiceService;
    }

    /** 预检并取数（过滤谓词与列表共用；>5000 抛 EXP_001——controller 须在写响应头前调用）。 */
    public List<Invoice> filteredForExport(AppPrincipal principal, InvoiceService.ListQuery q) {
        List<Invoice> rows = invoiceMapper.selectListByQuery(
                invoiceService.buildFilterWrapper(principal, q).orderBy("issued_date", false));
        if (rows.size() > EXPORT_LIMIT) {
            throw new ApiException(ErrorCode.EXP_001);
        }
        return rows;
    }

    public String filename() {
        return "票夹通导出_" + LocalDateTime.now(BIZ)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
    }

    public void writeCsv(OutputStream out, List<Invoice> rows) throws Exception {
        out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});   // BOM：Excel 打开不乱码
        out.write("开票日期,发票代码,发票号码,抬头,票种,介质,状态,金额,税额,价税合计,备注,录入时间\n"
                .getBytes(StandardCharsets.UTF_8));
        for (Invoice r : rows) {
            String line = String.join(",",
                    csv(r.getIssuedDate().toString()),
                    csv(r.getInvoiceCode() == null ? "" : r.getInvoiceCode()),
                    csv(r.getInvoiceNumber()),
                    csv(r.getTitle()),
                    csv(r.getCategory() == InvoiceCategory.SPECIAL ? "专票" : "普票"),
                    csv(r.getMedium() == InvoiceMedium.ELECTRONIC ? "电子" : "纸质"),
                    csv(r.getStatus() == InvoiceStatus.NORMAL ? "正常"
                            : r.getStatus() == InvoiceStatus.VOIDED ? "作废" : "红冲"),
                    csv(r.getAmount().toPlainString()),
                    csv(r.getTaxAmount().toPlainString()),
                    csv(r.getTotalAmount().toPlainString()),
                    csv(r.getRemark() == null ? "" : r.getRemark()),
                    csv(r.getCreatedAt() == null ? "" :
                            TS.format(r.getCreatedAt().atZoneSameInstant(BIZ).toLocalDateTime()))) + "\n";
            out.write(line.getBytes(StandardCharsets.UTF_8));
        }
        out.flush();
    }

    /** CSV 转义：含逗号/引号/换行时加引号并双写引号（tech-stack §3.8）。 */
    private String csv(String v) {
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }
}
