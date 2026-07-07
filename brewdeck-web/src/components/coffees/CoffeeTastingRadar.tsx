'use client';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import {
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
} from 'recharts';
import { EmptyState } from '@/components/ui/EmptyState';

type CoffeeTastingRadarProps = {
  acidity: number | null;
  body: number | null;
  sweetness: number | null;
  bitterness: number | null;
};

export function CoffeeTastingRadar({
  acidity,
  body,
  sweetness,
  bitterness,
}: CoffeeTastingRadarProps) {
  const complete =
    acidity !== null && body !== null && sweetness !== null && bitterness !== null;

  const data = [
    { axis: 'Acidity', score: acidity },
    { axis: 'Body', score: body },
    { axis: 'Sweetness', score: sweetness },
    { axis: 'Bitterness', score: bitterness },
  ];

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="h6" component="h2" gutterBottom>
          Tasting profile
        </Typography>
        {complete ? (
          <Box sx={{ display: 'flex', justifyContent: 'center' }}>
            <RadarChart
              width={300}
              height={260}
              data={data}
              margin={{ top: 8, right: 24, bottom: 8, left: 24 }}
            >
              <PolarGrid />
              <PolarAngleAxis dataKey="axis" fontSize={12} />
              <PolarRadiusAxis domain={[0, 5]} tickCount={6} fontSize={10} />
              <Radar dataKey="score" stroke="#1976d2" fill="#1976d2" fillOpacity={0.4} />
            </RadarChart>
          </Box>
        ) : (
          <EmptyState message="Add tasting scores to see the flavor profile." />
        )}
      </CardContent>
    </Card>
  );
}
