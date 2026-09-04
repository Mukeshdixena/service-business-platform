import { Navigate, Route, Routes } from 'react-router-dom'
import { RootLayout } from '../app/RootLayout'
import { BusinessProfileEditor } from '../features/business/BusinessProfileEditor'
import { DashboardHome } from '../features/business/DashboardHome'
import { DashboardLayout } from '../features/business/DashboardLayout'
import { HoursEditor } from '../features/business/HoursEditor'
import { MembershipPlansManager } from '../features/business/MembershipPlansManager'
import { ServicesManager } from '../features/business/ServicesManager'
import { StaffManager } from '../features/business/StaffManager'
import { BookingFlow } from '../features/booking/BookingFlow'
import { MyBookingsPage } from '../features/booking/MyBookingsPage'
import { BookingsManager } from '../features/dashboard/BookingsManager'
import { MembershipsManager } from '../features/dashboard/MembershipsManager'
import { QueueManager } from '../features/dashboard/QueueManager'
import { BusinessProfilePage } from '../features/discovery/BusinessProfilePage'
import { BusinessSearchPage } from '../features/discovery/BusinessSearchPage'
import { MyMembershipsPage } from '../features/membership/MyMembershipsPage'
import { MyQueuePage } from '../features/queue/MyQueuePage'
import { LoginPage } from './LoginPage'
import { NotFoundPage } from './NotFoundPage'
import { RegisterPage } from './RegisterPage'
import { RequireAuth } from './RequireAuth'
import { RequireBusinessAccess } from './RequireBusinessAccess'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<RootLayout />}>
        <Route path="/" element={<BusinessSearchPage />} />
        <Route path="/businesses/:slug" element={<BusinessProfilePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route element={<RequireAuth />}>
          <Route path="/book/:slug" element={<BookingFlow />} />
          <Route path="/my-bookings" element={<MyBookingsPage />} />
          <Route path="/my-queue" element={<MyQueuePage />} />
          <Route path="/my-memberships" element={<MyMembershipsPage />} />
          <Route path="/dashboard" element={<DashboardHome />} />

          <Route path="/dashboard/:businessId" element={<RequireBusinessAccess />}>
            <Route element={<DashboardLayout />}>
              <Route index element={<Navigate to="profile" replace />} />
              <Route path="profile" element={<BusinessProfileEditor />} />
              <Route path="services" element={<ServicesManager />} />
              <Route path="staff" element={<StaffManager />} />
              <Route path="hours" element={<HoursEditor />} />
              <Route path="bookings" element={<BookingsManager />} />
              <Route path="queue" element={<QueueManager />} />
              <Route path="membership-plans" element={<MembershipPlansManager />} />
              <Route path="memberships" element={<MembershipsManager />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
