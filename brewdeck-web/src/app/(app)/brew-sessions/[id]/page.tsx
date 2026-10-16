import { BrewSessionDetailView } from '@/components/brew-sessions/BrewSessionDetailView';

export default async function BrewSessionDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <BrewSessionDetailView sessionId={Number(id)} />;
}
