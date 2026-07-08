import { CoffeeDetailView } from '@/components/coffees/CoffeeDetailView';

export default async function CoffeeDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <CoffeeDetailView coffeeId={Number(id)} />;
}
