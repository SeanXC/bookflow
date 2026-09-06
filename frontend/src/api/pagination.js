export const DEFAULT_PAGE_SIZE = 20
export const MAX_PAGE_SIZE = 100

/**
 * @template T
 * @typedef {object} PageResponse
 * @property {T[]} content
 * @property {number} page
 * @property {number} size
 * @property {number} totalElements
 * @property {number} totalPages
 */

/**
 * Converts MUI DataGrid pagination and sorting state to Spring request params.
 *
 * @param {{
 *   page?: number,
 *   pageSize?: number,
 *   sortModel?: Array<{ field: string, sort?: 'asc' | 'desc' | null }>
 * }} [options]
 * @returns {{
 *   page: number,
 *   size: number,
 *   sort?: string[]
 * }}
 */
export function toPageParams(options = {}) {
  const page = Math.max(0, options.page ?? 0)
  const size = Math.min(
    MAX_PAGE_SIZE,
    Math.max(1, options.pageSize ?? DEFAULT_PAGE_SIZE),
  )
  const sort = (options.sortModel ?? [])
    .filter((item) => item.sort)
    .map((item) => `${item.field},${item.sort}`)

  return {
    page,
    size,
    ...(sort.length > 0 ? { sort } : {}),
  }
}
