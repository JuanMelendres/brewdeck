import { PublicRecipeView } from '@/components/recipes/PublicRecipeView';

export default async function SharedRecipePage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = await params;
  return <PublicRecipeView token={token} />;
}
