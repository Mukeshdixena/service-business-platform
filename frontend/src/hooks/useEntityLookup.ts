import { useQueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'
import type { BusinessPublicDto, PageResponse } from '../types'

export interface EntityLabels {
  businessName?: string
  businessSlug?: string
  serviceName?: string
}

/**
 * BookingDto (per API_CONTRACT.md) only carries `businessId`/`serviceId` —
 * no embedded names. There is no documented endpoint a CUSTOMER can call to
 * resolve an arbitrary business/service name from an id (the owner-scoped
 * `/businesses/{id}` and `/businesses/{id}/services` endpoints require a
 * business membership the customer doesn't have).
 *
 * As a best-effort, non-authoritative convenience, this looks up names from
 * whatever discovery data react-query already has cached in this session
 * (e.g. the customer browsed or booked through that business's public
 * profile earlier). If nothing is cached, callers fall back to a short id.
 * This performs no extra network calls and invents no new API surface.
 */
export function useEntityLookup() {
  const queryClient = useQueryClient()

  return useCallback(
    (businessId: string, serviceId?: string): EntityLabels => {
      const detailEntries = queryClient.getQueriesData<BusinessPublicDto>({ queryKey: ['discovery', 'business'] })
      for (const [, data] of detailEntries) {
        if (data?.id === businessId) {
          return {
            businessName: data.name,
            businessSlug: data.slug,
            serviceName: serviceId ? data.services?.find((service) => service.id === serviceId)?.name : undefined,
          }
        }
      }

      const listEntries = queryClient.getQueriesData<PageResponse<BusinessPublicDto>>({
        queryKey: ['discovery', 'businesses'],
      })
      for (const [, page] of listEntries) {
        const match = page?.content.find((business) => business.id === businessId)
        if (match) return { businessName: match.name, businessSlug: match.slug }
      }

      return {}
    },
    [queryClient],
  )
}
