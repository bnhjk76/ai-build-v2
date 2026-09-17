package com.ticketwallet.common.web;

/** 脱敏工具（design-spec §6 脱敏矩阵）：账号标识与发票号码的默认掩码口径。 */
public final class MaskingUtils {

    private MaskingUtils() {}

    /** 邮箱 a***@x.com；手机号 138****5678。 */
    public static String maskAccount(String account) {
        if (account == null || account.isEmpty()) return "";
        int at = account.indexOf('@');
        if (at > 0) {
            String local = account.substring(0, at);
            String domain = account.substring(at);
            String head = local.substring(0, Math.min(1, local.length()));
            return head + "***" + domain;
        }
        if (account.length() >= 7) {
            return account.substring(0, 3) + "****" + account.substring(account.length() - 4);
        }
        return account.charAt(0) + "****";
    }

    /** 发票号码默认掩码 ****5678（详情/导出/show_sensitive=1 不脱敏）。 */
    public static String maskInvoiceNumber(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.length() < 4) return "****";
        return "****" + invoiceNumber.substring(invoiceNumber.length() - 4);
    }
}
