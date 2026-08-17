import type { Page } from '../types/common'

export async function fetchAllPages<T>(
  fetchPage: (page: number, size: number) => Promise<Page<T>>,
  size = 50,
): Promise<T[]> {
  const first = await fetchPage(0, size)
  const content = [...first.content]

  for (let page = 1; page < first.totalPages; page += 1) {
    const next = await fetchPage(page, size)
    content.push(...next.content)
  }

  return content
}
