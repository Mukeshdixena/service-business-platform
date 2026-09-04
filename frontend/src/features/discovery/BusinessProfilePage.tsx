import { useMutation, useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { BusinessHeader } from '../../components/business/BusinessHeader'
import { HoursTable } from '../../components/business/HoursTable'
import { ServiceCard } from '../../components/business/ServiceCard'
import { StaffCard } from '../../components/business/StaffCard'
import { ClassCard } from '../../components/class/ClassCard'
import { MembershipPlanCard } from '../../components/membership/MembershipPlanCard'
import { ResourceCard } from '../../components/resource/ResourceCard'
import { Button, Card, EmptyState, ErrorState, LoadingSpinner } from '../../components/ui'
import { useAuth } from '../auth/AuthContext'
import { queryKeys } from '../../hooks/queryKeys'
import { useToast } from '../../hooks/useToast'
import { customerClassApi } from '../../services/api/classApi'
import { discoveryApi } from '../../services/api/discoveryApi'
import { customerMembershipApi } from '../../services/api/membershipApi'
import { customerQueueApi } from '../../services/api/queueApi'
import { getFriendlyErrorMessage } from '../../utils/apiError'

export function BusinessProfilePage() {
  const { slug } = useParams<{ slug: string }>()
  const { status: authStatus } = useAuth()
  const navigate = useNavigate()
  const { showToast } = useToast()

  const businessQuery = useQuery({
    queryKey: queryKeys.discoveryBusinessBySlug(slug ?? ''),
    queryFn: () => discoveryApi.getBusinessBySlug(slug ?? ''),
    enabled: Boolean(slug),
  })

  const business = businessQuery.data
  const hasQueueCapability = Boolean(business?.capabilities.includes('QUEUE'))
  const hasMembershipsCapability = Boolean(business?.capabilities.includes('MEMBERSHIPS'))
  const hasClassesCapability = Boolean(business?.capabilities.includes('CLASSES'))
  const hasRentalsCapability = Boolean(
    business?.capabilities.includes('RESOURCES') || business?.capabilities.includes('RENTALS'),
  )

  const joinQueueMutation = useMutation({
    mutationFn: (serviceId: string) => customerQueueApi.join(business?.id ?? '', { serviceId }),
    onSuccess: () => {
      showToast('You joined the queue.', 'success')
      navigate('/my-queue')
    },
    onError: (error: unknown) => showToast(getFriendlyErrorMessage(error), 'error'),
  })

  const enrollMutation = useMutation({
    mutationFn: (classId: string) => customerClassApi.enroll(business?.id ?? '', classId),
    onSuccess: (enrollment) => {
      showToast(
        enrollment.status === 'WAITLISTED' ? "You're waitlisted — this class is currently full." : 'Enrolled!',
        'success',
      )
      navigate('/my-classes')
    },
    onError: (error: unknown) => showToast(getFriendlyErrorMessage(error), 'error'),
  })

  const purchaseMembershipMutation = useMutation({
    mutationFn: (membershipPlanId: string) => customerMembershipApi.purchase(business?.id ?? '', { membershipPlanId }),
    onSuccess: () => {
      showToast('Membership purchased.', 'success')
      navigate('/my-memberships')
    },
    onError: (error: unknown) => showToast(getFriendlyErrorMessage(error), 'error'),
  })

  if (businessQuery.isLoading) {
    return <LoadingSpinner label="Loading business profile…" className="min-h-[50vh]" />
  }

  if (businessQuery.isError) {
    return <ErrorState error={businessQuery.error} onRetry={() => businessQuery.refetch()} />
  }

  if (!business) return null

  const activeServices = business.services?.filter((service) => service.status === 'ACTIVE') ?? []
  const activeStaff = business.staff?.filter((member) => member.status === 'ACTIVE') ?? []
  const canBook = activeServices.length > 0 && business.capabilities.includes('APPOINTMENTS')
  // The public discovery response only ever includes ACTIVE plans already
  // (API_CONTRACT.md), but filter defensively in case a business has the
  // capability enabled with no plans, or the field is omitted entirely.
  const activePlans = business.membershipPlans?.filter((plan) => plan.status === 'ACTIVE') ?? []
  const upcomingClasses = business.classes?.filter((classItem) => classItem.status === 'SCHEDULED') ?? []
  const availableResources = business.resources?.filter((resource) => resource.status !== 'UNAVAILABLE') ?? []

  function requireAuthThen(action: () => void) {
    if (authStatus !== 'authenticated') {
      navigate('/login')
      return
    }
    action()
  }

  return (
    <div className="flex flex-col gap-6">
      <BusinessHeader
        business={business}
        action={
          canBook ? (
            <Link to={`/book/${business.slug}`}>
              <Button>Book now</Button>
            </Link>
          ) : undefined
        }
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="flex flex-col gap-4 lg:col-span-2">
          <Card title="Services">
            {activeServices.length === 0 ? (
              <EmptyState title="No services listed yet" description="This business hasn't added any bookable services." />
            ) : (
              <div className="flex flex-col gap-3">
                {activeServices.map((service) => {
                  const isQueueEligible =
                    hasQueueCapability && (service.bookingType === 'QUEUE' || service.bookingType === 'WALK_IN')
                  const isRentalEligible =
                    hasRentalsCapability && service.bookingType === 'RENTAL' && availableResources.length > 0
                  return (
                    <ServiceCard
                      key={service.id}
                      service={service}
                      action={
                        service.bookingType === 'APPOINTMENT' && business.capabilities.includes('APPOINTMENTS') ? (
                          <Link to={`/book/${business.slug}?serviceId=${service.id}`}>
                            <Button variant="secondary" size="sm">
                              Book
                            </Button>
                          </Link>
                        ) : isQueueEligible ? (
                          <Button
                            variant="secondary"
                            size="sm"
                            isLoading={joinQueueMutation.isPending && joinQueueMutation.variables === service.id}
                            onClick={() => requireAuthThen(() => joinQueueMutation.mutate(service.id))}
                          >
                            Join queue
                          </Button>
                        ) : isRentalEligible ? (
                          <Link to={`/rent/${business.slug}?serviceId=${service.id}`}>
                            <Button variant="secondary" size="sm">
                              Rent
                            </Button>
                          </Link>
                        ) : undefined
                      }
                    />
                  )
                })}
              </div>
            )}
          </Card>

          <Card title="Staff">
            {activeStaff.length === 0 ? (
              <EmptyState title="No staff listed" description="This business hasn't added staff profiles yet." />
            ) : (
              <div className="flex flex-col gap-3">
                {activeStaff.map((member) => (
                  <StaffCard key={member.id} staff={member} />
                ))}
              </div>
            )}
          </Card>

          {hasMembershipsCapability && (
            <Card title="Membership plans">
              {activePlans.length === 0 ? (
                <EmptyState title="No membership plans yet" description="This business hasn't published any membership plans." />
              ) : (
                <div className="flex flex-col gap-3">
                  {activePlans.map((plan) => (
                    <MembershipPlanCard
                      key={plan.id}
                      plan={plan}
                      action={
                        <Button
                          variant="secondary"
                          size="sm"
                          isLoading={purchaseMembershipMutation.isPending && purchaseMembershipMutation.variables === plan.id}
                          onClick={() => requireAuthThen(() => purchaseMembershipMutation.mutate(plan.id))}
                        >
                          Buy
                        </Button>
                      }
                    />
                  ))}
                </div>
              )}
            </Card>
          )}

          {hasClassesCapability && (
            <Card title="Classes">
              {upcomingClasses.length === 0 ? (
                <EmptyState title="No upcoming classes" description="This business hasn't scheduled any classes yet." />
              ) : (
                <div className="flex flex-col gap-3">
                  {upcomingClasses.map((classItem) => (
                    <ClassCard
                      key={classItem.id}
                      classItem={classItem}
                      action={
                        <Button
                          variant="secondary"
                          size="sm"
                          isLoading={enrollMutation.isPending && enrollMutation.variables === classItem.id}
                          onClick={() => requireAuthThen(() => enrollMutation.mutate(classItem.id))}
                        >
                          {classItem.enrolledCount >= classItem.capacity ? 'Join waitlist' : 'Enroll'}
                        </Button>
                      }
                    />
                  ))}
                </div>
              )}
            </Card>
          )}

          {hasRentalsCapability && (
            <Card title="Resources for rent">
              {availableResources.length === 0 ? (
                <EmptyState title="No resources listed" description="This business hasn't listed any rentable resources yet." />
              ) : (
                <div className="flex flex-col gap-3">
                  {availableResources.map((resource) => (
                    <ResourceCard key={resource.id} resource={resource} />
                  ))}
                </div>
              )}
            </Card>
          )}
        </div>

        <div className="flex flex-col gap-4">
          <Card title="Hours">
            <HoursTable hours={business.hours ?? []} />
          </Card>
          <Card title="Contact">
            <dl className="flex flex-col gap-2 text-sm">
              {business.phone && (
                <div className="flex justify-between gap-2">
                  <dt className="text-slate-500">Phone</dt>
                  <dd className="text-slate-700">{business.phone}</dd>
                </div>
              )}
              {business.email && (
                <div className="flex justify-between gap-2">
                  <dt className="text-slate-500">Email</dt>
                  <dd className="text-slate-700">{business.email}</dd>
                </div>
              )}
              {business.locations?.map((location) => (
                <div key={location.id} className="flex justify-between gap-2">
                  <dt className="text-slate-500">{location.label}</dt>
                  <dd className="text-right text-slate-700">
                    {location.addressLine1}, {location.city}
                  </dd>
                </div>
              ))}
              {!business.phone && !business.email && !business.locations?.length && (
                <p className="text-slate-500">No contact details published.</p>
              )}
            </dl>
          </Card>
        </div>
      </div>
    </div>
  )
}
