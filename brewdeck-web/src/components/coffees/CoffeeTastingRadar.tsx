'use client';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import { useTheme } from '@mui/material/styles';
import Typography from '@mui/material/Typography';
import {
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
} from 'recharts';
import { useTranslations } from 'next-intl';
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
  const t = useTranslations('coffees.radar');
  const tf = useTranslations('coffees.fields');
  const theme = useTheme();
  // CSS variable so the chart follows the active color scheme; plain palette value without one.
  const lineColor = (theme.vars ?? theme).palette.primary.main;
  const complete =
    acidity !== null && body !== null && sweetness !== null && bitterness !== null;

  const data = [
    { axis: tf('acidityScore'), score: acidity },
    { axis: tf('bodyScore'), score: body },
    { axis: tf('sweetnessScore'), score: sweetness },
    { axis: tf('bitternessScore'), score: bitterness },
  ];

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="h6" component="h2" gutterBottom>
          {t('title')}
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
              <Radar
                dataKey="score"
                stroke={lineColor}
                fill={lineColor}
                fillOpacity={0.4}
              />
            </RadarChart>
          </Box>
        ) : (
          <EmptyState message={t('empty')} />
        )}
      </CardContent>
    </Card>
  );
}
