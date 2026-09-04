import { Link } from 'react-router-dom'
import { Button } from '../components/ui'

export function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-24 text-center">
      <p className="text-sm font-medium text-brand-600">404</p>
      <h1 className="text-2xl font-semibold text-slate-900">Page not found</h1>
      <p className="max-w-sm text-sm text-slate-500">The page you&apos;re looking for doesn&apos;t exist or has moved.</p>
      <Link to="/">
        <Button className="mt-2">Back to home</Button>
      </Link>
    </div>
  )
}
