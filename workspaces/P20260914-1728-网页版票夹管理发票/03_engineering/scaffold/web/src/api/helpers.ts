/**
 * 生成函数的返回类型是包络 ApiResponse<T>，而 customClient 运行时已解包返回 data 本体
 * （防线三注意项）。unwrap 做显式类型断言对齐，调用处以生成模型内层类型传参。
 */
export const unwrap = async <D>(p: Promise<unknown>): Promise<D> => (await p) as D
