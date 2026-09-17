import { EmptyState } from '../components/EmptyState'

/** W2 实现（invoices 列表+五维筛选）；W1 占位：空态双型之首录引导型 */
export default function InvoicesPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 py-8">
      <h1 className="text-[var(--text-xl)] font-semibold mb-6">我的票夹</h1>
      <EmptyState
        type="first-use"
        title="票夹还是空的"
        hint="录入第一张发票，2 分钟搞定"
      />
    </div>
  )
}
